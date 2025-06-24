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

import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
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
    private SAXBuilder saxBuilder;

    @BeforeEach
    void setUp() {
        strategy = new ModelUpgradeStrategy();
        saxBuilder = new SAXBuilder();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    private UpgradeContext createMockContext(UpgradeOptions options) {
        return TestUtils.createMockContext(options);
    }

    private UpgradeOptions createDefaultOptions() {
        return TestUtils.createDefaultOptions();
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
                    Arguments.of(false, "4.1.0", true, "Should be applicable for model upgrade even when --all=false"));
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
                String description)
                throws Exception {

            String pomXml = PomBuilder.create()
                    .namespace(initialNamespace)
                    .modelVersion(initialModelVersion)
                    .groupId("test")
                    .artifactId("test")
                    .version("1.0.0")
                    .build();

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext(TestUtils.createOptionsWithModelVersion(targetModelVersion));

            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Model upgrade should succeed: " + description);
            assertEquals(expectedModifiedCount, result.modifiedCount(), description);

            // Verify the model version and namespace
            Element root = document.getRootElement();
            assertEquals(expectedNamespace, root.getNamespaceURI(), "Namespace should be updated: " + description);

            Element modelVersionElement = root.getChild("modelVersion", root.getNamespace());
            if (expectedModelVersion != null) {
                assertNotNull(modelVersionElement, "Model version should exist: " + description);
                assertEquals(
                        expectedModelVersion,
                        modelVersionElement.getTextTrim(),
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
            String pomXml =
                    """
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

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            // Create context with --model-version=4.1.0 option to trigger namespace update
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.modelVersion()).thenReturn(Optional.of("4.1.0"));
            when(options.all()).thenReturn(Optional.empty());
            UpgradeContext context = createMockContext(options);

            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Model upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have upgraded namespace");

            // Verify namespace was updated recursively
            Element root = document.getRootElement();
            Namespace newNamespace = Namespace.getNamespace("http://maven.apache.org/POM/4.1.0");
            assertEquals(newNamespace, root.getNamespace());

            // Verify child elements namespace updated recursively
            Element dependencies = root.getChild("dependencies", newNamespace);
            assertNotNull(dependencies);
            assertEquals(newNamespace, dependencies.getNamespace());

            Element dependency = dependencies.getChild("dependency", newNamespace);
            assertNotNull(dependency);
            assertEquals(newNamespace, dependency.getNamespace());

            Element groupId = dependency.getChild("groupId", newNamespace);
            assertNotNull(groupId);
            assertEquals(newNamespace, groupId.getNamespace());
        }

        @Test
        @DisplayName("should convert modules to subprojects in 4.1.0")
        void shouldConvertModulesToSubprojectsIn410() throws Exception {
            String pomXml =
                    """
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

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            // Create context with --model-version=4.1.0 option to trigger module conversion
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.modelVersion()).thenReturn(Optional.of("4.1.0"));
            when(options.all()).thenReturn(Optional.empty());
            UpgradeContext context = createMockContext(options);

            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Model upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have converted modules to subprojects");

            // Verify modules element was renamed to subprojects
            Element root = document.getRootElement();
            Namespace namespace = root.getNamespace();
            assertNull(root.getChild("modules", namespace));
            Element subprojects = root.getChild("subprojects", namespace);
            assertNotNull(subprojects);

            // Verify module elements were renamed to subproject
            assertEquals(0, subprojects.getChildren("module", namespace).size());
            assertEquals(2, subprojects.getChildren("subproject", namespace).size());

            assertEquals(
                    "module1",
                    subprojects.getChildren("subproject", namespace).get(0).getText());
            assertEquals(
                    "module2",
                    subprojects.getChildren("subproject", namespace).get(1).getText());
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
}
