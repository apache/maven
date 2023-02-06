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
package org.apache.maven.artifact.transform;

import java.util.List;

import org.apache.maven.repository.legacy.resolver.transform.ArtifactTransformationManager;
import org.apache.maven.repository.legacy.resolver.transform.LatestArtifactTransformation;
import org.apache.maven.repository.legacy.resolver.transform.ReleaseArtifactTransformation;
import org.apache.maven.repository.legacy.resolver.transform.SnapshotTransformation;
import org.codehaus.plexus.PlexusTestCase;

/** @author Jason van Zyl */
public class TransformationManagerTest extends PlexusTestCase {
    public void testTransformationManager() throws Exception {
        ArtifactTransformationManager tm = lookup(ArtifactTransformationManager.class);

        List tms = tm.getArtifactTransformations();

        assertEquals(3, tms.size());

        assertTrue(
                "We expected the release transformation and got " + tms.get(0),
                tms.get(0) instanceof ReleaseArtifactTransformation);

        assertTrue(
                "We expected the latest transformation and got " + tms.get(1),
                tms.get(1) instanceof LatestArtifactTransformation);

        assertTrue(
                "We expected the snapshot transformation and got " + tms.get(2),
                tms.get(2) instanceof SnapshotTransformation);
    }
}
