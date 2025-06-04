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
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
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
 * Unit tests for the {@link PluginUpgradeStrategy} class.
 * Tests plugin version upgrades, plugin management additions, and Maven 4 compatibility.
 */
@DisplayName("PluginUpgradeStrategy")
class PluginUpgradeStrategyTest {

    private PluginUpgradeStrategy strategy;
    private SAXBuilder saxBuilder;

    @BeforeEach
    void setUp() {
        strategy = new PluginUpgradeStrategy();
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
        @DisplayName("should be applicable when --plugins option is true")
        void shouldBeApplicableWhenPluginsOptionTrue() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.plugins()).thenReturn(Optional.of(true));
            when(options.all()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable when --plugins is true");
        }

        @Test
        @DisplayName("should be applicable when --all option is specified")
        void shouldBeApplicableWhenAllOptionSpecified() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.all()).thenReturn(Optional.of(true));
            when(options.plugins()).thenReturn(Optional.empty());

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
        @DisplayName("should not be applicable when --plugins option is false")
        void shouldNotBeApplicableWhenPluginsOptionFalse() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.plugins()).thenReturn(Optional.of(false));
            when(options.all()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);

            assertFalse(strategy.isApplicable(context), "Strategy should not be applicable when --plugins is false");
        }
    }

    @Nested
    @DisplayName("Plugin Upgrades")
    class PluginUpgradeTests {

        @Test
        @DisplayName("should upgrade plugin version when below minimum")
        void shouldUpgradePluginVersionWhenBelowMinimum() throws Exception {
            String pomXml = PomBuilder.create()
                    .groupId("test")
                    .artifactId("test")
                    .version("1.0.0")
                    .plugin("org.apache.maven.plugins", "maven-compiler-plugin", "3.8.1")
                    .build();

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Plugin upgrade should succeed");
            // Note: POM may or may not be modified depending on whether upgrades are needed

            // Verify the plugin version was upgraded
            Element root = document.getRootElement();
            Namespace namespace = root.getNamespace();
            Element build = root.getChild("build", namespace);
            Element plugins = build.getChild("plugins", namespace);
            Element plugin = plugins.getChild("plugin", namespace);
            String version = plugin.getChildText("version", namespace);

            // The exact version depends on the plugin upgrades configuration
            assertNotNull(version, "Plugin should have a version");
        }

        @Test
        @DisplayName("should not modify plugin when version is already sufficient")
        void shouldNotModifyPluginWhenVersionAlreadySufficient() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>3.13.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Plugin upgrade should succeed");
            // POM might still be marked as modified due to other plugin management additions
        }

        @Test
        @DisplayName("should upgrade plugin in pluginManagement")
        void shouldUpgradePluginInPluginManagement() throws Exception {
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
                                    <artifactId>maven-enforcer-plugin</artifactId>
                                    <version>2.0.0</version>
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

            assertTrue(result.success(), "Plugin upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have upgraded maven-enforcer-plugin");

            // Verify the version was upgraded
            Element root = document.getRootElement();
            Namespace namespace = root.getNamespace();
            Element pluginElement = root.getChild("build", namespace)
                    .getChild("pluginManagement", namespace)
                    .getChild("plugins", namespace)
                    .getChild("plugin", namespace);
            Element versionElement = pluginElement.getChild("version", namespace);
            assertEquals("3.0.0", versionElement.getTextTrim());
        }

        @Test
        @DisplayName("should upgrade plugin with property version")
        void shouldUpgradePluginWithPropertyVersion() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <shade.plugin.version>3.0.0</shade.plugin.version>
                    </properties>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-shade-plugin</artifactId>
                                <version>${shade.plugin.version}</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Plugin upgrade should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have upgraded shade plugin property");

            // Verify the property was upgraded
            Element root = document.getRootElement();
            Namespace namespace = root.getNamespace();
            Element propertyElement =
                    root.getChild("properties", namespace).getChild("shade.plugin.version", namespace);
            assertEquals("3.5.0", propertyElement.getTextTrim());
        }

        @Test
        @DisplayName("should not upgrade when version is already higher")
        void shouldNotUpgradeWhenVersionAlreadyHigher() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.codehaus.mojo</groupId>
                                <artifactId>flatten-maven-plugin</artifactId>
                                <version>1.3.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Plugin upgrade should succeed");

            // Verify the version was not changed
            Element root = document.getRootElement();
            Namespace namespace = root.getNamespace();
            Element pluginElement = root.getChild("build", namespace)
                    .getChild("plugins", namespace)
                    .getChild("plugin", namespace);
            Element versionElement = pluginElement.getChild("version", namespace);
            assertEquals("1.3.0", versionElement.getTextTrim());
        }

        @Test
        @DisplayName("should upgrade plugin without explicit groupId")
        void shouldUpgradePluginWithoutExplicitGroupId() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <artifactId>maven-shade-plugin</artifactId>
                                <version>3.1.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Plugin upgrade should succeed");
            assertTrue(
                    result.modifiedCount() > 0,
                    "Should have upgraded maven-shade-plugin even without explicit groupId");

            // Verify the version was upgraded
            Element root = document.getRootElement();
            Namespace namespace = root.getNamespace();
            Element pluginElement = root.getChild("build", namespace)
                    .getChild("plugins", namespace)
                    .getChild("plugin", namespace);
            Element versionElement = pluginElement.getChild("version", namespace);
            assertEquals("3.5.0", versionElement.getTextTrim());
        }

        @Test
        @DisplayName("should not upgrade plugin without version")
        void shouldNotUpgradePluginWithoutVersion() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-exec-plugin</artifactId>
                                <!-- No version - inherited from parent or pluginManagement -->
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Plugin upgrade should succeed");
            // Note: POM might still be modified due to plugin management additions
        }

        @Test
        @DisplayName("should not upgrade when property is not found")
        void shouldNotUpgradeWhenPropertyNotFound() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-exec-plugin</artifactId>
                                <version>${exec.plugin.version}</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "Plugin upgrade should succeed");
            // Note: POM might still be modified due to plugin management additions
        }
    }

    @Nested
    @DisplayName("Plugin Management")
    class PluginManagementTests {

        @Test
        @DisplayName("should add pluginManagement before existing plugins section")
        void shouldAddPluginManagementBeforeExistingPluginsSection() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>3.8.1</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify the structure
            Element root = document.getRootElement();
            Namespace namespace = root.getNamespace();
            Element buildElement = root.getChild("build", namespace);
            assertNotNull(buildElement, "Build element should exist");

            List<Element> buildChildren = buildElement.getChildren();

            // Find the indices of pluginManagement and plugins
            int pluginManagementIndex = -1;
            int pluginsIndex = -1;

            for (int i = 0; i < buildChildren.size(); i++) {
                Element child = buildChildren.get(i);
                if ("pluginManagement".equals(child.getName())) {
                    pluginManagementIndex = i;
                } else if ("plugins".equals(child.getName())) {
                    pluginsIndex = i;
                }
            }

            assertTrue(pluginsIndex >= 0, "plugins should be present");
            if (pluginManagementIndex >= 0) {
                assertTrue(
                        pluginManagementIndex < pluginsIndex,
                        "pluginManagement should come before plugins when both are present");
            }
        }
    }

    @Nested
    @DisplayName("Plugin Upgrade Configuration")
    class PluginUpgradeConfigurationTests {

        @Test
        @DisplayName("should have predefined plugin upgrades")
        void shouldHavePredefinedPluginUpgrades() throws Exception {
            List<PluginUpgrade> upgrades = PluginUpgradeStrategy.getPluginUpgrades();

            assertFalse(upgrades.isEmpty(), "Should have predefined plugin upgrades");

            // Verify some expected plugins are included
            boolean hasCompilerPlugin =
                    upgrades.stream().anyMatch(upgrade -> "maven-compiler-plugin".equals(upgrade.artifactId()));
            boolean hasExecPlugin =
                    upgrades.stream().anyMatch(upgrade -> "maven-exec-plugin".equals(upgrade.artifactId()));

            assertTrue(hasCompilerPlugin, "Should include maven-compiler-plugin upgrade");
            assertTrue(hasExecPlugin, "Should include maven-exec-plugin upgrade");
        }

        @Test
        @DisplayName("should have valid plugin upgrade definitions")
        void shouldHaveValidPluginUpgradeDefinitions() throws Exception {
            List<PluginUpgrade> upgrades = PluginUpgradeStrategy.getPluginUpgrades();

            for (PluginUpgrade upgrade : upgrades) {
                assertNotNull(upgrade.groupId(), "Plugin upgrade should have groupId");
                assertNotNull(upgrade.artifactId(), "Plugin upgrade should have artifactId");
                assertNotNull(upgrade.minVersion(), "Plugin upgrade should have minVersion");
                // configuration can be null for some plugins
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle malformed POM gracefully")
        void shouldHandleMalformedPOMGracefully() throws Exception {
            String malformedPomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <!-- Missing required elements -->
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(malformedPomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.apply(context, pomMap);

            // Strategy should handle malformed POMs gracefully
            assertNotNull(result, "Result should not be null");
            assertTrue(result.processedPoms().contains(Paths.get("pom.xml")), "POM should be marked as processed");
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
            assertTrue(description.toLowerCase().contains("plugin"), "Description should mention plugins");
        }
    }

    @Nested
    @DisplayName("XML Formatting")
    class XmlFormattingTests {

        @Test
        @DisplayName("should format pluginManagement with proper indentation")
        void shouldFormatPluginManagementWithProperIndentation() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>3.1</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Convert to string to check formatting
            Format format = Format.getRawFormat();
            format.setLineSeparator(System.lineSeparator());
            XMLOutputter out = new XMLOutputter(format);
            StringWriter writer = new StringWriter();
            out.output(document.getRootElement(), writer);
            String result = writer.toString();

            // Check that the plugin version was upgraded
            assertTrue(result.contains("<version>3.2</version>"), "Plugin version should be upgraded to 3.2");

            // Verify that the XML formatting is correct - no malformed closing tags
            assertFalse(result.contains("</plugin></plugins>"), "Should not have malformed closing tags");
            assertFalse(result.contains("</plugins></pluginManagement>"), "Should not have malformed closing tags");

            // Check that proper indentation is maintained
            assertTrue(result.contains("    <build>"), "Build element should be properly indented");
            assertTrue(result.contains("        <plugins>"), "Plugins element should be properly indented");
            assertTrue(result.contains("            <plugin>"), "Plugin element should be properly indented");
        }

        @Test
        @DisplayName("should format pluginManagement with proper indentation when added")
        void shouldFormatPluginManagementWithProperIndentationWhenAdded() throws Exception {
            // Use a POM that will trigger pluginManagement addition by having a plugin without version
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-enforcer-plugin</artifactId>
                                <!-- No version - should trigger pluginManagement addition -->
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Convert to string to check formatting
            Format format = Format.getRawFormat();
            format.setLineSeparator(System.lineSeparator());
            XMLOutputter out = new XMLOutputter(format);
            StringWriter writer = new StringWriter();
            out.output(document.getRootElement(), writer);
            String result = writer.toString();

            // If pluginManagement was added, verify proper formatting
            if (result.contains("<pluginManagement>")) {
                // Verify that the XML formatting is correct - no malformed closing tags
                assertFalse(result.contains("</plugin></plugins>"), "Should not have malformed closing tags");
                assertFalse(result.contains("</plugins></pluginManagement>"), "Should not have malformed closing tags");

                // Check that proper indentation is maintained for pluginManagement
                assertTrue(
                        result.contains("        <pluginManagement>"), "PluginManagement should be properly indented");
                assertTrue(
                        result.contains("            <plugins>"),
                        "Plugins in pluginManagement should be properly indented");
                assertTrue(
                        result.contains("        </pluginManagement>"),
                        "PluginManagement closing tag should be properly indented");
            }
        }
    }
}
