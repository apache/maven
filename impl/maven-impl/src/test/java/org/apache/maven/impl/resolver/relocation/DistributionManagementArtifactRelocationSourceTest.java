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
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link DistributionManagementArtifactRelocationSource}.
 */
class DistributionManagementArtifactRelocationSourceTest {

    private final DistributionManagementArtifactRelocationSource source =
            new DistributionManagementArtifactRelocationSource();

    @Test
    void validRelocationReturnsRelocatedArtifact() {
        Model model = Model.newBuilder()
                .distributionManagement(DistributionManagement.newBuilder()
                        .relocation(Relocation.newBuilder()
                                .groupId("org.apache.new")
                                .artifactId("new-artifact")
                                .version("2.0.0")
                                .build())
                        .build())
                .build();

        Artifact result = source.relocatedTarget(null, descriptorResult(), model);
        assertNotNull(result);
    }

    @Test
    void noRelocationReturnsNull() {
        Model model = Model.newBuilder().build();
        Artifact result = source.relocatedTarget(null, descriptorResult(), model);
        assertNull(result);
    }

    @Test
    void pathTraversalGroupIdThrows() {
        Model model = modelWithRelocation("..", "new-artifact", "1.0");
        assertThrows(IllegalArgumentException.class, () -> source.relocatedTarget(null, descriptorResult(), model));
    }

    @Test
    void pathTraversalArtifactIdThrows() {
        Model model = modelWithRelocation("org.apache", "../../../etc/passwd", "1.0");
        assertThrows(IllegalArgumentException.class, () -> source.relocatedTarget(null, descriptorResult(), model));
    }

    @Test
    void pathTraversalVersionThrows() {
        Model model = modelWithRelocation("org.apache", "artifact", "../../evil");
        assertThrows(IllegalArgumentException.class, () -> source.relocatedTarget(null, descriptorResult(), model));
    }

    @Test
    void colonInGroupIdThrows() {
        Model model = modelWithRelocation("C:", "artifact", "1.0");
        assertThrows(IllegalArgumentException.class, () -> source.relocatedTarget(null, descriptorResult(), model));
    }

    @Test
    void emptyRelocationFieldsAreAccepted() {
        // Empty fields mean "keep the original coordinate"
        Model model = modelWithRelocation("", "", "");
        Artifact result = source.relocatedTarget(null, descriptorResult(), model);
        assertNotNull(result);
    }

    private static Model modelWithRelocation(String groupId, String artifactId, String version) {
        return Model.newBuilder()
                .distributionManagement(DistributionManagement.newBuilder()
                        .relocation(Relocation.newBuilder()
                                .groupId(groupId)
                                .artifactId(artifactId)
                                .version(version)
                                .build())
                        .build())
                .build();
    }

    private static ArtifactDescriptorResult descriptorResult() {
        Artifact artifact = new DefaultArtifact("org.example", "old-artifact", "jar", "1.0");
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
        request.setArtifact(artifact);
        return new ArtifactDescriptorResult(request);
    }
}
