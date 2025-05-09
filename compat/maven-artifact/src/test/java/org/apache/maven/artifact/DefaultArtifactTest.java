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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

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
    void getVersionReturnsResolvedVersionOnSnapshot() {
        assertThat(snapshotArtifact.getVersion()).isEqualTo(snapshotResolvedVersion);

        // this is FOUL!
        //        snapshotArtifact.isSnapshot();

        assertThat(snapshotArtifact.getBaseVersion()).isEqualTo(snapshotSpecVersion);
    }

    @Test
    void getDependencyConflictId() {
        assertThat(artifact.getDependencyConflictId()).isEqualTo(groupId + ":" + artifactId + ":" + type + ":" + classifier);
    }

    @Test
    void getDependencyConflictIdNullGroupId() {
        artifact.setGroupId(null);
        assertThat(artifact.getDependencyConflictId()).isEqualTo(null + ":" + artifactId + ":" + type + ":" + classifier);
    }

    @Test
    void getDependencyConflictIdNullClassifier() {
        artifact = new DefaultArtifact(groupId, artifactId, versionRange, scope, type, null, artifactHandler);
        assertThat(artifact.getDependencyConflictId()).isEqualTo(groupId + ":" + artifactId + ":" + type);
    }

    @Test
    void getDependencyConflictIdNullScope() {
        artifact.setScope(null);
        assertThat(artifact.getDependencyConflictId()).isEqualTo(groupId + ":" + artifactId + ":" + type + ":" + classifier);
    }

    @Test
    void testToString() {
        assertThat(artifact.toString()).isEqualTo(groupId + ":" + artifactId + ":" + type + ":" + classifier + ":" + version + ":" + scope);
    }

    @Test
    void toStringNullGroupId() {
        artifact.setGroupId(null);
        assertThat(artifact.toString()).isEqualTo(artifactId + ":" + type + ":" + classifier + ":" + version + ":" + scope);
    }

    @Test
    void toStringNullClassifier() {
        artifact = new DefaultArtifact(groupId, artifactId, versionRange, scope, type, null, artifactHandler);
        assertThat(artifact.toString()).isEqualTo(groupId + ":" + artifactId + ":" + type + ":" + version + ":" + scope);
    }

    @Test
    void toStringNullScope() {
        artifact.setScope(null);
        assertThat(artifact.toString()).isEqualTo(groupId + ":" + artifactId + ":" + type + ":" + classifier + ":" + version);
    }

    @Test
    void comparisonByVersion() {
        Artifact artifact1 = new DefaultArtifact(
                groupId, artifactId, VersionRange.createFromVersion("5.0"), scope, type, classifier, artifactHandler);
        Artifact artifact2 = new DefaultArtifact(
                groupId, artifactId, VersionRange.createFromVersion("12.0"), scope, type, classifier, artifactHandler);

        assertThat(artifact1.compareTo(artifact2) < 0).isTrue();
        assertThat(artifact2.compareTo(artifact1) > 0).isTrue();

        Artifact artifact = new DefaultArtifact(
                groupId, artifactId, VersionRange.createFromVersion("5.0"), scope, type, classifier, artifactHandler);
        assertThat(artifact.compareTo(artifact1)).isEqualTo(0);
        assertThat(artifact1.compareTo(artifact)).isEqualTo(0);
    }

    @Test
    void nonResolvedVersionRangeConsistentlyYieldsNullVersions() throws Exception {
        VersionRange vr = VersionRange.createFromVersionSpec("[1.0,2.0)");
        artifact = new DefaultArtifact(groupId, artifactId, vr, scope, type, null, artifactHandler);
        assertThat(artifact.getVersion()).isNull();
        assertThat(artifact.getBaseVersion()).isNull();
    }

    @Test
    void mng7780() throws Exception {
        VersionRange vr = VersionRange.createFromVersionSpec("[1.0,2.0)");
        artifact = new DefaultArtifact(groupId, artifactId, vr, scope, type, null, artifactHandler);
        assertThat(artifact.getVersion()).isNull();
        assertThat(artifact.getBaseVersion()).isNull();

        DefaultArtifact nullVersionArtifact =
                new DefaultArtifact(groupId, artifactId, vr, scope, type, null, artifactHandler);
        assertThat(nullVersionArtifact).isEqualTo(artifact);
    }

    @ParameterizedTest
    @MethodSource("invalidMavenCoordinates")
    void illegalCoordinatesInConstructor(String groupId, String artifactId, String version) {
        assertThatExceptionOfType(InvalidArtifactRTException.class).isThrownBy(() -> new DefaultArtifact(
                groupId, artifactId, version, scope, type, classifier, artifactHandler, false));
        if (version == null) {
            assertThatExceptionOfType(InvalidArtifactRTException.class).isThrownBy(() -> new DefaultArtifact(
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
