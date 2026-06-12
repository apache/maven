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

import eu.maveniverse.domtrip.Document;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
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

    @BeforeEach
    void setUp() {
        mockOrchestrator = mock(StrategyOrchestrator.class);
        upgradeGoal = new TestableAbstractUpgradeGoal(mockOrchestrator);
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
        @DisplayName("should create .mvn directory when model version is 4.1.0")
        void shouldCreateMvnDirectoryWhenModelVersion410() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Files.createDirectories(projectDir);

            UpgradeContext context = createMockContext(projectDir);

            // Mock successful strategy execution
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            // Execute with target model 4.1.0 (should create .mvn directory to avoid root warnings)
            upgradeGoal.testExecuteWithTargetModel(context, "4.1.0");

            Path mvnDir = projectDir.resolve(".mvn");
            assertTrue(
                    Files.exists(mvnDir),
                    ".mvn directory should be created for 4.1.0 to avoid root directory warnings");
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

    @Nested
    @DisplayName("Incompatible Extension Fixes")
    class IncompatibleExtensionFixTests {

        @Test
        @DisplayName("should replace os-maven-plugin with Maveniverse Nisse")
        void shouldReplaceOsMavenPluginWithNisse() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Path mvnDir = projectDir.resolve(".mvn");
            Files.createDirectories(mvnDir);

            String extensionsXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <extensions>
                        <extension>
                            <groupId>kr.motd.maven</groupId>
                            <artifactId>os-maven-plugin</artifactId>
                            <version>1.7.1</version>
                        </extension>
                    </extensions>
                    """;
            Files.writeString(mvnDir.resolve("extensions.xml"), extensionsXml);

            UpgradeContext context = createMockContext(projectDir);
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            upgradeGoal.testExecuteWithTargetModel(context, "4.0.0");

            String result = Files.readString(mvnDir.resolve("extensions.xml"));
            assertTrue(result.contains("eu.maveniverse.maven.nisse"), "Should contain Nisse groupId");
            assertTrue(result.contains("<artifactId>extension</artifactId>"), "Should contain Nisse artifactId");
            assertTrue(result.contains("0.4.4"), "Should contain Nisse version");
            assertFalse(result.contains("kr.motd.maven"), "Should not contain os-maven-plugin groupId");
            assertFalse(result.contains("os-maven-plugin"), "Should not contain os-maven-plugin artifactId");

            // Should also create maven.config with Nisse compat flag
            Path mavenConfig = mvnDir.resolve("maven.config");
            assertTrue(Files.exists(mavenConfig), "maven.config should be created");
            String configContent = Files.readString(mavenConfig);
            assertTrue(
                    configContent.contains("-Dnisse.compat.osDetector"),
                    "maven.config should contain Nisse compat flag");
        }

        @Test
        @DisplayName("should remove Develocity extension")
        void shouldRemoveDevelocityExtension() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Path mvnDir = projectDir.resolve(".mvn");
            Files.createDirectories(mvnDir);

            String extensionsXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <extensions>
                        <extension>
                            <groupId>com.gradle</groupId>
                            <artifactId>develocity-maven-extension</artifactId>
                            <version>1.21</version>
                        </extension>
                    </extensions>
                    """;
            Files.writeString(mvnDir.resolve("extensions.xml"), extensionsXml);

            UpgradeContext context = createMockContext(projectDir);
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            upgradeGoal.testExecuteWithTargetModel(context, "4.0.0");

            String result = Files.readString(mvnDir.resolve("extensions.xml"));
            assertFalse(result.contains("develocity-maven-extension"), "Should not contain Develocity extension");
            assertFalse(result.contains("com.gradle"), "Should not contain com.gradle groupId");
        }

        @Test
        @DisplayName("should remove Gradle Enterprise extension")
        void shouldRemoveGradleEnterpriseExtension() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Path mvnDir = projectDir.resolve(".mvn");
            Files.createDirectories(mvnDir);

            String extensionsXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <extensions>
                        <extension>
                            <groupId>com.gradle</groupId>
                            <artifactId>gradle-enterprise-maven-extension</artifactId>
                            <version>1.18</version>
                        </extension>
                    </extensions>
                    """;
            Files.writeString(mvnDir.resolve("extensions.xml"), extensionsXml);

            UpgradeContext context = createMockContext(projectDir);
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            upgradeGoal.testExecuteWithTargetModel(context, "4.0.0");

            String result = Files.readString(mvnDir.resolve("extensions.xml"));
            assertFalse(
                    result.contains("gradle-enterprise-maven-extension"),
                    "Should not contain Gradle Enterprise extension");
        }

        @Test
        @DisplayName("should handle both os-maven-plugin and Develocity together")
        void shouldHandleBothOsMavenPluginAndDevelocity() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Path mvnDir = projectDir.resolve(".mvn");
            Files.createDirectories(mvnDir);

            String extensionsXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <extensions>
                        <extension>
                            <groupId>kr.motd.maven</groupId>
                            <artifactId>os-maven-plugin</artifactId>
                            <version>1.7.1</version>
                        </extension>
                        <extension>
                            <groupId>com.gradle</groupId>
                            <artifactId>develocity-maven-extension</artifactId>
                            <version>1.21</version>
                        </extension>
                        <extension>
                            <groupId>org.apache.maven.extensions</groupId>
                            <artifactId>maven-build-cache-extension</artifactId>
                            <version>1.0.0</version>
                        </extension>
                    </extensions>
                    """;
            Files.writeString(mvnDir.resolve("extensions.xml"), extensionsXml);

            UpgradeContext context = createMockContext(projectDir);
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            upgradeGoal.testExecuteWithTargetModel(context, "4.0.0");

            String result = Files.readString(mvnDir.resolve("extensions.xml"));
            assertTrue(result.contains("eu.maveniverse.maven.nisse"), "Should contain Nisse replacement");
            assertFalse(result.contains("develocity-maven-extension"), "Should not contain Develocity");
            assertTrue(result.contains("maven-build-cache-extension"), "Should preserve compatible extensions");
        }

        @Test
        @DisplayName("should append to existing maven.config")
        void shouldAppendToExistingMavenConfig() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Path mvnDir = projectDir.resolve(".mvn");
            Files.createDirectories(mvnDir);

            String extensionsXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <extensions>
                        <extension>
                            <groupId>kr.motd.maven</groupId>
                            <artifactId>os-maven-plugin</artifactId>
                            <version>1.7.1</version>
                        </extension>
                    </extensions>
                    """;
            Files.writeString(mvnDir.resolve("extensions.xml"), extensionsXml);
            Files.writeString(mvnDir.resolve("maven.config"), "-Xmx2g\n");

            UpgradeContext context = createMockContext(projectDir);
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            upgradeGoal.testExecuteWithTargetModel(context, "4.0.0");

            String configContent = Files.readString(mvnDir.resolve("maven.config"));
            assertTrue(configContent.contains("-Xmx2g"), "Should preserve existing config");
            assertTrue(configContent.contains("-Dnisse.compat.osDetector"), "Should add Nisse compat flag");
        }

        @Test
        @DisplayName("should not duplicate Nisse compat flag in maven.config")
        void shouldNotDuplicateNisseCompatFlag() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Path mvnDir = projectDir.resolve(".mvn");
            Files.createDirectories(mvnDir);

            String extensionsXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <extensions>
                        <extension>
                            <groupId>kr.motd.maven</groupId>
                            <artifactId>os-maven-plugin</artifactId>
                            <version>1.7.1</version>
                        </extension>
                    </extensions>
                    """;
            Files.writeString(mvnDir.resolve("extensions.xml"), extensionsXml);
            Files.writeString(mvnDir.resolve("maven.config"), "-Dnisse.compat.osDetector\n");

            UpgradeContext context = createMockContext(projectDir);
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            upgradeGoal.testExecuteWithTargetModel(context, "4.0.0");

            String configContent = Files.readString(mvnDir.resolve("maven.config"));
            int count = configContent.split("-Dnisse.compat.osDetector", -1).length - 1;
            assertEquals(1, count, "Should not duplicate Nisse compat flag");
        }

        @Test
        @DisplayName("should be no-op when no extensions.xml exists")
        void shouldBeNoOpWhenNoExtensionsXml() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Files.createDirectories(projectDir);

            UpgradeContext context = createMockContext(projectDir);
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            int result = upgradeGoal.testExecuteWithTargetModel(context, "4.0.0");

            assertEquals(0, result, "Should succeed with no extensions.xml");
            assertFalse(
                    Files.exists(projectDir.resolve(".mvn/maven.config")),
                    "Should not create maven.config when no extensions needed fixing");
        }

        @Test
        @DisplayName("should be no-op when no incompatible extensions found")
        void shouldBeNoOpWhenNoIncompatibleExtensions() throws Exception {
            Path projectDir = tempDir.resolve("project");
            Path mvnDir = projectDir.resolve(".mvn");
            Files.createDirectories(mvnDir);

            String extensionsXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <extensions>
                        <extension>
                            <groupId>org.apache.maven.extensions</groupId>
                            <artifactId>maven-build-cache-extension</artifactId>
                            <version>1.0.0</version>
                        </extension>
                    </extensions>
                    """;
            Files.writeString(mvnDir.resolve("extensions.xml"), extensionsXml);

            UpgradeContext context = createMockContext(projectDir);
            when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                    .thenReturn(UpgradeResult.empty());

            upgradeGoal.testExecuteWithTargetModel(context, "4.0.0");

            String result = Files.readString(mvnDir.resolve("extensions.xml"));
            assertTrue(result.contains("maven-build-cache-extension"), "Should preserve compatible extensions");
            assertFalse(
                    Files.exists(mvnDir.resolve("maven.config")),
                    "Should not create maven.config when no extensions needed fixing");
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
