package org.apache.maven.artifact.repository.metadata;

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

import org.junit.Test;

import static org.junit.Assert.*;

public class MetadataTest {

    @Test
    public void emptyMetadataMergeShouldNotGiveAnyChange() throws Exception {
        Metadata metadata = new Metadata();
        assertFalse(metadata.merge(new Metadata()));
    }

    @Test
    public void artifactIdGroupIdAndVersionRemainsUntouched() throws Exception {
        Metadata target = new Metadata();
        Metadata source = new Metadata();
        target.setArtifactId("target-artifact");
        target.setGroupId("target-group");
        target.setVersion("1.0");
        source.setArtifactId("source-artifact");
        source.setGroupId("source-group");
        source.setVersion("2.0");
        assertFalse(target.merge(source));
        assertEquals("target-artifact", target.getArtifactId());
        assertEquals("target-group", target.getGroupId());
        assertEquals("1.0", target.getVersion());
    }

    @Test
    public void shouldMergeSnapshotFromSource() throws Exception {
        Metadata target = new Metadata();
        Metadata source = new Metadata();
        target.setArtifactId("target-artifact");
        target.setGroupId("target-group");
        target.setVersion("1.0");
        Versioning targetVersioning = new Versioning();
        target.setVersioning(targetVersioning);
        Snapshot snapshot = new Snapshot();
        snapshot.setBuildNumber(3);
        snapshot.setTimestamp("20200710.072412");
        targetVersioning.setSnapshot(snapshot);
        targetVersioning.setLastUpdated("20200921071745");
        SnapshotVersion sv = new SnapshotVersion();
        sv.setClassifier("sources");
        sv.setExtension("jar");
        sv.setUpdated("20200710072412");
        targetVersioning.addSnapshotVersion(sv);


        source.setArtifactId("source-artifact");
        source.setGroupId("source-group");
        source.setVersion("2.0");
        Versioning sourceVersioning = new Versioning();
        source.setVersioning(sourceVersioning);

        assertTrue(target.merge(source));

        // NOTE! Merge updates last updated to source
        assertEquals("20200921071745", source.getVersioning().getLastUpdated());


        assertEquals("target-artifact", target.getArtifactId());
        assertEquals("target-group", target.getGroupId());

        assertEquals(3, target.getVersioning().getSnapshot().getBuildNumber());
        assertEquals("20200710.072412", target.getVersioning().getSnapshot().getTimestamp());

        assertEquals(1, target.getVersioning().getSnapshotVersions().size());
        assertEquals("sources", target.getVersioning().getSnapshotVersions().get(0).getClassifier());
        assertEquals("jar", target.getVersioning().getSnapshotVersions().get(0).getExtension());
        assertEquals("20200710072412", target.getVersioning().getSnapshotVersions().get(0).getUpdated());
    }

    @Test
    public void shouldReplaceExistingSnapshotVersionFromSource() throws Exception {
        Metadata target = new Metadata();
        Metadata source = new Metadata();
        target.setArtifactId("target-artifact");
        target.setGroupId("target-group");
        target.setVersion("1.0");
        Versioning targetVersioning = new Versioning();
        target.setVersioning(targetVersioning);
        Snapshot snapshot = new Snapshot();
        snapshot.setBuildNumber(3);
        snapshot.setTimestamp("20200710.072412");
        targetVersioning.setSnapshot(snapshot);
        targetVersioning.setLastUpdated("20200921071745");
        SnapshotVersion sv = new SnapshotVersion();
        sv.setClassifier("sources");
        sv.setExtension("jar");
        sv.setUpdated("20200710072412");
        targetVersioning.addSnapshotVersion(sv);


        source.setArtifactId("source-artifact");
        source.setGroupId("source-group");
        source.setVersion("2.0");
        Versioning sourceVersioning = new Versioning();
        source.setVersioning(sourceVersioning);
        snapshot = new Snapshot();
        snapshot.setBuildNumber(3);
        snapshot.setTimestamp("20200710.072413");
        sourceVersioning.setSnapshot(snapshot);
        sourceVersioning.setLastUpdated("20200921071746");
        sv = new SnapshotVersion();
        sv.setClassifier("sources");
        sv.setExtension("jar");
        sv.setUpdated("20200710072413");
        sourceVersioning.addSnapshotVersion(sv);



        assertTrue(target.merge(source));

        // NOTE! Merge updates last updated to source
        assertEquals("20200921071746", source.getVersioning().getLastUpdated());


        assertEquals("target-artifact", target.getArtifactId());
        assertEquals("target-group", target.getGroupId());

        assertEquals(3, target.getVersioning().getSnapshot().getBuildNumber());
        assertEquals("20200710.072413", target.getVersioning().getSnapshot().getTimestamp());

        assertEquals(1, target.getVersioning().getSnapshotVersions().size());
        assertEquals("sources", target.getVersioning().getSnapshotVersions().get(0).getClassifier());
        assertEquals("jar", target.getVersioning().getSnapshotVersions().get(0).getExtension());
        assertEquals("20200710072413", target.getVersioning().getSnapshotVersions().get(0).getUpdated());
    }

    @Test
    public void shouldRetainsExistingSnapshotVersionIfSourceDoNotProvideNewOne() throws Exception {
        Metadata target = new Metadata();
        Metadata source = new Metadata();
        target.setArtifactId("target-artifact");
        target.setGroupId("target-group");
        target.setVersion("1.0");
        Versioning targetVersioning = new Versioning();
        target.setVersioning(targetVersioning);
        Snapshot snapshot = new Snapshot();
        snapshot.setBuildNumber(3);
        snapshot.setTimestamp("20200710.072412");
        targetVersioning.setSnapshot(snapshot);
        targetVersioning.setLastUpdated("20200921071745");
        SnapshotVersion sv = new SnapshotVersion();
        sv.setClassifier("tests");
        sv.setExtension("jar");
        sv.setUpdated("20200710072412");
        targetVersioning.addSnapshotVersion(sv);


        source.setArtifactId("source-artifact");
        source.setGroupId("source-group");
        source.setVersion("2.0");
        Versioning sourceVersioning = new Versioning();
        source.setVersioning(sourceVersioning);
        snapshot = new Snapshot();
        snapshot.setBuildNumber(3);
        snapshot.setTimestamp("20200710.072413");
        sourceVersioning.setSnapshot(snapshot);
        sourceVersioning.setLastUpdated("20200921071746");
        sv = new SnapshotVersion();
        sv.setClassifier("sources");
        sv.setExtension("jar");
        sv.setUpdated("20200710072413");
        sourceVersioning.addSnapshotVersion(sv);



        assertTrue(target.merge(source));

        // NOTE! Merge updates last updated to source
        assertEquals("20200921071746", source.getVersioning().getLastUpdated());


        assertEquals("target-artifact", target.getArtifactId());
        assertEquals("target-group", target.getGroupId());

        assertEquals(3, target.getVersioning().getSnapshot().getBuildNumber());
        assertEquals("20200710.072413", target.getVersioning().getSnapshot().getTimestamp());

        assertEquals(2, target.getVersioning().getSnapshotVersions().size());
        assertEquals("sources", target.getVersioning().getSnapshotVersions().get(0).getClassifier());
        assertEquals("jar", target.getVersioning().getSnapshotVersions().get(0).getExtension());
        assertEquals("20200710072413", target.getVersioning().getSnapshotVersions().get(0).getUpdated());

        assertEquals("tests", target.getVersioning().getSnapshotVersions().get(1).getClassifier());
        assertEquals("jar", target.getVersioning().getSnapshotVersions().get(1).getExtension());
        assertEquals("20200710072412", target.getVersioning().getSnapshotVersions().get(1).getUpdated());
    }

}