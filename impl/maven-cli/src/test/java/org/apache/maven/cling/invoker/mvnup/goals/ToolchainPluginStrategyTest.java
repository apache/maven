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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link ToolchainPluginStrategy} class.
 */
@DisplayName("ToolchainPluginStrategy")
class ToolchainPluginStrategyTest {

    private static final Path POM_PATH = Paths.get("/project/pom.xml");

    @Nested
    @DisplayName("Applicability")
    class ApplicabilityTests {

        @Test
        @DisplayName("should be applicable when --all option is true")
        void applicableWithAll() {
            ToolchainPluginStrategy strategy = new ToolchainPluginStrategy();
            assertTrue(strategy.isApplicable(TestUtils.createMockContext(TestUtils.createOptionsWithAll(true))));
        }

        @Test
        @DisplayName("should be applicable with default options (no flags)")
        void applicableWithDefaults() {
            ToolchainPluginStrategy strategy = new ToolchainPluginStrategy();
            assertTrue(strategy.isApplicable(TestUtils.createMockContext()));
        }

        @Test
        @DisplayName("should be applicable when --model is true")
        void applicableWithModel() {
            ToolchainPluginStrategy strategy = new ToolchainPluginStrategy();
            assertTrue(strategy.isApplicable(TestUtils.createMockContext(TestUtils.createOptionsWithFixModel(true))));
        }

        @Test
        @DisplayName("should not be applicable when --model is false")
        void notApplicableWithModelFalse() {
            ToolchainPluginStrategy strategy = new ToolchainPluginStrategy();
            assertFalse(strategy.isApplicable(TestUtils.createMockContext(TestUtils.createOptionsWithFixModel(false))));
        }
    }

    @Nested
    @DisplayName("Source level detection")
    class SourceLevelDetectionTests {

        private ToolchainPluginStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new ToolchainPluginStrategy();
        }

        @Test
        @DisplayName("should detect source level from maven.compiler.release property")
        void detectFromRelease() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <properties>
                            <maven.compiler.release>6</maven.compiler.release>
                        </properties>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            assertEquals(6, strategy.detectSourceLevel(doc));
        }

        @Test
        @DisplayName("should detect source level from maven.compiler.source property")
        void detectFromSource() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <properties>
                            <maven.compiler.source>1.6</maven.compiler.source>
                        </properties>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            assertEquals(6, strategy.detectSourceLevel(doc));
        }

        @Test
        @DisplayName("release property takes precedence over source property")
        void releaseTakesPrecedenceOverSource() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <properties>
                            <maven.compiler.release>7</maven.compiler.release>
                            <maven.compiler.source>6</maven.compiler.source>
                        </properties>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            assertEquals(7, strategy.detectSourceLevel(doc));
        }

        @Test
        @DisplayName("should detect from compiler plugin <release>")
        void detectFromCompilerPluginRelease() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <artifactId>maven-compiler-plugin</artifactId>
                                    <configuration>
                                        <release>6</release>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            assertEquals(6, strategy.detectSourceLevel(doc));
        }

        @Test
        @DisplayName("should detect from compiler plugin <source>")
        void detectFromCompilerPluginSource() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <artifactId>maven-compiler-plugin</artifactId>
                                    <configuration>
                                        <source>1.5</source>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            assertEquals(5, strategy.detectSourceLevel(doc));
        }

        @Test
        @DisplayName("should return -1 when no source level configured")
        void noSourceLevel() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            assertEquals(-1, strategy.detectSourceLevel(doc));
        }

        @Test
        @DisplayName("should detect from pluginManagement")
        void detectFromPluginManagement() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <build>
                            <pluginManagement>
                                <plugins>
                                    <plugin>
                                        <artifactId>maven-compiler-plugin</artifactId>
                                        <configuration>
                                            <release>6</release>
                                        </configuration>
                                    </plugin>
                                </plugins>
                            </pluginManagement>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            assertEquals(6, strategy.detectSourceLevel(doc));
        }
    }

    @Nested
    @DisplayName("Toolchains plugin detection")
    class ToolchainsPluginDetectionTests {

        private ToolchainPluginStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new ToolchainPluginStrategy();
        }

        @Test
        @DisplayName("should detect existing select-jdk-toolchain goal")
        void detectExistingGoal() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-toolchains-plugin</artifactId>
                                    <executions>
                                        <execution>
                                            <goals>
                                                <goal>select-jdk-toolchain</goal>
                                            </goals>
                                        </execution>
                                    </executions>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            assertTrue(strategy.hasToolchainsPluginWithSelectGoal(doc));
        }

        @Test
        @DisplayName("should not detect toolchains plugin without select-jdk-toolchain goal")
        void noSelectGoal() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-toolchains-plugin</artifactId>
                                    <executions>
                                        <execution>
                                            <goals>
                                                <goal>toolchain</goal>
                                            </goals>
                                        </execution>
                                    </executions>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            assertFalse(strategy.hasToolchainsPluginWithSelectGoal(doc));
        }

        @Test
        @DisplayName("should not detect when no toolchains plugin present")
        void noToolchainsPlugin() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            assertFalse(strategy.hasToolchainsPluginWithSelectGoal(doc));
        }
    }

    @Nested
    @DisplayName("Plugin addition")
    class PluginAdditionTests {

        private ToolchainPluginStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new ToolchainPluginStrategy();
        }

        @Test
        @DisplayName("should add toolchains plugin with version constraint to POM without build section")
        void addToEmptyPom() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            strategy.addToolchainsPlugin(doc, 11);

            assertTrue(strategy.hasToolchainsPluginWithSelectGoal(doc));
            String output = doc.toXml();
            assertTrue(output.contains("<version>(,11]</version>"), "Expected version constraint in output: " + output);
            assertTrue(
                    output.contains("<version>3.2.0</version>"), "Expected plugin version 3.2.0 in output: " + output);
        }

        @Test
        @DisplayName("should add toolchains plugin with version constraint to POM with existing build section")
        void addToExistingBuild() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <artifactId>maven-compiler-plugin</artifactId>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            strategy.addToolchainsPlugin(doc, 8);

            assertTrue(strategy.hasToolchainsPluginWithSelectGoal(doc));
            String output = doc.toXml();
            assertTrue(output.contains("<version>(,8]</version>"), "Expected version constraint in output: " + output);
            assertTrue(
                    output.contains("<version>3.2.0</version>"), "Expected plugin version 3.2.0 in output: " + output);
        }

        @Test
        @DisplayName("should reuse existing toolchains-plugin and not create a duplicate")
        void reuseExistingToolchainsPlugin() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-toolchains-plugin</artifactId>
                                    <version>3.2.0</version>
                                    <executions>
                                        <execution>
                                            <goals>
                                                <goal>toolchain</goal>
                                            </goals>
                                        </execution>
                                    </executions>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            strategy.addToolchainsPlugin(doc, 11);

            assertTrue(strategy.hasToolchainsPluginWithSelectGoal(doc));
            String output = doc.toXml();
            // Should NOT create a duplicate plugin entry
            assertEquals(
                    1,
                    output.split("maven-toolchains-plugin").length - 1,
                    "Should have exactly one toolchains-plugin entry: " + output);
            // Old toolchain goal should still be present
            assertTrue(
                    output.contains("<goal>toolchain</goal>"), "Old toolchain goal should still be present: " + output);
            // New select-jdk-toolchain goal should be added
            assertTrue(
                    output.contains("<goal>select-jdk-toolchain</goal>"),
                    "New select-jdk-toolchain goal should be present: " + output);
        }

        @Test
        @DisplayName("should not change existing version when reusing plugin")
        void preserveExistingVersion() {
            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <build>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-toolchains-plugin</artifactId>
                                    <version>3.3.0</version>
                                </plugin>
                            </plugins>
                        </build>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            strategy.addToolchainsPlugin(doc, 11);

            String output = doc.toXml();
            // Version should remain 3.3.0 — ToolchainPluginStrategy does not touch versions;
            // version upgrades are handled by PluginUpgradeStrategy
            assertTrue(output.contains("<version>3.3.0</version>"), "Version 3.3.0 should not be changed: " + output);
        }
    }

    @Nested
    @DisplayName("Full apply")
    class ApplyTests {

        @Test
        @DisplayName("should add plugin when source level is incompatible with running JDK")
        void addsPluginWhenIncompatible() {
            // Simulate running JDK 21, project targets source 6
            ToolchainPluginStrategy strategy = new ToolchainPluginStrategy() {
                @Override
                int getRunningJdkMajor() {
                    return 21;
                }
            };

            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <properties>
                            <maven.compiler.release>6</maven.compiler.release>
                        </properties>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            assertTrue(strategy.hasToolchainsPluginWithSelectGoal(doc));
        }

        @Test
        @DisplayName("should not modify POM when source level is compatible")
        void noModificationWhenCompatible() {
            // Simulate running JDK 17, project targets source 11
            ToolchainPluginStrategy strategy = new ToolchainPluginStrategy() {
                @Override
                int getRunningJdkMajor() {
                    return 17;
                }
            };

            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <properties>
                            <maven.compiler.release>11</maven.compiler.release>
                        </properties>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(0, result.modifiedPoms().size());
        }

        @Test
        @DisplayName("should not modify POM when no source level configured")
        void noModificationWithoutSourceLevel() {
            ToolchainPluginStrategy strategy = new ToolchainPluginStrategy() {
                @Override
                int getRunningJdkMajor() {
                    return 21;
                }
            };

            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
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
        @DisplayName("should not add duplicate plugin when already present")
        void noDuplicatePlugin() {
            ToolchainPluginStrategy strategy = new ToolchainPluginStrategy() {
                @Override
                int getRunningJdkMajor() {
                    return 21;
                }
            };

            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.example</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0</version>
                        <properties>
                            <maven.compiler.release>6</maven.compiler.release>
                        </properties>
                        <build>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-toolchains-plugin</artifactId>
                                    <executions>
                                        <execution>
                                            <goals>
                                                <goal>select-jdk-toolchain</goal>
                                            </goals>
                                        </execution>
                                    </executions>
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
    }
}
