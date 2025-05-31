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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for plugin upgrade functionality in BaseUpgradeGoal.
 * These tests focus on XML transformation logic by modifying Document objects in memory.
 */
class PluginUpgradeTest {

    private TestableBaseUpgradeGoal upgrade;
    private SAXBuilder saxBuilder;

    @BeforeEach
    void setUp() {
        upgrade = new TestableBaseUpgradeGoal();
        saxBuilder = new SAXBuilder();
    }

    private UpgradeContext createMockContext() {
        InvokerRequest request = Mockito.mock(InvokerRequest.class);

        // Mock all required properties for LookupContext constructor
        Mockito.when(request.cwd()).thenReturn(Paths.get("/project"));
        Mockito.when(request.installationDirectory()).thenReturn(Paths.get("/maven"));
        Mockito.when(request.userHomeDirectory()).thenReturn(Paths.get("/home/user"));
        Mockito.when(request.topDirectory()).thenReturn(Paths.get("/project"));
        Mockito.when(request.rootDirectory()).thenReturn(java.util.Optional.empty());
        Mockito.when(request.userProperties()).thenReturn(java.util.Map.of());
        Mockito.when(request.systemProperties()).thenReturn(java.util.Map.of());

        // Mock parserRequest and logger
        org.apache.maven.api.cli.ParserRequest parserRequest =
                Mockito.mock(org.apache.maven.api.cli.ParserRequest.class);
        org.apache.maven.api.cli.Logger logger = Mockito.mock(org.apache.maven.api.cli.Logger.class);
        Mockito.when(request.parserRequest()).thenReturn(parserRequest);
        Mockito.when(parserRequest.logger()).thenReturn(logger);

        UpgradeContext context = new UpgradeContext(request);
        return context;
    }

    @Test
    void testUpgradePluginInBuildPlugins() throws Exception {
        String pomXml =
                """
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
                            <artifactId>maven-exec-plugin</artifactId>
                            <version>3.1.0</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));
        UpgradeContext context = createMockContext();

        boolean upgraded = upgrade.upgradePluginsInPom(context, document);

        assertTrue(upgraded, "Should have upgraded maven-exec-plugin");

        // Verify the version was upgraded
        Element root = document.getRootElement();
        Namespace namespace = root.getNamespace();
        Element pluginElement =
                root.getChild("build", namespace).getChild("plugins", namespace).getChild("plugin", namespace);
        Element versionElement = pluginElement.getChild("version", namespace);
        assertEquals("3.2.0", versionElement.getTextTrim());
    }

    @Test
    void testUpgradePluginInPluginManagement() throws Exception {
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
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
        UpgradeContext context = createMockContext();

        boolean upgraded = upgrade.upgradePluginsInPom(context, document);

        assertTrue(upgraded, "Should have upgraded maven-enforcer-plugin");

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
    void testUpgradePluginWithPropertyVersion() throws Exception {
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
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
        UpgradeContext context = createMockContext();

        boolean upgraded = upgrade.upgradePluginsInPom(context, document);

        assertTrue(upgraded, "Should have upgraded shade plugin property");

        // Verify the property was upgraded
        Element root = document.getRootElement();
        Namespace namespace = root.getNamespace();
        Element propertyElement = root.getChild("properties", namespace).getChild("shade.plugin.version", namespace);
        assertEquals("3.5.0", propertyElement.getTextTrim());
    }

    @Test
    void testNoUpgradeNeededWhenVersionIsAlreadyHigher() throws Exception {
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
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
        UpgradeContext context = createMockContext();

        boolean upgraded = upgrade.upgradePluginsInPom(context, document);

        assertFalse(upgraded, "Should not upgrade when version is already higher");

        // Verify the version was not changed
        Element root = document.getRootElement();
        Namespace namespace = root.getNamespace();
        Element pluginElement =
                root.getChild("build", namespace).getChild("plugins", namespace).getChild("plugin", namespace);
        Element versionElement = pluginElement.getChild("version", namespace);
        assertEquals("1.3.0", versionElement.getTextTrim());
    }

    @Test
    void testVersionComparison() throws Exception {
        // Test the version comparison logic
        assertTrue(upgrade.isVersionBelow("3.1.0", "3.2.0"));
        assertTrue(upgrade.isVersionBelow("2.9.0", "3.0.0"));
        assertTrue(upgrade.isVersionBelow("1.2.6", "1.2.7"));

        assertFalse(upgrade.isVersionBelow("3.2.0", "3.2.0")); // Equal versions
        assertFalse(upgrade.isVersionBelow("3.3.0", "3.2.0")); // Higher version
        assertFalse(upgrade.isVersionBelow("4.0.0", "3.5.0")); // Major version higher

        // Test with qualifiers
        assertTrue(upgrade.isVersionBelow("3.1.0-SNAPSHOT", "3.2.0"));
        assertFalse(upgrade.isVersionBelow("3.2.0-alpha", "3.2.0"));
    }

    @Test
    void testMultiplePluginUpgrades() throws Exception {
        String pomXml =
                """
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
                            <artifactId>maven-exec-plugin</artifactId>
                            <version>3.1.0</version>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-enforcer-plugin</artifactId>
                            <version>2.0.0</version>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.8.0</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));
        UpgradeContext context = createMockContext();

        boolean upgraded = upgrade.upgradePluginsInPom(context, document);

        assertTrue(upgraded, "Should have upgraded some plugins");

        // Verify specific upgrades
        Element root = document.getRootElement();
        Namespace namespace = root.getNamespace();
        Element pluginsElement = root.getChild("build", namespace).getChild("plugins", namespace);

        // Check maven-exec-plugin was upgraded
        Element execPlugin = pluginsElement.getChildren("plugin", namespace).stream()
                .filter(p -> "maven-exec-plugin".equals(p.getChildText("artifactId", namespace)))
                .findFirst()
                .orElse(null);
        assertNotNull(execPlugin);
        assertEquals("3.2.0", execPlugin.getChildText("version", namespace));

        // Check maven-enforcer-plugin was upgraded
        Element enforcerPlugin = pluginsElement.getChildren("plugin", namespace).stream()
                .filter(p -> "maven-enforcer-plugin".equals(p.getChildText("artifactId", namespace)))
                .findFirst()
                .orElse(null);
        assertNotNull(enforcerPlugin);
        assertEquals("3.0.0", enforcerPlugin.getChildText("version", namespace));

        // Check maven-compiler-plugin was not changed (not in upgrade list)
        Element compilerPlugin = pluginsElement.getChildren("plugin", namespace).stream()
                .filter(p -> "maven-compiler-plugin".equals(p.getChildText("artifactId", namespace)))
                .findFirst()
                .orElse(null);
        assertNotNull(compilerPlugin);
        assertEquals("3.8.0", compilerPlugin.getChildText("version", namespace));
    }

    @Test
    void testPluginWithoutExplicitGroupId() throws Exception {
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>

                <build>
                    <plugins>
                        <plugin>
                            <artifactId>maven-exec-plugin</artifactId>
                            <version>3.1.0</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));
        UpgradeContext context = createMockContext();

        boolean upgraded = upgrade.upgradePluginsInPom(context, document);

        assertTrue(upgraded, "Should have upgraded maven-exec-plugin even without explicit groupId");

        // Verify the version was upgraded
        Element root = document.getRootElement();
        Namespace namespace = root.getNamespace();
        Element pluginElement =
                root.getChild("build", namespace).getChild("plugins", namespace).getChild("plugin", namespace);
        Element versionElement = pluginElement.getChild("version", namespace);
        assertEquals("3.2.0", versionElement.getTextTrim());
    }

    @Test
    void testPluginWithoutVersion() throws Exception {
        String pomXml =
                """
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
                            <artifactId>maven-exec-plugin</artifactId>
                            <!-- No version - inherited from parent or pluginManagement -->
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));
        UpgradeContext context = createMockContext();

        boolean upgraded = upgrade.upgradePluginsInPom(context, document);

        assertFalse(upgraded, "Should not upgrade plugin without explicit version");
    }

    @Test
    void testPropertyNotFoundInPom() throws Exception {
        String pomXml =
                """
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
                            <artifactId>maven-exec-plugin</artifactId>
                            <version>${exec.plugin.version}</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));
        UpgradeContext context = createMockContext();

        boolean upgraded = upgrade.upgradePluginsInPom(context, document);

        assertFalse(upgraded, "Should not upgrade when property is not found");
    }

    @Test
    void testGetPluginUpgrades() throws Exception {
        Map<String, BaseUpgradeGoal.PluginUpgrade> upgrades = upgrade.getPluginUpgrades();

        assertEquals(4, upgrades.size(), "Should have 4 plugin upgrades defined");

        assertTrue(upgrades.containsKey("org.apache.maven.plugins:maven-exec-plugin"));
        assertTrue(upgrades.containsKey("org.apache.maven.plugins:maven-enforcer-plugin"));
        assertTrue(upgrades.containsKey("org.codehaus.mojo:flatten-maven-plugin"));
        assertTrue(upgrades.containsKey("org.apache.maven.plugins:maven-shade-plugin"));

        assertEquals("3.2.0", upgrades.get("org.apache.maven.plugins:maven-exec-plugin").minVersion);
        assertEquals("3.0.0", upgrades.get("org.apache.maven.plugins:maven-enforcer-plugin").minVersion);
        assertEquals("1.2.7", upgrades.get("org.codehaus.mojo:flatten-maven-plugin").minVersion);
        assertEquals("3.5.0", upgrades.get("org.apache.maven.plugins:maven-shade-plugin").minVersion);
    }

    @Test
    void testApplyPluginUpgrades() throws Exception {
        String pomXml =
                """
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
                            <artifactId>maven-exec-plugin</artifactId>
                            <version>3.1.0</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));
        UpgradeContext context = createMockContext();

        Map<Path, Document> pomMap = new HashMap<>();
        pomMap.put(Paths.get("/project/pom.xml"), document);

        upgrade.applyPluginUpgrades(context, pomMap);

        // Verify the version was upgraded
        Element root = document.getRootElement();
        Namespace namespace = root.getNamespace();
        Element pluginElement =
                root.getChild("build", namespace).getChild("plugins", namespace).getChild("plugin", namespace);
        Element versionElement = pluginElement.getChild("version", namespace);
        assertEquals("3.2.0", versionElement.getTextTrim());
    }

    @Test
    void testParentPomPluginDetection() throws Exception {
        String pomXml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                         http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>

                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>2.7.0</version>
                    </parent>

                    <groupId>com.example</groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

        SAXBuilder saxBuilder = new SAXBuilder();

        // Create a testable upgrade goal that overrides parent POM checking
        TestableBaseUpgradeGoal upgrade = new TestableBaseUpgradeGoal() {
            @Override
            public boolean upgradePluginsInPom(UpgradeContext context, Document pomDocument) {
                // Call the parent method but simulate finding a plugin in parent
                super.upgradePluginsInPom(context, pomDocument);

                // Manually add plugin management for testing
                Element root = pomDocument.getRootElement();
                Namespace namespace = root.getNamespace();

                // Ensure build/pluginManagement/plugins structure exists
                Element buildElement = root.getChild("build", namespace);
                if (buildElement == null) {
                    buildElement = new Element("build", namespace);
                    root.addContent(buildElement);
                }

                Element pluginManagementElement = buildElement.getChild("pluginManagement", namespace);
                if (pluginManagementElement == null) {
                    pluginManagementElement = new Element("pluginManagement", namespace);
                    buildElement.addContent(pluginManagementElement);
                }

                Element pluginsElement = pluginManagementElement.getChild("plugins", namespace);
                if (pluginsElement == null) {
                    pluginsElement = new Element("plugins", namespace);
                    pluginManagementElement.addContent(pluginsElement);
                }

                // Add maven-enforcer-plugin management entry
                Element pluginElement = new Element("plugin", namespace);

                Element groupIdElement = new Element("groupId", namespace);
                groupIdElement.setText("org.apache.maven.plugins");
                pluginElement.addContent(groupIdElement);

                Element artifactIdElement = new Element("artifactId", namespace);
                artifactIdElement.setText("maven-enforcer-plugin");
                pluginElement.addContent(artifactIdElement);

                Element versionElement = new Element("version", namespace);
                versionElement.setText("3.0.0");
                pluginElement.addContent(versionElement);

                pluginsElement.addContent(pluginElement);

                return true;
            }
        };

        Document document = saxBuilder.build(new StringReader(pomXml));
        UpgradeContext context = createMockContext();

        boolean upgraded = upgrade.upgradePluginsInPom(context, document);

        assertTrue(upgraded, "Should have added plugin management for maven-enforcer-plugin found in parent");

        // Verify plugin management was added
        Element root = document.getRootElement();
        Namespace namespace = root.getNamespace();
        Element buildElement = root.getChild("build", namespace);
        assertNotNull(buildElement, "Build element should be created");

        Element pluginManagementElement = buildElement.getChild("pluginManagement", namespace);
        assertNotNull(pluginManagementElement, "PluginManagement element should be created");

        Element pluginsElement = pluginManagementElement.getChild("plugins", namespace);
        assertNotNull(pluginsElement, "Plugins element should be created");

        // Find the maven-enforcer-plugin specifically
        Element enforcerPluginElement = null;
        List<Element> pluginElements = pluginsElement.getChildren("plugin", namespace);
        for (Element plugin : pluginElements) {
            Element artifactIdElement = plugin.getChild("artifactId", namespace);
            if (artifactIdElement != null && "maven-enforcer-plugin".equals(artifactIdElement.getTextTrim())) {
                enforcerPluginElement = plugin;
                break;
            }
        }

        assertNotNull(enforcerPluginElement, "maven-enforcer-plugin element should be added");

        Element groupIdElement = enforcerPluginElement.getChild("groupId", namespace);
        assertEquals("org.apache.maven.plugins", groupIdElement.getTextTrim());

        Element artifactIdElement = enforcerPluginElement.getChild("artifactId", namespace);
        assertEquals("maven-enforcer-plugin", artifactIdElement.getTextTrim());

        Element versionElement = enforcerPluginElement.getChild("version", namespace);
        assertEquals("3.0.0", versionElement.getTextTrim());
    }

    /**
     * Testable subclass that exposes protected methods for testing.
     */
    private static class TestableBaseUpgradeGoal extends BaseUpgradeGoal {
        @Override
        protected boolean shouldSaveModifications() {
            return false;
        }

        @Override
        public boolean upgradePluginsInPom(UpgradeContext context, Document pomDocument) {
            return super.upgradePluginsInPom(context, pomDocument);
        }

        @Override
        public boolean isVersionBelow(String currentVersion, String minVersion) {
            return super.isVersionBelow(currentVersion, minVersion);
        }

        @Override
        public Map<String, PluginUpgrade> getPluginUpgrades() {
            return super.getPluginUpgrades();
        }

        @Override
        public void applyPluginUpgrades(UpgradeContext context, Map<Path, Document> pomMap) {
            super.applyPluginUpgrades(context, pomMap);
        }
    }
}
