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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Indentation.FOUR_SPACES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Indentation.TAB;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Indentation.TWO_SPACES;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for JDomUtils functionality including indentation detection and XML manipulation.
 */
class JDomUtilsTest {

    private SAXBuilder saxBuilder;

    @BeforeEach
    void setUp() {
        saxBuilder = new SAXBuilder();
    }

    @Test
    void testDetectTwoSpaceIndentation() throws Exception {
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
        Element root = document.getRootElement();

        String baseIndent = JDomUtils.detectBaseIndentationUnit(root);
        assertEquals(TWO_SPACES, baseIndent, "Should detect 2-space indentation");
    }

    @Test
    void testDetectFourSpaceIndentation() throws Exception {
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
        Element root = document.getRootElement();

        String baseIndent = JDomUtils.detectBaseIndentationUnit(root);
        assertEquals(FOUR_SPACES, baseIndent, "Should detect 4-space indentation");
    }

    @Test
    void testDetectTabIndentation() throws Exception {
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
            \t<modelVersion>4.0.0</modelVersion>
            \t<groupId>test</groupId>
            \t<artifactId>test</artifactId>
            \t<version>1.0.0</version>
            \t<build>
            \t\t<plugins>
            \t\t\t<plugin>
            \t\t\t\t<groupId>org.apache.maven.plugins</groupId>
            \t\t\t\t<artifactId>maven-compiler-plugin</artifactId>
            \t\t\t\t<version>3.8.1</version>
            \t\t\t</plugin>
            \t\t</plugins>
            \t</build>
            </project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));
        Element root = document.getRootElement();

        String baseIndent = JDomUtils.detectBaseIndentationUnit(root);
        assertEquals(TAB, baseIndent, "Should detect tab indentation");
    }

    @Test
    void testDetectIndentationWithMixedContent() throws Exception {
        // POM with mostly 4-space indentation but some 2-space (should prefer 4-space)
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <properties>
                    <maven.compiler.source>11</maven.compiler.source>
                    <maven.compiler.target>11</maven.compiler.target>
                </properties>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.8.1</version>
                        </plugin>
                    </plugins>
                </build>
              <profiles>
                <profile>
                  <id>test</id>
                </profile>
              </profiles>
            </project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));
        Element root = document.getRootElement();

        String baseIndent = JDomUtils.detectBaseIndentationUnit(root);
        assertEquals(FOUR_SPACES, baseIndent, "Should detect 4-space indentation as the most common pattern");
    }

    @Test
    void testDetectIndentationFromBuildElement() throws Exception {
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
        Element root = document.getRootElement();
        Element buildElement = root.getChild("build", root.getNamespace());

        String baseIndent = JDomUtils.detectBaseIndentationUnit(buildElement);
        assertEquals(FOUR_SPACES, baseIndent, "Should detect 4-space indentation from build element");
    }

    @Test
    void testDetectIndentationFallbackToDefault() throws Exception {
        // Minimal POM with no clear indentation pattern
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>test</artifactId><version>1.0.0</version></project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));
        Element root = document.getRootElement();

        String baseIndent = JDomUtils.detectBaseIndentationUnit(root);
        assertEquals(TWO_SPACES, baseIndent, "Should fallback to 2-space default when no pattern is detected");
    }

    @Test
    void testDetectIndentationConsistency() throws Exception {
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
        Element root = document.getRootElement();
        Element buildElement = root.getChild("build", root.getNamespace());
        Element pluginsElement = buildElement.getChild("plugins", buildElement.getNamespace());

        // All elements should detect the same base indentation unit
        String rootIndent = JDomUtils.detectBaseIndentationUnit(root);
        String buildIndent = JDomUtils.detectBaseIndentationUnit(buildElement);
        String pluginsIndent = JDomUtils.detectBaseIndentationUnit(pluginsElement);

        assertEquals(FOUR_SPACES, rootIndent, "Root should detect 4-space indentation");
        assertEquals(FOUR_SPACES, buildIndent, "Build should detect 4-space indentation");
        assertEquals(FOUR_SPACES, pluginsIndent, "Plugins should detect 4-space indentation");
        assertEquals(rootIndent, buildIndent, "All elements should detect the same indentation");
        assertEquals(buildIndent, pluginsIndent, "All elements should detect the same indentation");
    }

    @Test
    void testAddElementWithCorrectIndentation() throws Exception {
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
        Element root = document.getRootElement();
        Element buildElement = root.getChild("build", root.getNamespace());

        // Add a new pluginManagement element using JDomUtils
        JDomUtils.insertNewElement("pluginManagement", buildElement);

        // Verify the element was added with correct indentation
        XMLOutputter outputter = new XMLOutputter(Format.getRawFormat());
        String pomString = outputter.outputString(document);

        // The pluginManagement should be indented with 4 spaces (same as plugins)
        assertTrue(pomString.contains("    <pluginManagement>"), "pluginManagement should be indented with 4 spaces");
        assertTrue(
                pomString.contains("    </pluginManagement>"),
                "pluginManagement closing tag should be indented with 4 spaces");
    }

    @Test
    void testRealWorldScenarioWithPluginManagementAddition() throws Exception {
        // Real-world POM with 4-space indentation
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                         http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.example</groupId>
                <artifactId>my-project</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <packaging>jar</packaging>

                <properties>
                    <maven.compiler.source>11</maven.compiler.source>
                    <maven.compiler.target>11</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>

                <dependencies>
                    <dependency>
                        <groupId>junit</groupId>
                        <artifactId>junit</artifactId>
                        <version>4.13.2</version>
                        <scope>test</scope>
                    </dependency>
                </dependencies>

                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.8.1</version>
                            <configuration>
                                <source>11</source>
                                <target>11</target>
                            </configuration>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-surefire-plugin</artifactId>
                            <version>3.0.0-M7</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));
        Element root = document.getRootElement();
        Element buildElement = root.getChild("build", root.getNamespace());

        // Verify the detected indentation is 4 spaces
        String baseIndent = JDomUtils.detectBaseIndentationUnit(root);
        assertEquals(FOUR_SPACES, baseIndent, "Should detect 4-space indentation from real-world POM");

        // Add pluginManagement section using the detected indentation
        Element pluginManagementElement = JDomUtils.insertNewElement("pluginManagement", buildElement);
        Element managedPluginsElement = JDomUtils.insertNewElement("plugins", pluginManagementElement);
        Element managedPluginElement = JDomUtils.insertNewElement("plugin", managedPluginsElement);

        // Add plugin details
        JDomUtils.insertContentElement(managedPluginElement, "groupId", "org.apache.maven.plugins");
        JDomUtils.insertContentElement(managedPluginElement, "artifactId", "maven-exec-plugin");
        JDomUtils.insertContentElement(managedPluginElement, "version", "3.2.0");

        // Verify the output maintains consistent 4-space indentation
        XMLOutputter outputter = new XMLOutputter(Format.getRawFormat());
        String pomString = outputter.outputString(document);

        // Check that pluginManagement is properly indented
        assertTrue(pomString.contains("    <pluginManagement>"), "pluginManagement should be indented with 4 spaces");
        assertTrue(
                pomString.contains("        <plugins>"),
                "plugins under pluginManagement should be indented with 8 spaces");
        assertTrue(
                pomString.contains("            <plugin>"),
                "plugin under pluginManagement should be indented with 12 spaces");
        assertTrue(
                pomString.contains("                <groupId>org.apache.maven.plugins</groupId>"),
                "plugin elements should be indented with 16 spaces");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
