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
import java.util.Optional;

import org.apache.maven.api.RemoteRepository;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultNodeTest {

    @Test
    void testAsString() {
        InternalSession session = Mockito.mock(InternalSession.class);

        // Create a basic dependency node
        DefaultArtifact artifact = new DefaultArtifact("org.example:myapp:1.0");
        Dependency dependency = new Dependency(artifact, "compile");
        DefaultDependencyNode node = new DefaultDependencyNode(dependency);

        // Test non-verbose mode
        DefaultNode defaultNode = new DefaultNode(session, node, false);
        assertEquals("org.example:myapp:jar:1.0:compile", defaultNode.asString());

        // Test verbose mode with managed version
        node.setData(DependencyManagerUtils.NODE_DATA_PREMANAGED_VERSION, "0.9");
        node.setManagedBits(DependencyNode.MANAGED_VERSION);
        defaultNode = new DefaultNode(session, node, true);
        assertEquals("org.example:myapp:jar:1.0:compile (version managed from 0.9)", defaultNode.asString());

        // Test verbose mode with managed scope
        node.setData(DependencyManagerUtils.NODE_DATA_PREMANAGED_SCOPE, "runtime");
        node.setManagedBits(DependencyNode.MANAGED_VERSION | DependencyNode.MANAGED_SCOPE);
        defaultNode = new DefaultNode(session, node, true);
        assertEquals(
                "org.example:myapp:jar:1.0:compile (version managed from 0.9; scope managed from runtime)",
                defaultNode.asString());

        // Test verbose mode with conflict resolution
        DefaultDependencyNode winner =
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("org.example:myapp:2.0"), "compile"));
        node.setData(ConflictResolver.NODE_DATA_WINNER, winner);
        node.setManagedBits(0);
        defaultNode = new DefaultNode(session, node, true);
        assertEquals("(org.example:myapp:jar:1.0:compile - omitted for conflict with 2.0)", defaultNode.asString());
    }

    @Test
    void testGetRepositoryReturnsPresentWhenFoundInLocalRepo() {
        // Set up a dependency node with an artifact and one candidate remote repository
        org.eclipse.aether.repository.RemoteRepository aetherRepo =
                new org.eclipse.aether.repository.RemoteRepository.Builder(
                                "central", "default", "https://repo1.maven.org/maven2")
                        .build();
        DefaultArtifact artifact = new DefaultArtifact("org.example:myapp:1.0");
        DefaultDependencyNode node = new DefaultDependencyNode(artifact);
        node.setRepositories(Collections.singletonList(aetherRepo));

        // LocalArtifactResult reports the artifact was fetched from aetherRepo
        LocalArtifactResult localResult = mock(LocalArtifactResult.class);
        when(localResult.getRepository()).thenReturn(aetherRepo);

        LocalRepositoryManager lrm = mock(LocalRepositoryManager.class);
        ArgumentCaptor<LocalArtifactRequest> requestCaptor = ArgumentCaptor.forClass(LocalArtifactRequest.class);
        when(lrm.find(any(RepositorySystemSession.class), requestCaptor.capture()))
                .thenReturn(localResult);

        RepositorySystemSession repoSession = mock(RepositorySystemSession.class);
        when(repoSession.getLocalRepositoryManager()).thenReturn(lrm);

        RemoteRepository mavenRepo = mock(RemoteRepository.class);

        InternalSession session = mock(InternalSession.class);
        when(session.getSession()).thenReturn(repoSession);
        when(session.getRemoteRepository(eq(aetherRepo))).thenReturn(mavenRepo);

        DefaultNode defaultNode = new DefaultNode(session, node, false);
        Optional<RemoteRepository> result = defaultNode.getRepository();

        assertTrue(result.isPresent());
        assertEquals(mavenRepo, result.get());
        // Verify the request was built with the correct artifact and repositories
        LocalArtifactRequest capturedRequest = requestCaptor.getValue();
        assertSame(artifact, capturedRequest.getArtifact());
        assertEquals(Collections.singletonList(aetherRepo), capturedRequest.getRepositories());
    }

    @Test
    void testGetRepositoryReturnsEmptyWhenNotInLocalRepo() {
        org.eclipse.aether.repository.RemoteRepository aetherRepo =
                new org.eclipse.aether.repository.RemoteRepository.Builder(
                                "central", "default", "https://repo1.maven.org/maven2")
                        .build();
        DefaultArtifact artifact = new DefaultArtifact("org.example:myapp:1.0");
        DefaultDependencyNode node = new DefaultDependencyNode(artifact);
        node.setRepositories(Collections.singletonList(aetherRepo));

        // LocalArtifactResult reports no repository (artifact not found / local install)
        LocalArtifactResult localResult = mock(LocalArtifactResult.class);
        when(localResult.getRepository()).thenReturn(null);

        LocalRepositoryManager lrm = mock(LocalRepositoryManager.class);
        ArgumentCaptor<LocalArtifactRequest> requestCaptor = ArgumentCaptor.forClass(LocalArtifactRequest.class);
        when(lrm.find(any(RepositorySystemSession.class), requestCaptor.capture()))
                .thenReturn(localResult);

        RepositorySystemSession repoSession = mock(RepositorySystemSession.class);
        when(repoSession.getLocalRepositoryManager()).thenReturn(lrm);

        InternalSession session = mock(InternalSession.class);
        when(session.getSession()).thenReturn(repoSession);

        DefaultNode defaultNode = new DefaultNode(session, node, false);
        assertFalse(defaultNode.getRepository().isPresent());
        // Verify the request was built with the correct artifact and repositories
        LocalArtifactRequest capturedRequest = requestCaptor.getValue();
        assertSame(artifact, capturedRequest.getArtifact());
        assertEquals(Collections.singletonList(aetherRepo), capturedRequest.getRepositories());
    }

    @Test
    void testGetRepositoryReturnsEmptyForRootNodeWithoutArtifact() {
        // Root node has no artifact
        DefaultDependencyNode node = new DefaultDependencyNode((org.eclipse.aether.graph.Dependency) null);

        InternalSession session = mock(InternalSession.class);

        DefaultNode defaultNode = new DefaultNode(session, node, false);
        assertFalse(defaultNode.getRepository().isPresent());
    }

    @Test
    void testGetRepositoryReturnsEmptyWhenNoRepositories() {
        // Node has artifact but empty repo list (e.g. local-only artifact)
        DefaultArtifact artifact = new DefaultArtifact("org.example:myapp:1.0");
        DefaultDependencyNode node = new DefaultDependencyNode(artifact);
        node.setRepositories(Collections.emptyList());

        InternalSession session = mock(InternalSession.class);

        DefaultNode defaultNode = new DefaultNode(session, node, false);
        assertFalse(defaultNode.getRepository().isPresent());
    }

    @Test
    void testGetRepositoryReturnsEmptyWhenLrmReturnsNull() {
        // Guard against third-party LRM implementations that return null from find()
        org.eclipse.aether.repository.RemoteRepository aetherRepo =
                new org.eclipse.aether.repository.RemoteRepository.Builder(
                                "central", "default", "https://repo1.maven.org/maven2")
                        .build();
        DefaultArtifact artifact = new DefaultArtifact("org.example:myapp:1.0");
        DefaultDependencyNode node = new DefaultDependencyNode(artifact);
        node.setRepositories(Collections.singletonList(aetherRepo));

        LocalRepositoryManager lrm = mock(LocalRepositoryManager.class);
        when(lrm.find(any(RepositorySystemSession.class), any(LocalArtifactRequest.class)))
                .thenReturn(null);

        RepositorySystemSession repoSession = mock(RepositorySystemSession.class);
        when(repoSession.getLocalRepositoryManager()).thenReturn(lrm);

        InternalSession session = mock(InternalSession.class);
        when(session.getSession()).thenReturn(repoSession);

        DefaultNode defaultNode = new DefaultNode(session, node, false);
        assertFalse(defaultNode.getRepository().isPresent());
    }

    @Test
    void testGetRepositoryIsCached() {
        // Verify that the LRM is only consulted once even when getRepository() is called multiple times
        org.eclipse.aether.repository.RemoteRepository aetherRepo =
                new org.eclipse.aether.repository.RemoteRepository.Builder(
                                "central", "default", "https://repo1.maven.org/maven2")
                        .build();
        DefaultArtifact artifact = new DefaultArtifact("org.example:myapp:1.0");
        DefaultDependencyNode node = new DefaultDependencyNode(artifact);
        node.setRepositories(Collections.singletonList(aetherRepo));

        LocalArtifactResult localResult = mock(LocalArtifactResult.class);
        when(localResult.getRepository()).thenReturn(aetherRepo);

        LocalRepositoryManager lrm = mock(LocalRepositoryManager.class);
        when(lrm.find(any(RepositorySystemSession.class), any(LocalArtifactRequest.class)))
                .thenReturn(localResult);

        RepositorySystemSession repoSession = mock(RepositorySystemSession.class);
        when(repoSession.getLocalRepositoryManager()).thenReturn(lrm);

        RemoteRepository mavenRepo = mock(RemoteRepository.class);

        InternalSession session = mock(InternalSession.class);
        when(session.getSession()).thenReturn(repoSession);
        when(session.getRemoteRepository(eq(aetherRepo))).thenReturn(mavenRepo);

        DefaultNode defaultNode = new DefaultNode(session, node, false);

        // Call getRepository() three times
        Optional<RemoteRepository> r1 = defaultNode.getRepository();
        Optional<RemoteRepository> r2 = defaultNode.getRepository();
        Optional<RemoteRepository> r3 = defaultNode.getRepository();

        assertTrue(r1.isPresent());
        assertEquals(mavenRepo, r1.get());
        assertEquals(r1, r2);
        assertEquals(r1, r3);

        // LRM must have been invoked exactly once
        Mockito.verify(lrm, Mockito.times(1)).find(any(RepositorySystemSession.class), any(LocalArtifactRequest.class));
    }
}
