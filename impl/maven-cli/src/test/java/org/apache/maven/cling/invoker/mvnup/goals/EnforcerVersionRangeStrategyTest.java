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
import java.util.Optional;

import eu.maveniverse.domtrip.Document;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link EnforcerVersionRangeStrategy} class.
 * Tests widening of RequireMavenVersion ranges to allow Maven 4.
 */
@DisplayName("EnforcerVersionRangeStrategy")
class EnforcerVersionRangeStrategyTest {

    private EnforcerVersionRangeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new EnforcerVersionRangeStrategy();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    private UpgradeContext createMockContext(UpgradeOptions options) {
        return TestUtils.createMockContext(options);
    }

    @Nested
    @DisplayName("Applicability")
    class ApplicabilityTests {

        @Test
        @DisplayName("should be applicable when --model option is true")
        void shouldBeApplicableWhenModelOptionTrue() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.model()).thenReturn(Optional.of(true));
            when(options.all()).thenReturn(Optional.empty());
            when(options.infer()).thenReturn(Optional.empty());
            when(options.plugins()).thenReturn(Optional.empty());
            when(options.modelVersion()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);
            assertTrue(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should be applicable when --all option is specified")
        void shouldBeApplicableWhenAllOptionSpecified() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.all()).thenReturn(Optional.of(true));
            when(options.model()).thenReturn(Optional.empty());
            when(options.infer()).thenReturn(Optional.empty());
            when(options.plugins()).thenReturn(Optional.empty());
            when(options.modelVersion()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);
            assertTrue(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should be applicable by default when no specific options provided")
        void shouldBeApplicableByDefaultWhenNoSpecificOptions() {
            UpgradeContext context = createMockContext(TestUtils.createDefaultOptions());
            assertTrue(strategy.isApplicable(context));
        }
    }

    @Nested
    @DisplayName("Version Range Widening")
    class VersionRangeWideningTests {

        @Test
        @DisplayName("should widen [3.8.8,4) to [3.8.8,5)")
        void shouldWidenStandardRange() {
            assertEquals("[3.8.8,5)", EnforcerVersionRangeStrategy.widenVersionRange("[3.8.8,4)"));
        }

        @Test
        @DisplayName("should widen [3,4) to [3,5)")
        void shouldWidenShortRange() {
            assertEquals("[3,5)", EnforcerVersionRangeStrategy.widenVersionRange("[3,4)"));
        }

        @Test
        @DisplayName("should widen (3.6.3,4) to (3.6.3,5)")
        void shouldWidenExclusiveLowerBoundRange() {
            assertEquals("(3.6.3,5)", EnforcerVersionRangeStrategy.widenVersionRange("(3.6.3,4)"));
        }

        @Test
        @DisplayName("should widen [3.8.8,4.0) to [3.8.8,5)")
        void shouldWidenRange4Dot0() {
            assertEquals("[3.8.8,5)", EnforcerVersionRangeStrategy.widenVersionRange("[3.8.8,4.0)"));
        }

        @Test
        @DisplayName("should widen [3.8.8,4.0.0) to [3.8.8,5)")
        void shouldWidenRange4Dot0Dot0() {
            assertEquals("[3.8.8,5)", EnforcerVersionRangeStrategy.widenVersionRange("[3.8.8,4.0.0)"));
        }

        @Test
        @DisplayName("should not widen [3.8.8,) — open upper bound already allows 4.x")
        void shouldNotWidenOpenUpperBound() {
            assertNull(EnforcerVersionRangeStrategy.widenVersionRange("[3.8.8,)"));
        }

        @Test
        @DisplayName("should not widen plain version 3.8.8")
        void shouldNotWidenPlainVersion() {
            assertNull(EnforcerVersionRangeStrategy.widenVersionRange("3.8.8"));
        }

        @Test
        @DisplayName("should not widen [3.8.8,5) — already allows Maven 4")
        void shouldNotWidenAlreadyWidened() {
            assertNull(EnforcerVersionRangeStrategy.widenVersionRange("[3.8.8,5)"));
        }

        @Test
        @DisplayName("should not widen [3.8.8,3.9) — upper bound is not at 4")
        void shouldNotWidenNon4UpperBound() {
            assertNull(EnforcerVersionRangeStrategy.widenVersionRange("[3.8.8,3.9)"));
        }
    }

    @Nested
    @DisplayName("Plugin Configuration")
    class PluginConfigurationTests {

        @Test
        @DisplayName("should widen requireMavenVersion in top-level plugin configuration")
        void shouldWidenInTopLevelConfiguration() {
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
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-enforcer-plugin</artifactId>
                                <configuration>
                                    <rules>
                                        <requireMavenVersion>
                                            <version>[3.8.8,4)</version>
                                        </requireMavenVersion>
                                    </rules>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertTrue(result.modifiedCount() > 0);

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("[3.8.8,5)"), "Version range should be widened to [3.8.8,5)");
            assertFalse(xml.contains("[3.8.8,4)"), "Original range should be replaced");
        }

        @Test
        @DisplayName("should widen requireMavenVersion in execution configuration")
        void shouldWidenInExecutionConfiguration() {
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
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-enforcer-plugin</artifactId>
                                <executions>
                                    <execution>
                                        <id>enforce-maven</id>
                                        <goals>
                                            <goal>enforce</goal>
                                        </goals>
                                        <configuration>
                                            <rules>
                                                <requireMavenVersion>
                                                    <version>[3,4)</version>
                                                </requireMavenVersion>
                                            </rules>
                                        </configuration>
                                    </execution>
                                </executions>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertTrue(result.modifiedCount() > 0);

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("[3,5)"), "Version range should be widened to [3,5)");
        }

        @Test
        @DisplayName("should widen requireMavenVersion in pluginManagement")
        void shouldWidenInPluginManagement() {
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
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-enforcer-plugin</artifactId>
                                    <configuration>
                                        <rules>
                                            <requireMavenVersion>
                                                <version>[3.6.3,4)</version>
                                            </requireMavenVersion>
                                        </rules>
                                    </configuration>
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

            assertTrue(result.success());
            assertTrue(result.modifiedCount() > 0);

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("[3.6.3,5)"), "Version range should be widened");
        }

        @Test
        @DisplayName("should not modify enforcer plugin with compatible version range")
        void shouldNotModifyCompatibleRange() {
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
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-enforcer-plugin</artifactId>
                                <configuration>
                                    <rules>
                                        <requireMavenVersion>
                                            <version>[3.8.8,)</version>
                                        </requireMavenVersion>
                                    </rules>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertEquals(0, result.modifiedCount(), "No changes expected for compatible range");
        }

        @Test
        @DisplayName("should not modify non-enforcer plugins")
        void shouldNotModifyNonEnforcerPlugins() {
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
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>3.11.0</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertEquals(0, result.modifiedCount(), "No changes expected for non-enforcer plugins");
        }

        @Test
        @DisplayName("should handle enforcer plugin without explicit groupId")
        void shouldHandleEnforcerWithoutGroupId() {
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
                                <artifactId>maven-enforcer-plugin</artifactId>
                                <configuration>
                                    <rules>
                                        <requireMavenVersion>
                                            <version>[3.8.8,4)</version>
                                        </requireMavenVersion>
                                    </rules>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertTrue(result.modifiedCount() > 0, "Should handle enforcer plugin without explicit groupId");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("[3.8.8,5)"));
        }
    }

    @Nested
    @DisplayName("Profile-Scoped")
    class ProfileScopedTests {

        @Test
        @DisplayName("should widen requireMavenVersion in profile-scoped enforcer plugin")
        void shouldWidenInProfileScopedPlugin() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <profiles>
                        <profile>
                            <id>enforce</id>
                            <build>
                                <plugins>
                                    <plugin>
                                        <groupId>org.apache.maven.plugins</groupId>
                                        <artifactId>maven-enforcer-plugin</artifactId>
                                        <configuration>
                                            <rules>
                                                <requireMavenVersion>
                                                    <version>[3.8.8,4)</version>
                                                </requireMavenVersion>
                                            </rules>
                                        </configuration>
                                    </plugin>
                                </plugins>
                            </build>
                        </profile>
                    </profiles>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertTrue(result.modifiedCount() > 0);

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("[3.8.8,5)"), "Profile-scoped range should be widened");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle POM with no build section")
        void shouldHandlePomWithNoBuild() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertEquals(0, result.modifiedCount());
        }

        @Test
        @DisplayName("should handle enforcer plugin with no rules")
        void shouldHandleEnforcerWithNoRules() {
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
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-enforcer-plugin</artifactId>
                                <configuration>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertEquals(0, result.modifiedCount());
        }

        @Test
        @DisplayName("should widen both top-level and execution-level configurations")
        void shouldWidenBothTopLevelAndExecutionLevel() {
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
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-enforcer-plugin</artifactId>
                                <configuration>
                                    <rules>
                                        <requireMavenVersion>
                                            <version>[3.6.3,4)</version>
                                        </requireMavenVersion>
                                    </rules>
                                </configuration>
                                <executions>
                                    <execution>
                                        <id>enforce</id>
                                        <configuration>
                                            <rules>
                                                <requireMavenVersion>
                                                    <version>[3.8.8,4)</version>
                                                </requireMavenVersion>
                                            </rules>
                                        </configuration>
                                    </execution>
                                </executions>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertTrue(result.modifiedCount() > 0);

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("[3.6.3,5)"), "Top-level configuration range should be widened");
            assertTrue(xml.contains("[3.8.8,5)"), "Execution-level configuration range should be widened");
            assertFalse(xml.contains(",4)"), "No ranges with upper bound at 4 should remain");
        }
    }
}
