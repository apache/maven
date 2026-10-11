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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.maven.api.Language;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.SourceRoot;
import org.apache.maven.api.model.Source;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.impl.DefaultModelVersionParser;
import org.apache.maven.impl.DefaultSourceRoot;
import org.apache.maven.impl.DefaultVersionParser;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DefaultProjectManagerTest {

    private DefaultProjectManager projectManager;

    private Project project;

    private ProducedArtifact artifact;

    private Path artifactPath;

    @Test
    void addGeneratedSourceRoots() {
        InternalMavenSession session = Mockito.mock(InternalMavenSession.class);
        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(
                Path.of("target", "project", "pom.xml").toAbsolutePath().toFile());
        Project project = new DefaultProject(session, mavenProject);
        DefaultProjectManager projectManager = new DefaultProjectManager(session, Mockito.mock(ArtifactManager.class));

        projectManager.addSourceRoot(
                project, ProjectScope.MAIN, Language.JAVA_FAMILY, Path.of("target/generated-sources/main"), true);
        projectManager.addSourceRoot(
                project, ProjectScope.TEST, Language.JAVA_FAMILY, Path.of("target/generated-test-sources/test"), true);
        projectManager.addSourceRoot(project, ProjectScope.MAIN, Language.JAVA_FAMILY, Path.of("src/main/java"));
        projectManager.addSourceRoot(
                project, ProjectScope.MAIN, Language.JAVA_FAMILY, Path.of("target/generated-sources/main"), false);

        List<SourceRoot> roots = projectManager.getSourceRoots(project).stream().toList();
        assertEquals(3, roots.size());
        assertTrue(roots.get(0).generated());
        assertTrue(roots.get(1).generated());
        assertFalse(roots.get(2).generated());
    }

    @Test
    void attachArtifact() {
        InternalMavenSession session = Mockito.mock(InternalMavenSession.class);
        ArtifactManager artifactManager = Mockito.mock(ArtifactManager.class);
        MavenProject mavenProject = new MavenProject();
        project = new DefaultProject(session, mavenProject);
        artifact = Mockito.mock(ProducedArtifact.class);
        artifactPath = Paths.get("");
        DefaultVersionParser versionParser =
                new DefaultVersionParser(new DefaultModelVersionParser(new GenericVersionScheme()));
        projectManager = new DefaultProjectManager(session, artifactManager);

        mavenProject.setGroupId("myGroup");
        mavenProject.setArtifactId("myArtifact");
        mavenProject.setVersion("1.0-SNAPSHOT");
        when(artifact.getGroupId()).thenReturn("myGroup");
        when(artifact.getArtifactId()).thenReturn("myArtifact");
        when(artifact.getBaseVersion()).thenReturn(versionParser.parseVersion("1.0-SNAPSHOT"));
        projectManager.attachArtifact(project, artifact, artifactPath);

        // Verify that an exception is thrown when the artifactId differs
        when(artifact.getArtifactId()).thenReturn("anotherArtifact");
        assertExceptionMessageContains("myGroup:myArtifact:1.0-SNAPSHOT", "myGroup:anotherArtifact:1.0-SNAPSHOT");

        // Add a Java module. It should relax the restriction on artifactId.
        projectManager.addSourceRoot(
                project,
                new DefaultSourceRoot(
                        ProjectScope.MAIN,
                        Language.JAVA_FAMILY,
                        "org.foo.bar",
                        null,
                        Path.of("myProject"),
                        null,
                        null,
                        false,
                        null,
                        true));

        // Verify that we get the same exception when the artifactId does not match the module name
        assertExceptionMessageContains("", "anotherArtifact");

        // Verify that no exception is thrown when the artifactId is the module name
        when(artifact.getArtifactId()).thenReturn("org.foo.bar");
        projectManager.attachArtifact(project, artifact, artifactPath);

        // Verify that an exception is thrown when the groupId differs
        when(artifact.getGroupId()).thenReturn("anotherGroup");
        assertExceptionMessageContains("myGroup:myArtifact:1.0-SNAPSHOT", "anotherGroup:org.foo.bar:1.0-SNAPSHOT");
    }

    @Test
    void ordinaryJarPackagingProducesMainArtifact() {
        InternalMavenSession session = Mockito.mock(InternalMavenSession.class);
        Packaging packaging = Mockito.mock(Packaging.class);
        when(packaging.producesMainArtifact()).thenReturn(true);
        when(session.requirePackaging("jar")).thenReturn(packaging);

        ProducedArtifact pomProduced = Mockito.mock(ProducedArtifact.class);
        ProducedArtifact jarProduced = Mockito.mock(ProducedArtifact.class);
        when(session.getArtifact(
                        Mockito.eq(ProducedArtifact.class),
                        Mockito.argThat(a -> a != null && "pom".equals(a.getExtension()))))
                .thenReturn(pomProduced);
        when(session.getArtifact(
                        Mockito.eq(ProducedArtifact.class),
                        Mockito.argThat(a -> a != null && "jar".equals(a.getExtension()))))
                .thenReturn(jarProduced);

        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId("org.foo");
        mavenProject.setArtifactId("bar");
        mavenProject.setVersion("1.0.0");
        mavenProject.setPackaging("jar");
        mavenProject.setFile(
                Path.of("target", "project", "pom.xml").toAbsolutePath().toFile());
        mavenProject.setArtifact(new org.apache.maven.artifact.DefaultArtifact(
                "org.foo", "bar", "1.0.0", "compile", "jar", "", new TestArtifactHandler("jar")));

        Project project = new DefaultProject(session, mavenProject);
        assertTrue(project.getPackaging().producesMainArtifact());

        List<ProducedArtifact> artifacts = project.getArtifacts();
        assertEquals(2, artifacts.size());
        assertTrue(artifacts.contains(pomProduced));
        assertTrue(artifacts.contains(jarProduced));

        DefaultProjectManager projectManager = new DefaultProjectManager(session, Mockito.mock(ArtifactManager.class));
        Collection<ProducedArtifact> all = projectManager.getAllArtifacts(project);
        assertEquals(2, all.size());
    }

    @Test
    void pomPackagingDoesNotProduceMainArtifact() {
        InternalMavenSession session = Mockito.mock(InternalMavenSession.class);
        Packaging packaging = Mockito.mock(Packaging.class);
        when(packaging.producesMainArtifact()).thenReturn(false);
        when(session.requirePackaging("pom")).thenReturn(packaging);

        ProducedArtifact pomProduced = Mockito.mock(ProducedArtifact.class);
        when(session.getArtifact(Mockito.eq(ProducedArtifact.class), Mockito.any()))
                .thenReturn(pomProduced);

        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId("org.foo");
        mavenProject.setArtifactId("bar");
        mavenProject.setVersion("1.0.0");
        mavenProject.setPackaging("pom");
        mavenProject.setFile(
                Path.of("target", "project", "pom.xml").toAbsolutePath().toFile());
        mavenProject.setArtifact(new org.apache.maven.artifact.DefaultArtifact(
                "org.foo", "bar", "1.0.0", "compile", "pom", "", new TestArtifactHandler("pom")));

        Project project = new DefaultProject(session, mavenProject);
        assertFalse(project.getPackaging().producesMainArtifact());

        List<ProducedArtifact> artifacts = project.getArtifacts();
        assertEquals(1, artifacts.size());
        assertTrue(artifacts.contains(pomProduced));

        DefaultProjectManager projectManager = new DefaultProjectManager(session, Mockito.mock(ArtifactManager.class));
        assertEquals(1, projectManager.getAllArtifacts(project).size());
    }

    @Test
    void bomPackagingBehavesCorrectly() {
        InternalMavenSession session = Mockito.mock(InternalMavenSession.class);
        Packaging packaging = Mockito.mock(Packaging.class);
        when(packaging.producesMainArtifact()).thenReturn(false);
        when(session.requirePackaging("bom")).thenReturn(packaging);

        ProducedArtifact pomProduced = Mockito.mock(ProducedArtifact.class);
        when(session.getArtifact(Mockito.eq(ProducedArtifact.class), Mockito.any()))
                .thenReturn(pomProduced);

        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId("org.foo");
        mavenProject.setArtifactId("bar-bom");
        mavenProject.setVersion("1.0.0");
        mavenProject.setPackaging("bom");
        mavenProject.setFile(
                Path.of("target", "project", "pom.xml").toAbsolutePath().toFile());
        mavenProject.setArtifact(new org.apache.maven.artifact.DefaultArtifact(
                "org.foo", "bar-bom", "1.0.0", "compile", "pom", "", new TestArtifactHandler("pom")));

        Project project = new DefaultProject(session, mavenProject);
        assertFalse(project.getPackaging().producesMainArtifact());

        List<ProducedArtifact> artifacts = project.getArtifacts();
        assertEquals(1, artifacts.size());
        assertTrue(artifacts.contains(pomProduced));
    }

    @Test
    void staticModuleSourceHierarchyDoesNotProduceMainArtifact() {
        InternalMavenSession session = Mockito.mock(InternalMavenSession.class);
        Packaging packaging = Mockito.mock(Packaging.class);
        when(packaging.producesMainArtifact()).thenReturn(true);
        when(session.requirePackaging("jar")).thenReturn(packaging);

        ProducedArtifact pomProduced = Mockito.mock(ProducedArtifact.class);
        ProducedArtifact jarProduced = Mockito.mock(ProducedArtifact.class);

        when(session.getArtifact(
                        Mockito.eq(ProducedArtifact.class),
                        Mockito.argThat(a -> a != null && "pom".equals(a.getExtension()))))
                .thenReturn(pomProduced);
        when(session.getArtifact(
                        Mockito.eq(ProducedArtifact.class),
                        Mockito.argThat(a ->
                                a != null && "jar".equals(a.getExtension()) && "myArtifact".equals(a.getArtifactId()))))
                .thenReturn(jarProduced);

        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId("myGroup");
        mavenProject.setArtifactId("myArtifact");
        mavenProject.setVersion("1.0-SNAPSHOT");
        mavenProject.setPackaging("jar");
        mavenProject.setFile(
                Path.of("target", "project", "pom.xml").toAbsolutePath().toFile());
        mavenProject.setArtifact(new org.apache.maven.artifact.DefaultArtifact(
                "myGroup", "myArtifact", "1.0-SNAPSHOT", "compile", "jar", "", new TestArtifactHandler("jar")));

        Source source = Source.newBuilder().module("org.foo.bar").build();
        org.apache.maven.api.model.Build modelBuild = org.apache.maven.api.model.Build.newBuilder()
                .sources(List.of(source))
                .build();
        mavenProject.setBuild(new org.apache.maven.model.Build(modelBuild));

        Project project = new DefaultProject(session, mavenProject);

        assertFalse(project.getPackaging().producesMainArtifact());

        List<ProducedArtifact> artifacts = project.getArtifacts();
        assertEquals(1, artifacts.size());
        assertTrue(artifacts.contains(pomProduced));
        assertFalse(artifacts.contains(jarProduced));

        DefaultProjectManager projectManager = new DefaultProjectManager(session, Mockito.mock(ArtifactManager.class));
        Collection<ProducedArtifact> all = projectManager.getAllArtifacts(project);
        assertEquals(1, all.size());
        assertTrue(all.contains(pomProduced));
        assertFalse(all.contains(jarProduced));
    }

    @Test
    void moduleSourceHierarchyDoesNotProduceMainArtifactAndAttachesModuleJars() {
        InternalMavenSession session = Mockito.mock(InternalMavenSession.class);
        Packaging packaging = Mockito.mock(Packaging.class);
        when(packaging.producesMainArtifact()).thenReturn(true);
        when(session.requirePackaging("jar")).thenReturn(packaging);

        DefaultVersionParser versionParser =
                new DefaultVersionParser(new DefaultModelVersionParser(new GenericVersionScheme()));
        when(session.parseVersion("1.0-SNAPSHOT")).thenReturn(versionParser.parseVersion("1.0-SNAPSHOT"));

        ProducedArtifact pomProduced = Mockito.mock(ProducedArtifact.class);
        ProducedArtifact jarProduced = Mockito.mock(ProducedArtifact.class);
        ProducedArtifact moduleJarProduced = Mockito.mock(ProducedArtifact.class);

        when(session.getArtifact(
                        Mockito.eq(ProducedArtifact.class),
                        Mockito.argThat(a -> a != null && "pom".equals(a.getExtension()))))
                .thenReturn(pomProduced);
        when(session.getArtifact(
                        Mockito.eq(ProducedArtifact.class),
                        Mockito.argThat(a ->
                                a != null && "jar".equals(a.getExtension()) && "myArtifact".equals(a.getArtifactId()))))
                .thenReturn(jarProduced);
        when(session.getArtifact(
                        Mockito.eq(ProducedArtifact.class),
                        Mockito.argThat(a -> a != null && "org.foo.bar".equals(a.getArtifactId()))))
                .thenReturn(moduleJarProduced);

        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId("myGroup");
        mavenProject.setArtifactId("myArtifact");
        mavenProject.setVersion("1.0-SNAPSHOT");
        mavenProject.setPackaging("jar");
        mavenProject.setFile(
                Path.of("target", "project", "pom.xml").toAbsolutePath().toFile());
        org.apache.maven.artifact.Artifact legacyArtifact = new org.apache.maven.artifact.DefaultArtifact(
                "myGroup", "myArtifact", "1.0-SNAPSHOT", "compile", "jar", "", new TestArtifactHandler("jar"));
        mavenProject.setArtifact(legacyArtifact);

        Project project = new DefaultProject(session, mavenProject);
        ArtifactManager artifactManager = Mockito.mock(ArtifactManager.class);
        DefaultProjectManager projectManager = new DefaultProjectManager(session, artifactManager);

        // Before adding module source root: standard JAR produces main artifact
        assertTrue(project.getPackaging().producesMainArtifact());
        assertEquals(2, project.getArtifacts().size());

        // Add module source root
        projectManager.addSourceRoot(
                project,
                new DefaultSourceRoot(
                        ProjectScope.MAIN,
                        Language.JAVA_FAMILY,
                        "org.foo.bar",
                        null,
                        Path.of("myProject"),
                        null,
                        null,
                        false,
                        null,
                        true));

        // After adding module source hierarchy: producesMainArtifact is false
        assertFalse(project.getPackaging().producesMainArtifact());

        // project.getArtifacts() contains ONLY POM artifact (directory is NOT exposed as main artifact)
        List<ProducedArtifact> artifacts = project.getArtifacts();
        assertEquals(1, artifacts.size());
        assertTrue(artifacts.contains(pomProduced));
        assertFalse(artifacts.contains(jarProduced));

        // Legacy MavenProject.getArtifact() remains intact for backward compatibility
        assertEquals(legacyArtifact, mavenProject.getArtifact());

        // Attach module JAR
        ProducedArtifact attachedModuleJar = Mockito.mock(ProducedArtifact.class);
        when(attachedModuleJar.getGroupId()).thenReturn("myGroup");
        when(attachedModuleJar.getArtifactId()).thenReturn("org.foo.bar");
        when(attachedModuleJar.getBaseVersion()).thenReturn(versionParser.parseVersion("1.0-SNAPSHOT"));
        when(attachedModuleJar.getExtension()).thenReturn("jar");
        when(attachedModuleJar.getClassifier()).thenReturn("");
        when(session.toArtifact(attachedModuleJar))
                .thenReturn(new org.eclipse.aether.artifact.DefaultArtifact(
                        "myGroup", "org.foo.bar", "", "jar", "1.0-SNAPSHOT"));
        projectManager.attachArtifact(project, attachedModuleJar, Path.of("target/org.foo.bar-1.0-SNAPSHOT.jar"));

        // All artifacts includes POM and attached module JAR, without the directory main artifact
        Collection<ProducedArtifact> allArtifacts = projectManager.getAllArtifacts(project);
        assertEquals(2, allArtifacts.size());
        assertTrue(allArtifacts.contains(pomProduced));
        assertTrue(allArtifacts.contains(moduleJarProduced));
        assertFalse(allArtifacts.contains(jarProduced));
    }

    /**
     * Verifies that {@code projectManager.attachArtifact(…)} throws an exception,
     * and that the exception message contains the expected and actual <abbr>GAV</abbr>.
     *
     * @param expectedGAV the expected <abbr>GAV</abbr> that the exception message should contain
     * @param actualGAV the actual <abbr>GAV</abbr> that the exception message should contain
     */
    private void assertExceptionMessageContains(String expectedGAV, String actualGAV) {
        String cause = assertThrows(
                        IllegalArgumentException.class,
                        () -> projectManager.attachArtifact(project, artifact, artifactPath))
                .getMessage();
        Supplier<String> message = () ->
                String.format("The exception message does not contain the expected GAV. Message was:%n%s%n", cause);

        assertTrue(cause.contains(expectedGAV), message);
        assertTrue(cause.contains(actualGAV), message);
    }
}
