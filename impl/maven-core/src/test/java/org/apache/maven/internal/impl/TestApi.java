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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectBuilderRequest;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.services.ToolchainsBuilder;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.resolver.MavenSessionBuilderSupplier;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    DefaultToolchainManager toolchainManagerPrivate;

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
                .withLocalRepositoryBaseDirectories(new File("target/test-classes/apiv4-repo").toPath())
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
        org.apache.maven.api.RemoteRepository remoteRepository = session.getRemoteRepository(
                new RemoteRepository.Builder("mirror", "default", "file:target/test-classes/repo").build());
        this.session = session.withRemoteRepositories(Collections.singletonList(remoteRepository));
        InternalSession.associate(rss, this.session);
        sessionScope.enter();
        sessionScope.seed(InternalMavenSession.class, InternalMavenSession.from(this.session));
    }

    private Project project(Artifact artifact) {
        return session.getService(ProjectBuilder.class)
                .build(ProjectBuilderRequest.builder()
                        .session(session)
                        .path(session.getPathForLocalArtifact(artifact))
                        .processPlugins(false)
                        .build())
                .getProject()
                .get();
    }

    @Test
    void testCreateAndResolveArtifact() {
        ArtifactCoordinates coords =
                session.createArtifactCoordinates("org.codehaus.plexus", "plexus-utils", "1.4.5", "pom");

        DownloadedArtifact resolved = session.resolveArtifact(coords);
        assertNotNull(resolved);
        assertNotNull(resolved.getPath());
        Optional<Path> op = session.getArtifactPath(resolved);
        assertTrue(op.isPresent());
        assertEquals(resolved.getPath(), op.get());
    }

    @Test
    void testBuildProject() {
        Artifact artifact = session.createArtifact("org.codehaus.plexus", "plexus-utils", "1.4.5", "pom");

        Project project = project(artifact);
        assertNotNull(project);
    }

    @Test
    void testCollectArtifactDependencies() {
        Artifact artifact =
                session.createArtifact("org.codehaus.plexus", "plexus-container-default", "1.0-alpha-32", "jar");
        Node root = session.collectDependencies(artifact, PathScope.MAIN_RUNTIME);
        assertNotNull(root);
    }

    @Test
    void testResolveArtifactCoordinatesDependencies() {
        DependencyCoordinates coords = session.createDependencyCoordinates(
                session.createArtifactCoordinates("org.apache.maven.core.test", "test-extension", "1", "jar"));

        List<Path> paths = session.resolveDependencies(coords);

        assertNotNull(paths);
        assertEquals(10, paths.size());
        assertEquals("test-extension-1.jar", paths.get(0).getFileName().toString());

        // JUnit has an "Automatic-Module-Name", so it appears on the module path.
        Map<PathType, List<Path>> dispatched = session.resolveDependencies(
                coords, PathScope.TEST_COMPILE, Arrays.asList(JavaPathType.CLASSES, JavaPathType.MODULES));
        List<Path> classes = dispatched.get(JavaPathType.CLASSES);
        List<Path> modules = dispatched.get(JavaPathType.MODULES);
        List<Path> unresolved = dispatched.get(PathType.UNRESOLVED);
        assertEquals(3, dispatched.size());
        assertEquals(1, unresolved.size());
        assertEquals(8, classes.size()); // "plexus.pom" and "junit.jar" are excluded.
        assertEquals(1, modules.size());
        assertEquals("plexus-1.0.11.pom", unresolved.get(0).getFileName().toString());
        assertEquals("test-extension-1.jar", classes.get(0).getFileName().toString());
        assertEquals("junit-4.13.1.jar", modules.get(0).getFileName().toString());
        assertTrue(paths.containsAll(classes));
        assertTrue(paths.containsAll(modules));

        // If caller wants only a classpath, JUnit shall move there.
        dispatched = session.resolveDependencies(coords, PathScope.TEST_COMPILE, Arrays.asList(JavaPathType.CLASSES));
        classes = dispatched.get(JavaPathType.CLASSES);
        modules = dispatched.get(JavaPathType.MODULES);
        unresolved = dispatched.get(PathType.UNRESOLVED);
        assertEquals(2, dispatched.size());
        assertEquals(1, unresolved.size());
        assertEquals(9, classes.size());
        assertNull(modules);
        assertTrue(paths.containsAll(classes));
        assertEquals("plexus-1.0.11.pom", unresolved.get(0).getFileName().toString());
    }

    @Test
    void testMetadataGeneratorFactory() throws ComponentLookupException {
        List<MetadataGeneratorFactory> factories = plexusContainer.lookupList(MetadataGeneratorFactory.class);
        assertNotNull(factories);
        factories.forEach(f -> System.out.println(f.getClass().getName()));
        assertEquals(3, factories.size());
    }

    @Test
    void testProjectDependencies() {
        Artifact pom = session.createArtifact("org.codehaus.plexus", "plexus-container-default", "1.0-alpha-32", "pom");

        Project project = project(pom);
        assertNotNull(project);

        Artifact artifact = session.createArtifact("org.apache.maven.core.test", "test-extension", "1", "jar");
        Node root = session.collectDependencies(artifact, PathScope.MAIN_RUNTIME);
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
