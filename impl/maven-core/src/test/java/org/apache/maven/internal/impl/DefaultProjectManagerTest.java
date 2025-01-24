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

import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.impl.DefaultModelVersionParser;
import org.apache.maven.impl.DefaultVersionParser;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class DefaultProjectManagerTest {

    @Test
    void attachArtifact() {
        InternalMavenSession session = Mockito.mock(InternalMavenSession.class);
        ArtifactManager artifactManager = Mockito.mock(ArtifactManager.class);
        MavenProject mavenProject = new MavenProject();
        Project project = new DefaultProject(session, mavenProject);
        ProducedArtifact artifact = Mockito.mock(ProducedArtifact.class);
        Path path = Paths.get("");
        DefaultVersionParser versionParser =
                new DefaultVersionParser(new DefaultModelVersionParser(new GenericVersionScheme()));
        DefaultProjectManager projectManager = new DefaultProjectManager(session, artifactManager);

        mavenProject.setGroupId("myGroup");
        mavenProject.setArtifactId("myArtifact");
        mavenProject.setVersion("1.0-SNAPSHOT");
        when(artifact.getGroupId()).thenReturn("myGroup");
        when(artifact.getArtifactId()).thenReturn("myArtifact");
        when(artifact.getBaseVersion()).thenReturn(versionParser.parseVersion("1.0-SNAPSHOT"));
        projectManager.attachArtifact(project, artifact, path);

        when(artifact.getArtifactId()).thenReturn("anotherArtifact");
        assertThrows(IllegalArgumentException.class, () -> projectManager.attachArtifact(project, artifact, path));
    }
}
