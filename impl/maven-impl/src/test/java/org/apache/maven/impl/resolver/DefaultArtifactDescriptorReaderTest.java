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
package org.apache.maven.impl.resolver;

import java.util.Collections;
import java.util.List;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.impl.InternalSession;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultArtifactDescriptorReaderTest {

    private VersionResolver versionResolver;
    private ArtifactResolver artifactResolver;
    private ModelBuilder modelBuilder;
    private RepositoryEventDispatcher repositoryEventDispatcher;
    private DefaultArtifactDescriptorReader descriptorReader;

    private DefaultRepositorySystemSession session;
    private InternalSession internalSession;
    private ArtifactTypeRegistry typeRegistry;

    @BeforeEach
    void setUp() throws Exception {
        versionResolver = mock(VersionResolver.class);
        artifactResolver = mock(ArtifactResolver.class);
        modelBuilder = mock(ModelBuilder.class);
        repositoryEventDispatcher = mock(RepositoryEventDispatcher.class);

        descriptorReader = new DefaultArtifactDescriptorReader(
                versionResolver, artifactResolver, modelBuilder, repositoryEventDispatcher, Collections.emptyMap());

        session = new DefaultRepositorySystemSession(h -> false);
        internalSession = mock(InternalSession.class);
        typeRegistry = mock(ArtifactTypeRegistry.class);

        session.setArtifactTypeRegistry(typeRegistry);
        when(internalSession.getSession()).thenReturn(session);
        InternalSession.associate(session, internalSession);

        when(versionResolver.resolveVersion(eq(session), any(VersionRequest.class)))
                .thenAnswer(inv -> {
                    VersionRequest req = inv.getArgument(1);
                    return new VersionResult(req).setVersion(req.getArtifact().getVersion());
                });
    }

    @Test
    void testWorkspaceSuppliesSyntheticModelWithoutPhysicalPom() throws Exception {
        Model syntheticModel = Model.newBuilder()
                .groupId("org.apache.maven.test")
                .artifactId("synthetic-artifact")
                .version("1.0.0")
                .packaging("jar")
                .dependencies(List.of(Dependency.newBuilder()
                        .groupId("org.apache.maven.test")
                        .artifactId("dep-artifact")
                        .version("1.0.0")
                        .scope("compile")
                        .build()))
                .build();

        MavenWorkspaceReader workspaceReader = mock(MavenWorkspaceReader.class);
        WorkspaceRepository workspaceRepo = new WorkspaceRepository("ide", getClass());
        when(workspaceReader.getRepository()).thenReturn(workspaceRepo);
        when(workspaceReader.findModel(any(Artifact.class))).thenAnswer(inv -> {
            Artifact artifact = inv.getArgument(0);
            if ("org.apache.maven.test".equals(artifact.getGroupId())
                    && "synthetic-artifact".equals(artifact.getArtifactId())) {
                return syntheticModel;
            }
            return null;
        });

        session.setWorkspaceReader(workspaceReader);

        Artifact artifact = new DefaultArtifact("org.apache.maven.test", "synthetic-artifact", "jar", "1.0.0");
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
        request.setArtifact(artifact);

        ArtifactDescriptorResult result = descriptorReader.readArtifactDescriptor(session, request);

        assertNotNull(result);
        assertSame(workspaceRepo, result.getRepository());
        assertEquals(1, result.getDependencies().size());
        assertEquals(
                "dep-artifact", result.getDependencies().get(0).getArtifact().getArtifactId());

        verify(artifactResolver, never()).resolveArtifact(any(), any());
    }

    @Test
    void testNormalResolutionWhenWorkspaceSuppliesNoModel() throws Exception {
        MavenWorkspaceReader workspaceReader = mock(MavenWorkspaceReader.class);
        WorkspaceRepository workspaceRepo = new WorkspaceRepository("ide", getClass());
        when(workspaceReader.getRepository()).thenReturn(workspaceRepo);
        when(workspaceReader.findModel(any(Artifact.class))).thenReturn(null);

        session.setWorkspaceReader(workspaceReader);

        Artifact artifact = new DefaultArtifact("org.apache.maven.test", "normal-artifact", "jar", "1.0.0");
        Artifact pomArtifact = new DefaultArtifact("org.apache.maven.test", "normal-artifact", "", "pom", "1.0.0");
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
        request.setArtifact(artifact);

        ArtifactResult missingPomResult = new ArtifactResult(new ArtifactRequest(pomArtifact, List.of(), ""));
        missingPomResult.addException(
                ArtifactResult.NO_REPOSITORY, new ArtifactNotFoundException(pomArtifact, "POM not found"));
        when(artifactResolver.resolveArtifact(eq(session), any(ArtifactRequest.class)))
                .thenThrow(new ArtifactResolutionException(List.of(missingPomResult)));

        assertThrows(
                ArtifactDescriptorException.class, () -> descriptorReader.readArtifactDescriptor(session, request));

        verify(artifactResolver).resolveArtifact(eq(session), any(ArtifactRequest.class));
    }
}
