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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link DuplicateElementStrategy}.
 */
@DisplayName("DuplicateElementStrategy")
class DuplicateElementStrategyTest {

    private DuplicateElementStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DuplicateElementStrategy();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
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

        @Test
        @DisplayName("should be applicable when --all is set")
        void shouldBeApplicableWithAll() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptionsWithAll(true));
            assertTrue(strategy.isApplicable(context));
        }
    }

    @Nested
    @DisplayName("Duplicate Element Removal")
    class DuplicateElementRemovalTests {

        @Test
        @DisplayName("should remove duplicate artifactId keeping last occurrence")
        void shouldRemoveDuplicateArtifactId() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>first-name</artifactId>
                    <artifactId>second-name</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount(), "Should have modified 1 POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<artifactId>second-name</artifactId>"), "Should keep last artifactId");
            assertFalse(xml.contains("<artifactId>first-name</artifactId>"), "Should remove first artifactId");
        }

        @Test
        @DisplayName("should remove duplicate properties blocks keeping last")
        void shouldRemoveDuplicateProperties() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <old.property>old-value</old.property>
                    </properties>
                    <properties>
                        <new.property>new-value</new.property>
                    </properties>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount(), "Should have modified 1 POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<new.property>new-value</new.property>"), "Should keep last properties block");
            assertFalse(xml.contains("<old.property>old-value</old.property>"), "Should remove first properties block");
        }

        @Test
        @DisplayName("should remove duplicate version element keeping last")
        void shouldRemoveDuplicateVersion() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <version>2.0.0</version>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount(), "Should have modified 1 POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<version>2.0.0</version>"), "Should keep last version");
            assertFalse(xml.contains("<version>1.0.0</version>"), "Should remove first version");
        }

        @Test
        @DisplayName("should remove duplicate groupId element keeping last")
        void shouldRemoveDuplicateGroupId() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.old</groupId>
                    <groupId>com.new</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount(), "Should have modified 1 POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<groupId>com.new</groupId>"), "Should keep last groupId");
            assertFalse(xml.contains("<groupId>com.old</groupId>"), "Should remove first groupId");
        }

        @Test
        @DisplayName("should remove multiple different duplicate elements in same POM")
        void shouldRemoveMultipleDifferentDuplicates() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.old</groupId>
                    <groupId>com.new</groupId>
                    <artifactId>old-name</artifactId>
                    <artifactId>new-name</artifactId>
                    <version>1.0.0</version>
                    <version>2.0.0</version>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount(), "Should have modified 1 POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<groupId>com.new</groupId>"), "Should keep last groupId");
            assertFalse(xml.contains("<groupId>com.old</groupId>"), "Should remove first groupId");
            assertTrue(xml.contains("<artifactId>new-name</artifactId>"), "Should keep last artifactId");
            assertFalse(xml.contains("<artifactId>old-name</artifactId>"), "Should remove first artifactId");
            assertTrue(xml.contains("<version>2.0.0</version>"), "Should keep last version");
            assertFalse(xml.contains("<version>1.0.0</version>"), "Should remove first version");
        }

        @Test
        @DisplayName("should not modify POM with no duplicates")
        void shouldNotModifyWhenNoDuplicates() {
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

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(0, result.modifiedCount(), "Should not have modified any POM");
        }
    }

    @Nested
    @DisplayName("Nested Duplicate Elements")
    class NestedDuplicateElementTests {

        @Test
        @DisplayName("should remove duplicate groupId inside a dependency element")
        void shouldRemoveDuplicateGroupIdInDependency() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>old-group</groupId>
                            <groupId>new-group</groupId>
                            <artifactId>some-lib</artifactId>
                            <version>1.0</version>
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
            assertTrue(xml.contains("<groupId>new-group</groupId>"), "Should keep last groupId in dependency");
            assertFalse(xml.contains("<groupId>old-group</groupId>"), "Should remove first groupId in dependency");
        }

        @Test
        @DisplayName("should remove duplicate version inside parent element")
        void shouldRemoveDuplicateVersionInParent() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0.0</version>
                        <version>2.0.0</version>
                    </parent>
                    <artifactId>child</artifactId>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(1, result.modifiedCount(), "Should have modified 1 POM");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("<version>2.0.0</version>"), "Should keep last version in parent");
            assertFalse(xml.contains("<version>1.0.0</version>"), "Should remove first version in parent");
        }
    }

    @Nested
    @DisplayName("List Container Skipping")
    class ListContainerSkippingTests {

        @Test
        @DisplayName("should not remove multiple dependency elements in dependencies")
        void shouldNotRemoveDependenciesInList() {
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

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(0, result.modifiedCount(), "Should not have modified any POM");

            Element deps = document.root().childElement("dependencies").orElse(null);
            assertNotNull(deps, "dependencies element should still exist");
            assertEquals(2, deps.childElements("dependency").count(), "Should still have 2 dependency elements");
        }

        @Test
        @DisplayName("should not remove multiple plugin elements in plugins")
        void shouldNotRemovePluginsInList() {
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
                                <version>3.13.0</version>
                            </plugin>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.2.5</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(0, result.modifiedCount(), "Should not have modified any POM");

            Element plugins = document.root()
                    .childElement("build")
                    .orElseThrow()
                    .childElement("plugins")
                    .orElse(null);
            assertNotNull(plugins, "plugins element should still exist");
            assertEquals(2, plugins.childElements("plugin").count(), "Should still have 2 plugin elements");
        }

        @Test
        @DisplayName("should not remove multiple module elements in modules")
        void shouldNotRemoveModulesInList() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test-parent</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>module-a</module>
                        <module>module-b</module>
                        <module>module-c</module>
                    </modules>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(0, result.modifiedCount(), "Should not have modified any POM");

            Element modules = document.root().childElement("modules").orElse(null);
            assertNotNull(modules, "modules element should still exist");
            assertEquals(3, modules.childElements("module").count(), "Should still have 3 module elements");
        }
    }

    @Nested
    @DisplayName("Strategy Description")
    class StrategyDescriptionTests {

        @Test
        @DisplayName("should return non-null description")
        void shouldReturnNonNullDescription() {
            assertNotNull(strategy.getDescription(), "Description should not be null");
        }

        @Test
        @DisplayName("should return non-empty description")
        void shouldReturnNonEmptyDescription() {
            assertFalse(strategy.getDescription().isEmpty(), "Description should not be empty");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should return success with 0 modifications for POM with no duplicates")
        void shouldReturnSuccessWithZeroModifications() {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <description>A simple project</description>
                    <properties>
                        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                    </properties>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Strategy should succeed");
            assertEquals(0, result.modifiedCount(), "Should report 0 modifications");
            assertEquals(0, result.errorCount(), "Should report 0 errors");
        }
    }
}
