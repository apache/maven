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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Node;
import org.apache.maven.api.RemoteRepository;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictIdSorter;
import org.eclipse.aether.util.graph.transformer.ConflictMarker;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver;
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
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
    void testMetadataIsEmptyWhenNotCollected() {
        InternalSession session = mock(InternalSession.class);
        DefaultDependencyNode node =
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("org.example:myapp:1.0"), "compile"));

        DefaultNode defaultNode = new DefaultNode(session, node, false);

        assertEquals(Optional.empty(), defaultNode.winner());
        assertEquals(Optional.empty(), defaultNode.premanagedVersion());
        assertEquals(Optional.empty(), defaultNode.premanagedScope());
        assertEquals(Optional.empty(), defaultNode.originalScope());
    }

    @Test
    void testPremanagedVersionAndScope() {
        InternalSession session = mock(InternalSession.class);
        DefaultDependencyNode node =
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("org.example:myapp:2.0"), "runtime"));
        node.setData(DependencyManagerUtils.NODE_DATA_PREMANAGED_VERSION, "1.0");
        node.setData(DependencyManagerUtils.NODE_DATA_PREMANAGED_SCOPE, "compile");
        node.setManagedBits(DependencyNode.MANAGED_VERSION | DependencyNode.MANAGED_SCOPE);

        DefaultNode defaultNode = new DefaultNode(session, node, true);

        assertEquals(Optional.of("1.0"), defaultNode.premanagedVersion());
        assertEquals(Optional.of("compile"), defaultNode.premanagedScope());
        assertEquals(
                DependencyManagerUtils.getPremanagedVersion(node),
                defaultNode.premanagedVersion().get());
        assertEquals(
                DependencyManagerUtils.getPremanagedScope(node),
                defaultNode.premanagedScope().get());
    }

    @Test
    void testWinnerOfConflictingNodes() {
        // root -> a -> common:1 and root -> b -> common:2 (both at depth 2, so the first one declared wins)
        DefaultDependencyNode root = new DefaultDependencyNode(new DefaultArtifact("org.example:root:1"));
        DefaultDependencyNode a = node("org.example:a:1", "compile");
        DefaultDependencyNode b = node("org.example:b:1", "compile");
        DefaultDependencyNode common1 = node("org.example:common:1", "compile");
        DefaultDependencyNode common2 = node("org.example:common:2", "compile");
        a.setChildren(new ArrayList<>(List.of(common1)));
        b.setChildren(new ArrayList<>(List.of(common2)));
        root.setChildren(new ArrayList<>(List.of(a, b)));

        resolveConflicts(root);

        Node tree = session().getNode(root, true);
        Node loser =
                tree.stream().filter(n -> n.winner().isPresent()).findFirst().orElseThrow();
        Node winner = loser.winner().orElseThrow();

        assertEquals("common", loser.artifact().getArtifactId());
        assertEquals("common", winner.artifact().getArtifactId());
        assertEquals(
                ((DefaultNode) loser).node.getData().get(ConflictResolver.NODE_DATA_WINNER),
                ((DefaultNode) winner).node);
        assertEquals(Optional.empty(), winner.winner());
    }

    @Test
    void testOriginalScopeAfterConflictResolution() {
        // common:1 is reached through a test and a compile dependency: the first path wins but is widened to compile
        DefaultDependencyNode root = new DefaultDependencyNode(new DefaultArtifact("org.example:root:1"));
        DefaultDependencyNode b = node("org.example:b:1", "test");
        DefaultDependencyNode commonViaB = node("org.example:common:1", "test");
        DefaultDependencyNode a = node("org.example:a:1", "compile");
        DefaultDependencyNode commonViaA = node("org.example:common:1", "compile");
        b.setChildren(new ArrayList<>(List.of(commonViaB)));
        a.setChildren(new ArrayList<>(List.of(commonViaA)));
        root.setChildren(new ArrayList<>(List.of(b, a)));

        resolveConflicts(root);

        assertEquals("compile", commonViaB.getDependency().getScope());
        Node updated = session().getNode(commonViaB, true);

        assertEquals(Optional.of("test"), updated.originalScope());
        assertEquals(
                commonViaB.getData().get(ConflictResolver.NODE_DATA_ORIGINAL_SCOPE),
                updated.originalScope().get());
    }

    @Test
    void testMetadataSurvivesFiltering() {
        InternalSession session = mock(InternalSession.class);
        DefaultDependencyNode node =
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("org.example:myapp:2.0"), "runtime"));
        node.setData(DependencyManagerUtils.NODE_DATA_PREMANAGED_VERSION, "1.0");
        node.setData(DependencyManagerUtils.NODE_DATA_PREMANAGED_SCOPE, "compile");
        node.setManagedBits(DependencyNode.MANAGED_VERSION | DependencyNode.MANAGED_SCOPE);
        node.setData(ConflictResolver.NODE_DATA_ORIGINAL_SCOPE, "test");
        DefaultDependencyNode winner =
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("org.example:myapp:3.0"), "runtime"));
        node.setData(ConflictResolver.NODE_DATA_WINNER, winner);
        Node winnerNode = mock(Node.class);
        when(session.getNode(winner, true)).thenReturn(winnerNode);

        Node filtered = new DefaultNode(session, node, true).filter(n -> true);

        assertEquals(Optional.of("1.0"), filtered.premanagedVersion());
        assertEquals(Optional.of("compile"), filtered.premanagedScope());
        assertEquals(Optional.of("test"), filtered.originalScope());
        assertEquals(Optional.of(winnerNode), filtered.winner());
    }

    private static DefaultDependencyNode node(String coords, String scope) {
        DefaultArtifact artifact = new DefaultArtifact(coords);
        DefaultDependencyNode node = new DefaultDependencyNode(new Dependency(artifact, scope));
        try {
            node.setVersionConstraint(new GenericVersionScheme().parseVersionConstraint(artifact.getVersion()));
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalStateException(e);
        }
        node.setVersion(node.getVersionConstraint().getVersion());
        return node;
    }

    private static InternalSession session() {
        InternalSession session = mock(InternalSession.class);
        when(session.getNode(any(DependencyNode.class), eq(true))).thenAnswer(invocation -> {
            DependencyNode n = invocation.getArgument(0);
            return new DefaultNode(session, n, true);
        });
        when(session.getArtifact(any(org.eclipse.aether.artifact.Artifact.class)))
                .thenAnswer(invocation -> {
                    org.eclipse.aether.artifact.Artifact a = invocation.getArgument(0);
                    org.apache.maven.api.Artifact artifact = mock(org.apache.maven.api.Artifact.class);
                    when(artifact.getArtifactId()).thenReturn(a.getArtifactId());
                    when(artifact.getVersion()).thenReturn(mock(org.apache.maven.api.Version.class));
                    return artifact;
                });
        return session;
    }

    /** Runs the real Resolver conflict resolution, in verbose mode so that it keeps its node data. */
    private static void resolveConflicts(DependencyNode root) {
        DefaultRepositorySystemSession repoSession = new DefaultRepositorySystemSession(h -> false);
        repoSession.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, true);
        repoSession.setDependencySelector(new ScopeDependencySelector());
        Map<Object, Object> values = new HashMap<>();
        DependencyGraphTransformationContext context = new DependencyGraphTransformationContext() {
            @Override
            public RepositorySystemSession getSession() {
                return repoSession;
            }

            @Override
            public Object get(Object key) {
                return values.get(key);
            }

            @Override
            public Object put(Object key, Object value) {
                return values.put(key, value);
            }
        };
        try {
            new ChainedDependencyGraphTransformer(
                            new ConflictMarker(),
                            new ConflictIdSorter(),
                            new ConflictResolver(
                                    new NearestVersionSelector(),
                                    new JavaScopeSelector(),
                                    new SimpleOptionalitySelector(),
                                    new JavaScopeDeriver()))
                    .transformGraph(root, context);
        } catch (org.eclipse.aether.RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }
}
