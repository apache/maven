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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MetadataTest {

    Artifact artifact;

    Metadata target;

    @Before
    public void before() {
        artifact = new DefaultArtifact("myGroup:myArtifact:1.0-SNAPSHOT");
        target = createMetadataFromArtifact(artifact);
    }

    /*--- START test common metadata ---*/
    @Test
    public void mergeEmptyMetadata() throws Exception {
        Metadata metadata = new Metadata();
        assertFalse(metadata.merge(new Metadata()));
    }

    @Test
    public void mergeDifferentGAV() throws Exception {
        // merge implicitly assumes that merge is only called on the same GAV and does not perform any validation here!
        Metadata source = new Metadata();
        source.setArtifactId("source-artifact");
        source.setGroupId("source-group");
        source.setVersion("2.0");
        assertFalse(target.merge(source));
        assertEquals("myArtifact", target.getArtifactId());
        assertEquals("myGroup", target.getGroupId());
        assertEquals("1.0-SNAPSHOT", target.getVersion());
    }
    /*--- END test common metadata ---*/

    /*--- START test "groupId/artifactId/version" metadata ---*/
    @Test
    public void mergeSnapshotWithEmptyList() throws Exception {
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
        assertTrue(target.merge(source));

        // NOTE! Merge updates last updated to source
        assertEquals("20200921071745", source.getVersioning().getLastUpdated());

        assertEquals("myArtifact", target.getArtifactId());
        assertEquals("myGroup", target.getGroupId());

        assertEquals(3, target.getVersioning().getSnapshot().getBuildNumber());
        assertEquals("20200710.072412", target.getVersioning().getSnapshot().getTimestamp());

        assertEquals(1, target.getVersioning().getSnapshotVersions().size());
        assertEquals(
                "sources", target.getVersioning().getSnapshotVersions().get(0).getClassifier());
        assertEquals("jar", target.getVersioning().getSnapshotVersions().get(0).getExtension());
        assertEquals(
                "20200710072412",
                target.getVersioning().getSnapshotVersions().get(0).getUpdated());
    }

    @Test
    public void mergeWithSameSnapshotWithDifferentVersionsAndNewerLastUpdated() {
        Metadata source = createMetadataFromArtifact(artifact);
        Date before = new Date(System.currentTimeMillis() - 5000);
        Date after = new Date(System.currentTimeMillis());
        addSnapshotVersion(target.getVersioning(), "jar", before, "1", 1);
        SnapshotVersion sv2 =
                addSnapshotVersion(source.getVersioning(), "jar", after, "1.0-" + formatDate(after, true) + "-2", 2);
        SnapshotVersion sv3 =
                addSnapshotVersion(source.getVersioning(), "pom", after, "1.0-" + formatDate(after, true) + "-2", 2);
        assertTrue(target.merge(source));
        Versioning actualVersioning = target.getVersioning();
        assertEquals(2, actualVersioning.getSnapshotVersions().size());
        assertEquals(sv2, actualVersioning.getSnapshotVersions().get(0));
        assertEquals(sv3, actualVersioning.getSnapshotVersions().get(1));
        assertEquals(formatDate(after, false), actualVersioning.getLastUpdated());
        assertEquals(formatDate(after, true), actualVersioning.getSnapshot().getTimestamp());
        assertEquals(2, actualVersioning.getSnapshot().getBuildNumber());
    }

    @Test
    public void mergeWithSameSnapshotWithDifferentVersionsAndOlderLastUpdated() {
        Metadata source = createMetadataFromArtifact(artifact);
        Date before = new Date(System.currentTimeMillis() - 5000);
        Date after = new Date(System.currentTimeMillis());
        SnapshotVersion sv1 = addSnapshotVersion(target.getVersioning(), after, artifact);
        addSnapshotVersion(source.getVersioning(), before, artifact);
        // nothing should be updated, as the target was already updated at a later date than source
        assertFalse(target.merge(source));
        assertEquals(1, target.getVersioning().getSnapshotVersions().size());
        assertEquals(sv1, target.getVersioning().getSnapshotVersions().get(0));
        assertEquals(formatDate(after, false), target.getVersioning().getLastUpdated());
        assertEquals(
                formatDate(after, true), target.getVersioning().getSnapshot().getTimestamp());
    }

    @Test
    public void mergeWithSameSnapshotWithSameVersionAndTimestamp() {
        Metadata source = createMetadataFromArtifact(artifact);
        Date date = new Date();
        addSnapshotVersion(target.getVersioning(), date, artifact);
        SnapshotVersion sv1 = addSnapshotVersion(source.getVersioning(), date, artifact);
        // although nothing has changed merge returns true, as the last modified date is equal
        // TODO: improve merge here?
        assertTrue(target.merge(source));
        assertEquals(1, target.getVersioning().getSnapshotVersions().size());
        assertEquals(sv1, target.getVersioning().getSnapshotVersions().get(0));
        assertEquals(formatDate(date, false), target.getVersioning().getLastUpdated());
        assertEquals(
                formatDate(date, true), target.getVersioning().getSnapshot().getTimestamp());
    }

    @Test
    public void mergeLegacyWithSnapshotLegacy() {
        Metadata source = createMetadataFromArtifact(artifact);
        Date before = new Date(System.currentTimeMillis() - 5000);
        Date after = new Date(System.currentTimeMillis());
        // legacy metadata did not have "versioning.snapshotVersions"
        addSnapshotVersionLegacy(target.getVersioning(), before, 1);
        addSnapshotVersionLegacy(source.getVersioning(), after, 2);
        // although nothing has changed merge returns true, as the last modified date is equal
        // TODO: improve merge here?
        assertTrue(target.merge(source));
        assertEquals(0, target.getVersioning().getSnapshotVersions().size());
        assertEquals(formatDate(after, false), target.getVersioning().getLastUpdated());
        assertEquals(
                formatDate(after, true), target.getVersioning().getSnapshot().getTimestamp());
    }

    @Test
    public void mergeLegacyWithSnapshot() {
        Metadata source = createMetadataFromArtifact(artifact);
        Date before = new Date(System.currentTimeMillis() - 5000);
        Date after = new Date(System.currentTimeMillis());
        // legacy metadata did not have "versioning.snapshotVersions"
        addSnapshotVersionLegacy(target.getVersioning(), before, 1);
        addSnapshotVersion(source.getVersioning(), after, artifact);
        // although nothing has changed merge returns true, as the last modified date is equal
        // TODO: improve merge here?
        assertTrue(target.merge(source));
        // never convert from legacy format to v1.1 format
        assertEquals(0, target.getVersioning().getSnapshotVersions().size());
        assertEquals(formatDate(after, false), target.getVersioning().getLastUpdated());
        assertEquals(
                formatDate(after, true), target.getVersioning().getSnapshot().getTimestamp());
    }

    @Test
    public void mergeWithSnapshotLegacy() {
        Metadata source = createMetadataFromArtifact(artifact);
        Date before = new Date(System.currentTimeMillis() - 5000);
        Date after = new Date(System.currentTimeMillis());
        addSnapshotVersion(target.getVersioning(), before, artifact);
        // legacy metadata did not have "versioning.snapshotVersions"
        addSnapshotVersionLegacy(source.getVersioning(), after, 2);
        // although nothing has changed merge returns true, as the last modified date is equal
        // TODO: improve merge here?
        assertTrue(target.merge(source));
        // the result must be legacy format as well
        assertEquals(0, target.getVersioning().getSnapshotVersions().size());
        assertEquals(formatDate(after, false), target.getVersioning().getLastUpdated());
        assertEquals(
                formatDate(after, true), target.getVersioning().getSnapshot().getTimestamp());
        assertEquals(2, target.getVersioning().getSnapshot().getBuildNumber());
    }
    /*-- END test "groupId/artifactId/version" metadata ---*/

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
