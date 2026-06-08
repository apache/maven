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
import java.util.function.Supplier;

import org.apache.maven.api.Language;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.impl.DefaultModelVersionParser;
import org.apache.maven.impl.DefaultSourceRoot;
import org.apache.maven.impl.DefaultVersionParser;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class DefaultProjectManagerTest {

    private DefaultProjectManager projectManager;

    private Project project;

    private ProducedArtifact artifact;

    private Path artifactPath;

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

    /**
     * Verifies that {@code projectManager.attachArtifact(…)} throws an exception,
     * and that the expecption message contains the expected and actual <abbr>GAV</abbr>.
     *
     * @param expectedGAV the actual <abbr>GAV</abbr> that the exception message should contain
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
