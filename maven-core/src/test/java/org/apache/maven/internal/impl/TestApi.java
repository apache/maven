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
package org.apache.maven.internal.impl;

import javax.inject.Inject;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.api.*;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectBuilderRequest;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.repository.internal.MavenSessionBuilderSupplier;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.session.scope.internal.SessionScope;
import org.apache.maven.toolchain.DefaultToolchainManagerPrivate;
import org.apache.maven.toolchain.building.ToolchainsBuilder;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@PlexusTest
class TestApi {

    Session session;

    @Inject
    RepositorySystem repositorySystem;

    @Inject
    org.apache.maven.project.ProjectBuilder projectBuilder;

    @Inject
    MavenRepositorySystem mavenRepositorySystem;

    @Inject
    DefaultToolchainManagerPrivate toolchainManagerPrivate;

    @Inject
    PlexusContainer plexusContainer;

    @Inject
    MojoExecutionScope mojoExecutionScope;

    @Inject
    RuntimeInformation runtimeInformation;

    @Inject
    ArtifactHandlerManager artifactHandlerManager;

    @Inject
    SessionScope sessionScope;

    @Inject
    SettingsBuilder settingsBuilder;

    @Inject
    ToolchainsBuilder toolchainsBuilder;

    @BeforeEach
    void setup() {
        // create session with any local repo, is redefined anyway below
        RepositorySystemSession rss = new MavenSessionBuilderSupplier(repositorySystem)
                .get()
                .withLocalRepositoryBaseDirectories(new File("target"))
                .build();
        DefaultMavenExecutionRequest mer = new DefaultMavenExecutionRequest();
        DefaultMavenExecutionResult meres = new DefaultMavenExecutionResult();
        MavenSession ms = new MavenSession(rss, mer, meres);
        DefaultSession session = new DefaultSession(
                ms,
                repositorySystem,
                Collections.emptyList(),
                mavenRepositorySystem,
                new DefaultLookup(plexusContainer),
                runtimeInformation);
        DefaultLocalRepository localRepository =
                new DefaultLocalRepository(new LocalRepository("target/test-classes/apiv4-repo"));
        org.apache.maven.api.RemoteRepository remoteRepository = session.getRemoteRepository(
                new RemoteRepository.Builder("mirror", "default", "file:target/test-classes/repo").build());
        this.session = session.withLocalRepository(localRepository)
                .withRemoteRepositories(Collections.singletonList(remoteRepository));

        sessionScope.enter();
        sessionScope.seed(InternalSession.class, InternalSession.from(this.session));
    }

    @Test
    void testCreateAndResolveArtifact() {
        ArtifactCoordinate coord =
                session.createArtifactCoordinate("org.codehaus.plexus", "plexus-utils", "1.4.5", "pom");

        Map.Entry<Artifact, Path> resolved = session.resolveArtifact(coord);
        assertNotNull(resolved);
        assertNotNull(resolved.getKey());
        assertNotNull(resolved.getValue());
        Optional<Path> op = session.getArtifactPath(resolved.getKey());
        assertTrue(op.isPresent());
        assertEquals(resolved.getValue(), op.get());
    }

    @Test
    void testBuildProject() {
        Artifact artifact = session.createArtifact("org.codehaus.plexus", "plexus-utils", "1.4.5", "pom");

        Project project = session.getService(ProjectBuilder.class)
                .build(ProjectBuilderRequest.builder()
                        .session(session)
                        .path(session.getPathForLocalArtifact(artifact))
                        .processPlugins(false)
                        .build())
                .getProject()
                .get();
        assertNotNull(project);
    }

    @Test
    void testCollectArtifactDependencies() {
        Artifact artifact =
                session.createArtifact("org.codehaus.plexus", "plexus-container-default", "1.0-alpha-32", "jar");
        Node root = session.collectDependencies(artifact);
        assertNotNull(root);
    }

    @Test
    void testResolveArtifactCoordinateDependencies() {
        ArtifactCoordinate coord =
                session.createArtifactCoordinate("org.apache.maven.core.test", "test-extension", "1", "jar");

        List<Path> paths = session.resolveDependencies(session.createDependencyCoordinate(coord));

        assertNotNull(paths);
        assertEquals(10, paths.size());
        assertTrue(paths.get(0).getFileName().toString().equals("test-extension-1.jar"));
    }

    @Test
    void testProjectDependencies() {
        Artifact pom = session.createArtifact("org.codehaus.plexus", "plexus-container-default", "1.0-alpha-32", "pom");

        Project project = session.getService(ProjectBuilder.class)
                .build(ProjectBuilderRequest.builder()
                        .session(session)
                        .path(session.getPathForLocalArtifact(pom))
                        .processPlugins(false)
                        .build())
                .getProject()
                .get();
        assertNotNull(project);

        Artifact artifact = session.createArtifact("org.apache.maven.core.test", "test-extension", "1", "jar");
        Node root = session.collectDependencies(artifact);
        assertNotNull(root);

        DependencyResolverResult result =
                session.getService(DependencyResolver.class).resolve(session, project, PathScope.MAIN_RUNTIME);
        assertNotNull(result);
        List<Dependency> deps = new ArrayList<>(result.getDependencies().keySet());
        List<Dependency> deps2 = result.getNodes().stream()
                .map(Node::getDependency)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertEquals(deps, deps2);
        for (Dependency dep : deps2) {
            dep.getVersion();
        }
    }
}
