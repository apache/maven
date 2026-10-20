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
package org.apache.maven.impl;

import java.util.List;
import java.util.Optional;

import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultDependencyResolverTest {

    @Test
    void collectAppliesProjectManagementToCallerSuppliedDependencies() throws Exception {
        InternalSession session = mock(InternalSession.class);
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        RepositorySystemSession repositorySession = mock(RepositorySystemSession.class);
        DependencyResolverRequest request = mock(DependencyResolverRequest.class);
        Project project = mock(Project.class);
        DependencyCoordinates projectDependency = mock(DependencyCoordinates.class);
        DependencyCoordinates callerDependency = mock(DependencyCoordinates.class);
        DependencyCoordinates managedDependency = mock(DependencyCoordinates.class);
        List<DependencyCoordinates> projectDependencies = List.of(projectDependency);
        List<DependencyCoordinates> callerDependencies = List.of(callerDependency);
        List<DependencyCoordinates> managedDependencies = List.of(managedDependency);
        Dependency resolverDependency =
                new Dependency(new DefaultArtifact("org.example", "processor", "", "jar", ""), "", false);
        Exclusion exclusion = new Exclusion("org.example", "excluded", "*", "*");
        Dependency resolverManagedDependency = new Dependency(
                new DefaultArtifact("org.example:processor:jar:1.2.3"), "runtime", false, List.of(exclusion));

        when(request.getSession()).thenReturn(session);
        when(request.getProject()).thenReturn(Optional.of(project));
        when(request.getDependencies()).thenReturn(callerDependencies);
        when(request.getRepositories()).thenReturn(List.of());
        when(project.getDependencies()).thenReturn(projectDependencies);
        when(project.getManagedDependencies()).thenReturn(managedDependencies);
        when(session.toDependencies(callerDependencies, false)).thenReturn(List.of(resolverDependency));
        when(session.toDependencies(managedDependencies, true)).thenReturn(List.of(resolverManagedDependency));
        when(session.toResolvingRepositories(List.of())).thenReturn(List.of());
        when(session.getRepositorySystem()).thenReturn(repositorySystem);
        when(session.getSession()).thenReturn(repositorySession);
        when(repositorySystem.collectDependencies(eq(repositorySession), any(CollectRequest.class)))
                .thenAnswer(invocation -> new CollectResult(invocation.getArgument(1))
                        .setRoot(new DefaultDependencyNode(resolverDependency)));
        when(session.getNode(any(), eq(false))).thenReturn(mock(Node.class));

        new DefaultDependencyResolver().collect(request);

        ArgumentCaptor<CollectRequest> requestCaptor = ArgumentCaptor.forClass(CollectRequest.class);
        verify(repositorySystem).collectDependencies(eq(repositorySession), requestCaptor.capture());
        Dependency collectedDependency =
                requestCaptor.getValue().getDependencies().get(0);
        assertEquals("1.2.3", collectedDependency.getArtifact().getVersion());
        assertEquals("runtime", collectedDependency.getScope());
        assertIterableEquals(List.of(exclusion), collectedDependency.getExclusions());
        verify(session).toDependencies(callerDependencies, false);
        verify(session, never()).toDependencies(projectDependencies, false);
    }

    @Test
    void appliesProjectManagementToPluginDependency() {
        Dependency dependency =
                new Dependency(new DefaultArtifact("org.example", "processor", "", "jar", ""), "", false);
        Exclusion exclusion = new Exclusion("org.example", "excluded", "*", "*");
        Dependency managed = new Dependency(
                new DefaultArtifact("org.example:processor:jar:1.2.3"), "runtime", false, List.of(exclusion));

        Dependency result = DefaultDependencyResolver.applyDependencyManagement(List.of(dependency), List.of(managed))
                .get(0);

        assertEquals("1.2.3", result.getArtifact().getVersion());
        assertEquals("runtime", result.getScope());
        assertIterableEquals(List.of(exclusion), result.getExclusions());
    }

    @Test
    void keepsValuesDeclaredByPluginDependency() {
        Exclusion declaredExclusion = new Exclusion("org.example", "declared", "*", "*");
        Dependency dependency = new Dependency(
                new DefaultArtifact("org.example:processor:jar:2.0.0"), "test", false, List.of(declaredExclusion));
        Exclusion managedExclusion = new Exclusion("org.example", "managed", "*", "*");
        Dependency managed = new Dependency(
                new DefaultArtifact("org.example:processor:jar:1.2.3"), "runtime", false, List.of(managedExclusion));

        Dependency result = DefaultDependencyResolver.applyDependencyManagement(List.of(dependency), List.of(managed))
                .get(0);

        assertEquals("2.0.0", result.getArtifact().getVersion());
        assertEquals("test", result.getScope());
        assertIterableEquals(List.of(declaredExclusion), result.getExclusions());
    }

    @Test
    void applyingDependencyManagementToEmptyDependenciesIsNoOp() {
        Dependency managed = new Dependency(new DefaultArtifact("org.example:processor:jar:1.2.3"), "runtime");

        assertTrue(DefaultDependencyResolver.applyDependencyManagement(List.of(), List.of(managed))
                .isEmpty());
    }

    @Test
    void appliesDependencyManagementOnlyToMatchingDependencies() {
        Dependency matching = new Dependency(new DefaultArtifact("org.example", "processor", "", "jar", ""), "", false);
        Dependency unmatched = new Dependency(new DefaultArtifact("org.example:standalone:jar:2.0.0"), "compile");
        Dependency managed = new Dependency(new DefaultArtifact("org.example:processor:jar:1.2.3"), "runtime");

        List<Dependency> result =
                DefaultDependencyResolver.applyDependencyManagement(List.of(matching, unmatched), List.of(managed));

        assertEquals("1.2.3", result.get(0).getArtifact().getVersion());
        assertSame(unmatched, result.get(1));
    }
}
