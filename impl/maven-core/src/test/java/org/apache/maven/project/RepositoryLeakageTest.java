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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test to verify that repositories from one project don't leak to sibling projects.
 */
@PlexusTest
public class RepositoryLeakageTest extends AbstractMavenProjectTestCase {

    @Test
    @SuppressWarnings("checkstyle:MethodLength")
    public void testRepositoryLeakageBetweenSiblings() throws Exception {
        // Create a temporary directory structure for our test
        Path tempDir = Files.createTempDirectory("maven-repo-leakage-test");

        try {
            // Create parent POM
            Path parentPom = tempDir.resolve("pom.xml");
            Files.writeString(parentPom, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0</version>
                    <packaging>pom</packaging>

                    <modules>
                        <module>child1</module>
                        <module>child2</module>
                    </modules>
                </project>
                """);

            // Create child1 with specific repository
            Path child1Dir = tempDir.resolve("child1");
            Files.createDirectories(child1Dir);
            Path child1Pom = child1Dir.resolve("pom.xml");
            Files.writeString(child1Pom, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>test</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0</version>
                    </parent>
                    <artifactId>child1</artifactId>

                    <repositories>
                        <repository>
                            <id>child1-repo</id>
                            <url>https://child1.example.com/repo</url>
                        </repository>
                    </repositories>
                </project>
                """);

            // Create child2 with different repository
            Path child2Dir = tempDir.resolve("child2");
            Files.createDirectories(child2Dir);
            Path child2Pom = child2Dir.resolve("pom.xml");
            Files.writeString(child2Pom, """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>test</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0</version>
                    </parent>
                    <artifactId>child2</artifactId>

                    <repositories>
                        <repository>
                            <id>child2-repo</id>
                            <url>https://child2.example.com/repo</url>
                        </repository>
                    </repositories>
                </project>
                """);

            // Create a shared ProjectBuildingRequest
            ProjectBuildingRequest sharedRequest = newBuildingRequest();

            // Build child1 first
            ProjectBuildingResult result1 = projectBuilder.build(child1Pom.toFile(), sharedRequest);
            MavenProject child1Project = result1.getProject();

            // Capture repositories after building child1

            // Build child2 using the same shared request
            ProjectBuildingResult result2 = projectBuilder.build(child2Pom.toFile(), sharedRequest);
            MavenProject child2Project = result2.getProject();

            // Capture repositories after building child2
            List<ArtifactRepository> repositoriesAfterChild2 = List.copyOf(sharedRequest.getRemoteRepositories());

            // Verify that child1 has its own repository
            boolean child1HasOwnRepo = child1Project.getRemoteArtifactRepositories().stream()
                    .anyMatch(repo -> "child1-repo".equals(repo.getId()));
            assertTrue(child1HasOwnRepo, "Child1 should have its own repository");

            // Verify that child2 has its own repository
            boolean child2HasOwnRepo = child2Project.getRemoteArtifactRepositories().stream()
                    .anyMatch(repo -> "child2-repo".equals(repo.getId()));
            assertTrue(child2HasOwnRepo, "Child2 should have its own repository");

            // Print debug information
            System.out.println("=== REPOSITORY LEAKAGE TEST RESULTS ===");
            System.out.println(
                    "Repositories in shared request after building child2: " + repositoriesAfterChild2.size());
            repositoriesAfterChild2.forEach(
                    repo -> System.out.println("  - " + repo.getId() + " (" + repo.getUrl() + ")"));

            System.out.println("Child1 project repositories:");
            child1Project
                    .getRemoteArtifactRepositories()
                    .forEach(repo -> System.out.println("  - " + repo.getId() + " (" + repo.getUrl() + ")"));

            System.out.println("Child2 project repositories:");
            child2Project
                    .getRemoteArtifactRepositories()
                    .forEach(repo -> System.out.println("  - " + repo.getId() + " (" + repo.getUrl() + ")"));
            System.out.println("=======================================");

            // Check for leakage: child2 should NOT have child1's repository
            boolean child2HasChild1Repo = child2Project.getRemoteArtifactRepositories().stream()
                    .anyMatch(repo -> "child1-repo".equals(repo.getId()));
            assertFalse(child2HasChild1Repo, "Child2 should NOT have child1's repository (leakage detected!)");

            // Check for leakage in the shared request
            boolean sharedRequestHasChild1Repo =
                    repositoriesAfterChild2.stream().anyMatch(repo -> "child1-repo".equals(repo.getId()));
            boolean sharedRequestHasChild2Repo =
                    repositoriesAfterChild2.stream().anyMatch(repo -> "child2-repo".equals(repo.getId()));

            // Print debug information
            /*
            System.out.println("Repositories after child1: " + repositoriesAfterChild1.size());
            repositoriesAfterChild1.forEach(repo -> System.out.println("  - " + repo.getId() + ": " + repo.getUrl()));

            System.out.println("Repositories after child2: " + repositoriesAfterChild2.size());
            repositoriesAfterChild2.forEach(repo -> System.out.println("  - " + repo.getId() + ": " + repo.getUrl()));
            */

            // The shared request should not accumulate repositories from both children
            if (sharedRequestHasChild1Repo && sharedRequestHasChild2Repo) {
                fail("REPOSITORY LEAKAGE DETECTED: Shared request contains repositories from both children!");
            }

        } finally {
            // Clean up
            deleteRecursively(tempDir.toFile());
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
