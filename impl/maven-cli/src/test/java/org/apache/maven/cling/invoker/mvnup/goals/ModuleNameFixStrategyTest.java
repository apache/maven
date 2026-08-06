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
    @DisplayName("Apply")
    class ApplyTests {

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
            // Verify the fix was applied
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

            assertEquals(0, result.modifiedPoms().size());
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
}
