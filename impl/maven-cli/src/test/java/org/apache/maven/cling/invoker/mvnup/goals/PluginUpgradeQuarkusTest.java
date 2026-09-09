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
import java.util.List;
import java.util.Map;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Tests for Quarkus plugin upgrade behavior in {@link PluginUpgradeStrategy}.
 * Extracted from PluginUpgradeStrategyTest to keep file lengths within the Checkstyle limit.
 */
@DisplayName("PluginUpgradeStrategy — Quarkus Plugin Upgrades")
class PluginUpgradeQuarkusTest {

    private PluginUpgradeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PluginUpgradeStrategy();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    @Test
    @DisplayName("should upgrade quarkus-maven-plugin with io.quarkus groupId when below minimum")
    void shouldUpgradeQuarkusPluginWithIoQuarkusGroupId() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>3.16.3</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        UpgradeResult result = strategy.doApply(context, pomMap);

        assertTrue(result.success(), "Plugin upgrade should succeed");
        assertTrue(result.modifiedCount() > 0, "Should have upgraded quarkus-maven-plugin");

        Editor editor = new Editor(document);
        String version = editor.root()
                .path("build", "plugins", "plugin", "version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("3.26.0", version, "quarkus-maven-plugin should be upgraded to 3.26.0");
    }

    @Test
    @DisplayName("should upgrade quarkus-maven-plugin with io.quarkus.platform groupId when below minimum")
    void shouldUpgradeQuarkusPluginWithPlatformGroupId() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus.platform</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>3.16.3</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        UpgradeResult result = strategy.doApply(context, pomMap);

        assertTrue(result.success(), "Plugin upgrade should succeed");
        assertTrue(result.modifiedCount() > 0, "Should have upgraded quarkus-maven-plugin");

        Editor editor = new Editor(document);
        String version = editor.root()
                .path("build", "plugins", "plugin", "version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("3.26.0", version, "quarkus-maven-plugin should be upgraded to 3.26.0");
    }

    @Test
    @DisplayName("should not upgrade quarkus-maven-plugin when version is already sufficient")
    void shouldNotUpgradeQuarkusPluginWhenVersionSufficient() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>3.31.4</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        strategy.doApply(context, pomMap);

        Editor editor = new Editor(document);
        String version = editor.root()
                .path("build", "plugins", "plugin", "version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("3.31.4", version, "Version 3.31.4 should be preserved");
    }

    @Test
    @DisplayName("should decouple plugin version from shared BOM property")
    void shouldDecouplePluginVersionFromSharedBomProperty() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <quarkus.platform.version>3.16.3</quarkus.platform.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.quarkus.platform</groupId>
                                <artifactId>quarkus-bom</artifactId>
                                <version>${quarkus.platform.version}</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>${quarkus.platform.version}</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        UpgradeResult result = strategy.doApply(context, pomMap);

        assertTrue(result.success(), "Plugin upgrade should succeed");
        assertTrue(result.modifiedCount() > 0, "Should have modified POM");

        // BOM property should be unchanged
        Editor editor = new Editor(document);
        String bomVersion = editor.root()
                .path("properties", "quarkus.platform.version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("3.16.3", bomVersion, "BOM property should remain unchanged at 3.16.3");

        // New property should be introduced
        String pluginVersion = editor.root()
                .path("properties", "quarkus-plugin.version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("3.26.0", pluginVersion, "New quarkus-plugin.version property should be 3.26.0");

        // Plugin version should reference the new property
        String pluginVersionRef = editor.root()
                .path("build", "plugins", "plugin", "version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals(
                "${quarkus-plugin.version}",
                pluginVersionRef,
                "Plugin should reference the new quarkus-plugin.version property");
    }

    @Test
    @DisplayName("should not decouple when plugin has its own property not shared with BOM")
    void shouldNotDecoupleWhenPluginHasOwnProperty() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <quarkus-plugin.version>3.16.3</quarkus-plugin.version>
                    </properties>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>${quarkus-plugin.version}</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        UpgradeResult result = strategy.doApply(context, pomMap);

        assertTrue(result.success(), "Plugin upgrade should succeed");
        assertTrue(result.modifiedCount() > 0, "Should have upgraded quarkus-maven-plugin");

        // The property should be upgraded directly (no decoupling needed)
        Editor editor = new Editor(document);
        String version = editor.root()
                .path("properties", "quarkus-plugin.version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("3.26.0", version, "Property should be upgraded directly to 3.26.0");

        // Plugin should still reference the same property
        String pluginVersionRef = editor.root()
                .path("build", "plugins", "plugin", "version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("${quarkus-plugin.version}", pluginVersionRef, "Plugin should still reference the same property");
    }

    @Test
    @DisplayName("should not decouple when BOM version is already sufficient")
    void shouldNotDecoupleWhenBomVersionSufficient() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <quarkus.platform.version>3.31.4</quarkus.platform.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.quarkus.platform</groupId>
                                <artifactId>quarkus-bom</artifactId>
                                <version>${quarkus.platform.version}</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>${quarkus.platform.version}</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        strategy.doApply(context, pomMap);

        // No new property should be introduced — version is already sufficient
        Editor editor = new Editor(document);
        String bomVersion = editor.root()
                .path("properties", "quarkus.platform.version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("3.31.4", bomVersion, "BOM property should remain unchanged");

        // No quarkus-plugin.version should exist
        Element newProp =
                editor.root().path("properties", "quarkus-plugin.version").orElse(null);
        assertTrue(newProp == null, "Should not introduce new property when version is already sufficient");
    }

    @Test
    @DisplayName("should emit version gap warning when decoupling")
    void shouldEmitVersionGapWarningWhenDecoupling() throws Exception {
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <quarkus.platform.version>3.16.3</quarkus.platform.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.quarkus.platform</groupId>
                                <artifactId>quarkus-bom</artifactId>
                                <version>${quarkus.platform.version}</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>${quarkus.platform.version}</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        strategy.doApply(context, pomMap);

        // Verify warning was emitted about the version gap
        verify(context.logger, atLeastOnce())
                .warn(argThat(msg -> msg.contains("quarkus-maven-plugin upgraded to 3.26.0")
                        && msg.contains("3.16.3")
                        && msg.contains("mismatched plugin and platform")));
    }

    @Test
    @DisplayName("should include quarkus-maven-plugin in predefined plugin upgrades")
    void shouldIncludeQuarkusPluginInPredefinedUpgrades() {
        List<PluginUpgrade> upgrades = PluginUpgradeStrategy.getPluginUpgrades();

        boolean hasIoQuarkus = upgrades.stream()
                .anyMatch(u -> "io.quarkus".equals(u.groupId()) && "quarkus-maven-plugin".equals(u.artifactId()));
        boolean hasIoQuarkusPlatform = upgrades.stream()
                .anyMatch(u ->
                        "io.quarkus.platform".equals(u.groupId()) && "quarkus-maven-plugin".equals(u.artifactId()));

        assertTrue(hasIoQuarkus, "Should include io.quarkus:quarkus-maven-plugin upgrade");
        assertTrue(hasIoQuarkusPlatform, "Should include io.quarkus.platform:quarkus-maven-plugin upgrade");

        // Verify the reason text
        upgrades.stream()
                .filter(u -> "quarkus-maven-plugin".equals(u.artifactId()))
                .forEach(u -> assertEquals(
                        "Maven 4 compatibility (Aether API changes)",
                        u.reason(),
                        "Quarkus plugin upgrade should have the correct reason"));
    }

    @Test
    @DisplayName("should upgrade quarkus-maven-plugin in pluginManagement")
    void shouldUpgradeQuarkusPluginInPluginManagement() throws Exception {
        String pomXml = """
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
                                    <groupId>io.quarkus</groupId>
                                    <artifactId>quarkus-maven-plugin</artifactId>
                                    <version>3.16.3</version>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        UpgradeResult result = strategy.doApply(context, pomMap);

        assertTrue(result.success(), "Plugin upgrade should succeed");
        assertTrue(result.modifiedCount() > 0, "Should have upgraded quarkus-maven-plugin in pluginManagement");

        Editor editor = new Editor(document);
        String version = editor.root()
                .path("build", "pluginManagement", "plugins", "plugin", "version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals("3.26.0", version, "quarkus-maven-plugin in pluginManagement should be upgraded to 3.26.0");
    }

    @Test
    @DisplayName("should not decouple when shared property is inherited from parent POM")
    void shouldNotDecoupleWhenSharedPropertyIsInherited() throws Exception {
        // The shared property is NOT declared in this POM — it's inherited from a parent.
        // We cannot resolve its value, so decoupling should be skipped to avoid
        // introducing a quarkus-plugin.version=3.26.0 that might downgrade an already-sufficient version.
        String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.example</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.quarkus.platform</groupId>
                                <artifactId>quarkus-bom</artifactId>
                                <version>${quarkus.platform.version}</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>io.quarkus</groupId>
                                <artifactId>quarkus-maven-plugin</artifactId>
                                <version>${quarkus.platform.version}</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

        Document document = Document.of(pomXml);
        Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

        UpgradeContext context = createMockContext();
        strategy.doApply(context, pomMap);

        Editor editor = new Editor(document);
        Element newProp =
                editor.root().path("properties", "quarkus-plugin.version").orElse(null);
        assertTrue(newProp == null, "Should not introduce quarkus-plugin.version when shared property is inherited");

        // The plugin version reference should remain unchanged
        String pluginVersion = editor.root()
                .path("build", "plugins", "plugin", "version")
                .map(Element::textContentTrimmed)
                .orElse(null);
        assertEquals(
                "${quarkus.platform.version}",
                pluginVersion,
                "Plugin version should remain as the inherited property reference");
    }
}
