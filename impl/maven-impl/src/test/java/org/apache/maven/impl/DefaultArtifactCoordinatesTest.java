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
package org.apache.maven.impl;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.api.Version;
import org.apache.maven.api.VersionConstraint;
import org.apache.maven.api.VersionRange;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Tests for DefaultArtifactCoordinates equality and hash semantics.
 */
class DefaultArtifactCoordinatesTest {

    /**
     * Tiny stub for VersionConstraint that compares on the raw string.
     */
    private static final class StubVC implements VersionConstraint {
        private final String raw;

        StubVC(final String raw) {
            this.raw = raw;
        }

        @Override
        public VersionRange getVersionRange() {
            return null;
        }

        @Override
        public Version getRecommendedVersion() {
            return null;
        }

        @Override
        public boolean contains(Version version) {
            return true;
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof StubVC && raw.equals(((StubVC) o).raw);
        }

        @Override
        public int hashCode() {
            return raw.hashCode();
        }

        @Override
        public String toString() {
            return raw;
        }
    }

    @Test
    void equalsIncludesExtension() {
        final InternalSession session = Mockito.mock(InternalSession.class);
        Mockito.when(session.parseVersionConstraint(anyString())).thenAnswer(inv -> new StubVC(inv.getArgument(0)));

        final DefaultArtifact jar = new DefaultArtifact("g", "a", "jar", "1.0");
        final DefaultArtifact pom = new DefaultArtifact("g", "a", "pom", "1.0");

        final DefaultArtifactCoordinates cJar = new DefaultArtifactCoordinates(session, jar);
        final DefaultArtifactCoordinates cPom = new DefaultArtifactCoordinates(session, pom);

        assertNotEquals(cJar, cPom, "jar and pom coordinates must differ");
        assertNotEquals(cPom, cJar, "symmetry");
    }

    @Test
    void hashConsidersExtensionForSetMembership() {
        final InternalSession session = Mockito.mock(InternalSession.class);
        Mockito.when(session.parseVersionConstraint(anyString())).thenAnswer(inv -> new StubVC(inv.getArgument(0)));

        final DefaultArtifact jar = new DefaultArtifact("g", "a", "jar", "1.0");
        final DefaultArtifact pom = new DefaultArtifact("g", "a", "pom", "1.0");

        final DefaultArtifactCoordinates cJar = new DefaultArtifactCoordinates(session, jar);
        final DefaultArtifactCoordinates cPom = new DefaultArtifactCoordinates(session, pom);

        final Set<DefaultArtifactCoordinates> set = new HashSet<>();
        set.add(cJar);
        assertFalse(set.contains(cPom), "set must not treat pom as the same key as jar");
    }

    @Test
    void hashIncludesExtension() {
        final InternalSession session = Mockito.mock(InternalSession.class);
        Mockito.when(session.parseVersionConstraint(anyString())).thenAnswer(inv -> new StubVC(inv.getArgument(0)));

        final DefaultArtifact jar = new DefaultArtifact("g", "a", "jar", "1.0");
        final DefaultArtifact pom = new DefaultArtifact("g", "a", "pom", "1.0");

        final DefaultArtifactCoordinates cJar = new DefaultArtifactCoordinates(session, jar);
        final DefaultArtifactCoordinates cPom = new DefaultArtifactCoordinates(session, pom);
        assertNotEquals(cJar.hashCode(), cPom.hashCode(), "hash must reflect extension");
    }
}
