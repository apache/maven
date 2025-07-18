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

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link CompatibilityFixStrategy} class.
 * Tests Maven 4 compatibility fixes including duplicate dependency and plugin handling.
 */
@DisplayName("CompatibilityFixStrategy")
class CompatibilityFixStrategyTest {

    private CompatibilityFixStrategy strategy;
    private SAXBuilder saxBuilder;

    @BeforeEach
    void setUp() {
        strategy = new CompatibilityFixStrategy();
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

        @Test
        @DisplayName("should be applicable when --model option is true")
        void shouldBeApplicableWhenModelOptionTrue() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.model()).thenReturn(Optional.of(true));
            when(options.all()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable when --model is true");
        }

        @Test
        @DisplayName("should be applicable when --all option is specified")
        void shouldBeApplicableWhenAllOptionSpecified() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.all()).thenReturn(Optional.of(true));
            when(options.model()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable when --all is specified");
        }

        @Test
        @DisplayName("should be applicable by default when no specific options provided")
        void shouldBeApplicableByDefaultWhenNoSpecificOptions() {
            UpgradeOptions options = createDefaultOptions();

            UpgradeContext context = createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable by default");
        }

        @Test
        @DisplayName("should not be applicable when --model option is false")
        void shouldNotBeApplicableWhenModelOptionFalse() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.model()).thenReturn(Optional.of(false));
            when(options.all()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);

            assertFalse(strategy.isApplicable(context), "Strategy should not be applicable when --model is false");
        }

        @Test
        @DisplayName("should handle all options disabled")
        void shouldHandleAllOptionsDisabled() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptions(
                    false, // --all
                    false, // --infer
                    false, // --fix-model
                    false, // --plugins
                    null // --model
                    ));

            // Should apply default behavior when all options are explicitly disabled
            assertTrue(
                    strategy.isApplicable(context),
                    "Strategy should apply default behavior when all options are disabled");
        }
    }

    @Nested
    @DisplayName("Duplicate Dependency Fixes")
    class DuplicateDependencyFixesTests {

        @Test
        @DisplayName("should remove duplicate dependencies in dependencyManagement")
        void shouldRemoveDuplicateDependenciesInDependencyManagement() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.apache.commons</groupId>
                                <artifactId>commons-lang3</artifactId>
                                <version>3.12.0</version>
                            </dependency>
                            <dependency>
                                <groupId>org.apache.commons</groupId>
                                <artifactId>commons-lang3</artifactId>
                                <version>3.13.0</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Compatibility fix should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have removed duplicate dependency");

            // Verify only one dependency remains
            Element root = document.getRootElement();
            Element dependencyManagement = root.getChild("dependencyManagement", root.getNamespace());
            Element dependencies = dependencyManagement.getChild("dependencies", root.getNamespace());
            assertEquals(
                    1,
                    dependencies.getChildren("dependency", root.getNamespace()).size(),
                    "Should have only one dependency after duplicate removal");
        }

        @Test
        @DisplayName("should remove duplicate dependencies in regular dependencies")
        void shouldRemoveDuplicateDependenciesInRegularDependencies() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>junit</groupId>
                            <artifactId>junit</artifactId>
                            <version>4.13.2</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>junit</groupId>
                            <artifactId>junit</artifactId>
                            <version>4.13.2</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Compatibility fix should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have removed duplicate dependency");

            // Verify only one dependency remains
            Element root = document.getRootElement();
            Element dependencies = root.getChild("dependencies", root.getNamespace());
            assertEquals(
                    1,
                    dependencies.getChildren("dependency", root.getNamespace()).size(),
                    "Should have only one dependency after duplicate removal");
        }
    }

    @Nested
    @DisplayName("Duplicate Plugin Fixes")
    class DuplicatePluginFixesTests {

        @Test
        @DisplayName("should remove duplicate plugins in pluginManagement")
        void shouldRemoveDuplicatePluginsInPluginManagement() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-compiler-plugin</artifactId>
                                    <version>3.11.0</version>
                                </plugin>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-compiler-plugin</artifactId>
                                    <version>3.12.0</version>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                    </build>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Compatibility fix should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have removed duplicate plugin");

            // Verify only one plugin remains
            Element root = document.getRootElement();
            Element build = root.getChild("build", root.getNamespace());
            Element pluginManagement = build.getChild("pluginManagement", root.getNamespace());
            Element plugins = pluginManagement.getChild("plugins", root.getNamespace());
            assertEquals(
                    1,
                    plugins.getChildren("plugin", root.getNamespace()).size(),
                    "Should have only one plugin after duplicate removal");
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
                    description.toLowerCase().contains("compatibility")
                            || description.toLowerCase().contains("fix"),
                    "Description should mention compatibility or fix");
        }
    }
}
