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
import java.util.List;

import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultDependencyResolverTest {

    @Test
    void flattenKeepsSystemScopeOnCompileAndTestPaths() throws Exception {
        // main-runtime deliberately excludes system; main-runtimePlusSystem is a separate resolution scope.
        InternalSession session = mock(InternalSession.class);
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        RepositorySystemSession repositorySession = mock(RepositorySystemSession.class);
        when(session.getRepositorySystem()).thenReturn(repositorySystem);
        when(session.getSession()).thenReturn(repositorySession);

        DependencyNode root = scopedNode("root", "compile");
        List<DependencyNode> children = List.of(
                scopedNode("syslib", "system"),
                scopedNode("compilelib", "compile"),
                scopedNode("runtimelib", "runtime"));
        Node rootNode = new DefaultNode(session, root, false);
        when(repositorySystem.flattenDependencyNodes(eq(repositorySession), eq(root), any(DependencyFilter.class)))
                .thenAnswer(invocation -> {
                    DependencyFilter filter = invocation.getArgument(2);
                    List<DependencyNode> kept = new ArrayList<>();
                    for (DependencyNode child : children) {
                        if (filter.accept(child, List.of(root))) {
                            kept.add(child);
                        }
                    }
                    return kept;
                });
        List<String> scopes = new ArrayList<>();
        when(session.getNode(any(DependencyNode.class))).thenAnswer(invocation -> {
            DependencyNode dependencyNode = invocation.getArgument(0);
            scopes.add(dependencyNode.getDependency().getScope());
            return mock(Node.class);
        });

        DefaultDependencyResolver resolver = new DefaultDependencyResolver();
        assertEquals(
                List.of("system", "compile"),
                flattenedScopes(resolver, session, rootNode, PathScope.MAIN_COMPILE, scopes));
        assertEquals(
                List.of("system", "compile"),
                flattenedScopes(resolver, session, rootNode, PathScope.TEST_COMPILE, scopes));
        assertEquals(
                List.of("system", "compile", "runtime"),
                flattenedScopes(resolver, session, rootNode, PathScope.TEST_RUNTIME, scopes));
        assertEquals(
                List.of("compile", "runtime"),
                flattenedScopes(resolver, session, rootNode, PathScope.MAIN_RUNTIME, scopes));
    }

    private static List<String> flattenedScopes(
            DefaultDependencyResolver resolver,
            InternalSession session,
            Node rootNode,
            PathScope pathScope,
            List<String> scopes)
            throws Exception {
        scopes.clear();
        resolver.flatten(session, rootNode, pathScope);
        return List.copyOf(scopes);
    }

    private static DependencyNode scopedNode(String artifactId, String scope) {
        return new DefaultDependencyNode(
                new Dependency(new DefaultArtifact("org.example:" + artifactId + ":jar:1"), scope));
    }
}
