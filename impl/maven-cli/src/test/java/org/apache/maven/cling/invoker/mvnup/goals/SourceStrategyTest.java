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

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SourceStrategy")
class SourceStrategyTest {

    private SourceStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SourceStrategy();
    }

    @Nested
    @DisplayName("Applicability")
    class ApplicabilityTests {

        @Test
        @DisplayName("should not be applicable by default")
        void shouldNotBeApplicableByDefault() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createDefaultOptions());
            assertFalse(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should be applicable with --model-version 4.1.0")
        void shouldBeApplicableWithModelVersion410() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            assertTrue(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should be applicable with --model-version 4.2.0")
        void shouldBeApplicableWithModelVersion420() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            assertTrue(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should not be applicable with --model-version 4.0.0")
        void shouldNotBeApplicableWithModelVersion400() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.0.0"));
            assertFalse(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should be applicable with --all")
        void shouldBeApplicableWithAll() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithAll(true));
            assertTrue(strategy.isApplicable(context));
        }
    }

    @Nested
    @DisplayName("Compiler Properties Migration")
    class CompilerPropertiesTests {

        @Test
        @DisplayName("should migrate maven.compiler.release to targetVersion")
        void shouldMigrateCompilerRelease() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element source = doc.root().path("build", "sources", "source").orElseThrow();

            assertEquals("17", source.childTextTrimmed("targetVersion"));
            assertFalse(doc.root().childElement("properties").isPresent());
        }

        @Test
        @DisplayName("should migrate matching maven.compiler.source and target")
        void shouldMigrateMatchingSourceTarget() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <properties>
                        <maven.compiler.source>11</maven.compiler.source>
                        <maven.compiler.target>11</maven.compiler.target>
                    </properties>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element source = doc.root().path("build", "sources", "source").orElseThrow();

            assertEquals("11", source.childTextTrimmed("targetVersion"));
            assertFalse(doc.root().childElement("properties").isPresent());
        }

        @Test
        @DisplayName("should skip when source and target differ")
        void shouldSkipWhenSourceTargetDiffer() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <properties>
                        <maven.compiler.source>11</maven.compiler.source>
                        <maven.compiler.target>17</maven.compiler.target>
                    </properties>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            UpgradeResult result = strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            assertFalse(doc.root().childElement("build").isPresent());
            assertTrue(doc.root().childElement("properties").isPresent());
            assertEquals(0, result.modifiedCount());
        }

        @Test
        @DisplayName("should prefer release over source/target")
        void shouldPreferReleaseOverSourceTarget() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <properties>
                        <maven.compiler.release>21</maven.compiler.release>
                        <maven.compiler.source>17</maven.compiler.source>
                        <maven.compiler.target>17</maven.compiler.target>
                    </properties>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element source = doc.root().path("build", "sources", "source").orElseThrow();

            assertEquals("21", source.childTextTrimmed("targetVersion"));
            assertFalse(doc.root().childElement("properties").isPresent());
        }

        @Test
        @DisplayName("should keep other properties when removing compiler properties")
        void shouldKeepOtherProperties() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                    </properties>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element properties = doc.root().childElement("properties").orElseThrow();
            assertEquals("UTF-8", properties.childTextTrimmed("project.build.sourceEncoding"));
            assertFalse(properties.childElement("maven.compiler.release").isPresent());
        }
    }

    @Nested
    @DisplayName("Compiler Plugin Migration")
    class CompilerPluginTests {

        @Test
        @DisplayName("should migrate plugin release configuration")
        void shouldMigratePluginRelease() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <configuration>
                                    <release>17</release>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element source = doc.root().path("build", "sources", "source").orElseThrow();

            assertEquals("17", source.childTextTrimmed("targetVersion"));

            // Plugin should be removed since it has no remaining config
            assertFalse(doc.root().path("build", "plugins").isPresent());
        }

        @Test
        @DisplayName("should migrate plugin source/target configuration")
        void shouldMigratePluginSourceTarget() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <configuration>
                                    <source>11</source>
                                    <target>11</target>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element source = doc.root().path("build", "sources", "source").orElseThrow();

            assertEquals("11", source.childTextTrimmed("targetVersion"));
        }

        @Test
        @DisplayName("should keep plugin with remaining executions")
        void shouldKeepPluginWithExecutions() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <configuration>
                                    <release>17</release>
                                </configuration>
                                <executions>
                                    <execution>
                                        <id>custom</id>
                                    </execution>
                                </executions>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element plugin = doc.root().path("build", "plugins", "plugin").orElseThrow();
            assertFalse(plugin.childElement("configuration").isPresent());
            assertTrue(plugin.childElement("executions").isPresent());
        }

        @Test
        @DisplayName("should not extract from plugin when properties already provided targetVersion")
        void shouldNotDuplicateTargetVersion() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <properties>
                        <maven.compiler.release>21</maven.compiler.release>
                    </properties>
                    <build>
                        <plugins>
                            <plugin>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <configuration>
                                    <release>21</release>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            var sources = doc.root()
                    .path("build", "sources")
                    .orElseThrow()
                    .childElements("source")
                    .toList();

            assertEquals(1, sources.size());
            assertEquals("21", sources.get(0).childTextTrimmed("targetVersion"));
            assertFalse(doc.root().path("build", "plugins").isPresent());
        }

        @Test
        @DisplayName("should preserve plugin config when it differs from migrated property value")
        void shouldPreservePluginConfigWhenDifferent() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                    <build>
                        <plugins>
                            <plugin>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <configuration>
                                    <release>21</release>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element plugin = doc.root().path("build", "plugins", "plugin").orElseThrow();
            Element configuration = plugin.childElement("configuration").orElseThrow();
            assertEquals("21", configuration.childTextTrimmed("release"));

            Element source = doc.root().path("build", "sources", "source").orElseThrow();
            assertEquals("17", source.childTextTrimmed("targetVersion"));
        }

        @Test
        @DisplayName("should migrate compiler plugin from pluginManagement")
        void shouldMigrateFromPluginManagement() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-compiler-plugin</artifactId>
                                    <configuration>
                                        <release>17</release>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element source = doc.root().path("build", "sources", "source").orElseThrow();
            assertEquals("17", source.childTextTrimmed("targetVersion"));
            assertFalse(doc.root().path("build", "pluginManagement").isPresent());
        }
    }

    @Nested
    @DisplayName("Source Directory Migration")
    class SourceDirectoryTests {

        @Test
        @DisplayName("should migrate non-default sourceDirectory")
        void shouldMigrateNonDefaultSourceDirectory() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <sourceDirectory>src/main/java-custom</sourceDirectory>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element source = doc.root().path("build", "sources", "source").orElseThrow();

            assertEquals("src/main/java-custom", source.childTextTrimmed("directory"));
            assertFalse(doc.root().path("build", "sourceDirectory").isPresent());
        }

        @Test
        @DisplayName("should migrate non-default testSourceDirectory")
        void shouldMigrateNonDefaultTestSourceDirectory() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <testSourceDirectory>src/test/java-custom</testSourceDirectory>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element source = doc.root().path("build", "sources", "source").orElseThrow();

            assertEquals("test", source.childTextTrimmed("scope"));
            assertEquals("src/test/java-custom", source.childTextTrimmed("directory"));
        }

        @Test
        @DisplayName("should remove default sourceDirectory without creating source element")
        void shouldRemoveDefaultSourceDirectory() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <sourceDirectory>src/main/java</sourceDirectory>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            UpgradeResult result = strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            assertFalse(doc.root().childElement("build").isPresent());
            assertEquals(1, result.modifiedCount());
        }
    }

    @Nested
    @DisplayName("Resource Migration")
    class ResourceTests {

        @Test
        @DisplayName("should migrate resource with filtering")
        void shouldMigrateResourceWithFiltering() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                                <filtering>true</filtering>
                            </resource>
                        </resources>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element source = doc.root().path("build", "sources", "source").orElseThrow();

            assertEquals("resources", source.childTextTrimmed("lang"));
            assertEquals("true", source.childTextTrimmed("stringFiltering"));
            assertFalse(doc.root().path("build", "resources").isPresent());
        }

        @Test
        @DisplayName("should migrate resource with includes and excludes")
        void shouldMigrateResourceWithIncludesExcludes() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                                <includes>
                                    <include>**/*.xml</include>
                                </includes>
                                <excludes>
                                    <exclude>**/*.bak</exclude>
                                </excludes>
                            </resource>
                        </resources>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element source = doc.root().path("build", "sources", "source").orElseThrow();

            assertEquals("resources", source.childTextTrimmed("lang"));
            assertEquals("**/*.xml", source.path("includes").orElseThrow().childTextTrimmed("include"));
            assertEquals("**/*.bak", source.path("excludes").orElseThrow().childTextTrimmed("exclude"));
        }

        @Test
        @DisplayName("should migrate resource with targetPath")
        void shouldMigrateResourceWithTargetPath() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                                <targetPath>META-INF</targetPath>
                            </resource>
                        </resources>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element source = doc.root().path("build", "sources", "source").orElseThrow();

            assertEquals("resources", source.childTextTrimmed("lang"));
            assertEquals("META-INF", source.childTextTrimmed("targetPath"));
        }

        @Test
        @DisplayName("should migrate test resource")
        void shouldMigrateTestResource() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <testResources>
                            <testResource>
                                <directory>src/test/resources</directory>
                                <filtering>true</filtering>
                            </testResource>
                        </testResources>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element source = doc.root().path("build", "sources", "source").orElseThrow();

            assertEquals("test", source.childTextTrimmed("scope"));
            assertEquals("resources", source.childTextTrimmed("lang"));
            assertEquals("true", source.childTextTrimmed("stringFiltering"));
        }

        @Test
        @DisplayName("should remove default resource without creating source element")
        void shouldRemoveDefaultResource() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                            </resource>
                        </resources>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            UpgradeResult result = strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            assertFalse(doc.root().childElement("build").isPresent());
            assertEquals(1, result.modifiedCount());
        }
    }

    @Nested
    @DisplayName("Model Version Filtering")
    class ModelVersionFilteringTests {

        @Test
        @DisplayName("should skip POM at model version 4.0.0")
        void shouldSkipPomAt400() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            UpgradeResult result = strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            assertTrue(doc.root().childElement("properties").isPresent());
            assertEquals(0, result.modifiedCount());
        }

        @Test
        @DisplayName("should process POM at model version 4.1.0")
        void shouldProcessPomAt410() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            UpgradeResult result = strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            assertEquals(1, result.modifiedCount());
        }
    }

    @Nested
    @DisplayName("Combined Migration")
    class CombinedTests {

        @Test
        @DisplayName("should merge targetVersion and directory into single source element")
        void shouldMergeTargetVersionAndDirectory() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                    <build>
                        <sourceDirectory>src/main/java-custom</sourceDirectory>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            var sources = doc.root()
                    .path("build", "sources")
                    .orElseThrow()
                    .childElements("source")
                    .toList();

            assertEquals(1, sources.size());
            assertEquals("17", sources.get(0).childTextTrimmed("targetVersion"));
            assertEquals("src/main/java-custom", sources.get(0).childTextTrimmed("directory"));
        }

        @Test
        @DisplayName("should reuse existing source element when adding targetVersion")
        void shouldReuseExistingSourceElement() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                    <build>
                        <sources>
                            <source>
                                <directory>src/main/java-custom</directory>
                            </source>
                        </sources>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            var sources = doc.root()
                    .path("build", "sources")
                    .orElseThrow()
                    .childElements("source")
                    .toList();

            assertEquals(1, sources.size());
            assertEquals("17", sources.get(0).childTextTrimmed("targetVersion"));
            assertEquals("src/main/java-custom", sources.get(0).childTextTrimmed("directory"));
        }

        @Test
        @DisplayName("should create multiple source elements for different resource configs")
        void shouldCreateMultipleResourceSources() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <resources>
                            <resource>
                                <directory>src/main/resources</directory>
                                <filtering>true</filtering>
                            </resource>
                            <resource>
                                <directory>src/main/resources-extra</directory>
                            </resource>
                        </resources>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            var sources = doc.root()
                    .path("build", "sources")
                    .orElseThrow()
                    .childElements("source")
                    .toList();

            assertEquals(2, sources.size());
        }
    }
}
