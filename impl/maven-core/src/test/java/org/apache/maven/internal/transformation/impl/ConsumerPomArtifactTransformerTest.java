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
package org.apache.maven.internal.transformation.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Constants;
import org.apache.maven.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SessionData;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.installation.InstallRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ConsumerPomArtifactTransformerTest {

    @Test
    void transform() throws Exception {
        RepositorySystemSession systemSessionMock = Mockito.mock(RepositorySystemSession.class);
        SessionData sessionDataMock = Mockito.mock(SessionData.class);
        when(systemSessionMock.getData()).thenReturn(sessionDataMock);

        Path beforePomFile =
                Paths.get("src/test/resources/projects/transform/before.pom").toAbsolutePath();
        Path afterPomFile =
                Paths.get("src/test/resources/projects/transform/after.pom").toAbsolutePath();
        Path tempFile = Files.createTempFile("", ".pom");
        Files.delete(tempFile);
        try (InputStream expected = Files.newInputStream(beforePomFile)) {
            Model model = new Model(new MavenStaxReader().read(expected));
            MavenProject project = new MavenProject(model);
            project.setOriginalModel(model);
            ConsumerPomArtifactTransformer t = new ConsumerPomArtifactTransformer((s, p, f) -> {
                try (InputStream is = Files.newInputStream(f)) {
                    return DefaultConsumerPomBuilder.transformPom(new MavenStaxReader().read(is), project);
                }
            });

            t.transform(project, systemSessionMock, beforePomFile, tempFile);
        }
        Diff diff = DiffBuilder.compare(afterPomFile.toFile())
                .withTest(tempFile.toFile())
                .ignoreComments()
                .ignoreWhitespace()
                .build();
        assertFalse(diff.hasDifferences(), "XML files should be identical: " + diff.toString());
    }

    @Test
    void transformJarConsumerPom() throws Exception {
        RepositorySystemSession systemSessionMock = Mockito.mock(RepositorySystemSession.class);
        SessionData sessionDataMock = Mockito.mock(SessionData.class);
        when(systemSessionMock.getData()).thenReturn(sessionDataMock);

        Path beforePomFile = Paths.get("src/test/resources/projects/transform/jar/before.pom")
                .toAbsolutePath();
        Path afterPomFile =
                Paths.get("src/test/resources/projects/transform/jar/after.pom").toAbsolutePath();
        Path tempFile = Files.createTempFile("", ".pom");
        Files.delete(tempFile);
        try (InputStream expected = Files.newInputStream(beforePomFile)) {
            Model model = new Model(new MavenStaxReader().read(expected));
            MavenProject project = new MavenProject(model);
            project.setOriginalModel(model);
            ConsumerPomArtifactTransformer t = new ConsumerPomArtifactTransformer((s, p, f) -> {
                try (InputStream is = Files.newInputStream(f)) {
                    return DefaultConsumerPomBuilder.transformNonPom(new MavenStaxReader().read(is), project);
                }
            });

            t.transform(project, systemSessionMock, beforePomFile, tempFile);
        }
        Diff diff = DiffBuilder.compare(afterPomFile.toFile())
                .withTest(tempFile.toFile())
                .ignoreComments()
                .ignoreWhitespace()
                .build();
        assertFalse(diff.hasDifferences(), "XML files should be identical: " + diff.toString());
    }

    @Test
    void injectTransformedArtifactsWithoutPomShouldNotInjectAnyArtifacts() throws IOException {
        MavenProject emptyProject = new MavenProject();

        RepositorySystemSession systemSessionMock = Mockito.mock(RepositorySystemSession.class);
        SessionData sessionDataMock = Mockito.mock(SessionData.class);
        when(systemSessionMock.getData()).thenReturn(sessionDataMock);

        new ConsumerPomArtifactTransformer((session, project, src) -> null)
                .injectTransformedArtifacts(systemSessionMock, emptyProject);

        assertTrue(emptyProject.getAttachedArtifacts().isEmpty());
    }

    @Test
    void testDeployBuildPomEnabledByDefault() {
        // Test that build POM deployment is enabled by default
        ConsumerPomArtifactTransformer transformer = new ConsumerPomArtifactTransformer((s, p, f) -> null);

        RepositorySystemSession session = createMockSession(Map.of());
        DeployRequest request = createDeployRequestWithConsumerPom();

        DeployRequest result = transformer.remapDeployArtifacts(session, request);

        // Should have both consumer POM (no classifier) and build POM (with "build" classifier)
        Collection<Artifact> artifacts = result.getArtifacts();
        assertEquals(3, artifacts.size()); // original jar + consumer pom + build pom

        assertTrue(artifacts.stream().anyMatch(a -> "pom".equals(a.getExtension()) && "".equals(a.getClassifier())));
        assertTrue(
                artifacts.stream().anyMatch(a -> "pom".equals(a.getExtension()) && "build".equals(a.getClassifier())));
    }

    @Test
    void testDeployBuildPomDisabled() {
        // Test that build POM deployment can be disabled
        ConsumerPomArtifactTransformer transformer = new ConsumerPomArtifactTransformer((s, p, f) -> null);

        Map<String, Object> configProps = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "false");
        RepositorySystemSession session = createMockSession(configProps);
        DeployRequest request = createDeployRequestWithConsumerPom();

        DeployRequest result = transformer.remapDeployArtifacts(session, request);

        // Should have only consumer POM (no classifier), no build POM
        Collection<Artifact> artifacts = result.getArtifacts();
        assertEquals(2, artifacts.size()); // original jar + consumer pom (no build pom)

        assertTrue(artifacts.stream().anyMatch(a -> "pom".equals(a.getExtension()) && "".equals(a.getClassifier())));
        assertTrue(
                artifacts.stream().noneMatch(a -> "pom".equals(a.getExtension()) && "build".equals(a.getClassifier())));
    }

    @Test
    void testDeployBuildPomExplicitlyEnabled() {
        // Test that build POM deployment can be explicitly enabled
        ConsumerPomArtifactTransformer transformer = new ConsumerPomArtifactTransformer((s, p, f) -> null);

        Map<String, Object> configProps = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "true");
        RepositorySystemSession session = createMockSession(configProps);
        DeployRequest request = createDeployRequestWithConsumerPom();

        DeployRequest result = transformer.remapDeployArtifacts(session, request);

        // Should have both consumer POM (no classifier) and build POM (with "build" classifier)
        Collection<Artifact> artifacts = result.getArtifacts();
        assertEquals(3, artifacts.size()); // original jar + consumer pom + build pom

        assertTrue(artifacts.stream().anyMatch(a -> "pom".equals(a.getExtension()) && "".equals(a.getClassifier())));
        assertTrue(
                artifacts.stream().anyMatch(a -> "pom".equals(a.getExtension()) && "build".equals(a.getClassifier())));
    }

    @Test
    void testDeployBuildPomWithBooleanValue() {
        // Test that build POM deployment works with Boolean values (not just strings)
        ConsumerPomArtifactTransformer transformer = new ConsumerPomArtifactTransformer((s, p, f) -> null);

        Map<String, Object> configProps = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, Boolean.FALSE);
        RepositorySystemSession session = createMockSession(configProps);
        DeployRequest request = createDeployRequestWithConsumerPom();

        DeployRequest result = transformer.remapDeployArtifacts(session, request);

        // Should have only consumer POM (no classifier), no build POM
        Collection<Artifact> artifacts = result.getArtifacts();
        assertEquals(2, artifacts.size()); // original jar + consumer pom (no build pom)

        assertTrue(artifacts.stream().anyMatch(a -> "pom".equals(a.getExtension()) && "".equals(a.getClassifier())));
        assertTrue(
                artifacts.stream().noneMatch(a -> "pom".equals(a.getExtension()) && "build".equals(a.getClassifier())));
    }

    @Test
    void testInstallAlwaysIncludesBuildPom() {
        // Test that install always includes build POM regardless of the deployment setting
        ConsumerPomArtifactTransformer transformer = new ConsumerPomArtifactTransformer((s, p, f) -> null);

        Map<String, Object> configProps = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "false");
        RepositorySystemSession session = createMockSession(configProps);
        InstallRequest request = createInstallRequestWithConsumerPom();

        InstallRequest result = transformer.remapInstallArtifacts(session, request);

        // Should have both consumer POM and build POM even when deployment is disabled
        Collection<Artifact> artifacts = result.getArtifacts();
        assertEquals(3, artifacts.size()); // original jar + consumer pom + build pom

        assertTrue(artifacts.stream().anyMatch(a -> "pom".equals(a.getExtension()) && "".equals(a.getClassifier())));
        assertTrue(
                artifacts.stream().anyMatch(a -> "pom".equals(a.getExtension()) && "build".equals(a.getClassifier())));
    }

    @Test
    void testDeployWithoutConsumerPomIsUnaffected() {
        // Test that requests without consumer POMs are not affected by the setting
        ConsumerPomArtifactTransformer transformer = new ConsumerPomArtifactTransformer((s, p, f) -> null);

        Map<String, Object> configProps = Map.of(Constants.MAVEN_DEPLOY_BUILD_POM, "false");
        RepositorySystemSession session = createMockSession(configProps);
        DeployRequest request = createDeployRequestWithoutConsumerPom();

        DeployRequest result = transformer.remapDeployArtifacts(session, request);

        // Should be unchanged since there's no consumer POM
        assertEquals(request.getArtifacts(), result.getArtifacts());
    }

    private RepositorySystemSession createMockSession(Map<String, Object> configProperties) {
        RepositorySystemSession session = Mockito.mock(RepositorySystemSession.class);
        when(session.getConfigProperties()).thenReturn(configProperties);
        return session;
    }

    private DeployRequest createDeployRequestWithConsumerPom() {
        DeployRequest request = new DeployRequest();
        List<Artifact> artifacts = List.of(
                new DefaultArtifact("com.example", "test", "", "jar", "1.0.0"),
                new DefaultArtifact("com.example", "test", "", "pom", "1.0.0"), // main POM
                new DefaultArtifact("com.example", "test", "consumer", "pom", "1.0.0") // consumer POM
                );
        request.setArtifacts(artifacts);
        return request;
    }

    private InstallRequest createInstallRequestWithConsumerPom() {
        InstallRequest request = new InstallRequest();
        List<Artifact> artifacts = List.of(
                new DefaultArtifact("com.example", "test", "", "jar", "1.0.0"),
                new DefaultArtifact("com.example", "test", "", "pom", "1.0.0"), // main POM
                new DefaultArtifact("com.example", "test", "consumer", "pom", "1.0.0") // consumer POM
                );
        request.setArtifacts(artifacts);
        return request;
    }

    private DeployRequest createDeployRequestWithoutConsumerPom() {
        DeployRequest request = new DeployRequest();
        List<Artifact> artifacts = List.of(
                new DefaultArtifact("com.example", "test", "", "jar", "1.0.0"),
                new DefaultArtifact("com.example", "test", "", "pom", "1.0.0") // only main POM
                );
        request.setArtifacts(artifacts);
        return request;
    }
}
