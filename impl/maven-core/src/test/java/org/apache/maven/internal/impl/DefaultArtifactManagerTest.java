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

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultArtifactManager}.
 */
class DefaultArtifactManagerTest {

    /**
     * Regression test for MINSTALL-315: when {@code jar:jar} is run on a POM-packaged project,
     * it sets the project's main artifact file to the produced JAR. Since POM-packaged projects share
     * the same artifact coordinates for both the POM artifact and the project artifact, a subsequent
     * call to {@code setPath(pomArtifact, pomPath)} followed by {@code getPath(pomArtifact)} must
     * return the explicitly registered POM path — NOT the JAR file set by {@code jar:jar}.
     *
     * <p>The bug was that {@code getPath()} consulted {@code project.getArtifact().getFile()} before
     * checking the explicitly-registered {@code paths} map, so the JAR file was returned instead of
     * the POM path.
     */
    @Test
    void setPathTakesPrecedenceOverProjectArtifactFile() {
        // Simulate a pom-packaged project where jar:jar has set the project artifact's file to a JAR.
        // In a real build, this happens when the user runs "mvn jar:jar install:install" on a pom project.
        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId("com.example");
        mavenProject.setArtifactId("pom-project");
        mavenProject.setVersion("1.0");
        mavenProject.setPackaging("pom");

        // Create a pom-type artifact for the project (same as what ProjectArtifact wraps)
        ArtifactHandler pomHandler = new PomArtifactHandler();
        DefaultArtifact projectArtifact =
                new DefaultArtifact("com.example", "pom-project", "1.0", null, "pom", null, pomHandler);
        // jar:jar sets the project artifact's file to a JAR — this is the bug trigger
        File fakeJar = new File("target/pom-project-1.0.jar");
        projectArtifact.setFile(fakeJar);
        mavenProject.setArtifact(projectArtifact);

        // Wire up InternalMavenSession mock
        InternalMavenSession internalSession = mock(InternalMavenSession.class);
        MavenSession mavenSession = new MavenSession(
                null, new DefaultRepositorySystemSession(h -> false), new DefaultMavenExecutionRequest(), null);
        mavenSession.setAllProjects(List.of(mavenProject));
        when(internalSession.getMavenSession()).thenReturn(mavenSession);

        DefaultArtifactManager manager = new DefaultArtifactManager(internalSession);

        // Create a ProducedArtifact mock representing the POM artifact.
        // key() must match the format produced by Artifact.key():
        //   groupId + ':' + artifactId + ':' + extension + (classifier.isEmpty() ? "" : ":" + classifier) + ':' +
        // version
        // For a no-classifier POM artifact this is "com.example:pom-project:pom:1.0" (single colon before version).
        ProducedArtifact pomArtifact = mock(ProducedArtifact.class);
        when(pomArtifact.key()).thenReturn("com.example:pom-project:pom:1.0");

        Path pomPath = Path.of("pom.xml").toAbsolutePath();

        // Register the POM path explicitly (what maven-install-plugin does).
        // setPath() also calls projectArtifact.setFile(pomPath.toFile()) as a side-effect,
        // so immediately after this call even the old buggy code (project-loop first) would
        // return pomPath via the project loop — both implementations pass at that point.
        manager.setPath(pomArtifact, pomPath);

        // Re-arm the bug condition: simulate jar:jar running again after install:install has
        // already registered the POM path in the paths map. This is the critical state that
        // distinguishes the two implementations:
        //   - old code (project-loop first): returns fakeJar → WRONG
        //   - new code (paths-map first):    returns pomPath → CORRECT
        projectArtifact.setFile(fakeJar);

        // Assert: getPath() must return the POM path from the paths map,
        // NOT the JAR now in the project artifact's file field.
        // This assertion FAILS on the unfixed code (project-loop first → returns fakeJar)
        // and PASSES on the fixed code (paths-map first → returns pomPath).
        Optional<Path> result = manager.getPath(pomArtifact);
        assertTrue(result.isPresent(), "getPath() must return a non-empty Optional after setPath()");
        assertEquals(
                pomPath,
                result.get(),
                "getPath() must return the explicitly-set POM path from the paths map,"
                        + " not the JAR file in the project artifact (MINSTALL-315)");
    }

    /**
     * Minimal ArtifactHandler for POM-type artifacts, matching the handler used by
     * {@link org.apache.maven.project.artifact.ProjectArtifact}.
     */
    private static class PomArtifactHandler implements ArtifactHandler {
        @Override
        public String getClassifier() {
            return null;
        }

        @Override
        public String getDirectory() {
            return null;
        }

        @Override
        public String getExtension() {
            return "pom";
        }

        @Override
        public String getLanguage() {
            return "none";
        }

        @Override
        public String getPackaging() {
            return "pom";
        }

        @Override
        @Deprecated
        public boolean isAddedToClasspath() {
            return false;
        }

        @Override
        public boolean isIncludesDependencies() {
            return false;
        }
    }
}
