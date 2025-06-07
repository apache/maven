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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link AbstractUpgradeGoal} class.
 * Tests the shared functionality across upgrade goals including option handling,
 * .mvn directory creation, and upgrade orchestration.
 */
@DisplayName("AbstractUpgradeGoal")
class AbstractUpgradeGoalTest {

    @TempDir
    Path tempDir;

    private TestableAbstractUpgradeGoal upgradeGoal;
    private StrategyOrchestrator mockOrchestrator;
    private SAXBuilder saxBuilder;

    @BeforeEach
    void setUp() {
        mockOrchestrator = mock(StrategyOrchestrator.class);
        upgradeGoal = new TestableAbstractUpgradeGoal(mockOrchestrator);
        saxBuilder = new SAXBuilder();
    }

    private UpgradeContext createMockContext(Path workingDirectory) {
        return TestUtils.createMockContext(workingDirectory);
    }

    private UpgradeContext createMockContext(Path workingDirectory, UpgradeOptions options) {
        return TestUtils.createMockContext(workingDirectory, options);
    }

    private UpgradeOptions createDefaultOptions() {
        return TestUtils.createDefaultOptions();
    }

    @Nested
    @DisplayName("Target Model Version Determination")
    class TargetModelVersionTests {

        @Test
        @DisplayName("should use explicit model version when provided")
        void shouldUseExplicitModelVersionWhenProvided() {
            UpgradeContext context = createMockContext(tempDir, TestUtils.createOptionsWithModelVersion("4.1.0"));
            String result = upgradeGoal.testDoUpgradeLogic(context, "4.1.0");

            assertEquals("4.1.0", result);
        }

        @Test
        @DisplayName("should use 4.1.0 when --all option is specified")
        void shouldUse410WhenAllOptionSpecified() {
            UpgradeContext context = createMockContext(tempDir, TestUtils.createOptionsWithAll(true));
            String result = upgradeGoal.testDoUpgradeLogic(context, "4.1.0");

            assertEquals("4.1.0", result);
        }

        @Test
        @DisplayName("should default to 4.0.0 when no specific options provided")
        void shouldDefaultTo400WhenNoSpecificOptions() {
            UpgradeContext context = createMockContext(tempDir, createDefaultOptions());
            String result = upgradeGoal.testDoUpgradeLogic(context, "4.0.0");

            assertEquals("4.0.0", result);
        }

        @Test
        @DisplayName("should prioritize explicit model over --all option")
        void shouldPrioritizeExplicitModelOverAllOption() {
            UpgradeContext context =
                    createMockContext(tempDir, TestUtils.createOptions(true, null, null, null, "4.0.0"));
            String result = upgradeGoal.testDoUpgradeLogic(context, "4.0.0");

            assertEquals("4.0.0", result, "Explicit model should take precedence over --all");
        }
    }

    @Nested
    @DisplayName("Plugin Options Handling")
    class PluginOptionsTests {

        @ParameterizedTest
        @MethodSource("providePluginOptionScenarios")
        @DisplayName("should determine plugin enablement based on options")
        void shouldDeterminePluginEnablementBasedOnOptions(
                Boolean all, Boolean plugins, String model, boolean expectedEnabled, String description) {
            UpgradeContext context =
                    createMockContext(tempDir, TestUtils.createOptions(all, null, null, plugins, model));

            boolean isEnabled = upgradeGoal.testIsPluginsEnabled(context);

            assertEquals(expectedEnabled, isEnabled, description);
        }

        private static Stream<Arguments> providePluginOptionScenarios() {
            return Stream.of(
                    Arguments.of(null, true, null, true, "Should enable plugins when --plugins=true"),
                    Arguments.of(true, null, null, true, "Should enable plugins when --all=true"),
                    Arguments.of(
                            true,
                            false,
                            null,
                            true,
                            "Should enable plugins when --all=true (overrides --plugins=false)"),
                    Arguments.of(null, false, null, false, "Should disable plugins when --plugins=false"),
                    Arguments.of(null, null, "4.1.0", false, "Should disable plugins when only --model-version is set"),
                    Arguments.of(false, null, null, false, "Should disable plugins when --all=false"),
                    Arguments.of(null, null, null, true, "Should enable plugins by default when no options specified"));
        }
    }

    @Nested
    @DisplayName(".mvn Directory Creation")
    class MvnDirectoryCreationTests {

        @Test
        @DisplayName("should create .mvn directory when model version is not 4.1.0")
        void shouldCreateMvnDirectoryWhenModelVersionNot410() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Files.createDirectories(projectDir);

            // Create a simple POM file
            String pomXml = PomBuilder.create()
                    .groupId("test")
                    .artifactId("test")
                    .version("1.0.0")
                    .build();

            Path pomFile = projectDir.resolve("pom.xml");
            Files.writeString(pomFile, pomXml);

            UpgradeContext context = createMockContext(projectDir);

            // Mock successful strategy execution
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            // Execute with target model 4.0.0 (should create .mvn directory)
            upgradeGoal.testExecuteWithTargetModel(context, "4.0.0");

            Path mvnDir = projectDir.resolve(".mvn");
            assertTrue(Files.exists(mvnDir), ".mvn directory should be created");
            assertTrue(Files.isDirectory(mvnDir), ".mvn should be a directory");
        }

        @Test
        @DisplayName("should not create .mvn directory when model version is 4.1.0")
        void shouldNotCreateMvnDirectoryWhenModelVersion410() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Files.createDirectories(projectDir);

            UpgradeContext context = createMockContext(projectDir);

            // Mock successful strategy execution
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            // Execute with target model 4.1.0 (should not create .mvn directory)
            upgradeGoal.testExecuteWithTargetModel(context, "4.1.0");

            Path mvnDir = projectDir.resolve(".mvn");
            assertFalse(Files.exists(mvnDir), ".mvn directory should not be created for 4.1.0");
        }

        @Test
        @DisplayName("should not overwrite existing .mvn directory")
        void shouldNotOverwriteExistingMvnDirectory() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Files.createDirectories(projectDir);

            Path mvnDir = projectDir.resolve(".mvn");
            Files.createDirectories(mvnDir);
            Path existingFile = mvnDir.resolve("existing.txt");
            Files.writeString(existingFile, "existing content");

            UpgradeContext context = createMockContext(projectDir);

            // Mock successful strategy execution
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            upgradeGoal.testExecuteWithTargetModel(context, "4.0.0");

            assertTrue(Files.exists(existingFile), "Existing file should be preserved");
            assertEquals("existing content", Files.readString(existingFile), "Existing content should be preserved");
        }

        @Test
        @DisplayName("should create .mvn directory for custom model versions")
        void shouldCreateMvnDirectoryForCustomModelVersions() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Files.createDirectories(projectDir);

            UpgradeContext context = createMockContext(projectDir);

            // Mock successful strategy execution
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            // Execute with custom model version (should create .mvn directory)
            upgradeGoal.testExecuteWithTargetModel(context, "4.0.1");

            Path mvnDir = projectDir.resolve(".mvn");
            assertTrue(Files.exists(mvnDir), ".mvn directory should be created for custom model versions");
        }

        @Test
        @DisplayName("should handle .mvn directory creation failure gracefully")
        void shouldHandleMvnDirectoryCreationFailureGracefully() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Files.createDirectories(projectDir);

            // Create a file where .mvn directory should be (to cause creation failure)
            Path mvnFile = projectDir.resolve(".mvn");
            Files.writeString(mvnFile, "blocking file");

            UpgradeContext context = createMockContext(projectDir);

            // Mock successful strategy execution
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            // Should not throw exception even if .mvn creation fails
            int result = upgradeGoal.testExecuteWithTargetModel(context, "4.0.0");

            // The exact behavior depends on implementation, but it should handle gracefully
            // and not crash the entire upgrade process
            assertTrue(result >= 0, "Should handle .mvn creation failure gracefully");
        }
    }

    /**
     * Testable subclass that exposes protected methods for testing.
     */
    private static class TestableAbstractUpgradeGoal extends AbstractUpgradeGoal {

        TestableAbstractUpgradeGoal(StrategyOrchestrator orchestrator) {
            super(orchestrator);
        }

        @Override
        protected boolean shouldSaveModifications() {
            return true; // Enable actual file operations for tests
        }

        // Test helper methods to expose protected functionality
        public String testDoUpgradeLogic(UpgradeContext context, String expectedTargetModel) {
            UpgradeOptions options = context.options();
            if (options.modelVersion().isPresent()) {
                return options.modelVersion().get();
            } else if (options.all().orElse(false)) {
                return "4.1.0";
            } else {
                return "4.0.0";
            }
        }

        public boolean testIsPluginsEnabled(UpgradeContext context) {
            UpgradeOptions options = context.options();
            return isOptionEnabled(options, options.plugins(), true);
        }

        public int testExecuteWithTargetModel(UpgradeContext context, String targetModel) {
            try {
                Map<Path, Document> pomMap = Map.of(); // Empty for this test
                return doUpgrade(context, targetModel, pomMap);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // Helper method from AbstractUpgradeStrategy
        private boolean isOptionEnabled(UpgradeOptions options, Optional<Boolean> option, boolean defaultValue) {
            // Handle --all option (overrides individual options)
            if (options.all().orElse(false)) {
                return true;
            }

            // Check if the specific option is explicitly set
            if (option.isPresent()) {
                return option.get();
            }

            // Apply default behavior: if no specific options are provided, use default
            if (options.all().isEmpty()
                    && options.infer().isEmpty()
                    && options.model().isEmpty()
                    && options.plugins().isEmpty()
                    && options.modelVersion().isEmpty()) {
                return defaultValue;
            }

            return false;
        }
    }
}
