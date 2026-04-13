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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.apache.maven.repository.internal.MavenArtifactRelocationSource;
import org.apache.maven.repository.internal.RelocatedArtifact;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.sisu.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User controlled relocations.
 * This property is a comma separated list of entries with the syntax <code>GAV&gt;GAV</code>.
 * The first <code>GAV</code> can contain <code>*</code> for any elem (so <code>*:*:*</code> would mean ALL, something
 * you don't want). The second <code>GAV</code> is either fully specified, or also can contain <code>*</code>,
 * then it behaves as "ordinary relocation": the coordinate is preserved from relocated artifact.
 * Finally, if right hand <code>GAV</code> is absent (line looks like <code>GAV&gt;</code>), the left hand matching
 * <code>GAV</code> is banned fully (from resolving).
 * <br/>
 * Note: the <code>&gt;</code> means project level, while <code>&gt;&gt;</code> means global (whole session level,
 * so even plugins will get relocated artifacts) relocation.
 * <br/>
 * For example,
 * <pre>maven.relocations.entries = org.foo:*:*>, \\<br/>    org.here:*:*>org.there:*:*, \\<br/>    javax.inject:javax.inject:1>>jakarta.inject:jakarta.inject:1.0.5</pre>
 * means: 3 entries, ban <code>org.foo group</code> (exactly, so <code>org.foo.bar</code> is allowed),
 * relocate <code>org.here</code> to <code>org.there</code> and finally globally relocate (see <code>&gt;&gt;</code> above)
 * <code>javax.inject:javax.inject:1</code> to <code>jakarta.inject:jakarta.inject:1.0.5</code>.
 *
 * @since 3.10.0
 */
@Singleton
@Named(UserPropertiesArtifactRelocationSource.NAME)
@Priority(50)
public final class UserPropertiesArtifactRelocationSource implements MavenArtifactRelocationSource {
    public static final String NAME = "userProperties";
    private static final Logger LOGGER = LoggerFactory.getLogger(UserPropertiesArtifactRelocationSource.class);

    private static final String CONFIG_PROP_RELOCATIONS_ENTRIES = "maven.relocations.entries";

    private static final Artifact SENTINEL = new DefaultArtifact("org.apache.maven.banned:user-relocation:1.0");

    @Override
    public Artifact relocatedTarget(
            RepositorySystemSession session, ArtifactDescriptorResult artifactDescriptorResult, Model model)
            throws ArtifactDescriptorException {
        Relocations relocations = (Relocations) session.getData()
                .computeIfAbsent(getClass().getName() + ".relocations", () -> parseRelocations(session));
        if (relocations != null) {
            Artifact original = artifactDescriptorResult.getRequest().getArtifact();
            Relocation relocation = relocations.getRelocation(original);
            if (relocation != null
                    && (isProjectContext(artifactDescriptorResult.getRequest().getRequestContext())
                            || relocation.global)) {
                if (relocation.target == SENTINEL) {
                    String message = "The artifact " + original + " has been banned from resolution: "
                            + (relocation.global ? "User global ban" : "User project ban");
                    LOGGER.debug(message);
                    throw new ArtifactDescriptorException(artifactDescriptorResult, message);
                }
                Artifact result = new RelocatedArtifact(
                        original,
                        isAny(relocation.target.getGroupId()) ? null : relocation.target.getGroupId(),
                        isAny(relocation.target.getArtifactId()) ? null : relocation.target.getArtifactId(),
                        isAny(relocation.target.getClassifier()) ? null : relocation.target.getClassifier(),
                        isAny(relocation.target.getExtension()) ? null : relocation.target.getExtension(),
                        isAny(relocation.target.getVersion()) ? null : relocation.target.getVersion(),
                        relocation.global ? "User global relocation" : "User project relocation");
                LOGGER.debug(
                        "The artifact {} has been relocated to {}: {}",
                        original,
                        result,
                        relocation.global ? "User global relocation" : "User project relocation");
                return result;
            }
        }
        return null;
    }

    private boolean isProjectContext(String context) {
        return context != null && context.startsWith("project");
    }

    private static boolean isAny(String str) {
        return "*".equals(str);
    }

    private static boolean matches(String pattern, String str) {
        if (isAny(pattern)) {
            return true;
        } else if (pattern.endsWith("*")) {
            return str.startsWith(pattern.substring(0, pattern.length() - 1));
        } else {
            return Objects.equals(pattern, str);
        }
    }

    private static Predicate<Artifact> artifactPredicate(Artifact artifact) {
        return a -> matches(artifact.getGroupId(), a.getGroupId())
                && matches(artifact.getArtifactId(), a.getArtifactId())
                && matches(artifact.getBaseVersion(), a.getBaseVersion())
                && matches(artifact.getExtension(), a.getExtension())
                && matches(artifact.getClassifier(), a.getClassifier());
    }

    private static class Relocation {
        private final Predicate<Artifact> predicate;
        private final boolean global;
        private final Artifact source;
        private final Artifact target;

        private Relocation(boolean global, Artifact source, Artifact target) {
            this.predicate = artifactPredicate(source);
            this.global = global;
            this.source = source;
            this.target = target;
        }

        @Override
        public String toString() {
            return source + (global ? " >> " : " > ") + target;
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

    private Relocations parseRelocations(RepositorySystemSession session) {
        String relocationsEntries = (String) session.getConfigProperties().get(CONFIG_PROP_RELOCATIONS_ENTRIES);
        if (relocationsEntries == null) {
            return null;
        }
        String[] entries = relocationsEntries.split(",");
        try (Stream<String> lines = Arrays.stream(entries)) {
            List<Relocation> relocationList = lines.filter(
                            l -> l != null && !l.trim().isEmpty())
                    .map(l -> {
                        boolean global;
                        String splitExpr;
                        if (l.contains(">>")) {
                            global = true;
                            splitExpr = ">>";
                        } else if (l.contains(">")) {
                            global = false;
                            splitExpr = ">";
                        } else {
                            throw new IllegalArgumentException("Unrecognized entry: " + l);
                        }
                        String[] parts = l.split(splitExpr);
                        if (parts.length < 1) {
                            throw new IllegalArgumentException("Unrecognized entry: " + l);
                        }
                        Artifact s = parseArtifact(parts[0]);
                        Artifact t;
                        if (parts.length > 1) {
                            t = parseArtifact(parts[1]);
                        } else {
                            t = SENTINEL;
                        }
                        return new Relocation(global, s, t);
                    })
                    .collect(Collectors.toList());
            LOGGER.info("Parsed {} user relocations", relocationList.size());
            return new Relocations(relocationList);
        }
    }

    private static Artifact parseArtifact(String coords) {
        String[] parts = coords.split(":");
        switch (parts.length) {
            case 3:
                return new DefaultArtifact(parts[0], parts[1], "*", "*", parts[2]);
            case 4:
                return new DefaultArtifact(parts[0], parts[1], "*", parts[2], parts[3]);
            case 5:
                return new DefaultArtifact(parts[0], parts[1], parts[2], parts[3], parts[4]);
            default:
                throw new IllegalArgumentException("Bad artifact coordinates " + coords
                        + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
        }
    }
}
