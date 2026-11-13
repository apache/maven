/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.internal.build.incremental.impl.maven.digest;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.build.incremental.IncrementalContextException;
import org.apache.maven.api.services.ArtifactManager;

/**
 * Computes a content-based SHA-1 digest of Maven plugin classpath dependencies.
 *
 * <p>Unlike simple timestamp comparison, this digester hashes the actual <strong>contents</strong>
 * of each dependency artifact (JAR entries sorted by name, or recursive file contents for
 * exploded directories). This makes it immune to file timestamp changes caused by rebuilding
 * the same sources — a common scenario with SNAPSHOT dependencies in a reactor build.</p>
 *
 * <p>Digests are cached per session via {@link SessionData} so that the same artifact is not
 * re-hashed for every mojo execution that depends on it. The cache key is the artifact's
 * GAV coordinate string.</p>
 *
 * <p>This digester is used by {@link MojoConfigurationDigester} to produce the
 * {@code mojo.classpath} entry in the build context configuration parameters. When the
 * combined classpath digest changes between builds, the build context escalates to a full
 * rebuild.</p>
 *
 * @since 4.1.0
 * @see MojoConfigurationDigester
 */
class ClasspathDigester {

    @SuppressWarnings("unchecked")
    private static final SessionData.Key<ConcurrentMap<String, byte[]>> CACHE_KEY =
            (SessionData.Key<ConcurrentMap<String, byte[]>>)
                    (SessionData.Key<?>) SessionData.key(ConcurrentMap.class, ClasspathDigester.class.getName());

    private final ConcurrentMap<String, byte[]> cache;
    private final ArtifactManager artifactManager;

    ClasspathDigester(Session session) {
        this.cache = getCache(session);
        this.artifactManager = session.getService(ArtifactManager.class);
    }

    private static ConcurrentMap<String, byte[]> getCache(Session session) {
        // this assumes that session data does not change during reactor build
        SessionData sessionData = session.getData();
        return sessionData.computeIfAbsent(CACHE_KEY, ConcurrentHashMap::new);
    }

    static void digest(MessageDigest digester, InputStream is) {
        try {
            byte[] buf = new byte[64 * 1024];
            int r;
            while ((r = is.read(buf)) > 0) {
                digester.update(buf, 0, r);
            }
        } catch (IOException e) {
            throw new IncrementalContextException(e);
        }
    }

    static void digestFile(MessageDigest digester, Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            digest(digester, is);
        } catch (IOException e) {
            throw new IncrementalContextException(e);
        }
    }

    static void digestZip(MessageDigest digester, Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            zip.stream()
                    // sort entries.
                    // order of jar/zip entries is not important but may change from one build to the next
                    .sorted(Comparator.comparing(ZipEntry::getName))
                    .forEachOrdered(entry -> {
                        try (InputStream is = zip.getInputStream(entry)) {
                            digest(digester, is);
                        } catch (IOException e) {
                            throw new IncrementalContextException(e);
                        }
                    });
        } catch (IOException e) {
            throw new IncrementalContextException(e);
        }
    }

    private static byte[] digestArtifactFile(Path file) {
        byte[] hash;
        if (Files.isRegularFile(file)) {
            hash = digestZipOrFile(file);
        } else if (Files.isDirectory(file)) {
            hash = digestDirectory(file);
        } else {
            // does not exist, use token empty array to avoid rechecking
            hash = new byte[0];
        }
        return hash;
    }

    private static byte[] digestZipOrFile(Path file) {
        MessageDigest d = SHA1Digester.newInstance();
        try {
            digestZip(d, file);
        } catch (IncrementalContextException e) {
            if (e.getCause() instanceof ZipException) {
                // not a valid ZIP/JAR, fall back to plain file digest
                d = SHA1Digester.newInstance();
                digestFile(d, file);
            } else {
                throw e;
            }
        }
        return d.digest();
    }

    private static byte[] digestDirectory(Path file) {
        MessageDigest d = SHA1Digester.newInstance();
        try (Stream<Path> s = Files.walk(file)) {
            s.filter(Files::isRegularFile).sorted().forEach(f -> digestFile(d, f));
        } catch (IOException e) {
            throw new IncrementalContextException(e);
        }
        return d.digest();
    }

    public Serializable digest(List<Artifact> artifacts) {
        MessageDigest digester = SHA1Digester.newInstance();
        for (Artifact artifact : artifacts) {
            String cacheKey = getArtifactKey(artifact);

            // Released (non-SNAPSHOT) artifacts have immutable content — their GAV uniquely
            // identifies the bytes. Use the GAV string as the digest directly, skipping all
            // I/O. Only SNAPSHOT artifacts need content-based hashing.
            if (!artifact.isSnapshot()) {
                digester.update(cacheKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                continue;
            }

            Optional<Path> pathOpt = artifactManager.getPath(artifact);
            if (pathOpt.isEmpty()) {
                // Skip artifacts whose path is not resolved (e.g. the root dependency node
                // in the plugin's dependency tree, or provided-scope API artifacts)
                continue;
            }
            Path file = pathOpt.get();
            byte[] cached = cache.get(cacheKey);
            if (cached == null) {
                byte[] hash = digestArtifactFile(file);
                cached = cache.putIfAbsent(cacheKey, hash);
                if (cached == null) {
                    cached = hash;
                }
            }
            digester.update(cached);
        }
        return new BytesHash(digester.digest());
    }

    private String getArtifactKey(Artifact artifact) {
        StringBuilder sb = new StringBuilder();
        sb.append(artifact.getGroupId());
        sb.append(':');
        sb.append(artifact.getArtifactId());
        sb.append(':');
        sb.append(artifact.getExtension());
        sb.append(':');
        sb.append(artifact.getVersion());
        if (artifact.getClassifier() != null) {
            sb.append(':');
            sb.append(artifact.getClassifier());
        }
        return sb.toString();
    }
}
