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
package org.apache.maven.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A parent served by the workspace reader gets a basedir only when it is a project of the session.
 * The reactor reader also serves what it stored in the project-local repository, and such a parent
 * must look like one resolved from a repository, as on Maven 3.
 */
class WorkspaceParentProjectBuilderTest extends AbstractCoreMavenComponentTestCase {

    private static final String GROUP_ID = "org.apache.maven.its.workspace";

    @TempDir
    Path tempDir;

    private Path workspaceParentPom;

    @Override
    protected String getProjectsDirectory() {
        return "src/test/projects/project-builder";
    }

    @Override
    protected WorkspaceReader getWorkspaceReader() {
        return new WorkspaceReader() {
            private final WorkspaceRepository repository = new WorkspaceRepository("test");

            @Override
            public WorkspaceRepository getRepository() {
                return repository;
            }

            @Override
            public File findArtifact(Artifact artifact) {
                boolean parentPom = GROUP_ID.equals(artifact.getGroupId())
                        && "parent".equals(artifact.getArtifactId())
                        && "pom".equals(artifact.getExtension());
                return parentPom && workspaceParentPom != null ? workspaceParentPom.toFile() : null;
            }

            @Override
            public List<String> findVersions(Artifact artifact) {
                return findArtifact(artifact) != null ? List.of(artifact.getVersion()) : List.of();
            }
        };
    }

    @Test
    void parentFromProjectLocalRepositoryHasNoBasedir() throws Exception {
        workspaceParentPom = writePom(tempDir.resolve(".mvn/target/project-local-repo/parent-1.pom"), "parent");
        MavenSession session = createMavenSession(
                writePom(tempDir.resolve("other/pom.xml"), "other").toFile());

        MavenProject parent = buildChild(session).getParent();

        assertNotNull(parent);
        assertNull(parent.getFile());
        assertNull(parent.getBasedir());
    }

    @Test
    void parentOfTheSessionKeepsItsBasedir() throws Exception {
        workspaceParentPom = writePom(tempDir.resolve("parent/pom.xml"), "parent");
        MavenSession session = createMavenSession(workspaceParentPom.toFile());

        MavenProject parent = buildChild(session).getParent();

        assertNotNull(parent);
        assertEquals(workspaceParentPom.toFile(), parent.getFile());
        assertEquals(workspaceParentPom.getParent().toFile(), parent.getBasedir());
    }

    private MavenProject buildChild(MavenSession session) throws Exception {
        Path childPom = tempDir.resolve("child/pom.xml");
        Files.createDirectories(childPom.getParent());
        Files.writeString(childPom, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>%s</groupId>
                    <artifactId>parent</artifactId>
                    <version>1</version>
                    <relativePath/>
                  </parent>
                  <artifactId>child</artifactId>
                </project>
                """.formatted(GROUP_ID));
        ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
        configuration.setRepositorySession(session.getRepositorySession());
        return projectBuilder.build(childPom.toFile(), configuration).getProject();
    }

    private static Path writePom(Path pom, String artifactId) throws IOException {
        Files.createDirectories(pom.getParent());
        Files.writeString(pom, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                </project>
                """.formatted(GROUP_ID, artifactId));
        return pom;
    }
}
