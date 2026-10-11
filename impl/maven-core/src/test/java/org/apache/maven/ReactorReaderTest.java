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
package org.apache.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.Language;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.impl.DefaultSourceRoot;
import org.apache.maven.internal.impl.InternalMavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ReactorReaderTest {

    private MavenSession session;
    private InternalMavenSession internalSession;

    @BeforeEach
    void setUp() {
        session = Mockito.mock(MavenSession.class);
        internalSession = Mockito.mock(InternalMavenSession.class);
        when(session.getSession()).thenReturn(internalSession);
        MavenExecutionRequest request = Mockito.mock(MavenExecutionRequest.class);
        when(request.getRootDirectory()).thenReturn(Path.of("target"));
        when(request.getUserProperties()).thenReturn(new java.util.Properties());
        when(session.getRequest()).thenReturn(request);
    }

    @Test
    void producesMainArtifactForOrdinaryJarProject() {
        Packaging jarPackaging = Mockito.mock(Packaging.class);
        when(jarPackaging.producesMainArtifact()).thenReturn(true);
        when(internalSession.requirePackaging("jar")).thenReturn(jarPackaging);

        MavenProject project = new MavenProject();
        project.setGroupId("org.test");
        project.setArtifactId("standard-jar");
        project.setVersion("1.0.0");
        project.setPackaging("jar");
        project.setBuild(new Build());

        when(session.getProjects()).thenReturn(List.of(project));
        ReactorReader reader = new ReactorReader(session);

        assertTrue(reader.producesMainArtifact(project));
    }

    @Test
    void producesMainArtifactFalseForPomProject() {
        MavenProject project = new MavenProject();
        project.setGroupId("org.test");
        project.setArtifactId("parent-pom");
        project.setVersion("1.0.0");
        project.setPackaging("pom");

        when(session.getProjects()).thenReturn(List.of(project));
        ReactorReader reader = new ReactorReader(session);

        assertFalse(reader.producesMainArtifact(project));
    }

    @Test
    void producesMainArtifactFalseForBomProject() {
        Packaging bomPackaging = Mockito.mock(Packaging.class);
        when(bomPackaging.producesMainArtifact()).thenReturn(false);
        when(internalSession.requirePackaging("bom")).thenReturn(bomPackaging);

        MavenProject project = new MavenProject();
        project.setGroupId("org.test");
        project.setArtifactId("bom-project");
        project.setVersion("1.0.0");
        project.setPackaging("bom");

        when(session.getProjects()).thenReturn(List.of(project));
        ReactorReader reader = new ReactorReader(session);

        assertFalse(reader.producesMainArtifact(project));
    }

    @Test
    void producesMainArtifactFalseForModuleSourceHierarchyProject() {
        Packaging jarPackaging = Mockito.mock(Packaging.class);
        when(jarPackaging.producesMainArtifact()).thenReturn(true);
        when(internalSession.requirePackaging("jar")).thenReturn(jarPackaging);

        MavenProject project = new MavenProject();
        project.setGroupId("org.test");
        project.setArtifactId("modular-project");
        project.setVersion("1.0.0");
        project.setPackaging("jar");
        project.setBuild(new Build());

        // Add modular source root
        project.addSourceRoot(new DefaultSourceRoot(
                ProjectScope.MAIN,
                Language.JAVA_FAMILY,
                "org.test.module.a",
                null,
                Path.of("src/module-a"),
                null,
                null,
                false,
                null,
                true));

        when(session.getProjects()).thenReturn(List.of(project));
        ReactorReader reader = new ReactorReader(session);

        assertFalse(reader.producesMainArtifact(project));
    }

    @Test
    void reactorFindsPomAndAttachedArtifactsForModularProject() throws Exception {
        MavenProject project = new MavenProject();
        project.setGroupId("org.test");
        project.setArtifactId("modular-project");
        project.setVersion("1.0.0");
        project.setPackaging("jar");
        Build build = new Build();
        build.setOutputDirectory("target/classes");
        project.setBuild(build);

        File pomFile = File.createTempFile("pom", ".xml");
        pomFile.deleteOnExit();
        project.setFile(pomFile);
        project.setArtifact(new org.apache.maven.artifact.DefaultArtifact(
                "org.test",
                "modular-project",
                "1.0.0",
                "compile",
                "jar",
                "",
                new org.apache.maven.artifact.handler.DefaultArtifactHandler("jar")));

        // Attached module jar
        File moduleJarFile = File.createTempFile("module-a", ".jar");
        moduleJarFile.deleteOnExit();
        org.apache.maven.artifact.Artifact attached = new org.apache.maven.artifact.DefaultArtifact(
                "org.test",
                "modular-project",
                "1.0.0",
                "compile",
                "jar",
                "module-a",
                new org.apache.maven.artifact.handler.DefaultArtifactHandler("jar"));
        attached.setFile(moduleJarFile);
        project.addAttachedArtifact(attached);

        when(session.getProjects()).thenReturn(List.of(project));
        ReactorReader reader = new ReactorReader(session);

        // POM artifact resolution
        File foundPom = reader.findArtifact(new DefaultArtifact("org.test", "modular-project", "pom", "1.0.0"));
        assertEquals(pomFile, foundPom);

        // Attached artifact resolution
        File foundAttached =
                reader.findArtifact(new DefaultArtifact("org.test", "modular-project", "module-a", "jar", "1.0.0"));
        assertEquals(moduleJarFile, foundAttached);
    }
}
