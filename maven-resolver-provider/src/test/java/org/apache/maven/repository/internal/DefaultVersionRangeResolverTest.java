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

import javax.inject.Inject;

import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PlexusTest
public class DefaultVersionRangeResolverTest extends AbstractRepositoryTest {

    @Inject
    private VersionRangeResolver versionRangeResolver;

    @Test
    public void testVersionsListFromMetadataWithInvalidTokenIsRejected() throws Exception {
        VersionRangeRequest request = new VersionRangeRequest();
        request.addRepository(newTestRepository());
        Artifact artifact = new DefaultArtifact("org.apache.maven.its", "dep-invalid-versions", "jar", "[1.0,)");
        request.setArtifact(artifact);

        VersionRangeResult result = versionRangeResolver.resolveVersionRange(session, request);

        // The metadata carries a version token that is not a valid coordinate component, so the whole
        // metadata file is treated as invalid and none of its versions (valid or not) are offered.
        assertTrue(result.getVersions().isEmpty());
        assertFalse(result.getVersions().stream().anyMatch(v -> v.toString().contains("1.0:2.0")));
    }
}
