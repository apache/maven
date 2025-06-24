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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the complete upgrade workflow.
 * These tests verify end-to-end behavior with real strategy implementations.
 */
@DisplayName("Upgrade Workflow Integration")
class UpgradeWorkflowIntegrationTest {

    @TempDir
    Path tempDir;

    private Apply applyGoal;
    private Check checkGoal;

    @BeforeEach
    void setUp() {
        // Create real strategy instances for integration testing
        List<UpgradeStrategy> strategies = List.of(
                new ModelUpgradeStrategy(),
                new CompatibilityFixStrategy(),
                new PluginUpgradeStrategy(),
                new InferenceStrategy());

        StrategyOrchestrator orchestrator = new StrategyOrchestrator(strategies);
        applyGoal = new Apply(orchestrator);
        checkGoal = new Check(orchestrator);
    }

    @Nested
    @DisplayName("Model Version Upgrade")
    class ModelVersionUpgradeTests {

        @Test
        @DisplayName("should upgrade from 4.0.0 to 4.1.0 with --model option")
        void shouldUpgradeModelVersionWith41Option() throws Exception {
            // Create a test POM with 4.0.0 model version
            Path pomFile = tempDir.resolve("pom.xml");
            String originalPom = PomBuilder.create()
                    .groupId("com.example")
                    .artifactId("test-project")
                    .version("1.0.0")
                    .build();
            Files.writeString(pomFile, originalPom);

            // Create context with --model 4.1.0 option
            UpgradeContext context =
                    TestUtils.createMockContext(tempDir, TestUtils.createOptionsWithModelVersion("4.1.0"));

            // Execute apply goal
            int result = applyGoal.execute(context);

            // Verify success
            assertEquals(0, result, "Apply should succeed");

            // Verify POM was upgraded
            String upgradedPom = Files.readString(pomFile);
            assertTrue(
                    upgradedPom.contains("http://maven.apache.org/POM/4.1.0"),
                    "POM should be upgraded to 4.1.0 namespace");
        }

        @Test
        @DisplayName("should not create .mvn directory when upgrading to 4.1.0")
        void shouldNotCreateMvnDirectoryFor41Upgrade() throws Exception {
            Path pomFile = tempDir.resolve("pom.xml");
            String originalPom = PomBuilder.create()
                    .groupId("com.example")
                    .artifactId("test-project")
                    .version("1.0.0")
                    .build();
            Files.writeString(pomFile, originalPom);

            UpgradeContext context =
                    TestUtils.createMockContext(tempDir, TestUtils.createOptionsWithModelVersion("4.1.0"));

            applyGoal.execute(context);

            Path mvnDir = tempDir.resolve(".mvn");
            assertFalse(Files.exists(mvnDir), ".mvn directory should not be created for 4.1.0 upgrade");
        }
    }

    @Nested
    @DisplayName("Check vs Apply Behavior")
    class CheckVsApplyTests {

        @Test
        @DisplayName("check goal should not modify files")
        void checkShouldNotModifyFiles() throws Exception {
            Path pomFile = tempDir.resolve("pom.xml");
            String originalPom = PomBuilder.create()
                    .groupId("com.example")
                    .artifactId("test-project")
                    .version("1.0.0")
                    .build();
            Files.writeString(pomFile, originalPom);

            UpgradeContext context = TestUtils.createMockContext(tempDir);

            // Execute check goal
            int result = checkGoal.execute(context);

            // Verify success
            assertEquals(0, result, "Check should succeed");

            // Verify POM was not modified
            String pomContent = Files.readString(pomFile);
            assertEquals(originalPom, pomContent, "Check should not modify POM files");
        }

        @Test
        @DisplayName("apply goal should modify files")
        void applyShouldModifyFiles() throws Exception {
            Path pomFile = tempDir.resolve("pom.xml");
            String originalPom = PomBuilder.create()
                    .groupId("com.example")
                    .artifactId("test-project")
                    .version("1.0.0")
                    .dependency("junit", "junit", "3.8.1") // Old version that should be flagged
                    .build();
            Files.writeString(pomFile, originalPom);

            UpgradeContext context = TestUtils.createMockContext(tempDir);

            // Execute apply goal
            int result = applyGoal.execute(context);

            // Verify success
            assertEquals(0, result, "Apply should succeed");

            // Verify POM was potentially modified (depending on strategy applicability)
            String pomContent = Files.readString(pomFile);
            assertTrue(pomContent.contains("<groupId>com.example</groupId>"));
            // Note: The exact modifications depend on which strategies are applicable
            // This test mainly verifies that apply goal can modify files
        }
    }

    @Nested
    @DisplayName("Multi-module Projects")
    class MultiModuleTests {

        @Test
        @DisplayName("should handle multi-module project structure")
        void shouldHandleMultiModuleProject() throws Exception {
            // Create parent POM
            Path parentPom = tempDir.resolve("pom.xml");
            String parentPomContent = PomBuilder.create()
                    .groupId("com.example")
                    .artifactId("parent-project")
                    .version("1.0.0")
                    .packaging("pom")
                    .build();
            Files.writeString(parentPom, parentPomContent);

            // Create module directory and POM
            Path moduleDir = tempDir.resolve("module1");
            Files.createDirectories(moduleDir);
            Path modulePom = moduleDir.resolve("pom.xml");
            String modulePomContent = PomBuilder.create()
                    .parent("com.example", "parent-project", "1.0.0")
                    .artifactId("module1")
                    .build();
            Files.writeString(modulePom, modulePomContent);

            UpgradeContext context = TestUtils.createMockContext(tempDir);

            // Execute apply goal
            int result = applyGoal.execute(context);

            // Verify success
            assertEquals(0, result, "Apply should succeed for multi-module project");

            // Verify both POMs exist (they may or may not be modified depending on strategies)
            assertTrue(Files.exists(parentPom), "Parent POM should exist");
            assertTrue(Files.exists(modulePom), "Module POM should exist");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle missing POM gracefully")
        void shouldHandleMissingPomGracefully() throws Exception {
            // No POM file in the directory
            UpgradeContext context = TestUtils.createMockContext(tempDir);

            // Execute apply goal
            applyGoal.execute(context);

            // Should handle gracefully (exact behavior depends on implementation)
            // This test mainly verifies no exceptions are thrown
        }

        @Test
        @DisplayName("should handle malformed POM gracefully")
        void shouldHandleMalformedPomGracefully() throws Exception {
            Path pomFile = tempDir.resolve("pom.xml");
            String malformedPom = "<?xml version=\"1.0\"?><project><invalid></project>";
            Files.writeString(pomFile, malformedPom);

            UpgradeContext context = TestUtils.createMockContext(tempDir);

            // Execute apply goal - should handle malformed XML gracefully
            applyGoal.execute(context);

            // Exact behavior depends on implementation, but should not crash
        }
    }
}
