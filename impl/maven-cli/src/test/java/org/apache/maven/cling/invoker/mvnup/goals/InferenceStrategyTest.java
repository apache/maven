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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link InferenceStrategy} class.
 * Tests Maven 4.1.0+ inference optimizations including dependency and parent inference.
 */
@DisplayName("InferenceStrategy")
class InferenceStrategyTest {

    private InferenceStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new InferenceStrategy();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
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
        @DisplayName("should be applicable when --infer option is true")
        void shouldBeApplicableWhenInferOptionTrue() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.infer()).thenReturn(Optional.of(true));
            when(options.all()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable when --infer is true");
        }

        @Test
        @DisplayName("should be applicable when --all option is specified")
        void shouldBeApplicableWhenAllOptionSpecified() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.all()).thenReturn(Optional.of(true));
            when(options.infer()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);

            assertTrue(strategy.isApplicable(context), "Strategy should be applicable when --all is specified");
        }

        @Test
        @DisplayName("should be applicable by default when no specific options provided")
        void shouldBeApplicableByDefaultWhenNoSpecificOptions() {
            UpgradeOptions options = createDefaultOptions();

            UpgradeContext context = createMockContext(options);

            assertTrue(
                    strategy.isApplicable(context),
                    "Strategy should be applicable by default when no specific options are provided");
        }

        @Test
        @DisplayName("should not be applicable when --infer option is false")
        void shouldNotBeApplicableWhenInferOptionFalse() {
            UpgradeOptions options = mock(UpgradeOptions.class);
            when(options.infer()).thenReturn(Optional.of(false));
            when(options.all()).thenReturn(Optional.empty());

            UpgradeContext context = createMockContext(options);

            assertFalse(strategy.isApplicable(context), "Strategy should not be applicable when --infer is false");
        }
    }

    @Nested
    @DisplayName("Dependency Inference")
    class DependencyInferenceTests {

        @Test
        @DisplayName("should remove dependency version for project artifact")
        void shouldRemoveDependencyVersionForProjectArtifact() throws Exception {
            String parentPomXml = PomBuilder.create()
                    .namespace("http://maven.apache.org/POM/4.1.0")
                    .modelVersion("4.1.0")
                    .groupId("com.example")
                    .artifactId("parent-project")
                    .version("1.0.0")
                    .packaging("pom")
                    .build();

            String moduleAPomXml = PomBuilder.create()
                    .namespace("http://maven.apache.org/POM/4.1.0")
                    .modelVersion("4.1.0")
                    .parent("com.example", "parent-project", "1.0.0")
                    .artifactId("module-a")
                    .build();

            String moduleBPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>module-b</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>com.example</groupId>
                            <artifactId>module-a</artifactId>
                            <version>1.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document parentDoc = Document.of(parentPomXml);
            Document moduleADoc = Document.of(moduleAPomXml);
            Document moduleBDoc = Document.of(moduleBPomXml);

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "pom.xml"), parentDoc);
            pomMap.put(Paths.get("project", "module-a", "pom.xml"), moduleADoc);
            pomMap.put(Paths.get("project", "module-b", "pom.xml"), moduleBDoc);

            Editor editor = new Editor(moduleBDoc);
            Element moduleBRoot = editor.root();
            Element dependencies = DomUtils.findChildElement(moduleBRoot, "dependencies");
            Element dependency = DomUtils.findChildElement(dependencies, "dependency");

            // Verify dependency elements exist before inference
            assertNotNull(DomUtils.findChildElement(dependency, "groupId"));
            assertNotNull(DomUtils.findChildElement(dependency, "artifactId"));
            assertNotNull(DomUtils.findChildElement(dependency, "version"));

            // Apply dependency inference
            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            // Verify version was removed (can be inferred from project)
            assertNull(DomUtils.findChildElement(dependency, "version"));
            // groupId should also be removed (can be inferred from project)
            assertNull(DomUtils.findChildElement(dependency, "groupId"));
            // artifactId should remain (always required)
            assertNotNull(DomUtils.findChildElement(dependency, "artifactId"));
        }

        @Test
        @DisplayName("should keep dependency version for external artifact")
        void shouldKeepDependencyVersionForExternalArtifact() throws Exception {
            String modulePomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <groupId>com.example</groupId>
                    <artifactId>my-module</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.commons</groupId>
                            <artifactId>commons-lang3</artifactId>
                            <version>3.12.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

            Document moduleDoc = Document.of(modulePomXml);
            Map<Path, Document> pomMap = Map.of(Paths.get("project", "pom.xml"), moduleDoc);

            Editor editor = new Editor(moduleDoc);
            Element moduleRoot = editor.root();
            Element dependencies = DomUtils.findChildElement(moduleRoot, "dependencies");
            Element dependency = DomUtils.findChildElement(dependencies, "dependency");

            // Apply dependency inference
            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            // Verify all dependency elements remain (external dependency)
            assertNotNull(DomUtils.findChildElement(dependency, "groupId"));
            assertNotNull(DomUtils.findChildElement(dependency, "artifactId"));
            assertNotNull(DomUtils.findChildElement(dependency, "version"));
        }

        @Test
        @DisplayName("should keep dependency version when version mismatch")
        void shouldKeepDependencyVersionWhenVersionMismatch() throws Exception {
            String moduleAPomXml = PomBuilder.create()
                    .namespace("http://maven.apache.org/POM/4.1.0")
                    .modelVersion("4.1.0")
                    .groupId("com.example")
                    .artifactId("module-a")
                    .version("1.0.0")
                    .build();

            String moduleBPomXml = PomBuilder.create()
                    .namespace("http://maven.apache.org/POM/4.1.0")
                    .modelVersion("4.1.0")
                    .groupId("com.example")
                    .artifactId("module-b")
                    .version("1.0.0")
                    .dependency("com.example", "module-a", "0.9.0")
                    .build();

            Document moduleADoc = Document.of(moduleAPomXml);
            Document moduleBDoc = Document.of(moduleBPomXml);

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "module-a", "pom.xml"), moduleADoc);
            pomMap.put(Paths.get("project", "module-b", "pom.xml"), moduleBDoc);

            Editor editor = new Editor(moduleBDoc);
            Element moduleBRoot = editor.root();
            Element dependencies = DomUtils.findChildElement(moduleBRoot, "dependencies");
            Element dependency = DomUtils.findChildElement(dependencies, "dependency");

            // Apply dependency inference
            UpgradeContext context = createMockContext();
            strategy.doApply(context, pomMap);

            // Verify correct behavior when version doesn't match:
            // - groupId should be removed (can be inferred from project regardless of version)
            // - version should remain (doesn't match project version, so can't be inferred)
            // - artifactId should remain (always required)
            assertNull(DomUtils.findChildElement(dependency, "groupId"));
            assertNotNull(DomUtils.findChildElement(dependency, "artifactId"));
            assertNotNull(DomUtils.findChildElement(dependency, "version"));
        }

        @Test
        @DisplayName("should handle plugin dependencies")
        void shouldHandlePluginDependencies() throws Exception {
            String moduleAPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <groupId>com.example</groupId>
                    <artifactId>module-a</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            String moduleBPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <groupId>com.example</groupId>
                    <artifactId>module-b</artifactId>
                    <version>1.0.0</version>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <dependencies>
                                    <dependency>
                                        <groupId>com.example</groupId>
                                        <artifactId>module-a</artifactId>
                                        <version>1.0.0</version>
                                    </dependency>
                                </dependencies>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """;

            Document moduleADoc = Document.of(moduleAPomXml);
            Document moduleBDoc = Document.of(moduleBPomXml);

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "module-a", "pom.xml"), moduleADoc);
            pomMap.put(Paths.get("project", "module-b", "pom.xml"), moduleBDoc);

            Element moduleBRoot = moduleBDoc.root();
            Element build = moduleBRoot.child("build").orElse(null);
            Element plugins = build.child("plugins").orElse(null);
            Element plugin = plugins.child("plugin").orElse(null);
            Element dependencies = plugin.child("dependencies").orElse(null);
            Element dependency = dependencies.child("dependency").orElse(null);

            // Apply dependency inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify version and groupId were removed from plugin dependency
            assertNull(dependency.child("version").orElse(null));
            assertNull(dependency.child("groupId").orElse(null));
            assertNotNull(dependency.child("artifactId").orElse(null));
        }
    }

    @Nested
    @DisplayName("Parent Inference")
    class ParentInferenceTests {

        @Test
        @DisplayName("should remove parent groupId when child doesn't have explicit groupId")
        void shouldRemoveParentGroupIdWhenChildDoesntHaveExplicitGroupId() throws Exception {
            String parentPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            String childPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                        <relativePath>../pom.xml</relativePath>
                    </parent>
                    <artifactId>child-project</artifactId>
                    <!-- No explicit groupId - will inherit from parent -->
                    <!-- No explicit version - will inherit from parent -->
                </project>
                """;

            Document parentDoc = Document.of(parentPomXml);
            Document childDoc = Document.of(childPomXml);

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "pom.xml"), parentDoc);
            pomMap.put(Paths.get("project", "child", "pom.xml"), childDoc);

            Editor editor = new Editor(childDoc);
            Element childRoot = editor.root();
            Element parentElement = DomUtils.findChildElement(childRoot, "parent");

            // Verify parent elements exist before inference
            assertNotNull(DomUtils.findChildElement(parentElement, "groupId"));
            assertNotNull(DomUtils.findChildElement(parentElement, "artifactId"));
            assertNotNull(DomUtils.findChildElement(parentElement, "version"));

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify parent groupId and version were removed (since child doesn't have explicit ones)
            assertNull(parentElement.child("groupId").orElse(null));
            assertNull(parentElement.child("version").orElse(null));
            // artifactId should also be removed since parent POM is in pomMap
            assertNull(parentElement.child("artifactId").orElse(null));
        }

        @Test
        @DisplayName("should keep parent groupId when child has explicit groupId")
        void shouldKeepParentGroupIdWhenChildHasExplicitGroupId() throws Exception {
            String parentPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            String childPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                        <relativePath>../pom.xml</relativePath>
                    </parent>
                    <groupId>com.example.child</groupId>
                    <artifactId>child-project</artifactId>
                    <version>2.0.0</version>
                </project>
                """;

            Document parentDoc = Document.of(parentPomXml);
            Document childDoc = Document.of(childPomXml);

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "pom.xml"), parentDoc);
            pomMap.put(Paths.get("project", "child", "pom.xml"), childDoc);

            Editor editor = new Editor(childDoc);
            Element childRoot = editor.root();
            Element parentElement = DomUtils.findChildElement(childRoot, "parent");

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify parent elements are kept (since child has explicit values)
            assertNotNull(parentElement.child("groupId").orElse(null));
            assertNotNull(parentElement.child("version").orElse(null));
            // artifactId should still be removed since parent POM is in pomMap
            assertNull(parentElement.child("artifactId").orElse(null));
        }

        @Test
        @DisplayName("should not trim parent elements when parent is external")
        void shouldNotTrimParentElementsWhenParentIsExternal() throws Exception {
            String childPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.0.0</version>
                        <relativePath/>
                    </parent>
                    <artifactId>my-spring-app</artifactId>
                    <!-- No explicit groupId or version - would inherit from parent -->
                </project>
                """;

            Document childDoc = Document.of(childPomXml);

            Map<Path, Document> pomMap = Map.of(Paths.get("project", "pom.xml"), childDoc);

            Editor editor = new Editor(childDoc);
            Element childRoot = editor.root();
            Element parentElement = DomUtils.findChildElement(childRoot, "parent");

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify correct behavior for external parent:
            // - groupId should NOT be removed (external parents need groupId to be located)
            // - artifactId should NOT be removed (external parents need artifactId to be located)
            // - version should NOT be removed (external parents need version to be located)
            // This prevents the "parent.groupId is missing" error reported in issue #7934
            assertNotNull(parentElement.child("groupId").orElse(null));
            assertNotNull(parentElement.child("artifactId").orElse(null));
            assertNotNull(parentElement.child("version").orElse(null));
        }

        @Test
        @DisplayName("should trim parent elements when parent is in reactor")
        void shouldTrimParentElementsWhenParentIsInReactor() throws Exception {
            // Create parent POM
            String parentPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                </project>
                """;

            // Create child POM that references the parent
            String childPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>child-project</artifactId>
                    <!-- No explicit groupId or version - would inherit from parent -->
                </project>
                """;

            Document parentDoc = Document.of(parentPomXml);
            Document childDoc = Document.of(childPomXml);

            // Both POMs are in the reactor
            Map<Path, Document> pomMap = Map.of(
                    Paths.get("pom.xml"), parentDoc,
                    Paths.get("child", "pom.xml"), childDoc);

            Element childRoot = childDoc.root();
            Element parentElement = childRoot.child("parent").orElse(null);

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify correct behavior for reactor parent:
            // - groupId should be removed (child has no explicit groupId, parent is in reactor)
            // - artifactId should be removed (can be inferred from relativePath)
            // - version should be removed (child has no explicit version, parent is in reactor)
            assertNull(parentElement.child("groupId").orElse(null));
            assertNull(parentElement.child("artifactId").orElse(null));
            assertNull(parentElement.child("version").orElse(null));
        }
    }

    @Nested
    @DisplayName("Maven 4.0.0 Limited Inference")
    class Maven400LimitedInferenceTests {

        @Test
        @DisplayName("should remove child groupId and version when they match parent in 4.0.0")
        void shouldRemoveChildGroupIdAndVersionWhenTheyMatchParentIn400() throws Exception {
            String parentPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                </project>
                """;

            String childPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                        <relativePath>../pom.xml</relativePath>
                    </parent>
                    <groupId>com.example</groupId>
                    <artifactId>child-project</artifactId>
                    <version>1.0.0</version>
                    <!-- Child groupId and version match parent - can be inferred -->
                </project>
                """;

            Document parentDoc = Document.of(parentPomXml);
            Document childDoc = Document.of(childPomXml);

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "pom.xml"), parentDoc);
            pomMap.put(Paths.get("project", "child", "pom.xml"), childDoc);

            Editor editor = new Editor(childDoc);
            Element childRoot = editor.root();
            Element parentElement = DomUtils.findChildElement(childRoot, "parent");

            // Verify child and parent elements exist before inference
            assertNotNull(childRoot.child("groupId").orElse(null));
            assertNotNull(childRoot.child("version").orElse(null));
            assertNotNull(parentElement.child("groupId").orElse(null));
            assertNotNull(parentElement.child("artifactId").orElse(null));
            assertNotNull(parentElement.child("version").orElse(null));

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify child groupId and version were removed (Maven 4.0.0 can infer these from parent)
            assertNull(childRoot.child("groupId").orElse(null));
            assertNull(childRoot.child("version").orElse(null));
            // Child artifactId should remain (always required)
            assertNotNull(childRoot.child("artifactId").orElse(null));
            // Parent elements should all remain (no relativePath inference in 4.0.0)
            assertNotNull(parentElement.child("groupId").orElse(null));
            assertNotNull(parentElement.child("artifactId").orElse(null));
            assertNotNull(parentElement.child("version").orElse(null));
        }

        @Test
        @DisplayName("should keep child groupId when it differs from parent in 4.0.0")
        void shouldKeepChildGroupIdWhenItDiffersFromParentIn400() throws Exception {
            String parentPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                </project>
                """;

            String childPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                        <relativePath>../pom.xml</relativePath>
                    </parent>
                    <groupId>com.example.child</groupId>
                    <artifactId>child-project</artifactId>
                    <version>2.0.0</version>
                </project>
                """;

            Document parentDoc = Document.of(parentPomXml);
            Document childDoc = Document.of(childPomXml);

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "pom.xml"), parentDoc);
            pomMap.put(Paths.get("project", "child", "pom.xml"), childDoc);

            Editor editor = new Editor(childDoc);
            Element childRoot = editor.root();
            Element parentElement = childRoot.child("parent").orElse(null);

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify child elements are kept (since they differ from parent)
            assertNotNull(childRoot.child("groupId").orElse(null));
            assertNotNull(childRoot.child("version").orElse(null));
            assertNotNull(childRoot.child("artifactId").orElse(null));
            // Parent elements should all remain (no relativePath inference in 4.0.0)
            assertNotNull(parentElement.child("groupId").orElse(null));
            assertNotNull(parentElement.child("artifactId").orElse(null));
            assertNotNull(parentElement.child("version").orElse(null));
        }

        @Test
        @DisplayName("should handle partial inheritance in 4.0.0")
        void shouldHandlePartialInheritanceIn400() throws Exception {
            String parentPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                </project>
                """;

            String childPomXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                        <relativePath>../pom.xml</relativePath>
                    </parent>
                    <groupId>com.example</groupId>
                    <artifactId>child-project</artifactId>
                    <version>2.0.0</version>
                    <!-- Child groupId matches parent, but version differs -->
                </project>
                """;

            Document parentDoc = Document.of(parentPomXml);
            Document childDoc = Document.of(childPomXml);

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "pom.xml"), parentDoc);
            pomMap.put(Paths.get("project", "child", "pom.xml"), childDoc);

            Editor editor = new Editor(childDoc);
            Element childRoot = editor.root();
            Element parentElement = DomUtils.findChildElement(childRoot, "parent");

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify child groupId was removed (matches parent, can be inferred)
            assertNull(childRoot.child("groupId").orElse(null));
            // Verify child version was kept (differs from parent, cannot be inferred)
            assertNotNull(childRoot.child("version").orElse(null));
            // Verify child artifactId was kept (always required)
            assertNotNull(childRoot.child("artifactId").orElse(null));
            // Parent elements should all remain (no relativePath inference in 4.0.0)
            assertNotNull(parentElement.child("groupId").orElse(null));
            assertNotNull(parentElement.child("artifactId").orElse(null));
            assertNotNull(parentElement.child("version").orElse(null));
        }

        @Test
        @DisplayName("should not apply dependency inference to 4.0.0 models")
        void shouldNotApplyDependencyInferenceTo400Models() throws Exception {
            String moduleAPomXml = PomBuilder.create()
                    .namespace("http://maven.apache.org/POM/4.0.0")
                    .modelVersion("4.0.0")
                    .groupId("com.example")
                    .artifactId("module-a")
                    .version("1.0.0")
                    .build();

            String moduleBPomXml = PomBuilder.create()
                    .namespace("http://maven.apache.org/POM/4.0.0")
                    .modelVersion("4.0.0")
                    .groupId("com.example")
                    .artifactId("module-b")
                    .version("1.0.0")
                    .dependency("com.example", "module-a", "1.0.0")
                    .build();

            Document moduleADoc = Document.of(moduleAPomXml);
            Document moduleBDoc = Document.of(moduleBPomXml);

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "module-a", "pom.xml"), moduleADoc);
            pomMap.put(Paths.get("project", "module-b", "pom.xml"), moduleBDoc);

            Editor editor = new Editor(moduleBDoc);
            Element moduleBRoot = editor.root();
            Element dependency = moduleBRoot
                    .child("dependencies")
                    .orElse(null)
                    .children("dependency")
                    .findFirst()
                    .orElse(null);

            // Verify dependency elements exist before inference
            assertNotNull(dependency.child("groupId").orElse(null));
            assertNotNull(dependency.child("artifactId").orElse(null));
            assertNotNull(dependency.child("version").orElse(null));

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify dependency inference was NOT applied (all elements should remain for 4.0.0)
            assertNotNull(dependency.child("groupId").orElse(null));
            assertNotNull(dependency.child("artifactId").orElse(null));
            assertNotNull(dependency.child("version").orElse(null));
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
            assertTrue(description.toLowerCase().contains("infer"), "Description should mention inference");
        }
    }
}
