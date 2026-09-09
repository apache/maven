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

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultVersionRangeResolverTest extends AbstractRepositoryTestCase {
    @Inject
    private DefaultVersionRangeResolver versionRangeResolver;

    @Test
    void testRangeResolutionWithInvalidTokenInMetadataIsRejected() throws Exception {
        VersionRangeRequest request = new VersionRangeRequest();
        request.addRepository(newTestRepository());
        request.setArtifact(new DefaultArtifact("org.apache.maven.its", "dep-invalid-range", "jar", "[1.0,2.0]"));

        VersionRangeResult result = versionRangeResolver.resolveVersionRange(session, request);

        // The metadata carries a versions[] entry that is not a valid coordinate component, so the whole
        // document is treated as invalid and none of its versions (including the otherwise-valid 1.0 and 2.0)
        // are offered as candidates for the range.
        assertTrue(result.getVersions().isEmpty(), "expected no versions, got " + result.getVersions());
    }
}
