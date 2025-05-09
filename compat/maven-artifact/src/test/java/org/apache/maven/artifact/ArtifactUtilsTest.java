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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ArtifactUtils}.
 *
 */
class ArtifactUtilsTest {

    private Artifact newArtifact(String aid) {
        return new DefaultArtifact("group", aid, VersionRange.createFromVersion("1.0"), "test", "jar", "tests", null);
    }

    @Test
    void isSnapshot() {
        assertThat(ArtifactUtils.isSnapshot(null)).isFalse();
        assertThat(ArtifactUtils.isSnapshot("")).isFalse();
        assertThat(ArtifactUtils.isSnapshot("1.2.3")).isFalse();
        assertThat(ArtifactUtils.isSnapshot("1.2.3-SNAPSHOT")).isTrue();
        assertThat(ArtifactUtils.isSnapshot("1.2.3-snapshot")).isTrue();
        assertThat(ArtifactUtils.isSnapshot("1.2.3-20090413.094722-2")).isTrue();
        assertThat(ArtifactUtils.isSnapshot("1.2.3-20090413X094722-2")).isFalse();
    }

    @Test
    void toSnapshotVersion() {
        assertThat(ArtifactUtils.toSnapshotVersion("1.2.3")).isEqualTo("1.2.3");
        assertThat(ArtifactUtils.toSnapshotVersion("1.2.3-SNAPSHOT")).isEqualTo("1.2.3-SNAPSHOT");
        assertThat(ArtifactUtils.toSnapshotVersion("1.2.3-20090413.094722-2")).isEqualTo("1.2.3-SNAPSHOT");
        assertThat(ArtifactUtils.toSnapshotVersion("1.2.3-20090413X094722-2")).isEqualTo("1.2.3-20090413X094722-2");
    }

    /**
     * Tests that the ordering of the map resembles the ordering of the input collection of artifacts.
     */
    @Test
    void artifactMapByVersionlessIdOrdering() throws Exception {
        List<Artifact> list = new ArrayList<>();
        list.add(newArtifact("b"));
        list.add(newArtifact("a"));
        list.add(newArtifact("c"));
        list.add(newArtifact("e"));
        list.add(newArtifact("d"));

        Map<String, Artifact> map = ArtifactUtils.artifactMapByVersionlessId(list);
        assertThat(map).isNotNull();
        assertThat(new ArrayList<>(map.values())).isEqualTo(list);
    }
}
