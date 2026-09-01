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
import java.util.Map;

import eu.maveniverse.domtrip.Document;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link ModuleNameFixStrategy} class.
 */
@DisplayName("ModuleNameFixStrategy")
class ModuleNameFixStrategyTest {

    private static final Path POM_PATH = Paths.get("/project/pom.xml");

    @Nested
    @DisplayName("sanitizeModuleName")
    class SanitizeTests {

        @Test
        @DisplayName("should replace dashes with dots")
        void replacesDashes() {
            assertEquals(
                    "org.apache.geronimo.arthur.integration.test",
                    ModuleNameFixStrategy.sanitizeModuleName("org.apache.geronimo.arthur.integration-test"));
        }

        @Test
        @DisplayName("should replace multiple dashes")
        void replacesMultipleDashes() {
            assertEquals(
                    "org.apache.winegrower.cepages.winegrower.cepage.osgi.cdi",
                    ModuleNameFixStrategy.sanitizeModuleName(
                            "org.apache.winegrower.cepages.winegrower-cepage-osgi-cdi"));
        }

        @Test
        @DisplayName("should collapse consecutive dots")
        void collapsesConsecutiveDots() {
            assertEquals("foo.bar", ModuleNameFixStrategy.sanitizeModuleName("foo--bar"));
        }

        @Test
        @DisplayName("should leave valid names unchanged")
        void leavesValidUnchanged() {
            assertEquals("org.example.valid.name", ModuleNameFixStrategy.sanitizeModuleName("org.example.valid.name"));
        }

        @Test
        @DisplayName("should handle leading dash")
        void handlesLeadingDash() {
            assertEquals("foo.bar", ModuleNameFixStrategy.sanitizeModuleName("-foo.bar"));
        }

        @Test
        @DisplayName("should handle trailing dash")
        void handlesTrailingDash() {
            assertEquals("foo.bar", ModuleNameFixStrategy.sanitizeModuleName("foo.bar-"));
        }
    }

    @Nested
    @DisplayName("Fix existing entries")
    class FixExistingTests {

        @Test
        @DisplayName("should fix Automatic-Module-Name with dashes in jar plugin config")
        void fixesDashInJarPlugin() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>integration-test</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-jar-plugin</artifactId>
                                    <configuration>
                                        <archive>
                                            <manifestEntries>
                                                <Automatic-Module-Name>org.example.integration-test</Automatic-Module-Name>
                                            </manifestEntries>
                                        </archive>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String xml = doc.toXml();
            assertTrue(xml.contains("org.example.integration.test"));
            assertFalse(xml.contains("org.example.integration-test"));
        }

        @Test
        @DisplayName("should not modify valid Automatic-Module-Name")
        void noChangeForValid() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>my-module</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-jar-plugin</artifactId>
                                    <configuration>
                                        <archive>
                                            <manifestEntries>
                                                <Automatic-Module-Name>org.example.mymodule</Automatic-Module-Name>
                                            </manifestEntries>
                                        </archive>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(0, result.modifiedPoms().size());
        }

        @Test
        @DisplayName("should fix in pluginManagement section")
        void fixesInPluginManagement() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <build>
                            <pluginManagement>
                                <plugins>
                                    <plugin>
                                        <artifactId>maven-jar-plugin</artifactId>
                                        <configuration>
                                            <archive>
                                                <manifestEntries>
                                                    <Automatic-Module-Name>org.example.my-lib</Automatic-Module-Name>
                                                </manifestEntries>
                                            </archive>
                                        </configuration>
                                    </plugin>
                                </plugins>
                            </pluginManagement>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            assertTrue(doc.toXml().contains("org.example.my.lib"));
        }

        @Test
        @DisplayName("should fix in shade plugin configuration")
        void fixesInShadePlugin() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-shade-plugin</artifactId>
                                    <configuration>
                                        <archive>
                                            <manifestEntries>
                                                <Automatic-Module-Name>org.example.shaded-lib</Automatic-Module-Name>
                                            </manifestEntries>
                                        </archive>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            assertTrue(doc.toXml().contains("org.example.shaded.lib"));
        }

        @Test
        @DisplayName("should ignore non-packaging plugins")
        void ignoresNonPackagingPlugins() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <groupId>com.example</groupId>
                                    <artifactId>custom-plugin</artifactId>
                                    <configuration>
                                        <archive>
                                            <manifestEntries>
                                                <Automatic-Module-Name>org.example.my-lib</Automatic-Module-Name>
                                            </manifestEntries>
                                        </archive>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(0, result.modifiedPoms().size());
        }

        @Test
        @DisplayName("should fix in profile plugin configuration")
        void fixesInProfile() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <profiles>
                            <profile>
                                <id>release</id>
                                <build>
                                    <plugins>
                                        <plugin>
                                            <artifactId>maven-jar-plugin</artifactId>
                                            <configuration>
                                                <archive>
                                                    <manifestEntries>
                                                        <Automatic-Module-Name>org.example.my-release</Automatic-Module-Name>
                                                    </manifestEntries>
                                                </archive>
                                            </configuration>
                                        </plugin>
                                    </plugins>
                                </build>
                            </profile>
                        </profiles>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            assertTrue(doc.toXml().contains("org.example.my.release"));
        }
    }

    @Nested
    @DisplayName("Add missing module name for hyphenated artifactIds")
    class AddMissingTests {

        @Test
        @DisplayName("should add Automatic-Module-Name for hyphenated artifactId with default packaging")
        void addsForDefaultPackaging() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.apache.geronimo.arthur</groupId>
                        <artifactId>integration-test</artifactId>
                        <version>1.0</version>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String xml = doc.toXml();
            assertTrue(xml.contains("Automatic-Module-Name"));
            assertTrue(xml.contains("org.apache.geronimo.arthur.integration.test"));
            assertTrue(xml.contains("maven-jar-plugin"));
        }

        @Test
        @DisplayName("should add Automatic-Module-Name for hyphenated artifactId with explicit jar packaging")
        void addsForExplicitJarPackaging() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>my-library</artifactId>
                        <version>1.0</version>
                        <packaging>jar</packaging>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String xml = doc.toXml();
            assertTrue(xml.contains("org.example.my.library"));
        }

        @Test
        @DisplayName("should add to existing jar-plugin entry")
        void addsToExistingJarPlugin() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>my-library</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-jar-plugin</artifactId>
                                    <version>3.5.0</version>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String xml = doc.toXml();
            assertTrue(xml.contains("org.example.my.library"));
            // Should add config to the existing plugin, not create a new one (count opening tags only)
            assertEquals(1, countOccurrences(xml, ">maven-jar-plugin<"));
        }

        @Test
        @DisplayName("should find jar-plugin in pluginManagement")
        void addsToPluginManagementJarPlugin() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>my-library</artifactId>
                        <version>1.0</version>
                        <build>
                            <pluginManagement>
                                <plugins>
                                    <plugin>
                                        <artifactId>maven-jar-plugin</artifactId>
                                        <version>3.5.0</version>
                                    </plugin>
                                </plugins>
                            </pluginManagement>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String xml = doc.toXml();
            assertTrue(xml.contains("org.example.my.library"));
        }

        @Test
        @DisplayName("should skip pom packaging")
        void skipsPomPackaging() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>my-parent</artifactId>
                        <version>1.0</version>
                        <packaging>pom</packaging>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(0, result.modifiedPoms().size());
        }

        @Test
        @DisplayName("should skip war packaging")
        void skipsWarPackaging() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>my-webapp</artifactId>
                        <version>1.0</version>
                        <packaging>war</packaging>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(0, result.modifiedPoms().size());
        }

        @Test
        @DisplayName("should skip non-hyphenated artifactId")
        void skipsNonHyphenated() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>mymodule</artifactId>
                        <version>1.0</version>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(0, result.modifiedPoms().size());
        }

        @Test
        @DisplayName("should skip when explicit Automatic-Module-Name already exists")
        void skipsWhenExplicitEntryExists() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>my-library</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <artifactId>maven-jar-plugin</artifactId>
                                    <configuration>
                                        <archive>
                                            <manifestEntries>
                                                <Automatic-Module-Name>org.example.mylibrary</Automatic-Module-Name>
                                            </manifestEntries>
                                        </archive>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            // Should not modify — valid entry already exists
            assertEquals(0, result.modifiedPoms().size());
        }

        @Test
        @DisplayName("should resolve groupId from parent when not set on module")
        void resolvesGroupIdFromParent() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>org.apache.geronimo.arthur</groupId>
                            <artifactId>arthur</artifactId>
                            <version>1.0</version>
                        </parent>
                        <artifactId>integration-test</artifactId>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String xml = doc.toXml();
            assertTrue(xml.contains("org.apache.geronimo.arthur.integration.test"));
        }

        @Test
        @DisplayName("should handle multiple dashes in artifactId")
        void handlesMultipleDashes() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.apache.winegrower.cepages</groupId>
                        <artifactId>winegrower-cepage-osgi-cdi</artifactId>
                        <version>1.0</version>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String xml = doc.toXml();
            assertTrue(xml.contains("org.apache.winegrower.cepages.winegrower.cepage.osgi.cdi"));
        }

        @Test
        @DisplayName("should work with maven-plugin packaging")
        void worksWithMavenPluginPackaging() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>my-maven-plugin</artifactId>
                        <version>1.0</version>
                        <packaging>maven-plugin</packaging>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String xml = doc.toXml();
            assertTrue(xml.contains("org.example.my.maven.plugin"));
        }
    }

    @Nested
    @DisplayName("No plugins in POM")
    class NoPluginsTests {

        @Test
        @DisplayName("should handle POM without any plugins")
        void noPlugins() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            // No hyphen in artifactId, no existing entries → no changes
            assertEquals(0, result.modifiedPoms().size());
        }
    }

    @Nested
    @DisplayName("Combined scenarios")
    class CombinedTests {

        @Test
        @DisplayName("should fix existing entry AND skip auto-generation prevention when entry exists")
        void fixesExistingAndSkipsAutoGeneration() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            // This POM has a hyphenated artifactId AND an existing invalid entry
            // Part 1 fixes the entry, Part 2 sees it already exists and skips
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.example</groupId>
                        <artifactId>my-library</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <artifactId>maven-jar-plugin</artifactId>
                                    <configuration>
                                        <archive>
                                            <manifestEntries>
                                                <Automatic-Module-Name>org.example.my-library</Automatic-Module-Name>
                                            </manifestEntries>
                                        </archive>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String xml = doc.toXml();
            // The entry should be fixed
            assertTrue(xml.contains("org.example.my.library"));
            assertFalse(xml.contains("org.example.my-library"));
            // Only one Automatic-Module-Name element should exist (count opening tags)
            assertEquals(1, countOccurrences(xml, "<Automatic-Module-Name>"));
        }

        @Test
        @DisplayName("geronimo-arthur integration-test case from issue")
        void geronimoArthurCase() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>org.apache.geronimo.arthur</groupId>
                            <artifactId>arthur</artifactId>
                            <version>1.0.10-SNAPSHOT</version>
                        </parent>
                        <artifactId>integration-test</artifactId>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String xml = doc.toXml();
            assertTrue(xml.contains("org.apache.geronimo.arthur.integration.test"));
            assertTrue(xml.contains("maven-jar-plugin"));
        }

        @Test
        @DisplayName("karaf-winegrower cepage case from issue")
        void karafWinegrowerCase() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.apache.winegrower.cepages</groupId>
                        <artifactId>winegrower-cepage-osgi-cdi</artifactId>
                        <version>1.0</version>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String xml = doc.toXml();
            assertTrue(xml.contains("org.apache.winegrower.cepages.winegrower.cepage.osgi.cdi"));
        }

        @Test
        @DisplayName("openwebbeans-meecrowave no-cxf case from issue")
        void meecrowaveNoCxfCase() {
            ModuleNameFixStrategy strategy = new ModuleNameFixStrategy();
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.apache.meecrowave</groupId>
                        <artifactId>no-cxf</artifactId>
                        <version>1.0</version>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();
            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            String xml = doc.toXml();
            assertTrue(xml.contains("org.apache.meecrowave.no.cxf"));
        }
    }

    /**
     * Counts the number of occurrences of a substring in a string.
     */
    private static int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
