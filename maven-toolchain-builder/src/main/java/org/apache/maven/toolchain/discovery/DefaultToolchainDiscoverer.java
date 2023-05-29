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
package org.apache.maven.toolchain.discovery;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.internal.xml.XmlNodeImpl;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.apache.maven.toolchain.v4.MavenToolchainsXpp3Reader;
import org.apache.maven.toolchain.v4.MavenToolchainsXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ToolchainDiscoverer service
 */
@Named
@Singleton
public class DefaultToolchainDiscoverer implements ToolchainDiscoverer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultToolchainDiscoverer.class);

    private Map<Path, ToolchainModel> cache;
    private boolean cacheModified;

    @Override
    public PersistedToolchains discoverToolchains() {
        try {
            Set<String> jdks = JavaHomeFinder.suggestHomePaths(true);
            LOGGER.info("Found {} possible jdks: {}", jdks.size(), jdks);
            cacheModified = false;
            readCache();
            Path currentJdkHome = getCanonicalPath(Paths.get(System.getProperty("java.home")));
            List<ToolchainModel> tcs = jdks.parallelStream()
                    .map(s -> getToolchainModel(currentJdkHome, s))
                    .filter(Objects::nonNull)
                    .sorted(getToolchainModelComparator())
                    .collect(Collectors.toList());
            if (this.cacheModified) {
                writeCache();
            }
            PersistedToolchains ps = new PersistedToolchains();
            ps.setToolchains(tcs);
            return ps;
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.warn("Error discovering toolchains: " + e, e);
            } else {
                LOGGER.warn("Error discovering toolchains (enable debug level for more information): " + e);
            }
            return new PersistedToolchains();
        }
    }

    private void readCache() {
        cache = new ConcurrentHashMap<>();
        Path cacheFile =
                Paths.get(System.getProperty("user.home")).resolve(".m2").resolve("toolchains-cache.xml");
        try {
            if (Files.isRegularFile(cacheFile)) {
                try (Reader r = Files.newBufferedReader(cacheFile)) {
                    PersistedToolchains pt = new PersistedToolchains(new MavenToolchainsXpp3Reader().read(r, false));
                    cache = pt.getToolchains().stream()
                            .collect(Collectors.toConcurrentMap(this::getJdkHome, Function.identity()));
                }
            }
        } catch (IOException | XmlPullParserException e) {
            LOGGER.warn("Error reading toolchains cache: " + e);
        }
    }

    private void writeCache() {
        Path cacheFile = Paths.get(System.getProperty("user.home")).resolve(".m2/toolchains-cache.xml");
        try {
            Files.createDirectories(cacheFile.getParent());
            try (Writer w = Files.newBufferedWriter(cacheFile)) {
                PersistedToolchains pt = new PersistedToolchains();
                List<ToolchainModel> toolchains = new ArrayList<>();
                for (ToolchainModel tc : cache.values()) {
                    tc = new ToolchainModel(tc.getDelegate());
                    tc.getProvides().remove("current");
                    toolchains.add(tc);
                }
                pt.setToolchains(toolchains);
                new MavenToolchainsXpp3Writer().write(w, pt.getDelegate());
            }
        } catch (IOException e) {
            LOGGER.warn("Error writing toolchains cache: " + e);
        }
    }

    private Path getJdkHome(ToolchainModel toolchain) {
        Xpp3Dom dom = (Xpp3Dom) toolchain.getConfiguration();
        Xpp3Dom javahome = dom != null ? dom.getChild("jdkHome") : null;
        String jdk = javahome != null ? javahome.getValue() : null;
        return Paths.get(Objects.requireNonNull(jdk));
    }

    ToolchainModel getToolchainModel(Path currentJdkHome, String jdkPath) {
        LOGGER.debug("Computing model for " + jdkPath);
        Path jdk = getCanonicalPath(Paths.get(jdkPath));

        ToolchainModel model = cache.get(jdk);
        if (model == null) {
            model = doGetToolchainModel(jdk);
            cache.put(jdk, model);
            cacheModified = true;
        }

        if (Objects.equals(jdk, currentJdkHome)
                || currentJdkHome.getFileName().toString().equals("jre")
                        && Objects.equals(jdk, currentJdkHome.getParent())) {
            model.getProvides().setProperty("current", "true");
        }
        return model;
    }

    ToolchainModel doGetToolchainModel(Path jdk) {
        Path bin = jdk.resolve("bin");
        Path java = bin.resolve("java");
        if (!java.toFile().canExecute()) {
            java = bin.resolve("java.exe");
            if (!java.toFile().canExecute()) {
                LOGGER.debug("JDK toolchain discovered at {} will be ignored: unable to find java executable", jdk);
                return null;
            }
        }
        List<String> lines;
        try {
            Path temp = Files.createTempFile("jdk-opts-", ".out");
            try {
                new ProcessBuilder()
                        .command(java.toString(), "-XshowSettings:properties", "-version")
                        .redirectError(temp.toFile())
                        .start()
                        .waitFor();
                lines = Files.readAllLines(temp);
            } finally {
                Files.delete(temp);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.debug("JDK toolchain discovered at {} will be ignored: unable to execute java: " + e, jdk);
            return null;
        }

        Map<String, String> properties = new LinkedHashMap<>();
        for (String name : Arrays.asList(
                "java.version", "java.runtime.name", "java.runtime.version", "java.vendor", "java.vendor.version")) {
            String v = lines.stream()
                    .filter(l -> l.contains(name))
                    .map(l -> l.replaceFirst(".*=\\s*(.*)", "$1"))
                    .findFirst()
                    .orElse(null);
            String k = name.substring(5);
            if (v != null) {
                properties.put(k, v);
            }
        }
        if (!properties.containsKey("version")) {
            LOGGER.debug("JDK toolchain discovered at {} will be ignored: could not obtain java.version", jdk);
            return null;
        }

        return new ToolchainModel(org.apache.maven.api.toolchain.ToolchainModel.newBuilder()
                .type("jdk")
                .provides(properties)
                .configuration(new XmlNodeImpl(
                        "configuration",
                        null,
                        null,
                        Collections.singletonList(new XmlNodeImpl("jdkHome", jdk.toString())),
                        null))
                .build());
    }

    private static Path getCanonicalPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return getCanonicalPath(path.getParent()).resolve(path.getFileName());
        }
    }

    Comparator<ToolchainModel> getToolchainModelComparator() {
        return Comparator.comparing((ToolchainModel tc) -> tc.getProvides().getProperty("vendor"))
                .thenComparing(tc -> tc.getProvides().getProperty("version"), this::compareVersion);
    }

    int compareVersion(String v1, String v2) {
        String[] s1 = v1.split("\\.");
        String[] s2 = v2.split("\\.");
        return compare(s1, s2);
    }

    static <T extends Comparable<? super T>> int compare(T[] a, T[] b) {
        int length = Math.min(a.length, b.length);
        for (int i = 0; i < length; i++) {
            T oa = a[i];
            T ob = b[i];
            if (oa != ob) {
                // A null element is less than a non-null element
                if (oa == null || ob == null) {
                    return oa == null ? -1 : 1;
                }
                int v = oa.compareTo(ob);
                if (v != 0) {
                    return v;
                }
            }
        }
        return a.length - b.length;
    }
}
