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
package org.apache.maven.artifact.repository.metadata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.maven.metadata.v4.MetadataStaxReader;
import org.apache.maven.metadata.v4.MetadataStaxWriter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataTest {

    Artifact artifact;

    Metadata target;

    @BeforeEach
    void before() {
        artifact = new DefaultArtifact("myGroup:myArtifact:1.0-SNAPSHOT");
        target = createMetadataFromArtifact(artifact);
    }

    /*--- START test common metadata ---*/
    @Test
    void mergeEmptyMetadata() throws Exception {
        Metadata metadata = new Metadata();
        assertThat(metadata.merge(new Metadata())).isFalse();
    }

    @Test
    void mergeDifferentGAV() throws Exception {
        // merge implicitly assumes that merge is only called on the same GAV and does not perform any validation here!
        Metadata source = new Metadata();
        source.setArtifactId("source-artifact");
        source.setGroupId("source-group");
        source.setVersion("2.0");
        assertThat(target.merge(source)).isFalse();
        assertThat(target.getArtifactId()).isEqualTo("myArtifact");
        assertThat(target.getGroupId()).isEqualTo("myGroup");
        assertThat(target.getVersion()).isEqualTo("1.0-SNAPSHOT");
    }
    /*--- END test common metadata ---*/

    /*--- START test "groupId/artifactId/version" metadata ---*/
    @Test
    void mergeSnapshotWithEmptyList() throws Exception {
        Snapshot snapshot = new Snapshot();
        snapshot.setBuildNumber(3);
        snapshot.setTimestamp("20200710.072412");
        target.getVersioning().setSnapshot(snapshot);
        target.getVersioning().setLastUpdated("20200921071745");
        SnapshotVersion sv = new SnapshotVersion();
        sv.setClassifier("sources");
        sv.setExtension("jar");
        sv.setUpdated("20200710072412");
        target.getVersioning().addSnapshotVersion(sv);

        Metadata source = createMetadataFromArtifact(artifact);
        // nothing should be actually changed, but still merge returns true
        assertThat(target.merge(source)).isTrue();

        // NOTE! Merge updates last updated to source
        assertThat(source.getVersioning().getLastUpdated()).isEqualTo("20200921071745");

        assertThat(target.getArtifactId()).isEqualTo("myArtifact");
        assertThat(target.getGroupId()).isEqualTo("myGroup");

        assertThat(target.getVersioning().getSnapshot().getBuildNumber()).isEqualTo(3);
        assertThat(target.getVersioning().getSnapshot().getTimestamp()).isEqualTo("20200710.072412");

        assertThat(target.getVersioning().getSnapshotVersions().size()).isEqualTo(1);
        assertThat(target.getVersioning().getSnapshotVersions().get(0).getClassifier()).isEqualTo("sources");
        assertThat(target.getVersioning().getSnapshotVersions().get(0).getExtension()).isEqualTo("jar");
        assertThat(target.getVersioning().getSnapshotVersions().get(0).getUpdated()).isEqualTo("20200710072412");
    }

    @Test
    void mergeWithSameSnapshotWithDifferentVersionsAndNewerLastUpdated() {
        Metadata source = createMetadataFromArtifact(artifact);
        Date before = new Date(System.currentTimeMillis() - 5000);
        Date after = new Date(System.currentTimeMillis());
        addSnapshotVersion(target.getVersioning(), "jar", before, "1", 1);
        SnapshotVersion sv2 =
                addSnapshotVersion(source.getVersioning(), "jar", after, "1.0-" + formatDate(after, true) + "-2", 2);
        SnapshotVersion sv3 =
                addSnapshotVersion(source.getVersioning(), "pom", after, "1.0-" + formatDate(after, true) + "-2", 2);
        assertThat(target.merge(source)).isTrue();
        Versioning actualVersioning = target.getVersioning();
        assertThat(actualVersioning.getSnapshotVersions().size()).isEqualTo(2);
        assertThat(actualVersioning.getSnapshotVersions().get(0)).isEqualTo(sv2);
        assertThat(actualVersioning.getSnapshotVersions().get(1)).isEqualTo(sv3);
        assertThat(actualVersioning.getLastUpdated()).isEqualTo(formatDate(after, false));
        assertThat(actualVersioning.getSnapshot().getTimestamp()).isEqualTo(formatDate(after, true));
        assertThat(actualVersioning.getSnapshot().getBuildNumber()).isEqualTo(2);
    }

    @Test
    void mergeWithSameSnapshotWithDifferentVersionsAndOlderLastUpdated() {
        Metadata source = createMetadataFromArtifact(artifact);
        Date before = new Date(System.currentTimeMillis() - 5000);
        Date after = new Date(System.currentTimeMillis());
        SnapshotVersion sv1 = addSnapshotVersion(target.getVersioning(), after, artifact);
        addSnapshotVersion(source.getVersioning(), before, artifact);
        // nothing should be updated, as the target was already updated at a later date than source
        assertThat(target.merge(source)).isFalse();
        assertThat(target.getVersioning().getSnapshotVersions().size()).isEqualTo(1);
        assertThat(target.getVersioning().getSnapshotVersions().get(0)).isEqualTo(sv1);
        assertThat(target.getVersioning().getLastUpdated()).isEqualTo(formatDate(after, false));
        assertThat(target.getVersioning().getSnapshot().getTimestamp()).isEqualTo(formatDate(after, true));
    }

    @Test
    void mergeWithSameSnapshotWithSameVersionAndTimestamp() {
        Metadata source = createMetadataFromArtifact(artifact);
        Date date = new Date();
        addSnapshotVersion(target.getVersioning(), date, artifact);
        SnapshotVersion sv1 = addSnapshotVersion(source.getVersioning(), date, artifact);
        // although nothing has changed merge returns true, as the last modified date is equal
        // TODO: improve merge here?
        assertThat(target.merge(source)).isTrue();
        assertThat(target.getVersioning().getSnapshotVersions().size()).isEqualTo(1);
        assertThat(target.getVersioning().getSnapshotVersions().get(0)).isEqualTo(sv1);
        assertThat(target.getVersioning().getLastUpdated()).isEqualTo(formatDate(date, false));
        assertThat(target.getVersioning().getSnapshot().getTimestamp()).isEqualTo(formatDate(date, true));
    }

    @Test
    void mergeLegacyWithSnapshotLegacy() {
        Metadata source = createMetadataFromArtifact(artifact);
        Date before = new Date(System.currentTimeMillis() - 5000);
        Date after = new Date(System.currentTimeMillis());
        // legacy metadata did not have "versioning.snapshotVersions"
        addSnapshotVersionLegacy(target.getVersioning(), before, 1);
        addSnapshotVersionLegacy(source.getVersioning(), after, 2);
        // although nothing has changed merge returns true, as the last modified date is equal
        // TODO: improve merge here?
        assertThat(target.merge(source)).isTrue();
        assertThat(target.getVersioning().getSnapshotVersions().size()).isEqualTo(0);
        assertThat(target.getVersioning().getLastUpdated()).isEqualTo(formatDate(after, false));
        assertThat(target.getVersioning().getSnapshot().getTimestamp()).isEqualTo(formatDate(after, true));
    }

    @Test
    void mergeLegacyWithSnapshot() {
        Metadata source = createMetadataFromArtifact(artifact);
        Date before = new Date(System.currentTimeMillis() - 5000);
        Date after = new Date(System.currentTimeMillis());
        // legacy metadata did not have "versioning.snapshotVersions"
        addSnapshotVersionLegacy(target.getVersioning(), before, 1);
        addSnapshotVersion(source.getVersioning(), after, artifact);
        // although nothing has changed merge returns true, as the last modified date is equal
        // TODO: improve merge here?
        assertThat(target.merge(source)).isTrue();
        // never convert from legacy format to v1.1 format
        assertThat(target.getVersioning().getSnapshotVersions().size()).isEqualTo(0);
        assertThat(target.getVersioning().getLastUpdated()).isEqualTo(formatDate(after, false));
        assertThat(target.getVersioning().getSnapshot().getTimestamp()).isEqualTo(formatDate(after, true));
    }

    @Test
    void mergeWithSnapshotLegacy() {
        Metadata source = createMetadataFromArtifact(artifact);
        Date before = new Date(System.currentTimeMillis() - 5000);
        Date after = new Date(System.currentTimeMillis());
        addSnapshotVersion(target.getVersioning(), before, artifact);
        // legacy metadata did not have "versioning.snapshotVersions"
        addSnapshotVersionLegacy(source.getVersioning(), after, 2);
        // although nothing has changed merge returns true, as the last modified date is equal
        // TODO: improve merge here?
        assertThat(target.merge(source)).isTrue();
        // the result must be legacy format as well
        assertThat(target.getVersioning().getSnapshotVersions().size()).isEqualTo(0);
        assertThat(target.getVersioning().getLastUpdated()).isEqualTo(formatDate(after, false));
        assertThat(target.getVersioning().getSnapshot().getTimestamp()).isEqualTo(formatDate(after, true));
        assertThat(target.getVersioning().getSnapshot().getBuildNumber()).isEqualTo(2);
    }
    /*-- END test "groupId/artifactId/version" metadata ---*/

    @Test
    void roundtrip() throws Exception {
        Metadata source = new Metadata(org.apache.maven.api.metadata.Metadata.newBuilder(
                        createMetadataFromArtifact(artifact).getDelegate(), true)
                .modelEncoding("UTF-16")
                .build());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new MetadataStaxWriter().write(baos, source.getDelegate());
        Metadata source2 =
                new Metadata(new MetadataStaxReader().read(new ByteArrayInputStream(baos.toByteArray()), true));
        assertThat(source2).isNotNull();
    }

    /*-- START helper methods to populate metadata objects ---*/
    private static final String SNAPSHOT = "SNAPSHOT";

    private static final String DEFAULT_SNAPSHOT_TIMESTAMP_FORMAT = "yyyyMMdd.HHmmss";

    private static final String DEFAULT_DATE_FORMAT = "yyyyMMddHHmmss";

    private static String formatDate(Date date, boolean forSnapshotTimestamp) {
        // logic from metadata.mdo, class "Versioning"
        TimeZone timezone = TimeZone.getTimeZone("UTC");
        DateFormat fmt =
                new SimpleDateFormat(forSnapshotTimestamp ? DEFAULT_SNAPSHOT_TIMESTAMP_FORMAT : DEFAULT_DATE_FORMAT);
        fmt.setCalendar(new GregorianCalendar());
        fmt.setTimeZone(timezone);
        return fmt.format(date);
    }

    private static Metadata createMetadataFromArtifact(Artifact artifact) {
        Metadata metadata = new Metadata();
        metadata.setArtifactId(artifact.getArtifactId());
        metadata.setGroupId(artifact.getGroupId());
        metadata.setVersion(artifact.getVersion());
        metadata.setVersioning(new Versioning());
        return metadata;
    }

    private static SnapshotVersion addSnapshotVersion(Versioning versioning, Date timestamp, Artifact artifact) {
        int buildNumber = 1;
        // this generates timestamped versions like maven-resolver-provider:
        // https://github.com/apache/maven/blob/03df5f7c639db744a3597c7175c92c8e2a27767b/maven-resolver-provider/src/main/java/org/apache/maven/repository/internal/RemoteSnapshotMetadata.java#L79
        String version = artifact.getVersion();
        String qualifier = formatDate(timestamp, true) + '-' + buildNumber;
        version = version.substring(0, version.length() - SNAPSHOT.length()) + qualifier;
        return addSnapshotVersion(versioning, artifact.getExtension(), timestamp, version, buildNumber);
    }

    private static SnapshotVersion addSnapshotVersion(
            Versioning versioning, String extension, Date timestamp, String version, int buildNumber) {
        Snapshot snapshot = new Snapshot();
        snapshot.setBuildNumber(buildNumber);
        snapshot.setTimestamp(formatDate(timestamp, true));

        SnapshotVersion sv = new SnapshotVersion();
        sv.setExtension(extension);
        sv.setVersion(version);
        sv.setUpdated(formatDate(timestamp, false));
        versioning.addSnapshotVersion(sv);

        // make the new snapshot the current one
        versioning.setSnapshot(snapshot);
        versioning.setLastUpdatedTimestamp(timestamp);
        return sv;
    }

    // the format written by Maven 2
    // (https://maven.apache.org/ref/2.2.1/maven-repository-metadata/repository-metadata.html)
    private static void addSnapshotVersionLegacy(Versioning versioning, Date timestamp, int buildNumber) {
        Snapshot snapshot = new Snapshot();
        snapshot.setBuildNumber(buildNumber);
        snapshot.setTimestamp(formatDate(timestamp, true));

        versioning.setSnapshot(snapshot);
        versioning.setLastUpdatedTimestamp(timestamp);
    }
    /*-- END helper methods to populate metadata objects ---*/
}
