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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link ModelUpgradeStrategy} class.
 * Tests Maven model version upgrades and namespace transformations.
 */
@DisplayName("ModelUpgradeStrategy")
class ModelUpgradeStrategyTest {

    private ModelUpgradeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ModelUpgradeStrategy();
    }

    private UpgradeContext createMockContext(UpgradeOptions options) {
        return TestUtils.createMockContext(options);
    }

    @Nested
    @DisplayName("Applicability")
    class ApplicabilityTests {

        @ParameterizedTest
        @MethodSource("provideApplicabilityScenarios")
        @DisplayName("should determine applicability based on options")
        void shouldDetermineApplicabilityBasedOnOptions(
                Boolean all, String model, boolean expectedApplicable, String description) {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptions(all, null, null, null, model));

            boolean isApplicable = strategy.isApplicable(context);

            assertEquals(expectedApplicable, isApplicable, description);
        }

        private static Stream<Arguments> provideApplicabilityScenarios() {
            return Stream.of(
                    Arguments.of(null, "4.1.0", true, "Should be applicable when --model=4.1.0 is specified"),
                    Arguments.of(true, null, true, "Should be applicable when --all is specified"),
                    Arguments.of(true, "4.0.0", true, "Should be applicable when --all is specified (overrides model)"),
                    Arguments.of(null, null, false, "Should not be applicable by default"),
                    Arguments.of(false, null, false, "Should not be applicable when --all=false"),
                    Arguments.of(null, "4.0.0", false, "Should not be applicable for same version (4.0.0)"),
                    Arguments.of(false, "4.1.0", true, "Should be applicable for model upgrade even when --all=false"),
                    Arguments.of(false, "4.2.0", true, "Should be applicable for model upgrade even when --all=false"),
                    Arguments.of(null, "4.2.0", true, "Should be applicable when --model=4.2.0 is specified"));
        }

        @Test
        @DisplayName("should handle conflicting option combinations")
        void shouldHandleConflictingOptionCombinations() {
            // Test case where multiple conflicting options are set
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptions(
                    true, // --all
                    false, // --infer (conflicts with --all)
                    false, // --fix-model (conflicts with --all)
                    false, // --plugins (conflicts with --all)
                    "4.0.0" // --model (conflicts with --all)
                    ));

            // --all should take precedence and make strategy applicable
            assertTrue(
                    strategy.isApplicable(context),
                    "Strategy should be applicable when --all is set, regardless of other options");
        }
    }

    @Nested
    @DisplayName("Model Version Upgrades")
    class ModelVersionUpgradeTests {

        @ParameterizedTest
        @MethodSource("provideUpgradeScenarios")
        @DisplayName("should handle various model version upgrade scenarios")
        void shouldHandleVariousModelVersionUpgradeScenarios(
                String initialNamespace,
                String initialModelVersion,
                String targetModelVersion,
                String expectedNamespace,
                String expectedModelVersion,
                int expectedModifiedCount,
                String description) {

            String pomXml = PomBuilder.create()
                    .namespace(initialNamespace)
                    .modelVersion(initialModelVersion)
                    .groupId("test")
                    .artifactId("test")
                    .version("1.0.0")
                    .build();

            Document document = Document.of(pomXml);
            // Use a mutable map since the strategy modifies it
            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext(TestUtils.createOptionsWithModelVersion(targetModelVersion));

            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Model upgrade should succeed: " + description);
            assertEquals(expectedModifiedCount, result.modifiedCount(), description);

            // Verify the model version and namespace - use the updated document from pomMap
            Document updatedDocument = pomMap.get(Paths.get("pom.xml"));
            Editor editor = new Editor(updatedDocument);
            Element root = editor.root();
            assertEquals(expectedNamespace, root.namespaceURI(), "Namespace should be updated: " + description);

            Element modelVersionElement = DomUtils.findChildElement(root, "modelVersion");
            if (expectedModelVersion != null) {
                assertNotNull(modelVersionElement, "Model version should exist: " + description);
                assertEquals(
                        expectedModelVersion,
                        modelVersionElement.textContentTrimmed(),
                        "Model version should be correct: " + description);
            }
        }

        private static Stream<Arguments> provideUpgradeScenarios() {
            return Stream.of(
                    Arguments.of(
                            "http://maven.apache.org/POM/4.0.0",
                            "4.0.0",
                            "4.1.0",
                            "http://maven.apache.org/POM/4.1.0",
                            "4.1.0",
                            1,
                            "Should upgrade from 4.0.0 to 4.1.0"),
                    Arguments.of(
                            "http://maven.apache.org/POM/4.1.0",
                            "4.1.0",
                            "4.1.0",
                            "http://maven.apache.org/POM/4.1.0",
                            "4.1.0",
                            0,
                            "Should not modify when already at target version"),
                    Arguments.of(
                            "http://maven.apache.org/POM/4.0.0",
                            null,
                            "4.1.0",
                            "http://maven.apache.org/POM/4.1.0",
                            "4.1.0",
                            1,
                            "Should add model version when missing"));
        }
    }

    @Nested
    @DisplayName("Namespace Updates")
    class NamespaceUpdateTests {

        @Test
        @DisplayName("should update namespace recursively")
        void shouldUpdateNamespaceRecursively() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <dependencies>
                        <dependency>
                            <groupId>test</groupId>
                            <artifactId>test</artifactId>
                            <version>1.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("pom.xml"), document);

            // Create context with --model-version=4.1.0 option to trigger namespace update
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.modelVersion()).thenReturn(Optional.of("4.1.0"));
            when(options.all()).thenReturn(Optional.empty());
            UpgradeContext context = createMockContext(options);

            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Model upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have upgraded namespace");

            // Verify namespace was updated recursively - use the updated document from pomMap
            Document updatedDocument = pomMap.get(Paths.get("pom.xml"));
            Editor editor = new Editor(updatedDocument);
            Element root = editor.root();
            String expectedNamespaceUri = "http://maven.apache.org/POM/4.1.0";
            assertEquals(expectedNamespaceUri, root.namespaceURI());

            // Verify child elements namespace updated recursively
            Element dependencies = DomUtils.findChildElement(root, "dependencies");
            assertNotNull(dependencies);
            assertEquals(expectedNamespaceUri, dependencies.namespaceURI());

            Element dependency = DomUtils.findChildElement(dependencies, "dependency");
            assertNotNull(dependency);
            assertEquals(expectedNamespaceUri, dependency.namespaceURI());

            Element groupId = DomUtils.findChildElement(dependency, "groupId");
            assertNotNull(groupId);
            assertEquals(expectedNamespaceUri, groupId.namespaceURI());
        }

        @Test
        @DisplayName("should convert modules to subprojects in 4.1.0")
        void shouldConvertModulesToSubprojectsIn410() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <modules>
                        <module>module1</module>
                        <module>module2</module>
                    </modules>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("pom.xml"), document);

            // Create context with --model-version=4.1.0 option to trigger module conversion
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.modelVersion()).thenReturn(Optional.of("4.1.0"));
            when(options.all()).thenReturn(Optional.empty());
            UpgradeContext context = createMockContext(options);

            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Model upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have converted modules to subprojects");

            // Verify modules element was renamed to subprojects - use the updated document from pomMap
            Document updatedDocument = pomMap.get(Paths.get("pom.xml"));
            Editor editor = new Editor(updatedDocument);
            Element root = editor.root();
            assertNull(DomUtils.findChildElement(root, "modules"));
            Element subprojects = DomUtils.findChildElement(root, "subprojects");
            assertNotNull(subprojects);

            // Verify module elements were renamed to subproject
            var moduleElements = subprojects.children("module").toList();
            var subprojectElements = subprojects.children("subproject").toList();
            assertEquals(0, moduleElements.size());
            assertEquals(2, subprojectElements.size());

            assertEquals("module1", subprojectElements.get(0).textContent());
            assertEquals("module2", subprojectElements.get(1).textContent());
        }
    }

    @Nested
    @DisplayName("Strategy Description")
    class StrategyDescriptionTests {

        @Test
        @DisplayName("should provide meaningful description")
        void shouldProvideMeaningfulDescription() {
            String description = strategy.getDescription();

            assertNotNull(description, "Description should not be null");
            assertFalse(description.trim().isEmpty(), "Description should not be empty");
            assertTrue(
                    description.toLowerCase().contains("model")
                            || description.toLowerCase().contains("upgrade"),
                    "Description should mention model or upgrade");
        }
    }

    @Nested
    @DisplayName("Phase Upgrades")
    class PhaseUpgradeTests {

        @Test
        @DisplayName("should upgrade deprecated phases to Maven 4 equivalents in 4.1.0")
        void shouldUpgradeDeprecatedPhasesIn410() throws Exception {
            Document document = createDocumentWithDeprecatedPhases();
            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("pom.xml"), document);

            // Create context with --model-version=4.1.0 option to trigger phase upgrade
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.modelVersion()).thenReturn(Optional.of("4.1.0"));
            when(options.all()).thenReturn(Optional.empty());
            UpgradeContext context = createMockContext(options);

            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Model upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have upgraded phases");

            // Verify phases were upgraded
            verifyCleanPluginPhases(document);
            verifyFailsafePluginPhases(document);
            verifySitePluginPhases(document);
            verifyPluginManagementPhases(document);
            verifyProfilePhases(document);
        }

        private Document createDocumentWithDeprecatedPhases() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-clean-plugin</artifactId>
                                <version>3.2.0</version>
                                <executions>
                                    <execution>
                                        <id>pre-clean-test</id>
                                        <phase>pre-clean</phase>
                                        <goals>
                                            <goal>clean</goal>
                                        </goals>
                                    </execution>
                                    <execution>
                                        <id>post-clean-test</id>
                                        <phase>post-clean</phase>
                                        <goals>
                                            <goal>clean</goal>
                                        </goals>
                                    </execution>
                                </executions>
                            </plugin>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-failsafe-plugin</artifactId>
                                <version>3.0.0-M7</version>
                                <executions>
                                    <execution>
                                        <id>pre-integration-test-setup</id>
                                        <phase>pre-integration-test</phase>
                                        <goals>
                                            <goal>integration-test</goal>
                                        </goals>
                                    </execution>
                                    <execution>
                                        <id>post-integration-test-cleanup</id>
                                        <phase>post-integration-test</phase>
                                        <goals>
                                            <goal>verify</goal>
                                        </goals>
                                    </execution>
                                </executions>
                            </plugin>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-site-plugin</artifactId>
                                <version>3.12.1</version>
                                <executions>
                                    <execution>
                                        <id>pre-site-setup</id>
                                        <phase>pre-site</phase>
                                        <goals>
                                            <goal>site</goal>
                                        </goals>
                                    </execution>
                                    <execution>
                                        <id>post-site-cleanup</id>
                                        <phase>post-site</phase>
                                        <goals>
                                            <goal>deploy</goal>
                                        </goals>
                                    </execution>
                                </executions>
                            </plugin>
                        </plugins>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-compiler-plugin</artifactId>
                                    <version>3.11.0</version>
                                    <executions>
                                        <execution>
                                            <id>pre-clean-compile</id>
                                            <phase>pre-clean</phase>
                                            <goals>
                                                <goal>compile</goal>
                                            </goals>
                                        </execution>
                                    </executions>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                    </build>
                    <profiles>
                        <profile>
                            <id>test-profile</id>
                            <build>
                                <plugins>
                                    <plugin>
                                        <groupId>org.apache.maven.plugins</groupId>
                                        <artifactId>maven-antrun-plugin</artifactId>
                                        <version>3.1.0</version>
                                        <executions>
                                            <execution>
                                                <id>profile-pre-integration-test</id>
                                                <phase>pre-integration-test</phase>
                                                <goals>
                                                    <goal>run</goal>
                                                </goals>
                                            </execution>
                                        </executions>
                                    </plugin>
                                </plugins>
                            </build>
                        </profile>
                    </profiles>
                </project>
                """;

            return Document.of(pomXml);
        }

        private void verifyCleanPluginPhases(Document document) {
            Element root = document.root();
            Element build = root.child("build").orElse(null);
            Element plugins = build.child("plugins").orElse(null);

            Element cleanPlugin = plugins.children("plugin")
                    .filter(p -> "maven-clean-plugin"
                            .equals(p.child("artifactId").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(cleanPlugin);

            Element cleanExecutions = cleanPlugin.child("executions").orElse(null);
            Element preCleanExecution = cleanExecutions
                    .children("execution")
                    .filter(e ->
                            "pre-clean-test".equals(e.child("id").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(preCleanExecution);
            assertEquals(
                    "before:clean",
                    preCleanExecution.child("phase").orElse(null).textContent());

            Element postCleanExecution = cleanExecutions
                    .children("execution")
                    .filter(e ->
                            "post-clean-test".equals(e.child("id").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(postCleanExecution);
            assertEquals(
                    "after:clean",
                    postCleanExecution.child("phase").orElse(null).textContent());
        }

        private void verifyFailsafePluginPhases(Document document) {
            Element root = document.root();
            Element build = root.child("build").orElse(null);
            Element plugins = build.child("plugins").orElse(null);

            Element failsafePlugin = plugins.children("plugin")
                    .filter(p -> "maven-failsafe-plugin"
                            .equals(p.child("artifactId").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(failsafePlugin);

            Element failsafeExecutions = failsafePlugin.child("executions").orElse(null);
            Element preIntegrationExecution = failsafeExecutions
                    .children("execution")
                    .filter(e -> "pre-integration-test-setup"
                            .equals(e.child("id").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(preIntegrationExecution);
            assertEquals(
                    "before:integration-test",
                    preIntegrationExecution.child("phase").orElse(null).textContent());

            Element postIntegrationExecution = failsafeExecutions
                    .children("execution")
                    .filter(e -> "post-integration-test-cleanup"
                            .equals(e.child("id").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(postIntegrationExecution);
            assertEquals(
                    "after:integration-test",
                    postIntegrationExecution.child("phase").orElse(null).textContent());
        }

        private void verifySitePluginPhases(Document document) {
            Element root = document.root();
            Element build = root.child("build").orElse(null);
            Element plugins = build.child("plugins").orElse(null);

            Element sitePlugin = plugins.children("plugin")
                    .filter(p -> "maven-site-plugin"
                            .equals(p.child("artifactId").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(sitePlugin);

            Element siteExecutions = sitePlugin.child("executions").orElse(null);
            Element preSiteExecution = siteExecutions
                    .children("execution")
                    .filter(e ->
                            "pre-site-setup".equals(e.child("id").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(preSiteExecution);
            assertEquals(
                    "before:site", preSiteExecution.child("phase").orElse(null).textContent());

            Element postSiteExecution = siteExecutions
                    .children("execution")
                    .filter(e -> "post-site-cleanup"
                            .equals(e.child("id").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(postSiteExecution);
            assertEquals(
                    "after:site", postSiteExecution.child("phase").orElse(null).textContent());
        }

        private void verifyPluginManagementPhases(Document document) {
            Element root = document.root();
            Element build = root.child("build").orElse(null);
            Element pluginManagement = build.child("pluginManagement").orElse(null);
            Element managedPlugins = pluginManagement.child("plugins").orElse(null);
            Element compilerPlugin = managedPlugins
                    .children("plugin")
                    .filter(p -> "maven-compiler-plugin"
                            .equals(p.child("artifactId").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(compilerPlugin);

            Element compilerExecutions = compilerPlugin.child("executions").orElse(null);
            Element preCleanCompileExecution = compilerExecutions
                    .children("execution")
                    .filter(e -> "pre-clean-compile"
                            .equals(e.child("id").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(preCleanCompileExecution);
            assertEquals(
                    "before:clean",
                    preCleanCompileExecution.child("phase").orElse(null).textContent());
        }

        private void verifyProfilePhases(Document document) {
            Element root = document.root();
            Element profiles = root.child("profiles").orElse(null);
            Element profile = profiles.child("profile").orElse(null);
            Element profileBuild = profile.child("build").orElse(null);
            Element profilePlugins = profileBuild.child("plugins").orElse(null);
            Element antrunPlugin = profilePlugins
                    .children("plugin")
                    .filter(p -> "maven-antrun-plugin"
                            .equals(p.child("artifactId").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(antrunPlugin);

            Element antrunExecutions = antrunPlugin.child("executions").orElse(null);
            Element profilePreIntegrationExecution = antrunExecutions
                    .children("execution")
                    .filter(e -> "profile-pre-integration-test"
                            .equals(e.child("id").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(profilePreIntegrationExecution);
            assertEquals(
                    "before:integration-test",
                    profilePreIntegrationExecution.child("phase").orElse(null).textContent());
        }

        @Test
        @DisplayName("should not upgrade phases when upgrading to 4.0.0")
        void shouldNotUpgradePhasesWhenUpgradingTo400() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-clean-plugin</artifactId>
                                <version>3.2.0</version>
                                <executions>
                                    <execution>
                                        <id>pre-clean-test</id>
                                        <phase>pre-clean</phase>
                                        <goals>
                                            <goal>clean</goal>
                                        </goals>
                                    </execution>
                                </executions>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("pom.xml"), document);

            // Create context with --model-version=4.0.0 option (no phase upgrade)
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.modelVersion()).thenReturn(Optional.of("4.0.0"));
            when(options.all()).thenReturn(Optional.empty());
            UpgradeContext context = createMockContext(options);

            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Model upgrade should succeed");

            // Verify phases were NOT upgraded (should remain as pre-clean)
            Element root = document.root();
            Element build = root.child("build").orElse(null);
            Element plugins = build.child("plugins").orElse(null);
            Element cleanPlugin = plugins.child("plugin").orElse(null);
            Element executions = cleanPlugin.child("executions").orElse(null);
            Element execution = executions.child("execution").orElse(null);
            Element phase = execution.child("phase").orElse(null);

            assertEquals("pre-clean", phase.textContent(), "Phase should remain as pre-clean for 4.0.0");
        }

        @Test
        @DisplayName("should preserve non-deprecated phases")
        void shouldPreserveNonDeprecatedPhases() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>3.11.0</version>
                                <executions>
                                    <execution>
                                        <id>compile-test</id>
                                        <phase>compile</phase>
                                        <goals>
                                            <goal>compile</goal>
                                        </goals>
                                    </execution>
                                    <execution>
                                        <id>test-compile-test</id>
                                        <phase>test-compile</phase>
                                        <goals>
                                            <goal>testCompile</goal>
                                        </goals>
                                    </execution>
                                    <execution>
                                        <id>package-test</id>
                                        <phase>package</phase>
                                        <goals>
                                            <goal>compile</goal>
                                        </goals>
                                    </execution>
                                </executions>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("pom.xml"), document);

            // Create context with --model-version=4.1.0 option
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.modelVersion()).thenReturn(Optional.of("4.1.0"));
            when(options.all()).thenReturn(Optional.empty());
            UpgradeContext context = createMockContext(options);

            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Model upgrade should succeed");

            // Verify non-deprecated phases were preserved
            Element root = document.root();
            Element build = root.child("build").orElse(null);
            Element plugins = build.child("plugins").orElse(null);
            Element compilerPlugin = plugins.child("plugin").orElse(null);
            Element executions = compilerPlugin.child("executions").orElse(null);

            Element compileExecution = executions
                    .children("execution")
                    .filter(e ->
                            "compile-test".equals(e.child("id").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(compileExecution);
            assertEquals("compile", compileExecution.child("phase").orElse(null).textContent());

            Element testCompileExecution = executions
                    .children("execution")
                    .filter(e -> "test-compile-test"
                            .equals(e.child("id").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(testCompileExecution);
            assertEquals(
                    "test-compile",
                    testCompileExecution.child("phase").orElse(null).textContent());

            Element packageExecution = executions
                    .children("execution")
                    .filter(e ->
                            "package-test".equals(e.child("id").orElse(null).textContent()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(packageExecution);
            assertEquals("package", packageExecution.child("phase").orElse(null).textContent());
        }
    }

    @Nested
    @DisplayName("Downgrade Handling")
    class DowngradeHandlingTests {

        @Test
        @DisplayName("should fail with error when attempting downgrade from 4.1.0 to 4.0.0")
        void shouldFailWhenAttemptingDowngrade() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("pom.xml"), document);

            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.0.0"));

            UpgradeResult result = strategy.apply(context, pomMap);

            // Should have errors (not just warnings)
            assertTrue(result.errorCount() > 0, "Downgrade should result in errors");
            assertFalse(result.success(), "Downgrade should not be successful");
            assertEquals(1, result.errorCount(), "Should have exactly one error");
        }

        @Test
        @DisplayName("should succeed when upgrading from 4.0.0 to 4.1.0")
        void shouldSucceedWhenUpgrading() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("pom.xml"), document);

            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));

            UpgradeResult result = strategy.apply(context, pomMap);

            // Should succeed
            assertTrue(result.success(), "Valid upgrade should be successful");
            assertEquals(0, result.errorCount(), "Should have no errors");
            assertEquals(1, result.modifiedCount(), "Should have modified one POM");
        }
    }
}
