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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DependencyIdStrategy")
class DependencyIdStrategyTest {

    private DependencyIdStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DependencyIdStrategy();
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
        @DisplayName("should be applicable with --model-version 4.2.0")
        void shouldBeApplicableWithModelVersion420() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            assertTrue(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should not be applicable with --model-version 4.1.0")
        void shouldNotBeApplicableWithModelVersion410() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.1.0"));
            assertFalse(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should be applicable with --all")
        void shouldBeApplicableWithAll() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithAll(true));
            assertTrue(strategy.isApplicable(context));
        }

        @Test
        @DisplayName("should not be applicable with --all=false")
        void shouldNotBeApplicableWithAllFalse() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithAll(false));
            assertFalse(strategy.isApplicable(context));
        }
    }

    @Nested
    @DisplayName("Dependency Collapsing")
    class DependencyCollapsingTests {

        @Test
        @DisplayName("should collapse g:a:v dependency")
        void shouldCollapseGav() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <version>1.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), doc);

            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(pomMap));

            Element dependency = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:1.0", dependency.attribute("id"));
            assertNull(DomUtils.findChildElement(dependency, "groupId"));
            assertNull(DomUtils.findChildElement(dependency, "artifactId"));
            assertNull(DomUtils.findChildElement(dependency, "version"));
        }

        @Test
        @DisplayName("should collapse g:a:type:v dependency with non-default type")
        void shouldCollapseGavWithType() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <type>pom</type>
                            <version>1.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:pom:1.0", dependency.attribute("id"));
            assertNull(DomUtils.findChildElement(dependency, "type"));
        }

        @Test
        @DisplayName("should collapse g:a:type:classifier:v dependency")
        void shouldCollapseGavWithClassifier() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <type>jar</type>
                            <classifier>sources</classifier>
                            <version>1.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:jar:sources:1.0", dependency.attribute("id"));
            assertNull(DomUtils.findChildElement(dependency, "type"));
            assertNull(DomUtils.findChildElement(dependency, "classifier"));
        }

        @Test
        @DisplayName("should use default type jar in 5-part format when type absent but classifier present")
        void shouldUseDefaultTypeWhenClassifierPresent() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <classifier>sources</classifier>
                            <version>1.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:jar:sources:1.0", dependency.attribute("id"));
        }

        @Test
        @DisplayName("should not include default type jar in 3-part format")
        void shouldNotIncludeDefaultTypeInId() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <type>jar</type>
                            <version>1.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:1.0", dependency.attribute("id"));
            assertNull(DomUtils.findChildElement(dependency, "type"));
        }

        @Test
        @DisplayName("should preserve scope when collapsing")
        void shouldPreserveScopeWhenCollapsing() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <version>1.0</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:1.0", dependency.attribute("id"));
            assertNotNull(DomUtils.findChildElement(dependency, "scope"));
            assertEquals("test", dependency.childText("scope"));
        }

        @Test
        @DisplayName("should preserve optional when collapsing")
        void shouldPreserveOptionalWhenCollapsing() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <version>1.0</version>
                            <optional>true</optional>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:1.0", dependency.attribute("id"));
            assertNotNull(DomUtils.findChildElement(dependency, "optional"));
        }

        @Test
        @DisplayName("should collapse dependency without version to g:a")
        void shouldCollapseDependencyWithoutVersion() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib", dependency.attribute("id"));
            assertNull(DomUtils.findChildElement(dependency, "groupId"));
            assertNull(DomUtils.findChildElement(dependency, "artifactId"));
        }

        @Test
        @DisplayName("should collapse managed-version dependency with non-default type to g:a:type:")
        void shouldCollapseManagedWithType() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <type>pom</type>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:pom:", dependency.attribute("id"));
            assertNull(DomUtils.findChildElement(dependency, "groupId"));
            assertNull(DomUtils.findChildElement(dependency, "artifactId"));
            assertNull(DomUtils.findChildElement(dependency, "type"));
        }

        @Test
        @DisplayName("should collapse managed-version dependency with classifier to g:a:type:classifier:")
        void shouldCollapseManagedWithClassifier() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <type>jar</type>
                            <classifier>sources</classifier>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:jar:sources:", dependency.attribute("id"));
            assertNull(DomUtils.findChildElement(dependency, "groupId"));
            assertNull(DomUtils.findChildElement(dependency, "artifactId"));
            assertNull(DomUtils.findChildElement(dependency, "type"));
            assertNull(DomUtils.findChildElement(dependency, "classifier"));
        }

        @Test
        @DisplayName("should skip dependency that already has id attribute")
        void shouldSkipDependencyWithExistingId() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency id="org.example:lib:1.0"/>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            assertEquals(
                    "org.example:lib:1.0",
                    doc.root()
                            .childElement("dependencies")
                            .orElseThrow()
                            .childElement("dependency")
                            .orElseThrow()
                            .attribute("id"));
        }

        @Test
        @DisplayName("should collapse multiple dependencies")
        void shouldCollapseMultipleDependencies() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib-a</artifactId>
                            <version>1.0</version>
                        </dependency>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib-b</artifactId>
                            <version>2.0</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            var deps = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElements("dependency")
                    .toList();

            assertEquals(2, deps.size());
            assertEquals("org.example:lib-a:1.0", deps.get(0).attribute("id"));
            assertEquals("org.example:lib-b:2.0", deps.get(1).attribute("id"));
        }
    }

    @Nested
    @DisplayName("Exclusion Collapsing")
    class ExclusionCollapsingTests {

        @Test
        @DisplayName("should collapse exclusion into id attribute")
        void shouldCollapseExclusion() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <version>1.0</version>
                            <exclusions>
                                <exclusion>
                                    <groupId>org.unwanted</groupId>
                                    <artifactId>bad</artifactId>
                                </exclusion>
                            </exclusions>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:1.0", dependency.attribute("id"));

            Element exclusion = dependency
                    .childElement("exclusions")
                    .orElseThrow()
                    .childElement("exclusion")
                    .orElseThrow();

            assertEquals("org.unwanted:bad", exclusion.attribute("id"));
            assertNull(DomUtils.findChildElement(exclusion, "groupId"));
            assertNull(DomUtils.findChildElement(exclusion, "artifactId"));
        }

        @Test
        @DisplayName("should collapse multiple exclusions")
        void shouldCollapseMultipleExclusions() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <version>1.0</version>
                            <exclusions>
                                <exclusion>
                                    <groupId>org.a</groupId>
                                    <artifactId>bad-a</artifactId>
                                </exclusion>
                                <exclusion>
                                    <groupId>org.b</groupId>
                                    <artifactId>bad-b</artifactId>
                                </exclusion>
                            </exclusions>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            var exclusions = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow()
                    .childElement("exclusions")
                    .orElseThrow()
                    .childElements("exclusion")
                    .toList();

            assertEquals(2, exclusions.size());
            assertEquals("org.a:bad-a", exclusions.get(0).attribute("id"));
            assertEquals("org.b:bad-b", exclusions.get(1).attribute("id"));
        }
    }

    @Nested
    @DisplayName("Section Processing")
    class SectionProcessingTests {

        @Test
        @DisplayName("should process dependencyManagement dependencies")
        void shouldProcessDependencyManagement() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.example</groupId>
                                <artifactId>lib</artifactId>
                                <version>1.0</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("dependencyManagement")
                    .orElseThrow()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:1.0", dependency.attribute("id"));
        }

        @Test
        @DisplayName("should process profile dependencies")
        void shouldProcessProfileDependencies() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <profiles>
                        <profile>
                            <id>test-profile</id>
                            <dependencies>
                                <dependency>
                                    <groupId>org.example</groupId>
                                    <artifactId>lib</artifactId>
                                    <version>1.0</version>
                                </dependency>
                            </dependencies>
                        </profile>
                    </profiles>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("profiles")
                    .orElseThrow()
                    .childElement("profile")
                    .orElseThrow()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:1.0", dependency.attribute("id"));
        }

        @Test
        @DisplayName("should process profile dependencyManagement")
        void shouldProcessProfileDependencyManagement() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <profiles>
                        <profile>
                            <id>test-profile</id>
                            <dependencyManagement>
                                <dependencies>
                                    <dependency>
                                        <groupId>org.example</groupId>
                                        <artifactId>lib</artifactId>
                                        <version>1.0</version>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
                        </profile>
                    </profiles>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("profiles")
                    .orElseThrow()
                    .childElement("profile")
                    .orElseThrow()
                    .childElement("dependencyManagement")
                    .orElseThrow()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:1.0", dependency.attribute("id"));
        }

        @Test
        @DisplayName("should process plugin dependencies")
        void shouldProcessPluginDependencies() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>3.11.0</version>
                                <dependencies>
                                    <dependency>
                                        <groupId>org.example</groupId>
                                        <artifactId>lib</artifactId>
                                        <version>1.0</version>
                                    </dependency>
                                </dependencies>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("build")
                    .orElseThrow()
                    .childElement("plugins")
                    .orElseThrow()
                    .childElement("plugin")
                    .orElseThrow()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertEquals("org.example:lib:1.0", dependency.attribute("id"));
        }
    }

    @Nested
    @DisplayName("Model Version Filtering")
    class ModelVersionFilteringTests {

        @Test
        @DisplayName("should skip non-4.2.0 POMs")
        void shouldSkipNon420Poms() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <version>1.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc = Document.of(pomXml);
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            UpgradeResult result = strategy.doApply(context, new HashMap<>(Map.of(Paths.get("pom.xml"), doc)));

            Element dependency = doc.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow();

            assertNull(dependency.attribute("id"));
            assertNotNull(DomUtils.findChildElement(dependency, "groupId"));
            assertEquals(0, result.modifiedCount());
        }

        @Test
        @DisplayName("should process only 4.2.0 POMs in mixed map")
        void shouldProcessOnly420PomsInMixedMap() {
            String pom410Xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>old-module</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <version>1.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            String pom420Xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.2.0">
                    <modelVersion>4.2.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>new-module</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>lib</artifactId>
                            <version>1.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document doc410 = Document.of(pom410Xml);
            Document doc420 = Document.of(pom420Xml);
            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("old", "pom.xml"), doc410);
            pomMap.put(Paths.get("new", "pom.xml"), doc420);

            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithModelVersion("4.2.0"));
            UpgradeResult result = strategy.doApply(context, pomMap);

            // 4.1.0 POM should be untouched
            assertNull(doc410.root()
                    .childElement("dependencies")
                    .orElseThrow()
                    .childElement("dependency")
                    .orElseThrow()
                    .attribute("id"));

            // 4.2.0 POM should be collapsed
            assertEquals(
                    "org.example:lib:1.0",
                    doc420.root()
                            .childElement("dependencies")
                            .orElseThrow()
                            .childElement("dependency")
                            .orElseThrow()
                            .attribute("id"));

            assertEquals(1, result.modifiedCount());
        }
    }

    @Nested
    @DisplayName("buildIdValue")
    class BuildIdValueTests {

        @Test
        void twoPartFormat() {
            assertEquals("g:a", DependencyIdStrategy.buildIdValue("g", "a", null, null, null));
        }

        @Test
        void threePartFormat() {
            assertEquals("g:a:1.0", DependencyIdStrategy.buildIdValue("g", "a", "1.0", null, null));
        }

        @Test
        void threePartFormatWithDefaultType() {
            assertEquals("g:a:1.0", DependencyIdStrategy.buildIdValue("g", "a", "1.0", "jar", null));
        }

        @Test
        void fourPartFormat() {
            assertEquals("g:a:pom:1.0", DependencyIdStrategy.buildIdValue("g", "a", "1.0", "pom", null));
        }

        @Test
        void fivePartFormat() {
            assertEquals("g:a:jar:sources:1.0", DependencyIdStrategy.buildIdValue("g", "a", "1.0", "jar", "sources"));
        }

        @Test
        void fivePartFormatWithNonDefaultType() {
            assertEquals(
                    "g:a:test-jar:tests:1.0", DependencyIdStrategy.buildIdValue("g", "a", "1.0", "test-jar", "tests"));
        }

        @Test
        void fivePartFormatClassifierNoType() {
            assertEquals("g:a:jar:sources:1.0", DependencyIdStrategy.buildIdValue("g", "a", "1.0", null, "sources"));
        }

        @Test
        void fourPartTrailingColon() {
            assertEquals("g:a:pom:", DependencyIdStrategy.buildIdValue("g", "a", null, "pom", null));
        }

        @Test
        void fivePartTrailingColon() {
            assertEquals("g:a:jar:sources:", DependencyIdStrategy.buildIdValue("g", "a", null, "jar", "sources"));
        }

        @Test
        void fivePartTrailingColonClassifierNoType() {
            assertEquals("g:a:jar:sources:", DependencyIdStrategy.buildIdValue("g", "a", null, null, "sources"));
        }
    }
}
