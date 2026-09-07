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
import eu.maveniverse.domtrip.Element;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DeduplicateDependenciesStrategy}.
 */
@DisplayName("DeduplicateDependenciesStrategy")
class DeduplicateDependenciesStrategyTest {

    private DeduplicateDependenciesStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DeduplicateDependenciesStrategy();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    @Nested
    @DisplayName("Duplicate Dependency Removal")
    class DuplicateDependencyTests {

        @Test
        @DisplayName("should remove duplicate dependencies keeping last occurrence")
        void shouldRemoveDuplicateDependenciesKeepingLast() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>commons-io</groupId>
                            <artifactId>commons-io</artifactId>
                            <version>2.11.0</version>
                        </dependency>
                        <dependency>
                            <groupId>commons-io</groupId>
                            <artifactId>commons-io</artifactId>
                            <version>2.15.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount(), "Should have modified 1 POM");

            String xml = DomUtils.toXml(document);
            // Should keep last (2.15.0), remove first (2.11.0)
            assertTrue(xml.contains("<version>2.15.0</version>"), "Should keep last version");
            assertFalse(xml.contains("<version>2.11.0</version>"), "Should remove first version");

            Element deps = document.root().childElement("dependencies").orElse(null);
            long count = deps.childElements("dependency").count();
            assertEquals(1, count, "Should have only one dependency after dedup");
        }

        @Test
        @DisplayName("should remove duplicate in dependencyManagement keeping last")
        void shouldRemoveDuplicateInDependencyManagement() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.slf4j</groupId>
                                <artifactId>slf4j-api</artifactId>
                                <version>1.7.36</version>
                            </dependency>
                            <dependency>
                                <groupId>org.slf4j</groupId>
                                <artifactId>slf4j-api</artifactId>
                                <version>2.0.9</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount());

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<version>2.0.9</version>"), "Should keep last version");
            assertFalse(xml.contains("<version>1.7.36</version>"), "Should remove first version");
        }

        @Test
        @DisplayName("should keep last when three duplicates exist")
        void shouldKeepLastOfThreeDuplicates() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>commons-io</groupId>
                            <artifactId>commons-io</artifactId>
                            <version>1.0</version>
                        </dependency>
                        <dependency>
                            <groupId>commons-io</groupId>
                            <artifactId>commons-io</artifactId>
                            <version>2.0</version>
                        </dependency>
                        <dependency>
                            <groupId>commons-io</groupId>
                            <artifactId>commons-io</artifactId>
                            <version>3.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertEquals(1, result.modifiedCount());

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<version>3.0</version>"), "Should keep version 3.0 (last)");
            assertFalse(xml.contains("<version>1.0</version>"), "Should remove version 1.0");
            assertFalse(xml.contains("<version>2.0</version>"), "Should remove version 2.0");

            Element deps = document.root().childElement("dependencies").orElse(null);
            assertEquals(1, deps.childElements("dependency").count());
        }

        @Test
        @DisplayName("should handle duplicates with same type and classifier")
        void shouldHandleDuplicatesWithTypeAndClassifier() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.netty</groupId>
                            <artifactId>netty-transport</artifactId>
                            <version>4.1.0</version>
                            <type>jar</type>
                            <classifier>linux-x86_64</classifier>
                        </dependency>
                        <dependency>
                            <groupId>io.netty</groupId>
                            <artifactId>netty-transport</artifactId>
                            <version>4.2.0</version>
                            <type>jar</type>
                            <classifier>linux-x86_64</classifier>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertEquals(1, result.modifiedCount());

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<version>4.2.0</version>"));
            assertFalse(xml.contains("<version>4.1.0</version>"));
        }

        @Test
        @DisplayName("should not treat different classifiers as duplicates")
        void shouldNotDedupDifferentClassifiers() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.netty</groupId>
                            <artifactId>netty-transport</artifactId>
                            <version>4.1.0</version>
                            <classifier>linux-x86_64</classifier>
                        </dependency>
                        <dependency>
                            <groupId>io.netty</groupId>
                            <artifactId>netty-transport</artifactId>
                            <version>4.1.0</version>
                            <classifier>linux-aarch_64</classifier>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertEquals(0, result.modifiedCount(), "Different classifiers are not duplicates");
        }

        @Test
        @DisplayName("should handle profile-scoped duplicate dependencies")
        void shouldHandleProfileDuplicateDependencies() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <profiles>
                        <profile>
                            <id>test-profile</id>
                            <dependencies>
                                <dependency>
                                    <groupId>junit</groupId>
                                    <artifactId>junit</artifactId>
                                    <version>4.12</version>
                                </dependency>
                                <dependency>
                                    <groupId>junit</groupId>
                                    <artifactId>junit</artifactId>
                                    <version>4.13.2</version>
                                </dependency>
                            </dependencies>
                        </profile>
                    </profiles>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertEquals(1, result.modifiedCount());

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<version>4.13.2</version>"), "Should keep last version");
            assertFalse(xml.contains("<version>4.12</version>"), "Should remove first version");
        }

        @Test
        @DisplayName("should keep property references in version")
        void shouldKeepPropertyVersion() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>commons-io</groupId>
                            <artifactId>commons-io</artifactId>
                            <version>2.11.0</version>
                        </dependency>
                        <dependency>
                            <groupId>commons-io</groupId>
                            <artifactId>commons-io</artifactId>
                            <version>${commons.io.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertEquals(1, result.modifiedCount());

            String xml = DomUtils.toXml(document);
            assertTrue(
                    xml.contains("${commons.io.version}"),
                    "Should keep the last occurrence which has property version");
            assertFalse(xml.contains("<version>2.11.0</version>"));
        }

        @Test
        @DisplayName("should not modify when no duplicates exist")
        void shouldNotModifyWhenNoDuplicates() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>commons-io</groupId>
                            <artifactId>commons-io</artifactId>
                            <version>2.15.0</version>
                        </dependency>
                        <dependency>
                            <groupId>commons-lang</groupId>
                            <artifactId>commons-lang3</artifactId>
                            <version>3.14.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success());
            assertEquals(0, result.modifiedCount(), "Should not modify when no duplicates");
        }
    }

    @Nested
    @DisplayName("Duplicate Plugin Removal")
    class DuplicatePluginTests {

        @Test
        @DisplayName("should remove duplicate plugins keeping last occurrence")
        void shouldRemoveDuplicatePlugins() throws Exception {
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
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>3.13.0</version>
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
            assertEquals(1, result.modifiedCount());

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<version>3.13.0</version>"), "Should keep last version");
            assertFalse(xml.contains("<version>3.11.0</version>"), "Should remove first version");
        }

        @Test
        @DisplayName("should remove duplicate plugins in pluginManagement")
        void shouldRemoveDuplicatePluginsInPluginManagement() throws Exception {
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
                                    <artifactId>maven-surefire-plugin</artifactId>
                                    <version>3.1.0</version>
                                </plugin>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-surefire-plugin</artifactId>
                                    <version>3.2.5</version>
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
            assertEquals(1, result.modifiedCount());

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<version>3.2.5</version>"), "Should keep last version");
            assertFalse(xml.contains("<version>3.1.0</version>"), "Should remove first version");
        }

        @Test
        @DisplayName("should default groupId for maven plugins without explicit groupId")
        void shouldDefaultGroupIdForMavenPlugins() throws Exception {
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
                                <artifactId>maven-jar-plugin</artifactId>
                                <version>3.3.0</version>
                            </plugin>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-jar-plugin</artifactId>
                                <version>3.4.0</version>
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
            assertEquals(1, result.modifiedCount(), "Should detect duplicate even without explicit groupId");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<version>3.4.0</version>"), "Should keep last version");
        }
    }

    @Nested
    @DisplayName("Applicability")
    class ApplicabilityTests {

        @Test
        @DisplayName("should be applicable with default options")
        void shouldBeApplicableWithDefaults() {
            UpgradeContext context = createMockContext();
            assertTrue(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should be applicable when --all is set")
        void shouldBeApplicableWithAll() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithAll(true));
            assertTrue(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should be applicable when --model is true")
        void shouldBeApplicableWithModelTrue() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithFixModel(true));
            assertTrue(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should not be applicable when --model is false")
        void shouldNotBeApplicableWithModelFalse() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithFixModel(false));
            assertFalse(strategy.isApplicable(context));
        }
    }
}
