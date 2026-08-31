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
package org.apache.maven.project.artifact;

import java.util.Collections;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Relocation;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.legacy.metadata.DefaultMetadataResolutionRequest;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that a relocation read from a resolved project's distribution management is validated before its
 * components are applied to the artifact and project being resolved, exercising the real
 * {@link MavenMetadataSource#retrieve(MetadataResolutionRequest)} code path with mocked collaborators (no
 * on-disk artifact resolution, so this test does not depend on, or share, any module-level local repository).
 */
class MavenMetadataSourceRelocationTest {

    private MavenMetadataSource newSource(ProjectBuilder projectBuilder) {
        ArtifactFactory artifactFactory = mock(ArtifactFactory.class);
        when(artifactFactory.createProjectArtifact(any(), any(), any(), any()))
                .thenAnswer(invocation -> new DefaultArtifact(
                        (String) invocation.getArgument(0),
                        (String) invocation.getArgument(1),
                        (String) invocation.getArgument(2),
                        (String) invocation.getArgument(3),
                        "pom",
                        null,
                        new DefaultArtifactHandler("pom")));

        LegacySupport legacySupport = mock(LegacySupport.class);
        RepositorySystemSession repositorySession = mock(RepositorySystemSession.class);
        when(legacySupport.getRepositorySession()).thenReturn(repositorySession);
        when(legacySupport.getSession()).thenReturn(null);

        return new MavenMetadataSource(
                mock(RepositoryMetadataManager.class),
                artifactFactory,
                projectBuilder,
                mock(MavenMetadataCache.class),
                legacySupport,
                mock(MavenRepositorySystem.class));
    }

    private static Artifact newArtifact(String groupId, String artifactId, String version) {
        return new DefaultArtifact(
                groupId, artifactId, version, Artifact.SCOPE_COMPILE, "pom", null, new DefaultArtifactHandler("pom"));
    }

    private static MavenProject newProject(String groupId, String artifactId, String version, Relocation relocation) {
        MavenProject project = new MavenProject();
        project.setGroupId(groupId);
        project.setArtifactId(artifactId);
        project.setVersion(version);
        if (relocation != null) {
            DistributionManagement distMgmt = new DistributionManagement();
            distMgmt.setRelocation(relocation);
            project.setDistributionManagement(distMgmt);
        }
        return project;
    }

    private static MetadataResolutionRequest newRequest(Artifact artifact) {
        MetadataResolutionRequest request = new DefaultMetadataResolutionRequest();
        request.setArtifact(artifact);
        request.setLocalRepository(mock(ArtifactRepository.class));
        request.setRemoteRepositories(Collections.emptyList());
        return request;
    }

    @Test
    void testRelocationInvalidArtifactIdIsRejected() throws Exception {
        Relocation relocation = new Relocation();
        relocation.setArtifactId("a/b");

        MavenProject relocatingProject = newProject("group", "original", "1.0", relocation);
        MavenProject finalProject = newProject("group", "a/b", "1.0", null);

        ProjectBuildingResult first = mock(ProjectBuildingResult.class);
        when(first.getProject()).thenReturn(relocatingProject);
        ProjectBuildingResult second = mock(ProjectBuildingResult.class);
        when(second.getProject()).thenReturn(finalProject);

        ProjectBuilder projectBuilder = mock(ProjectBuilder.class);
        when(projectBuilder.build(any(Artifact.class), any(ProjectBuildingRequest.class)))
                .thenReturn(first, second);

        MavenMetadataSource source = newSource(projectBuilder);
        Artifact artifact = newArtifact("group", "original", "1.0");
        MetadataResolutionRequest request = newRequest(artifact);

        ArtifactMetadataRetrievalException exception =
                assertThrows(ArtifactMetadataRetrievalException.class, () -> source.retrieve(request));
        assertTrue(exception.getMessage().contains("a/b"));
        assertTrue(exception.getMessage().contains("artifactId"));
    }

    @Test
    void testWellFormedRelocationIsApplied() throws Exception {
        Relocation relocation = new Relocation();
        relocation.setGroupId("group.moved");
        relocation.setArtifactId("artifact-moved");
        relocation.setVersion("2.0");

        MavenProject relocatingProject = newProject("group", "original", "1.0", relocation);
        MavenProject finalProject = newProject("group.moved", "artifact-moved", "2.0", null);

        ProjectBuildingResult first = mock(ProjectBuildingResult.class);
        when(first.getProject()).thenReturn(relocatingProject);
        ProjectBuildingResult second = mock(ProjectBuildingResult.class);
        when(second.getProject()).thenReturn(finalProject);

        ProjectBuilder projectBuilder = mock(ProjectBuilder.class);
        when(projectBuilder.build(any(Artifact.class), any(ProjectBuildingRequest.class)))
                .thenReturn(first, second);

        MavenMetadataSource source = newSource(projectBuilder);
        Artifact artifact = newArtifact("group", "original", "1.0");
        MetadataResolutionRequest request = newRequest(artifact);

        ResolutionGroup result = source.retrieve(request);

        assertEquals("group.moved", artifact.getGroupId());
        assertEquals("artifact-moved", artifact.getArtifactId());
        assertEquals("2.0", artifact.getVersion());
        assertEquals(artifact, result.getRelocatedArtifact());
    }
}
