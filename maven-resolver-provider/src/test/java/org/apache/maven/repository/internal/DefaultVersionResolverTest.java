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
package org.apache.maven.repository.internal;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResult;

public class DefaultVersionResolverTest extends AbstractRepositoryTestCase {
    private DefaultVersionResolver versionResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // be sure we're testing the right class, i.e. DefaultVersionResolver.class
        versionResolver = (DefaultVersionResolver) lookup(VersionResolver.class, "default");
    }

    @Override
    protected void tearDown() throws Exception {
        versionResolver = null;
        super.tearDown();
    }

    public void testResolveSeparateInstalledClassifiedNonUniqueVersionedArtifacts() throws Exception {
        VersionRequest requestB = new VersionRequest();
        requestB.addRepository(newTestRepository());
        Artifact artifactB =
                new DefaultArtifact("org.apache.maven.its", "dep-mng5324", "classifierB", "jar", "07.20.3-SNAPSHOT");
        requestB.setArtifact(artifactB);

        VersionResult resultB = versionResolver.resolveVersion(session, requestB);
        assertEquals("07.20.3-20120809.112920-97", resultB.getVersion());

        VersionRequest requestA = new VersionRequest();
        requestA.addRepository(newTestRepository());

        Artifact artifactA =
                new DefaultArtifact("org.apache.maven.its", "dep-mng5324", "classifierA", "jar", "07.20.3-SNAPSHOT");
        requestA.setArtifact(artifactA);

        VersionResult resultA = versionResolver.resolveVersion(session, requestA);
        assertEquals("07.20.3-20120809.112124-88", resultA.getVersion());
    }

    public void testResolveSeparateInstalledClassifiedNonVersionedArtifacts() throws Exception {
        VersionRequest requestA = new VersionRequest();
        requestA.addRepository(newTestRepository());
        String versionA = "07.20.3-20120809.112124-88";
        Artifact artifactA = new DefaultArtifact("org.apache.maven.its", "dep-mng5324", "classifierA", "jar", versionA);
        requestA.setArtifact(artifactA);

        VersionResult resultA = versionResolver.resolveVersion(session, requestA);
        assertEquals(versionA, resultA.getVersion());

        VersionRequest requestB = new VersionRequest();
        requestB.addRepository(newTestRepository());
        String versionB = "07.20.3-20120809.112920-97";
        Artifact artifactB = new DefaultArtifact("org.apache.maven.its", "dep-mng5324", "classifierB", "jar", versionB);
        requestB.setArtifact(artifactB);

        VersionResult resultB = versionResolver.resolveVersion(session, requestB);
        assertEquals(versionB, resultB.getVersion());
    }
}
