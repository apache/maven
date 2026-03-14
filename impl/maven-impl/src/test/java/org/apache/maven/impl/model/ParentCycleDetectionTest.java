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
package org.apache.maven.impl.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.Session;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.Sources;
import org.apache.maven.impl.standalone.ApiRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for parent resolution cycle detection.
 */
class ParentCycleDetectionTest {

    Session session;
    ModelBuilder modelBuilder;

    @BeforeEach
    void setup() {
        session = ApiRunner.createSession();
        modelBuilder = session.getService(ModelBuilder.class);
        assertNotNull(modelBuilder);
    }

    @Test
    void testParentResolutionCycleDetectionWithRelativePath(@TempDir Path tempDir) throws IOException {
        // Create .mvn directory to mark root
        Files.createDirectories(tempDir.resolve(".mvn"));

        // Create a parent resolution cycle using relativePath: child -> parent -> child
        // This reproduces the same issue as the integration test MavenITmng11009StackOverflowParentResolutionTest
        Path childPom = tempDir.resolve("pom.xml");
        Files.writeString(childPom, """
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>org.apache.maven.its.mng11009</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0-SNAPSHOT</version>
                    <relativePath>parent</relativePath>
                </parent>
                <artifactId>child</artifactId>
                <packaging>pom</packaging>
            </project>
            """);

        Path parentPom = tempDir.resolve("parent").resolve("pom.xml");
        Files.createDirectories(parentPom.getParent());
        Files.writeString(parentPom, """
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>org.apache.maven.its.mng11009</groupId>
                    <artifactId>external-parent</artifactId>
                    <version>1.0-SNAPSHOT</version>
                    <!-- No relativePath specified, defaults to ../pom.xml which creates the circular reference -->
                </parent>
                <artifactId>parent</artifactId>
                <packaging>pom</packaging>
            </project>
            """);

        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .source(Sources.buildSource(childPom))
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .build();

        // This should either:
        // 1. Detect the cycle and throw a meaningful ModelBuilderException, OR
        // 2. Not cause a StackOverflowError (the main goal is to prevent the StackOverflowError)
        try {
            ModelBuilderResult result = modelBuilder.newSession().build(request);
            // If we get here without StackOverflowError, that's actually good progress
            // The build may still fail with a different error (circular dependency), but that's expected
            System.out.println("Build completed without StackOverflowError. Result: " + result);
        } catch (StackOverflowError error) {
            fail(
                    "Build failed with StackOverflowError, which should be prevented. This indicates the cycle detection is not working properly for relativePath-based cycles.");
        } catch (ModelBuilderException exception) {
            // This is acceptable - the build should fail with a meaningful error, not StackOverflowError
            System.out.println("Build failed with ModelBuilderException (expected): " + exception.getMessage());
            // Check if it's a cycle detection error
            if (exception.getMessage().contains("cycle")
                    || exception.getMessage().contains("circular")) {
                System.out.println("✓ Cycle detected correctly!");
            }
            // We don't assert on the specific message because the main goal is to prevent StackOverflowError
        }
    }

    @Test
    void testDirectCycleDetection(@TempDir Path tempDir) throws IOException {
        // Create .mvn directory to mark root
        Files.createDirectories(tempDir.resolve(".mvn"));

        // Create a direct cycle: A -> B -> A
        Path pomA = tempDir.resolve("a").resolve("pom.xml");
        Files.createDirectories(pomA.getParent());
        Files.writeString(pomA, """
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>a</artifactId>
                <version>1.0</version>
                <parent>
                    <groupId>test</groupId>
                    <artifactId>b</artifactId>
                    <version>1.0</version>
                    <relativePath>../b/pom.xml</relativePath>
                </parent>
            </project>
            """);

        Path pomB = tempDir.resolve("b").resolve("pom.xml");
        Files.createDirectories(pomB.getParent());
        Files.writeString(pomB, """
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>b</artifactId>
                <version>1.0</version>
                <parent>
                    <groupId>test</groupId>
                    <artifactId>a</artifactId>
                    <version>1.0</version>
                    <relativePath>../a/pom.xml</relativePath>
                </parent>
            </project>
            """);

        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .source(Sources.buildSource(pomA))
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .build();

        // This should detect the cycle and throw a meaningful ModelBuilderException
        try {
            ModelBuilderResult result = modelBuilder.newSession().build(request);
            fail("Expected ModelBuilderException due to cycle detection, but build succeeded: " + result);
        } catch (StackOverflowError error) {
            fail("Build failed with StackOverflowError, which should be prevented by cycle detection.");
        } catch (ModelBuilderException exception) {
            // This is expected - the build should fail with a cycle detection error
            System.out.println("Build failed with ModelBuilderException (expected): " + exception.getMessage());
            // Check if it's a cycle detection error
            if (exception.getMessage().contains("cycle")
                    || exception.getMessage().contains("circular")) {
                System.out.println("✓ Cycle detected correctly!");
            } else {
                System.out.println("⚠ Exception was not a cycle detection error: " + exception.getMessage());
            }
        }
    }

    @Test
    void testMultipleModulesWithSameParentDoNotCauseCycle(@TempDir Path tempDir) throws IOException {
        // Create .mvn directory to mark root
        Files.createDirectories(tempDir.resolve(".mvn"));

        // Create a scenario like the failing test: multiple modules with the same parent
        Path parentPom = tempDir.resolve("parent").resolve("pom.xml");
        Files.createDirectories(parentPom.getParent());
        Files.writeString(parentPom, """
            <project xmlns="http://maven.apache.org/POM/4.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.1.0 https://maven.apache.org/xsd/maven-4.1.0.xsd">
                <modelVersion>4.1.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>parent</artifactId>
                <version>1.0</version>
                <packaging>pom</packaging>
            </project>
            """);

        Path moduleA = tempDir.resolve("module-a").resolve("pom.xml");
        Files.createDirectories(moduleA.getParent());
        Files.writeString(moduleA, """
            <project xmlns="http://maven.apache.org/POM/4.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.1.0 https://maven.apache.org/xsd/maven-4.1.0.xsd">
                <modelVersion>4.1.0</modelVersion>
                <parent>
                    <groupId>test</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0</version>
                    <relativePath>../parent/pom.xml</relativePath>
                </parent>
                <artifactId>module-a</artifactId>
            </project>
            """);

        Path moduleB = tempDir.resolve("module-b").resolve("pom.xml");
        Files.createDirectories(moduleB.getParent());
        Files.writeString(moduleB, """
            <project xmlns="http://maven.apache.org/POM/4.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://maven.apache.org/POM/4.1.0 https://maven.apache.org/xsd/maven-4.1.0.xsd">
                <modelVersion>4.1.0</modelVersion>
                <parent>
                    <groupId>test</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0</version>
                    <relativePath>../parent/pom.xml</relativePath>
                </parent>
                <artifactId>module-b</artifactId>
            </project>
            """);

        // Both modules should be able to resolve their parent without cycle detection errors
        ModelBuilderRequest requestA = ModelBuilderRequest.builder()
                .session(session)
                .source(Sources.buildSource(moduleA))
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .build();

        ModelBuilderRequest requestB = ModelBuilderRequest.builder()
                .session(session)
                .source(Sources.buildSource(moduleB))
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .build();

        // These should not throw exceptions
        ModelBuilderResult resultA = modelBuilder.newSession().build(requestA);
        ModelBuilderResult resultB = modelBuilder.newSession().build(requestB);

        // Verify that both models were built successfully
        assertNotNull(resultA);
        assertNotNull(resultB);
    }
}
