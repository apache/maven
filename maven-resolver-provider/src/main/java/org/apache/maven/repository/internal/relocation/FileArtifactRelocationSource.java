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
package org.apache.maven.repository.internal.relocation;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.apache.maven.repository.internal.MavenArtifactRelocationSource;
import org.apache.maven.repository.internal.RelocatedArtifact;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Relocation source from a local file.
 *
 * @since 4.0.0
 */
@Singleton
@Named
@Priority(15)
public final class FileArtifactRelocationSource implements MavenArtifactRelocationSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileArtifactRelocationSource.class);

    private static final String CONFIG_PROP_RELOCATIONS = "maven.relocations.file";

    private static final String CONFIG_PROP_GLOBAL = "maven.relocations.file.global";

    @Override
    public Artifact relocatedTarget(RepositorySystemSession session, ArtifactDescriptorRequest request, Model model) {
        if (isActive(session, request.getRequestContext())) {
            Relocations relocations = (Relocations) session.getData()
                    .computeIfAbsent(getClass().getName() + ".relocations", () -> readRelocations(session));
            if (relocations != null) {
                Relocation relocation = relocations.getRelocation(request.getArtifact());
                if (relocation != null) {
                    LOGGER.info("User relocation: {}", relocation);
                    return new RelocatedArtifact(
                            request.getArtifact(),
                            isAny(relocation.target.getGroupId()) ? null : relocation.target.getGroupId(),
                            isAny(relocation.target.getArtifactId()) ? null : relocation.target.getArtifactId(),
                            isAny(relocation.target.getVersion()) ? null : relocation.target.getVersion(),
                            "User relocation");
                }
            }
        }
        return null;
    }

    private boolean isActive(RepositorySystemSession session, String context) {
        Object global = session.getConfigProperties().get(CONFIG_PROP_GLOBAL);
        if (global instanceof Boolean) {
            return (Boolean) global;
        } else if (global instanceof String) {
            return Boolean.parseBoolean((String) global);
        } else {
            return context != null && context.startsWith("project");
        }
    }

    private static boolean isAny(String str) {
        return "*".equals(str);
    }

    // starts with G
    // ends with G

    private static Predicate<Artifact> artifactPredicate(Artifact artifact) {
        return a -> {
            if ("*".equals(artifact.getGroupId()) || artifact.getGroupId().equals(a.getGroupId())) {
                if ("*".equals(artifact.getArtifactId())
                        || artifact.getArtifactId().equals(a.getArtifactId())) {
                    if ("*".equals(artifact.getBaseVersion())
                            || artifact.getBaseVersion().equals(a.getBaseVersion())) {
                        return true;
                    }
                }
            }
            return false;
        };
    }

    private static class Relocation {
        private final Predicate<Artifact> predicate;
        private final Artifact source;
        private final Artifact target;

        private Relocation(Artifact source, Artifact target) {
            this.predicate = artifactPredicate(source);
            this.source = source;
            this.target = target;
        }

        @Override
        public String toString() {
            return source + " > " + target;
        }
    }

    private static class Relocations {
        private final List<Relocation> relocations;

        private Relocations(List<Relocation> relocations) {
            this.relocations = relocations;
        }

        private Relocation getRelocation(Artifact artifact) {
            return relocations.stream()
                    .filter(r -> r.predicate.test(artifact))
                    .findFirst()
                    .orElse(null);
        }
    }

    private Relocations readRelocations(RepositorySystemSession session) {
        Object relocationsSource = session.getConfigProperties().get(CONFIG_PROP_RELOCATIONS);
        if (relocationsSource == null) {
            return null;
        }
        Path relocations;
        if (relocationsSource instanceof File) {
            relocations = ((File) relocationsSource).toPath();
        } else if (relocationsSource instanceof Path) {
            relocations = (Path) relocationsSource;
        } else if (relocationsSource instanceof String) {
            relocations = Paths.get((String) relocationsSource);
        } else {
            throw new IllegalArgumentException("The property " + CONFIG_PROP_RELOCATIONS + " type is not supported:"
                    + relocationsSource.getClass());
        }
        try (BufferedReader reader = Files.newBufferedReader(relocations);
                Stream<String> lines = reader.lines()) {
            List<Relocation> relocationList = lines.filter(l -> !l.trim().isEmpty() && !l.startsWith("#"))
                    .map(l -> {
                        String[] parts = l.split(">");
                        if (parts.length < 1) {
                            throw new IllegalArgumentException("Unrecognized line: " + l);
                        }
                        Artifact s = new DefaultArtifact(parts[0]);
                        Artifact t = null;
                        if (parts.length > 1) {
                            t = new DefaultArtifact(parts[1]);
                        } else {
                            t = new DefaultArtifact("org.apache.maven:banned:1.0");
                        }
                        return new Relocation(s, t);
                    })
                    .collect(Collectors.toList());
            LOGGER.info("Loaded up {} user relocations", relocationList.size());
            return new Relocations(relocationList);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
