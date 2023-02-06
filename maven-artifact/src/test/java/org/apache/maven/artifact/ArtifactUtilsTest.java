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

import junit.framework.TestCase;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * Tests {@link ArtifactUtils}.
 *
 * @author Benjamin Bentmann
 */
public class ArtifactUtilsTest extends TestCase {

    private Artifact newArtifact(String aid) {
        return new DefaultArtifact("group", aid, VersionRange.createFromVersion("1.0"), "test", "jar", "tests", null);
    }

    public void testIsSnapshot() {
        assertEquals(false, ArtifactUtils.isSnapshot(null));
        assertEquals(false, ArtifactUtils.isSnapshot(""));
        assertEquals(false, ArtifactUtils.isSnapshot("1.2.3"));
        assertEquals(true, ArtifactUtils.isSnapshot("1.2.3-SNAPSHOT"));
        assertEquals(true, ArtifactUtils.isSnapshot("1.2.3-snapshot"));
        assertEquals(true, ArtifactUtils.isSnapshot("1.2.3-20090413.094722-2"));
        assertEquals(false, ArtifactUtils.isSnapshot("1.2.3-20090413X094722-2"));
    }

    public void testToSnapshotVersion() {
        assertEquals("1.2.3", ArtifactUtils.toSnapshotVersion("1.2.3"));
        assertEquals("1.2.3-SNAPSHOT", ArtifactUtils.toSnapshotVersion("1.2.3-SNAPSHOT"));
        assertEquals("1.2.3-SNAPSHOT", ArtifactUtils.toSnapshotVersion("1.2.3-20090413.094722-2"));
        assertEquals("1.2.3-20090413X094722-2", ArtifactUtils.toSnapshotVersion("1.2.3-20090413X094722-2"));
    }

    /**
     * Tests that the ordering of the map resembles the ordering of the input collection of artifacts.
     */
    public void testArtifactMapByVersionlessIdOrdering() throws Exception {
        List<Artifact> list = new ArrayList<>();
        list.add(newArtifact("b"));
        list.add(newArtifact("a"));
        list.add(newArtifact("c"));
        list.add(newArtifact("e"));
        list.add(newArtifact("d"));

        Map<String, Artifact> map = ArtifactUtils.artifactMapByVersionlessId(list);
        assertNotNull(map);
        assertEquals(list, new ArrayList<>(map.values()));
    }
}
