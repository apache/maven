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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void testParentResolutionCycleDetection(@TempDir Path tempDir) throws IOException {
        // Create a parent resolution cycle: A -> B -> A
        Path pomA = tempDir.resolve("a").resolve("pom.xml");
        Files.createDirectories(pomA.getParent());
        Files.writeString(
                pomA,
                """
            <project root="true">
                <modelVersion>4.1.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>a</artifactId>
                <version>1.0</version>
                <parent>
                    <groupId>test</groupId>
                    <artifactId>b</artifactId>
                    <version>1.0</version>
                </parent>
            </project>
            """);

        Path pomB = tempDir.resolve("b").resolve("pom.xml");
        Files.createDirectories(pomB.getParent());
        Files.writeString(
                pomB,
                """
            <project>
                <modelVersion>4.1.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>b</artifactId>
                <version>1.0</version>
                <parent>
                    <groupId>test</groupId>
                    <artifactId>a</artifactId>
                    <version>1.0</version>
                </parent>
            </project>
            """);

        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .source(Sources.buildSource(pomA))
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .build();

        ModelBuilderException exception = assertThrows(ModelBuilderException.class, () -> {
            modelBuilder.newSession().build(request);
        });

        assertTrue(
                exception.getMessage().contains("The parents form a cycle"),
                "Expected cycle detection error, but got: " + exception.getMessage());
    }

    @Test
    void testMultipleModulesWithSameParentDoNotCauseCycle(@TempDir Path tempDir) throws IOException {
        // Create a scenario like the failing test: multiple modules with the same parent
        Path parentPom = tempDir.resolve("parent").resolve("pom.xml");
        Files.createDirectories(parentPom.getParent());
        Files.writeString(
                parentPom,
                """
            <project root="true">
                <modelVersion>4.1.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>parent</artifactId>
                <version>1.0</version>
                <packaging>pom</packaging>
            </project>
            """);

        Path moduleA = tempDir.resolve("module-a").resolve("pom.xml");
        Files.createDirectories(moduleA.getParent());
        Files.writeString(
                moduleA,
                """
            <project>
                <modelVersion>4.1.0</modelVersion>
                <parent>
                    <groupId>test</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0</version>
                </parent>
                <artifactId>module-a</artifactId>
            </project>
            """);

        Path moduleB = tempDir.resolve("module-b").resolve("pom.xml");
        Files.createDirectories(moduleB.getParent());
        Files.writeString(
                moduleB,
                """
            <project>
                <modelVersion>4.1.0</modelVersion>
                <parent>
                    <groupId>test</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0</version>
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
        assertTrue(resultA.getEffectiveModel().getGroupId().equals("test"));
        assertTrue(resultB.getEffectiveModel().getGroupId().equals("test"));
    }
}
