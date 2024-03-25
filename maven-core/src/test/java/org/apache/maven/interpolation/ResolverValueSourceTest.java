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
package org.apache.maven.interpolation;

import javax.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.api.model.Model;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.handler.manager.DefaultArtifactHandlerManager;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.impl.DefaultRemoteRepository;
import org.apache.maven.internal.impl.DefaultSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.interpolation.StringVisitorModelInterpolator;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.rtinfo.internal.DefaultRuntimeInformation;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.DefaultSessionData;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.internal.impl.DefaultLocalPathComposer;
import org.eclipse.aether.internal.impl.DefaultLocalPathPrefixComposerFactory;
import org.eclipse.aether.internal.impl.DefaultTrackingFileManager;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@PlexusTest
public class ResolverValueSourceTest {
    @Inject
    private StringVisitorModelInterpolator interpolator;

    @Inject
    private ModelReader reader;

    @Inject
    private SessionScope sessionScope;

    @Inject
    private PlexusContainer container;

    @Inject
    private RepositorySystem repositorySystem;

    @Test
    void resolvesExtendedInstance() {
        assertInstanceOf(ExtendedStringVisitorModelInterpolator.class, interpolator);
    }

    @Test
    void interpolateDependencyLocation(@TempDir final Path work) throws IOException, NoLocalRepositoryManagerException {
        final Path pom = Files.write(
                work.resolve("pom.xml"),
                ("" + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                                + "  <modelVersion>4.0.0</modelVersion>\n"
                                + "\n"
                                + "  <groupId>org.apache.maven.test</groupId>\n"
                                + "  <artifactId>maven-core</artifactId>\n"
                                + "  <version>5.6.7-SNAPSHOT</version>\n"
                                + "\n"
                                + "  <properties>\n"
                                + "    <dummy.version>1.2.3</dummy.version>\n"
                                + "  </properties>\n"
                                + "\n"
                                + "  <dependencies>\n"
                                + "    <dependency>\n"
                                + "      <groupId>com.bar</groupId>\n"
                                + "      <artifactId>dummy-agent</artifactId>\n"
                                + "      <version>${dummy.version}</version>\n"
                                + "    </dependency>\n"
                                + "  </dependencies>\n"
                                + "\n"
                                + "  <build>\n"
                                + "    <plugins>\n"
                                + "      <plugin>\n"
                                + "        <groupId>foo</groupId>\n"
                                + "        <artifactId>bar-plugin</artifactId>\n"
                                + "        <version>9.8.7</version>\n"
                                + "        <configuration>\n"
                                + "          <agents>\n"
                                + "            <agent>${project.dependencies.com.bar.dummy-agent}</agent>\n"
                                + "          </agents>\n"
                                + "        </configuration>\n"
                                + "      </plugin>\n"
                                + "    </plugins>\n"
                                + "  </build>\n"
                                + "</project>\n"
                                + "")
                        .getBytes(StandardCharsets.UTF_8));
        final Path repo = Files.createDirectories(work.resolve("m2"));
        final Path fakeArtifact = Files.write(
                        Files.createDirectories(repo.resolve("com/bar/dummy-agent/1.2.3"))
                                .resolve("dummy-agent-1.2.3.jar"),
                        "1.2.3".getBytes(StandardCharsets.UTF_8))
                .toAbsolutePath()
                .normalize();

        final Model model = reader.read(pom.toFile(), emptyMap()).getDelegate();

        final DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setPomFile(pom.toFile());

        final MavenProject currentProject = new MavenProject();
        currentProject.setDependencies(
                model.getDependencies().stream().map(Dependency::new).collect(toList()));

        final DefaultRepositorySystemSession repositorySession = new DefaultRepositorySystemSession();
        repositorySession.setLocalRepositoryManager(new EnhancedLocalRepositoryManagerFactory(
                        new DefaultLocalPathComposer(),
                        new DefaultTrackingFileManager(),
                        new DefaultLocalPathPrefixComposerFactory())
                .newInstance(repositorySession, new LocalRepository(repo.toFile())));
        repositorySession.setDependencyManager(new ClassicDependencyManager());
        repositorySession.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(false, false));
        repositorySession.setArtifactTypeRegistry(new DefaultArtifactTypeRegistry());
        repositorySession.setSystemProperties(emptyMap());
        repositorySession.setUserProperties(emptyMap());
        repositorySession.setConfigProperties(emptyMap());
        repositorySession.setMirrorSelector(new DefaultMirrorSelector());
        repositorySession.setProxySelector(new DefaultProxySelector());
        repositorySession.setAuthenticationSelector(new DefaultAuthenticationSelector());
        repositorySession.setData(new DefaultSessionData());

        final MavenSession mavenSession = new MavenSession(
                container, repositorySession, new DefaultMavenExecutionRequest(), new DefaultMavenExecutionResult());
        mavenSession.setCurrentProject(currentProject);
        mavenSession.setSession(new DefaultSession(
                mavenSession,
                repositorySystem,
                singletonList(new DefaultRemoteRepository(new RemoteRepository.Builder(
                                "test", "default", repo.toUri().toString())
                        .build())),
                new MavenRepositorySystem(
                        new DefaultArtifactHandlerManager(singletonMap("jar", new DefaultArtifactHandler("jar"))),
                        singletonMap("default", new DefaultRepositoryLayout())),
                container,
                new DefaultRuntimeInformation(new GenericVersionScheme())));

        sessionScope.enter();
        sessionScope.seed(MavenSession.class, mavenSession);
        try {
            final Model interpolated = interpolator.interpolateModel(model, pom.toFile(), request, req -> {
                throw new UnsupportedOperationException();
            });
            final String interpolatedValue = interpolated
                    .getBuild()
                    .getPlugins()
                    .get(0)
                    .getConfiguration()
                    .getChild("agents")
                    .getChildren()
                    .get(0)
                    .getValue();
            assertEquals(
                    fakeArtifact, Paths.get(interpolatedValue).toAbsolutePath().normalize());
        } finally {
            sessionScope.exit();
        }
    }
}
