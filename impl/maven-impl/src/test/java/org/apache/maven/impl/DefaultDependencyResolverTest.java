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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.Project;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.api.services.RequestTrace;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultDependencyResolverTest {

    @Test
    void flattenKeepsSystemScopeDependenciesOnCompileAndTestClasspaths() throws Exception {
        InternalSession session = mock(InternalSession.class);
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        RepositorySystemSession repositorySession = mock(RepositorySystemSession.class);

        Dependency rootDependency =
                new Dependency(new DefaultArtifact("org.example", "root", "", "jar", "1.0"), "compile");
        DefaultDependencyNode root = new DefaultDependencyNode(rootDependency);
        Dependency systemDependency = new Dependency(new DefaultArtifact("t", "syslib", "", "jar", "1"), "system");
        DefaultDependencyNode systemNode = new DefaultDependencyNode(systemDependency);
        Dependency compileDependency =
                new Dependency(new DefaultArtifact("org.example", "lib", "", "jar", "1"), "compile");
        DefaultDependencyNode compileNode = new DefaultDependencyNode(compileDependency);
        Dependency runtimeDependency =
                new Dependency(new DefaultArtifact("org.example", "rt", "", "jar", "1"), "runtime");
        DefaultDependencyNode runtimeNode = new DefaultDependencyNode(runtimeDependency);

        AbstractNode rootNode = mock(AbstractNode.class);
        when(rootNode.getDependencyNode()).thenReturn(root);

        when(session.getRepositorySystem()).thenReturn(repositorySystem);
        when(session.getSession()).thenReturn(repositorySession);
        when(repositorySystem.flattenDependencyNodes(eq(repositorySession), eq(root), any(DependencyFilter.class)))
                .thenAnswer(invocation -> {
                    DependencyFilter filter = invocation.getArgument(2);
                    List<DependencyNode> flattened = new ArrayList<>();
                    flattened.add(root);
                    for (DependencyNode child : List.of(systemNode, compileNode, runtimeNode)) {
                        if (filter.accept(child, List.of(root))) {
                            flattened.add(child);
                        }
                    }
                    return flattened;
                });
        Map<DependencyNode, Node> nodes = new HashMap<>();
        when(session.getNode(any())).thenAnswer(invocation -> {
            DependencyNode dependencyNode = invocation.getArgument(0);
            return nodes.computeIfAbsent(dependencyNode, node -> mock(Node.class));
        });

        DefaultDependencyResolver resolver = new DefaultDependencyResolver();

        // system-scope dependencies belong on the main compile, test compile and test runtime
        // classpaths, like provided-scope ones (see Maven4ScopeManagerConfiguration)
        List<Node> mainCompile = resolver.flatten(session, rootNode, PathScope.MAIN_COMPILE);
        assertTrue(mainCompile.contains(nodes.get(systemNode)));
        assertTrue(mainCompile.contains(nodes.get(compileNode)));
        assertFalse(mainCompile.contains(nodes.get(runtimeNode)));

        List<Node> testCompile = resolver.flatten(session, rootNode, PathScope.TEST_COMPILE);
        assertTrue(testCompile.contains(nodes.get(systemNode)));

        List<Node> testRuntime = resolver.flatten(session, rootNode, PathScope.TEST_RUNTIME);
        assertTrue(testRuntime.contains(nodes.get(systemNode)));

        // ... but not on the main runtime classpath
        List<Node> mainRuntime = resolver.flatten(session, rootNode, PathScope.MAIN_RUNTIME);
        assertFalse(mainRuntime.contains(nodes.get(systemNode)));
    }

    @Test
    void collectAppliesProjectManagementToCallerSuppliedDependencies() throws Exception {
        InternalSession session = mock(InternalSession.class);
        trackTrace(session, null);
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

        when(session.request(any(DependencyResolverRequest.class), any())).thenAnswer(invocation -> {
            DependencyResolverRequest req = invocation.getArgument(0);
            Function<DependencyResolverRequest, DependencyResolverResult> work = invocation.getArgument(1);
            return work.apply(req);
        });
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
    void collectWithProjectButNoCallerDepsSkipsManagementApplication() throws Exception {
        // When a Project is present but request.getDependencies() is empty, the guard
        // `request.getProject().isPresent() && !request.getDependencies().isEmpty()` is false,
        // so applyDependencyManagement must NOT be called: the project's own deps go through
        // as-is (no managed version/scope/exclusion merge).
        InternalSession session = mock(InternalSession.class);
        trackTrace(session, null);
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        RepositorySystemSession repositorySession = mock(RepositorySystemSession.class);
        DependencyResolverRequest request = mock(DependencyResolverRequest.class);
        Project project = mock(Project.class);
        DependencyCoordinates projectDependency = mock(DependencyCoordinates.class);
        DependencyCoordinates managedDependency = mock(DependencyCoordinates.class);
        List<DependencyCoordinates> projectDependencies = List.of(projectDependency);
        List<DependencyCoordinates> managedDependencies = List.of(managedDependency);
        // The resolver-level dependency has no version — management would set "1.2.3", but must NOT
        Dependency resolverProjectDependency =
                new Dependency(new DefaultArtifact("org.example", "processor", "", "jar", ""), "", false);
        Dependency resolverManagedDependency =
                new Dependency(new DefaultArtifact("org.example:processor:jar:1.2.3"), "runtime");

        when(session.request(any(DependencyResolverRequest.class), any())).thenAnswer(invocation -> {
            DependencyResolverRequest req = invocation.getArgument(0);
            Function<DependencyResolverRequest, DependencyResolverResult> work = invocation.getArgument(1);
            return work.apply(req);
        });
        when(request.getSession()).thenReturn(session);
        when(request.getProject()).thenReturn(Optional.of(project));
        when(request.getDependencies()).thenReturn(List.of()); // no caller-supplied deps
        when(request.getRepositories()).thenReturn(List.of());
        when(project.getDependencies()).thenReturn(projectDependencies);
        when(project.getManagedDependencies()).thenReturn(managedDependencies);
        when(session.toDependencies(projectDependencies, false)).thenReturn(List.of(resolverProjectDependency));
        when(session.toDependencies(managedDependencies, true)).thenReturn(List.of(resolverManagedDependency));
        when(session.toResolvingRepositories(List.of())).thenReturn(List.of());
        when(session.getRepositorySystem()).thenReturn(repositorySystem);
        when(session.getSession()).thenReturn(repositorySession);
        when(repositorySystem.collectDependencies(eq(repositorySession), any(CollectRequest.class)))
                .thenAnswer(invocation -> new CollectResult(invocation.getArgument(1))
                        .setRoot(new DefaultDependencyNode(resolverProjectDependency)));
        when(session.getNode(any(), eq(false))).thenReturn(mock(Node.class));

        new DefaultDependencyResolver().collect(request);

        ArgumentCaptor<CollectRequest> requestCaptor = ArgumentCaptor.forClass(CollectRequest.class);
        verify(repositorySystem).collectDependencies(eq(repositorySession), requestCaptor.capture());
        Dependency collectedDependency =
                requestCaptor.getValue().getDependencies().get(0);
        // version must remain empty — management was NOT applied
        assertEquals("", collectedDependency.getArtifact().getVersion());
        assertEquals("", collectedDependency.getScope());
        // project's own dependencies were used (not caller-supplied, since there were none)
        verify(session).toDependencies(projectDependencies, false);
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

    private static void trackTrace(InternalSession session, RequestTrace initial) {
        AtomicReference<RequestTrace> current = new AtomicReference<>(initial);
        when(session.getCurrentTrace()).thenAnswer(invocation -> current.get());
        doAnswer(invocation -> {
                    current.set(invocation.getArgument(0));
                    return null;
                })
                .when(session)
                .setCurrentTrace(any());
    }
}
