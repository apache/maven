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
package org.apache.maven.impl.resolver.relocation;

import org.apache.maven.api.model.DistributionManagement;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Relocation;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test cases for {@code DistributionManagementArtifactRelocationSource} coordinate handling.
 */
class DistributionManagementArtifactRelocationSourceTest {

    private final DistributionManagementArtifactRelocationSource source =
            new DistributionManagementArtifactRelocationSource();

    private static ArtifactDescriptorResult newResult() {
        final ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
        request.setArtifact(new DefaultArtifact("ut.simple:artifact:1.0"));
        return new ArtifactDescriptorResult(request);
    }

    private static Model newModel(String groupId, String artifactId, String version) {
        final Relocation relocation = Relocation.newBuilder()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .build();

        final DistributionManagement distMgmt =
                DistributionManagement.newBuilder().relocation(relocation).build();

        return Model.newBuilder().distributionManagement(distMgmt).build();
    }

    @Test
    void testWellFormedRelocationIsApplied() throws Exception {
        final Artifact relocated = source.relocatedTarget(null, newResult(), newModel("ut.moved", "artifact", "2.0"));

        assertNotNull(relocated);
        assertEquals("ut.moved", relocated.getGroupId());
        assertEquals("artifact", relocated.getArtifactId());
        assertEquals("2.0", relocated.getVersion());
    }

    @Test
    void testRelocationGroupIdWithBackslashIsRejected() {
        assertThrows(
                ArtifactDescriptorException.class,
                () -> source.relocatedTarget(null, newResult(), newModel("a\\b", null, null)));
    }

    @Test
    void testRelocationWithInvalidArtifactIdIsRejected() {
        assertThrows(
                ArtifactDescriptorException.class,
                () -> source.relocatedTarget(null, newResult(), newModel(null, "a/b", null)));
    }

    @Test
    void testRelocationArtifactIdWithControlCharacterIsRejected() {
        assertThrows(
                ArtifactDescriptorException.class,
                () -> source.relocatedTarget(null, newResult(), newModel(null, "a\nb", null)));
    }

    @Test
    void testRelocationWithInvalidVersionIsRejected() {
        assertThrows(
                ArtifactDescriptorException.class,
                () -> source.relocatedTarget(null, newResult(), newModel(null, null, "1.0:2.0")));
    }

    @Test
    void testVersionWithTrailingDotsIsAccepted() throws Exception {
        // only the exact ".." token is rejected; "1.." is an unusual but valid version string
        final Artifact relocated = source.relocatedTarget(null, newResult(), newModel(null, null, "1.."));

        assertNotNull(relocated);
        assertEquals("1..", relocated.getVersion());
    }
}
