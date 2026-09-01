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
import org.apache.maven.api.model.Model;
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

    @Nested
    @DisplayName("Effective model source level detection")
    class EffectiveModelSourceLevelTests {

        @Test
        @DisplayName("should detect source level from effective model maven.compiler.release")
        void detectFromEffectiveRelease() {
            Model model = Model.newBuilder()
                    .properties(Map.of("maven.compiler.release", "6"))
                    .build();
            assertEquals(6, ToolchainPluginStrategy.detectSourceLevelFromEffectiveModel(model));
        }

        @Test
        @DisplayName("should detect source level from effective model maven.compiler.source")
        void detectFromEffectiveSource() {
            Model model = Model.newBuilder()
                    .properties(Map.of("maven.compiler.source", "1.5"))
                    .build();
            assertEquals(5, ToolchainPluginStrategy.detectSourceLevelFromEffectiveModel(model));
        }

        @Test
        @DisplayName("release takes precedence over source in effective model")
        void effectiveReleasePrecedence() {
            Model model = Model.newBuilder()
                    .properties(Map.of(
                            "maven.compiler.release", "7",
                            "maven.compiler.source", "6"))
                    .build();
            assertEquals(7, ToolchainPluginStrategy.detectSourceLevelFromEffectiveModel(model));
        }

        @Test
        @DisplayName("should return -1 when effective model has no compiler properties")
        void noEffectiveSourceLevel() {
            Model model = Model.newBuilder()
                    .properties(Map.of("some.other.property", "value"))
                    .build();
            assertEquals(-1, ToolchainPluginStrategy.detectSourceLevelFromEffectiveModel(model));
        }

        @Test
        @DisplayName("should return -1 when effective model has empty properties")
        void emptyEffectiveProperties() {
            Model model = Model.newBuilder().properties(Map.of()).build();
            assertEquals(-1, ToolchainPluginStrategy.detectSourceLevelFromEffectiveModel(model));
        }

        @Test
        @DisplayName("should handle legacy 1.x format in effective model")
        void legacyFormatInEffectiveModel() {
            Model model = Model.newBuilder()
                    .properties(Map.of("maven.compiler.source", "1.6"))
                    .build();
            assertEquals(6, ToolchainPluginStrategy.detectSourceLevelFromEffectiveModel(model));
        }

        @Test
        @DisplayName("should add plugin when source level is inherited from parent POM via effective model")
        void addsPluginWhenSourceInherited() {
            // Simulate running JDK 21, source level inherited from parent (not in local POM)
            ToolchainPluginStrategy strategy = new ToolchainPluginStrategy() {
                @Override
                int getRunningJdkMajor() {
                    return 21;
                }

                @Override
                int detectEffectiveSourceLevel(UpgradeContext context, Path pomPath) {
                    // Simulate parent POM setting maven.compiler.source=1.5
                    return 5;
                }
            };

            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>org.apache.geronimo.genesis</groupId>
                            <artifactId>genesis</artifactId>
                            <version>1.0</version>
                        </parent>
                        <artifactId>test-child</artifactId>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            assertTrue(strategy.hasToolchainsPluginWithSelectGoal(doc));
            String output = doc.toXml();
            assertTrue(output.contains("<version>(,8]</version>"), "Expected JDK 8 constraint for source 5: " + output);
        }

        @Test
        @DisplayName("should not modify POM when inherited source level is compatible with running JDK")
        void noModificationWhenInheritedLevelCompatible() {
            // Simulate running JDK 17, inherited source level 11 (still supported)
            ToolchainPluginStrategy strategy = new ToolchainPluginStrategy() {
                @Override
                int getRunningJdkMajor() {
                    return 17;
                }

                @Override
                int detectEffectiveSourceLevel(UpgradeContext context, Path pomPath) {
                    return 11;
                }
            };

            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>com.example</groupId>
                            <artifactId>parent</artifactId>
                            <version>1.0</version>
                        </parent>
                        <artifactId>child</artifactId>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(0, result.modifiedPoms().size());
        }

        @Test
        @DisplayName("should fall back gracefully when effective model resolution fails")
        void fallbackWhenEffectiveModelFails() {
            // Simulate effective model resolution failure
            ToolchainPluginStrategy strategy = new ToolchainPluginStrategy() {
                @Override
                int getRunningJdkMajor() {
                    return 21;
                }

                @Override
                int detectEffectiveSourceLevel(UpgradeContext context, Path pomPath) {
                    return -1; // Simulate failure
                }
            };

            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>com.example</groupId>
                            <artifactId>parent</artifactId>
                            <version>1.0</version>
                        </parent>
                        <artifactId>child</artifactId>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            // Should not modify POM and not fail
            assertEquals(0, result.modifiedPoms().size());
            assertEquals(0, result.errorPoms().size());
        }

        @Test
        @DisplayName("local POM source level takes precedence over effective model")
        void localTakesPrecedenceOverEffective() {
            // Local POM has source=11, but effective model would return 6
            // Local should win — effective model is only consulted when local returns -1
            ToolchainPluginStrategy strategy = new ToolchainPluginStrategy() {
                @Override
                int getRunningJdkMajor() {
                    return 21;
                }

                @Override
                int detectEffectiveSourceLevel(UpgradeContext context, Path pomPath) {
                    // This should NOT be consulted because local detection succeeds
                    return 6;
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

            // Source 11 is supported by JDK 21, so no modification
            assertEquals(0, result.modifiedPoms().size());
        }

        @Test
        @DisplayName("should add plugin with correct JDK constraint for inherited source 6")
        void correctJdkConstraintForSource6() {
            ToolchainPluginStrategy strategy = new ToolchainPluginStrategy() {
                @Override
                int getRunningJdkMajor() {
                    return 21;
                }

                @Override
                int detectEffectiveSourceLevel(UpgradeContext context, Path pomPath) {
                    return 6; // Inherited source level 6 (e.g., geronimo-genesis)
                }
            };

            String pomXml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                        <modelVersion>4.0.0</modelVersion>
                        <parent>
                            <groupId>org.apache.geronimo.genesis</groupId>
                            <artifactId>genesis</artifactId>
                            <version>2.0</version>
                        </parent>
                        <artifactId>test-module</artifactId>
                    </project>
                    """;
            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext();

            UpgradeResult result = strategy.doApply(context, Map.of(POM_PATH, doc));

            assertEquals(1, result.modifiedPoms().size());
            assertTrue(strategy.hasToolchainsPluginWithSelectGoal(doc));
            String output = doc.toXml();
            // Source level 6 requires JDK <= 11
            assertTrue(
                    output.contains("<version>(,11]</version>"), "Expected JDK 11 constraint for source 6: " + output);
        }
    }
}
