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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultDependencyResolverTest {

    @Test
    void isTestArtifact() {
        assertFalse(DefaultDependencyResolver.isTestArtifact(null));

        DefaultArtifact standardJar = new DefaultArtifact("g", "a", null, "jar", "1.0");
        assertFalse(DefaultDependencyResolver.isTestArtifact(standardJar));

        DefaultArtifact typeTestJar = new DefaultArtifact(
                "g",
                "a",
                null,
                "jar",
                "1.0",
                Collections.singletonMap(ArtifactProperties.TYPE, "test-jar"),
                (java.nio.file.Path) null);
        assertTrue(DefaultDependencyResolver.isTestArtifact(typeTestJar));

        DefaultArtifact classifierTests = new DefaultArtifact("g", "a", "tests", "jar", "1.0");
        assertTrue(DefaultDependencyResolver.isTestArtifact(classifierTests));

        DefaultArtifact classifierTestsNonJar = new DefaultArtifact("g", "a", "tests", "war", "1.0");
        assertFalse(DefaultDependencyResolver.isTestArtifact(classifierTestsNonJar));
    }

    @Test
    void isTestSkipPrecedence() {
        InternalSession session = mock(InternalSession.class);
        RepositorySystemSession repoSession = mock(RepositorySystemSession.class);
        when(session.getSession()).thenReturn(repoSession);

        // Precedence: session userProperties > systemProperties
        when(session.getUserProperties()).thenReturn(Map.of("maven.test.skip", "false"));
        when(session.getSystemProperties()).thenReturn(Map.of("maven.test.skip", "true"));
        assertFalse(DefaultDependencyResolver.isTestSkip(session, null));

        // Precedence: repoSession userProperties > systemProperties > configProperties
        when(session.getUserProperties()).thenReturn(Map.of());
        when(session.getSystemProperties()).thenReturn(Map.of());
        when(repoSession.getUserProperties()).thenReturn(Map.of("maven.test.skip", "false"));
        when(repoSession.getSystemProperties()).thenReturn(Map.of("maven.test.skip", "true"));
        when(repoSession.getConfigProperties()).thenReturn(Map.of("maven.test.skip", "true"));
        assertFalse(DefaultDependencyResolver.isTestSkip(session, null));

        when(repoSession.getUserProperties()).thenReturn(Map.of());
        when(repoSession.getSystemProperties()).thenReturn(Map.of("maven.test.skip", "false"));
        when(repoSession.getConfigProperties()).thenReturn(Map.of("maven.test.skip", "true"));
        assertFalse(DefaultDependencyResolver.isTestSkip(session, null));
    }

    @Test
    void isTestSkipHandlesExceptionsGracefully() {
        InternalSession session = mock(InternalSession.class);
        when(session.getUserProperties()).thenThrow(new RuntimeException("Simulated exception"));
        when(session.getSession()).thenThrow(new RuntimeException("Simulated exception"));

        Project project = mock(Project.class);
        when(project.getModel()).thenThrow(new RuntimeException("Simulated exception"));

        assertFalse(DefaultDependencyResolver.isTestSkip(session, project));
    }

    @Test
    void flattenExcludesTestScopedTestJarWhenTestSkipInUserProperties() {
        DefaultDependencyResolver resolver = new DefaultDependencyResolver();

        InternalSession session = mock(InternalSession.class);
        RepositorySystem repoSystem = mock(RepositorySystem.class);
        RepositorySystemSession repoSession = mock(RepositorySystemSession.class);

        when(session.getSession()).thenReturn(repoSession);
        when(session.getRepositorySystem()).thenReturn(repoSystem);
        when(session.getUserProperties()).thenReturn(Map.of("maven.test.skip", "true"));

        DependencyNode aetherRoot = new DefaultDependencyNode(new DefaultArtifact("g", "root", null, "pom", "1.0"));
        Node rootNode = mock(AbstractNode.class);
        when(((AbstractNode) rootNode).getDependencyNode()).thenReturn(aetherRoot);

        when(repoSystem.flattenDependencyNodes(eq(repoSession), eq(aetherRoot), any()))
                .thenReturn(new java.util.ArrayList<>(List.of(aetherRoot)));

        resolver.flatten(session, rootNode, null);

        ArgumentCaptor<DependencyFilter> filterCaptor = ArgumentCaptor.forClass(DependencyFilter.class);
        verify(repoSystem).flattenDependencyNodes(eq(repoSession), eq(aetherRoot), filterCaptor.capture());

        DependencyFilter capturedFilter = filterCaptor.getValue();
        assertNotNull(capturedFilter);

        // Test-scoped test-jar must be excluded
        DefaultArtifact testJar = new DefaultArtifact(
                "g",
                "a",
                null,
                "jar",
                "1.0",
                Collections.singletonMap(ArtifactProperties.TYPE, "test-jar"),
                (java.nio.file.Path) null);
        DependencyNode testNode = new DefaultDependencyNode(new Dependency(testJar, "test"));
        assertFalse(capturedFilter.accept(testNode, Collections.emptyList()));

        // Compile-scoped test-jar must be accepted
        DependencyNode compileNode = new DefaultDependencyNode(new Dependency(testJar, "compile"));
        assertTrue(capturedFilter.accept(compileNode, Collections.emptyList()));

        // Test-scoped normal jar must be accepted
        DefaultArtifact standardJar = new DefaultArtifact("g", "junit", null, "jar", "5.0");
        DependencyNode standardNode = new DefaultDependencyNode(new Dependency(standardJar, "test"));
        assertTrue(capturedFilter.accept(standardNode, Collections.emptyList()));
    }

    @Test
    void flattenIncludesTestScopedTestJarWhenTestSkipNotSet() {
        DefaultDependencyResolver resolver = new DefaultDependencyResolver();

        InternalSession session = mock(InternalSession.class);
        RepositorySystem repoSystem = mock(RepositorySystem.class);
        RepositorySystemSession repoSession = mock(RepositorySystemSession.class);

        when(session.getSession()).thenReturn(repoSession);
        when(session.getRepositorySystem()).thenReturn(repoSystem);
        when(session.getUserProperties()).thenReturn(Map.of());
        when(session.getSystemProperties()).thenReturn(Map.of());
        when(repoSession.getConfigProperties()).thenReturn(Map.of());
        when(repoSession.getUserProperties()).thenReturn(Map.of());
        when(repoSession.getSystemProperties()).thenReturn(Map.of());

        DependencyNode aetherRoot = new DefaultDependencyNode(new DefaultArtifact("g", "root", null, "pom", "1.0"));
        Node rootNode = mock(AbstractNode.class);
        when(((AbstractNode) rootNode).getDependencyNode()).thenReturn(aetherRoot);

        when(repoSystem.flattenDependencyNodes(eq(repoSession), eq(aetherRoot), any()))
                .thenReturn(new java.util.ArrayList<>(List.of(aetherRoot)));

        resolver.flatten(session, rootNode, null);

        ArgumentCaptor<DependencyFilter> filterCaptor = ArgumentCaptor.forClass(DependencyFilter.class);
        verify(repoSystem).flattenDependencyNodes(eq(repoSession), eq(aetherRoot), filterCaptor.capture());

        // When scope is null and maven.test.skip is not set, filter must be null
        assertNull(filterCaptor.getValue());
    }

    @Test
    void flattenPropagatesProjectPropertiesFromSession() {
        DefaultDependencyResolver resolver = new DefaultDependencyResolver();

        InternalSession session = mock(InternalSession.class);
        RepositorySystem repoSystem = mock(RepositorySystem.class);
        RepositorySystemSession repoSession = mock(RepositorySystemSession.class);

        when(session.getSession()).thenReturn(repoSession);
        when(session.getRepositorySystem()).thenReturn(repoSystem);
        when(session.getUserProperties()).thenReturn(Map.of());
        when(session.getSystemProperties()).thenReturn(Map.of());
        when(repoSession.getConfigProperties()).thenReturn(Map.of());
        when(repoSession.getUserProperties()).thenReturn(Map.of());
        when(repoSession.getSystemProperties()).thenReturn(Map.of());

        Project project = mock(Project.class);
        org.apache.maven.api.model.Model model = mock(org.apache.maven.api.model.Model.class);
        when(project.getModel()).thenReturn(model);
        when(model.getProperties()).thenReturn(Map.of("maven.test.skip", "true"));
        when(project.getGroupId()).thenReturn("g");
        when(project.getArtifactId()).thenReturn("root");

        when(session.getProjects()).thenReturn(List.of(project));

        DependencyNode aetherRoot = new DefaultDependencyNode(new DefaultArtifact("g", "root", null, "pom", "1.0"));
        Node rootNode = mock(AbstractNode.class);
        org.apache.maven.api.Artifact apiRootArtifact = mock(org.apache.maven.api.Artifact.class);
        when(apiRootArtifact.getGroupId()).thenReturn("g");
        when(apiRootArtifact.getArtifactId()).thenReturn("root");
        when(rootNode.artifact()).thenReturn(apiRootArtifact);
        when(((AbstractNode) rootNode).getDependencyNode()).thenReturn(aetherRoot);

        when(repoSystem.flattenDependencyNodes(eq(repoSession), eq(aetherRoot), any()))
                .thenReturn(new java.util.ArrayList<>(List.of(aetherRoot)));

        resolver.flatten(session, rootNode, null);

        ArgumentCaptor<DependencyFilter> filterCaptor = ArgumentCaptor.forClass(DependencyFilter.class);
        verify(repoSystem).flattenDependencyNodes(eq(repoSession), eq(aetherRoot), filterCaptor.capture());

        DependencyFilter capturedFilter = filterCaptor.getValue();
        assertNotNull(capturedFilter);

        // Test-scoped test-jar must be excluded because project has maven.test.skip=true
        DefaultArtifact testJar = new DefaultArtifact(
                "g",
                "a",
                null,
                "jar",
                "1.0",
                Collections.singletonMap(ArtifactProperties.TYPE, "test-jar"),
                (java.nio.file.Path) null);
        DependencyNode testNode = new DefaultDependencyNode(new Dependency(testJar, "test"));
        assertFalse(capturedFilter.accept(testNode, Collections.emptyList()));
    }
}
