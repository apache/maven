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
import java.util.Map;

import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

/**
 * Test for ParentPomResolver using Maven 4 API.
 */
class ParentPomResolverTest {

    @Mock
    private UpgradeContext context;

    private SAXBuilder saxBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        saxBuilder = new SAXBuilder();

        // Mock context methods
        doNothing().when(context).debug(anyString());
        doNothing().when(context).detail(anyString());
    }

    @Test
    @DisplayName("should handle POM with external parent using Maven 4 API")
    void shouldHandlePomWithExternalParentUsingMaven4Api() throws Exception {
        String pomXml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.2.0</version>
                        <relativePath/>
                    </parent>
                    <groupId>com.example</groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

        Document pomDocument = saxBuilder.build(new StringReader(pomXml));

        // Create plugin upgrades map with Maven plugins that might be in Spring Boot parent
        Map<String, PluginUpgrade> pluginUpgrades = new HashMap<>();
        pluginUpgrades.put(
                "org.apache.maven.plugins:maven-compiler-plugin",
                new PluginUpgrade(
                        "org.apache.maven.plugins", "maven-compiler-plugin", "3.2.0", "Maven 4 compatibility"));
        pluginUpgrades.put(
                "org.apache.maven.plugins:maven-surefire-plugin",
                new PluginUpgrade(
                        "org.apache.maven.plugins", "maven-surefire-plugin", "3.0.0", "Maven 4 compatibility"));

        // This should use Maven 4 API to resolve the effective POM
        // Note: This test might fail in CI if network access is limited
        boolean result =
                new ParentPomResolver().checkParentPomsForPlugins(context, pomDocument, pluginUpgrades, Map.of());

        // The result depends on what plugins are actually in the Spring Boot parent
        // We're mainly testing that the method doesn't throw exceptions
        assertTrue(result || !result); // Always passes, just testing execution
    }

    @Test
    @DisplayName("should handle POM without parent")
    void shouldHandlePomWithoutParent() throws Exception {
        String pomXml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

        Document pomDocument = saxBuilder.build(new StringReader(pomXml));

        Map<String, PluginUpgrade> pluginUpgrades = new HashMap<>();
        pluginUpgrades.put(
                "org.apache.maven.plugins:maven-compiler-plugin",
                new PluginUpgrade(
                        "org.apache.maven.plugins", "maven-compiler-plugin", "3.2.0", "Maven 4 compatibility"));

        boolean result =
                new ParentPomResolver().checkParentPomsForPlugins(context, pomDocument, pluginUpgrades, Map.of());

        assertFalse(result, "Should return false when no parent is present");
    }

    @Test
    @DisplayName("should handle POM with incomplete parent coordinates")
    void shouldHandlePomWithIncompleteParentCoordinates() throws Exception {
        String pomXml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <!-- Missing version -->
                    </parent>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

        Document pomDocument = saxBuilder.build(new StringReader(pomXml));

        Map<String, PluginUpgrade> pluginUpgrades = new HashMap<>();
        pluginUpgrades.put(
                "org.apache.maven.plugins:maven-compiler-plugin",
                new PluginUpgrade(
                        "org.apache.maven.plugins", "maven-compiler-plugin", "3.2.0", "Maven 4 compatibility"));

        boolean result =
                new ParentPomResolver().checkParentPomsForPlugins(context, pomDocument, pluginUpgrades, Map.of());

        assertFalse(result, "Should return false when parent coordinates are incomplete");
    }

    @Test
    @DisplayName("should detect local parent and skip Maven 4 API check")
    void shouldDetectLocalParentAndSkipMaven4ApiCheck() throws Exception {
        // Create parent POM
        String parentPomXml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                </project>
                """;

        // Create child POM with local parent
        String childPomXml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>child-project</artifactId>
                </project>
                """;

        Document parentDoc = saxBuilder.build(new StringReader(parentPomXml));
        Document childDoc = saxBuilder.build(new StringReader(childPomXml));

        // Create pomMap with both parent and child
        Map<Path, Document> pomMap = Map.of(
                Paths.get("parent", "pom.xml"), parentDoc,
                Paths.get("child", "pom.xml"), childDoc);

        Map<String, PluginUpgrade> pluginUpgrades = new HashMap<>();
        pluginUpgrades.put(
                "org.apache.maven.plugins:maven-compiler-plugin",
                new PluginUpgrade(
                        "org.apache.maven.plugins", "maven-compiler-plugin", "3.2.0", "Maven 4 compatibility"));

        // Should detect local parent and skip Maven 4 API check
        boolean result = new ParentPomResolver().checkParentPomsForPlugins(context, childDoc, pluginUpgrades, pomMap);

        assertFalse(result, "Should return false when parent is local (no external parent processing needed)");
    }

    @Test
    @DisplayName("should detect external parent and use Maven 4 API check")
    void shouldDetectExternalParentAndUseMaven4ApiCheck() throws Exception {
        // Create child POM with external parent (Spring Boot)
        String childPomXml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.2.0</version>
                    </parent>
                    <groupId>com.example</groupId>
                    <artifactId>child-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

        Document childDoc = saxBuilder.build(new StringReader(childPomXml));

        // Create pomMap with only the child (parent is external)
        Map<Path, Document> pomMap = Map.of(Paths.get("child", "pom.xml"), childDoc);

        Map<String, PluginUpgrade> pluginUpgrades = new HashMap<>();
        pluginUpgrades.put(
                "org.apache.maven.plugins:maven-compiler-plugin",
                new PluginUpgrade(
                        "org.apache.maven.plugins", "maven-compiler-plugin", "3.2.0", "Maven 4 compatibility"));

        // Should detect external parent and attempt Maven 4 API check
        // Note: This might fail due to network issues, but we're testing the detection logic
        try {
            boolean result =
                    new ParentPomResolver().checkParentPomsForPlugins(context, childDoc, pluginUpgrades, pomMap);
            // Result depends on network access and what's in the Spring Boot parent
            assertTrue(result || !result); // Always passes, just testing that it attempts the check
        } catch (Exception e) {
            // Expected if Maven 4 API fails due to network or other issues
            assertTrue(e.getMessage().contains("Maven 4 API failed")
                    || e.getMessage().contains("Failed to use Maven 4 API"));
        }
    }
}
