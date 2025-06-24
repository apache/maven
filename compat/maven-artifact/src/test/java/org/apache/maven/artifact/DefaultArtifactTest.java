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
package org.apache.maven.artifact;

import java.util.stream.Stream;

import org.apache.maven.artifact.handler.ArtifactHandlerMock;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultArtifactTest {

    private DefaultArtifact artifact;

    private DefaultArtifact snapshotArtifact;

    private String groupId = "groupid",
            artifactId = "artifactId",
            version = "1.0",
            scope = "artifactScope",
            type = "type",
            classifier = "classifier";

    private String snapshotSpecVersion = "1.0-SNAPSHOT";
    private String snapshotResolvedVersion = "1.0-20070606.010101-1";

    private VersionRange versionRange;
    private VersionRange snapshotVersionRange;

    private ArtifactHandlerMock artifactHandler;

    @BeforeEach
    void setUp() throws Exception {
        artifactHandler = new ArtifactHandlerMock();
        versionRange = VersionRange.createFromVersion(version);
        artifact = new DefaultArtifact(groupId, artifactId, versionRange, scope, type, classifier, artifactHandler);

        snapshotVersionRange = VersionRange.createFromVersion(snapshotResolvedVersion);
        snapshotArtifact = new DefaultArtifact(
                groupId, artifactId, snapshotVersionRange, scope, type, classifier, artifactHandler);
    }

    @Test
    void testGetVersionReturnsResolvedVersionOnSnapshot() {
        assertEquals(snapshotResolvedVersion, snapshotArtifact.getVersion());

        // this is FOUL!
        //        snapshotArtifact.isSnapshot();

        assertEquals(snapshotSpecVersion, snapshotArtifact.getBaseVersion());
    }

    @Test
    void testGetDependencyConflictId() {
        assertEquals(groupId + ":" + artifactId + ":" + type + ":" + classifier, artifact.getDependencyConflictId());
    }

    @Test
    void testGetDependencyConflictIdNullGroupId() {
        artifact.setGroupId(null);
        assertEquals(null + ":" + artifactId + ":" + type + ":" + classifier, artifact.getDependencyConflictId());
    }

    @Test
    void testGetDependencyConflictIdNullClassifier() {
        artifact = new DefaultArtifact(groupId, artifactId, versionRange, scope, type, null, artifactHandler);
        assertEquals(groupId + ":" + artifactId + ":" + type, artifact.getDependencyConflictId());
    }

    @Test
    void testGetDependencyConflictIdNullScope() {
        artifact.setScope(null);
        assertEquals(groupId + ":" + artifactId + ":" + type + ":" + classifier, artifact.getDependencyConflictId());
    }

    @Test
    void testToString() {
        assertEquals(
                groupId + ":" + artifactId + ":" + type + ":" + classifier + ":" + version + ":" + scope,
                artifact.toString());
    }

    @Test
    void testToStringNullGroupId() {
        artifact.setGroupId(null);
        assertEquals(artifactId + ":" + type + ":" + classifier + ":" + version + ":" + scope, artifact.toString());
    }

    @Test
    void testToStringNullClassifier() {
        artifact = new DefaultArtifact(groupId, artifactId, versionRange, scope, type, null, artifactHandler);
        assertEquals(groupId + ":" + artifactId + ":" + type + ":" + version + ":" + scope, artifact.toString());
    }

    @Test
    void testToStringNullScope() {
        artifact.setScope(null);
        assertEquals(groupId + ":" + artifactId + ":" + type + ":" + classifier + ":" + version, artifact.toString());
    }

    @Test
    void testComparisonByVersion() {
        Artifact artifact1 = new DefaultArtifact(
                groupId, artifactId, VersionRange.createFromVersion("5.0"), scope, type, classifier, artifactHandler);
        Artifact artifact2 = new DefaultArtifact(
                groupId, artifactId, VersionRange.createFromVersion("12.0"), scope, type, classifier, artifactHandler);

        assertTrue(artifact1.compareTo(artifact2) < 0);
        assertTrue(artifact2.compareTo(artifact1) > 0);

        Artifact artifact = new DefaultArtifact(
                groupId, artifactId, VersionRange.createFromVersion("5.0"), scope, type, classifier, artifactHandler);
        assertTrue(artifact.compareTo(artifact1) == 0);
        assertTrue(artifact1.compareTo(artifact) == 0);
    }

    @Test
    void testNonResolvedVersionRangeConsistentlyYieldsNullVersions() throws Exception {
        VersionRange vr = VersionRange.createFromVersionSpec("[1.0,2.0)");
        artifact = new DefaultArtifact(groupId, artifactId, vr, scope, type, null, artifactHandler);
        assertNull(artifact.getVersion());
        assertNull(artifact.getBaseVersion());
    }

    @Test
    void testMNG7780() throws Exception {
        VersionRange vr = VersionRange.createFromVersionSpec("[1.0,2.0)");
        artifact = new DefaultArtifact(groupId, artifactId, vr, scope, type, null, artifactHandler);
        assertNull(artifact.getVersion());
        assertNull(artifact.getBaseVersion());

        DefaultArtifact nullVersionArtifact =
                new DefaultArtifact(groupId, artifactId, vr, scope, type, null, artifactHandler);
        assertEquals(artifact, nullVersionArtifact);
    }

    @ParameterizedTest
    @MethodSource("invalidMavenCoordinates")
    void testIllegalCoordinatesInConstructor(String groupId, String artifactId, String version) {
        assertThrows(
                InvalidArtifactRTException.class,
                () -> new DefaultArtifact(
                        groupId, artifactId, version, scope, type, classifier, artifactHandler, false));
        if (version == null) {
            assertThrows(
                    InvalidArtifactRTException.class,
                    () -> new DefaultArtifact(
                            groupId, artifactId, (VersionRange) null, scope, type, classifier, artifactHandler, false));
        }
    }

    static Stream<Arguments> invalidMavenCoordinates() {
        return Stream.of(
                Arguments.of(null, null, null),
                Arguments.of("", "", ""),
                Arguments.of(null, "artifactId", "1.0"),
                Arguments.of("", "artifactId", "1.0"),
                Arguments.of("groupId", null, "1.0"),
                Arguments.of("groupId", "", "1.0"),
                Arguments.of("groupId", "artifactId", null),
                Arguments.of("groupId", "artifactId", ""));
    }
}
