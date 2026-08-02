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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReactorReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void classifiedPomShouldResolveToAttachedArtifact() throws Exception {
        File projectPom = Files.createFile(tempDir.resolve("pom.xml")).toFile();
        File attachedPom = Files.createFile(tempDir.resolve("custom.pom")).toFile();

        MavenProject project = new MavenProject();
        project.setGroupId("org.apache.maven.its.mdep590");
        project.setArtifactId("producer");
        project.setVersion("1.0-SNAPSHOT");
        project.setFile(projectPom);
        project.setArtifact(newArtifact(null, projectPom));
        project.addAttachedArtifact(newArtifact("custom", attachedPom));

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(Collections.singletonList(project));

        ReactorReader reader = new ReactorReader(session);

        assertEquals(
                attachedPom,
                reader.findArtifact(new org.eclipse.aether.artifact.DefaultArtifact(
                        "org.apache.maven.its.mdep590:producer:pom:custom:1.0-SNAPSHOT")));
    }

    private static Artifact newArtifact(String classifier, File file) {
        Artifact artifact = new DefaultArtifact(
                "org.apache.maven.its.mdep590",
                "producer",
                "1.0-SNAPSHOT",
                Artifact.SCOPE_COMPILE,
                "pom",
                classifier,
                new DefaultArtifactHandler("pom"));
        artifact.setFile(file);
        return artifact;
    }
}
