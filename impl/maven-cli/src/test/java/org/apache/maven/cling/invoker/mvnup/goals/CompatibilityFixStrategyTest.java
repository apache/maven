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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link CompatibilityFixStrategy} class.
 * Tests Maven 4 compatibility fixes including duplicate dependency and plugin handling.
 */
@DisplayName("CompatibilityFixStrategy")
class CompatibilityFixStrategyTest {

    private CompatibilityFixStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new CompatibilityFixStrategy();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    private UpgradeContext createMockContext(Path workingDirectory) {
        return TestUtils.createMockContext(workingDirectory);
    }

    private UpgradeContext createMockContext(UpgradeOptions options) {
        return TestUtils.createMockContext(options);
    }

    private UpgradeOptions createDefaultOptions() {
        return TestUtils.createDefaultOptions();
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

            UpgradeContext context = createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable when --model is true");
        }

        @Test
        @DisplayName("should be applicable when --all option is specified")
        void shouldBeApplicableWhenAllOptionSpecified() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.all()).thenReturn(Optional.of(true));
            when(options.model()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable when --all is specified");
        }

        @Test
        @DisplayName("should be applicable by default when no specific options provided")
        void shouldBeApplicableByDefaultWhenNoSpecificOptions() {
            UpgradeOptions options = createDefaultOptions();

            UpgradeContext context = createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable by default");
        }

        @Test
        @DisplayName("should not be applicable when --model option is false")
        void shouldNotBeApplicableWhenModelOptionFalse() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.model()).thenReturn(Optional.of(false));
            when(options.all()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);

            assertFalse(strategy.isApplicable(context), "Strategy should not be applicable when --model is false");
        }

        @Test
        @DisplayName("should handle all options disabled")
        void shouldHandleAllOptionsDisabled() {
            UpgradeContext context = TestUtils.createMockContext(TestUtils.createOptions(
                    false, // --all
                    false, // --infer
                    false, // --fix-model
                    false, // --plugins
                    null // --model
                    ));

            // Should apply default behavior when all options are explicitly disabled
            assertTrue(
                    strategy.isApplicable(context),
                    "Strategy should apply default behavior when all options are disabled");
        }
    }

    @Nested
    @DisplayName("Duplicate Dependency Fixes")
    class DuplicateDependencyFixesTests {

        @Test
        @DisplayName("should remove duplicate dependencies in dependencyManagement")
        void shouldRemoveDuplicateDependenciesInDependencyManagement() throws Exception {
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
                                <groupId>org.apache.commons</groupId>
                                <artifactId>commons-lang3</artifactId>
                                <version>3.12.0</version>
                            </dependency>
                            <dependency>
                                <groupId>org.apache.commons</groupId>
                                <artifactId>commons-lang3</artifactId>
                                <version>3.13.0</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Compatibility fix should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have removed duplicate dependency");

            // Verify only one dependency remains
            Editor editor = new Editor(document);
            Element root = editor.root();
            Element dependencyManagement = DomUtils.findChildElement(root, "dependencyManagement");
            Element dependencies = DomUtils.findChildElement(dependencyManagement, "dependencies");
            var dependencyElements = dependencies.childElements("dependency").toList();
            assertEquals(1, dependencyElements.size(), "Should have only one dependency after duplicate removal");
        }

        @Test
        @DisplayName("should remove duplicate dependencies in regular dependencies")
        void shouldRemoveDuplicateDependenciesInRegularDependencies() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>junit</groupId>
                            <artifactId>junit</artifactId>
                            <version>4.13.2</version>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>junit</groupId>
                            <artifactId>junit</artifactId>
                            <version>4.13.2</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Compatibility fix should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have removed duplicate dependency");

            // Verify only one dependency remains
            Editor editor = new Editor(document);
            Element root = editor.root();
            Element dependencies = DomUtils.findChildElement(root, "dependencies");
            var dependencyElements = dependencies.childElements("dependency").toList();
            assertEquals(1, dependencyElements.size(), "Should have only one dependency after duplicate removal");
        }
    }

    @Nested
    @DisplayName("Duplicate Plugin Fixes")
    class DuplicatePluginFixesTests {

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
                                    <artifactId>maven-compiler-plugin</artifactId>
                                    <version>3.11.0</version>
                                </plugin>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-compiler-plugin</artifactId>
                                    <version>3.12.0</version>
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

            assertTrue(result.success(), "Compatibility fix should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have removed duplicate plugin");

            // Verify only one plugin remains
            Editor editor = new Editor(document);
            Element root = editor.root();
            Element build = DomUtils.findChildElement(root, "build");
            Element pluginManagement = DomUtils.findChildElement(build, "pluginManagement");
            Element plugins = DomUtils.findChildElement(pluginManagement, "plugins");
            var pluginElements = plugins.childElements("plugin").toList();
            assertEquals(1, pluginElements.size(), "Should have only one plugin after duplicate removal");
        }
    }

    @Nested
    @DisplayName("Repository Expression Fixes")
    class RepositoryExpressionFixesTests {

        @Test
        @DisplayName("should replace ${basedir} with ${project.basedir} in repository URLs")
        void shouldReplaceBasedirInRepositoryUrls() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <id>local-repo</id>
                            <url>file://${basedir}/internal-repository</url>
                        </repository>
                    </repositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Compatibility fix should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have fixed basedir expression");

            Element root = document.root();
            Element repositories = DomUtils.findChildElement(root, "repositories");
            Element repository = DomUtils.findChildElement(repositories, "repository");
            Element url = DomUtils.findChildElement(repository, "url");
            assertEquals(
                    "file://${project.basedir}/internal-repository",
                    url.textContent().trim(),
                    "Should have replaced ${basedir} with ${project.basedir}");
        }

        @Test
        @DisplayName("should replace ${pom.basedir} with ${project.basedir} in repository URLs")
        void shouldReplacePomBasedirInRepositoryUrls() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <id>local-repo</id>
                            <url>file://${pom.basedir}/lib</url>
                        </repository>
                    </repositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Compatibility fix should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have fixed pom.basedir expression");

            Element root = document.root();
            Element repositories = DomUtils.findChildElement(root, "repositories");
            Element repository = DomUtils.findChildElement(repositories, "repository");
            Element url = DomUtils.findChildElement(repository, "url");
            assertEquals(
                    "file://${project.basedir}/lib",
                    url.textContent().trim(),
                    "Should have replaced ${pom.basedir} with ${project.basedir}");
        }

        @Test
        @DisplayName("should replace ${basedir} in pluginRepository URLs")
        void shouldReplaceBasedirInPluginRepositoryUrls() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <pluginRepositories>
                        <pluginRepository>
                            <id>local-plugins</id>
                            <url>file://${basedir}/plugin-repo</url>
                        </pluginRepository>
                    </pluginRepositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Compatibility fix should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have fixed basedir in pluginRepository");

            Element root = document.root();
            Element pluginRepositories = DomUtils.findChildElement(root, "pluginRepositories");
            Element pluginRepository = DomUtils.findChildElement(pluginRepositories, "pluginRepository");
            Element url = DomUtils.findChildElement(pluginRepository, "url");
            assertEquals(
                    "file://${project.basedir}/plugin-repo",
                    url.textContent().trim(),
                    "Should have replaced ${basedir} with ${project.basedir}");
        }

        @Test
        @DisplayName("should replace ${basedir} in profile repository URLs")
        void shouldReplaceBasedirInProfileRepositoryUrls() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <profiles>
                        <profile>
                            <id>local</id>
                            <repositories>
                                <repository>
                                    <id>local-repo</id>
                                    <url>file://${basedir}/repo</url>
                                </repository>
                            </repositories>
                        </profile>
                    </profiles>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Compatibility fix should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have fixed basedir in profile repository");

            Element root = document.root();
            Element profiles = DomUtils.findChildElement(root, "profiles");
            Element profile = DomUtils.findChildElement(profiles, "profile");
            Element repositories = DomUtils.findChildElement(profile, "repositories");
            Element repository = DomUtils.findChildElement(repositories, "repository");
            Element url = DomUtils.findChildElement(repository, "url");
            assertEquals(
                    "file://${project.basedir}/repo",
                    url.textContent().trim(),
                    "Should have replaced ${basedir} with ${project.basedir}");
        }

        @Test
        @DisplayName("should not modify repository URLs without deprecated expressions")
        void shouldNotModifyUrlsWithoutDeprecatedExpressions() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <repositories>
                        <repository>
                            <id>central</id>
                            <url>https://repo.maven.apache.org/maven2</url>
                        </repository>
                        <repository>
                            <id>local-repo</id>
                            <url>file://${project.basedir}/repo</url>
                        </repository>
                    </repositories>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Compatibility fix should succeed");
            assertEquals(0, result.modifiedCount(), "Should not have modified any POMs");
        }
    }

    @Nested
    @DisplayName("Undefined Property Expression Fixes")
    class UndefinedPropertyExpressionFixesTests {

        @Test
        @DisplayName("should comment out dependency with undefined property expression")
        void shouldCommentOutDependencyWithUndefinedProperty() throws Exception {
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
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>${guava-version}</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            UpgradeResult result = strategy.doApply(context, pomMap);

            assertTrue(result.success(), "Compatibility fix should succeed");
            assertTrue(result.modifiedCount() > 0, "Should have commented out dependency");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("mvnup: commented out"), "Should contain comment-out marker");
            assertTrue(xml.contains("guava-version"), "Should mention the undefined property");

            Element root = document.root();
            Element depMgmt = DomUtils.findChildElement(root, "dependencyManagement");
            Element deps = DomUtils.findChildElement(depMgmt, "dependencies");
            assertEquals(0, deps.childElements("dependency").count(), "Should have no dependency elements");
        }

        @Test
        @DisplayName("should not comment out dependency with defined property")
        void shouldNotCommentOutDependencyWithDefinedProperty() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <guava-version>30.0-jre</guava-version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>${guava-version}</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            Element root = document.root();
            Element depMgmt = DomUtils.findChildElement(root, "dependencyManagement");
            Element deps = DomUtils.findChildElement(depMgmt, "dependencies");
            assertEquals(1, deps.childElements("dependency").count(), "Dependency should still be present");
        }

        @Test
        @DisplayName("should not comment out dependency with well-known built-in property")
        void shouldNotCommentOutDependencyWithBuiltinProperty() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>test</groupId>
                            <artifactId>test-dep</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("pom.xml"), document);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            Element root = document.root();
            Element deps = DomUtils.findChildElement(root, "dependencies");
            assertEquals(
                    1,
                    deps.childElements("dependency").count(),
                    "Dependency with built-in property should still be present");
        }

        @Test
        @DisplayName("should recognize property defined in another module POM")
        void shouldRecognizePropertyFromOtherPom() throws Exception {
            String parentPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>test</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <guava-version>30.0-jre</guava-version>
                    </properties>
                </project>
                """;

            String childPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>test</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>${guava-version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document parentDoc = Document.of(parentPom);
            Document childDoc = Document.of(childPom);
            Map<Path, Document> pomMap = Map.of(
                    Paths.get("pom.xml"), parentDoc,
                    Paths.get("child/pom.xml"), childDoc);

            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            Element root = childDoc.root();
            Element deps = DomUtils.findChildElement(root, "dependencies");
            assertEquals(
                    1,
                    deps.childElements("dependency").count(),
                    "Dependency should not be commented out when property is defined in another POM");
        }
    }

    @Nested
    @DisplayName("Undefined Property Expression Fixes with Effective Model")
    class UndefinedPropertyEffectiveModelTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("should recognize property inherited from external parent via relativePath")
        void shouldRecognizePropertyFromExternalParent() throws Exception {
            String parentPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>external-parent</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <properties>
                        <guava.version>32.1.3-jre</guava.version>
                    </properties>
                </project>
                """;

            String childPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>external-parent</artifactId>
                        <version>1.0.0</version>
                        <relativePath>../pom.xml</relativePath>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>${guava.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Path parentPomPath = tempDir.resolve("pom.xml");
            Path childDir = Files.createDirectories(tempDir.resolve("child"));
            Path childPomPath = childDir.resolve("pom.xml");

            Files.writeString(parentPomPath, parentPom);
            Files.writeString(childPomPath, childPom);

            Document parentDoc = Document.of(parentPom);
            Document childDoc = Document.of(childPom);
            Map<Path, Document> pomMap = Map.of(
                    parentPomPath, parentDoc,
                    childPomPath, childDoc);

            UpgradeContext context = createMockContext(tempDir);
            strategy.doApply(context, pomMap);

            Element root = childDoc.root();
            Element deps = DomUtils.findChildElement(root, "dependencies");
            assertEquals(
                    1,
                    deps.childElements("dependency").count(),
                    "Dependency should not be commented out when property is inherited from external parent");
        }

        @Test
        @DisplayName("should comment out dependency when property is not in parent either")
        void shouldCommentOutWhenPropertyNotInParent() throws Exception {
            String parentPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>external-parent</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                </project>
                """;

            String childPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>external-parent</artifactId>
                        <version>1.0.0</version>
                        <relativePath>../pom.xml</relativePath>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>${undefined.prop}</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Path parentPomPath = tempDir.resolve("pom.xml");
            Path childDir = Files.createDirectories(tempDir.resolve("child"));
            Path childPomPath = childDir.resolve("pom.xml");

            Files.writeString(parentPomPath, parentPom);
            Files.writeString(childPomPath, childPom);

            Document childDoc = Document.of(childPom);
            Map<Path, Document> pomMap = Map.of(childPomPath, childDoc);

            UpgradeContext context = createMockContext(tempDir);
            strategy.doApply(context, pomMap);

            String xml = DomUtils.toXml(childDoc);
            assertTrue(xml.contains("mvnup: commented out"), "Should contain comment-out marker");
        }

        @Test
        @DisplayName("should handle partial undefined - only comment out dependency with undefined property")
        void shouldHandlePartialUndefined() throws Exception {
            String pomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <commons.version>3.14.0</commons.version>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.commons</groupId>
                            <artifactId>commons-lang3</artifactId>
                            <version>${commons.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>${undefined.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Path pomPath = tempDir.resolve("pom.xml");
            Files.writeString(pomPath, pomXml);

            Document document = Document.of(pomXml);
            Map<Path, Document> pomMap = Map.of(pomPath, document);

            UpgradeContext context = createMockContext(tempDir);
            strategy.doApply(context, pomMap);

            Element root = document.root();
            Element deps = DomUtils.findChildElement(root, "dependencies");
            assertEquals(1, deps.childElements("dependency").count(), "Only defined-property dependency should remain");

            String xml = DomUtils.toXml(document);
            assertTrue(xml.contains("mvnup: commented out"), "Should contain comment-out marker");
            assertTrue(xml.contains("'undefined.version'"), "Should mention the undefined property in comment");
            assertFalse(xml.contains("'commons.version'"), "Should not flag the defined property as undefined");
        }

        @Test
        @DisplayName("should recognize property from grandparent POM")
        void shouldRecognizePropertyFromGrandparent() throws Exception {
            String grandparentPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>grandparent</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <properties>
                        <guava.version>32.1.3-jre</guava.version>
                    </properties>
                </project>
                """;

            String parentPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>grandparent</artifactId>
                        <version>1.0.0</version>
                        <relativePath>../pom.xml</relativePath>
                    </parent>
                    <artifactId>parent</artifactId>
                    <packaging>pom</packaging>
                </project>
                """;

            String childPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0.0</version>
                        <relativePath>../parent/pom.xml</relativePath>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>${guava.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Path grandparentPath = tempDir.resolve("pom.xml");
            Path parentDir = Files.createDirectories(tempDir.resolve("parent"));
            Path parentPath = parentDir.resolve("pom.xml");
            Path childDir = Files.createDirectories(tempDir.resolve("child"));
            Path childPomPath = childDir.resolve("pom.xml");

            Files.writeString(grandparentPath, grandparentPom);
            Files.writeString(parentPath, parentPom);
            Files.writeString(childPomPath, childPom);

            Document grandparentDoc = Document.of(grandparentPom);
            Document parentDoc = Document.of(parentPom);
            Document childDoc = Document.of(childPom);
            Map<Path, Document> pomMap = Map.of(
                    grandparentPath, grandparentDoc,
                    parentPath, parentDoc,
                    childPomPath, childDoc);

            UpgradeContext context = createMockContext(tempDir);
            strategy.doApply(context, pomMap);

            Element root = childDoc.root();
            Element deps = DomUtils.findChildElement(root, "dependencies");
            assertEquals(
                    1,
                    deps.childElements("dependency").count(),
                    "Dependency should not be commented out when property is inherited from grandparent");
        }
    }

    @Nested
    @DisplayName("Strategy Description")
    class StrategyDescriptionTests {

        @Test
        @DisplayName("should provide meaningful description")
        void shouldProvideMeaningfulDescription() {
            String description = strategy.getDescription();

            assertNotNull(description, "Description should not be null");
            assertFalse(description.trim().isEmpty(), "Description should not be empty");
            assertTrue(
                    description.toLowerCase().contains("compatibility")
                            || description.toLowerCase().contains("fix"),
                    "Description should mention compatibility or fix");
        }
    }
}
