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

import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
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
    private SAXBuilder saxBuilder;

    @BeforeEach
    void setUp() {
        strategy = new InferenceStrategy();
        saxBuilder = new SAXBuilder();
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

            String moduleBPomXml =
                    """
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

            Document parentDoc = saxBuilder.build(new StringReader(parentPomXml));
            Document moduleADoc = saxBuilder.build(new StringReader(moduleAPomXml));
            Document moduleBDoc = saxBuilder.build(new StringReader(moduleBPomXml));

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "pom.xml"), parentDoc);
            pomMap.put(Paths.get("project", "module-a", "pom.xml"), moduleADoc);
            pomMap.put(Paths.get("project", "module-b", "pom.xml"), moduleBDoc);

            Element moduleBRoot = moduleBDoc.getRootElement();
            Element dependencies = moduleBRoot.getChild("dependencies", moduleBRoot.getNamespace());
            Element dependency = dependencies.getChild("dependency", moduleBRoot.getNamespace());

            // Verify dependency elements exist before inference
            assertNotNull(dependency.getChild("groupId", moduleBRoot.getNamespace()));
            assertNotNull(dependency.getChild("artifactId", moduleBRoot.getNamespace()));
            assertNotNull(dependency.getChild("version", moduleBRoot.getNamespace()));

            // Apply dependency inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify version was removed (can be inferred from project)
            assertNull(dependency.getChild("version", moduleBRoot.getNamespace()));
            // groupId should also be removed (can be inferred from project)
            assertNull(dependency.getChild("groupId", moduleBRoot.getNamespace()));
            // artifactId should remain (always required)
            assertNotNull(dependency.getChild("artifactId", moduleBRoot.getNamespace()));
        }

        @Test
        @DisplayName("should keep dependency version for external artifact")
        void shouldKeepDependencyVersionForExternalArtifact() throws Exception {
            String modulePomXml =
                    """
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

            Document moduleDoc = saxBuilder.build(new StringReader(modulePomXml));
            Map<Path, Document> pomMap = Map.of(Paths.get("project", "pom.xml"), moduleDoc);

            Element moduleRoot = moduleDoc.getRootElement();
            Element dependencies = moduleRoot.getChild("dependencies", moduleRoot.getNamespace());
            Element dependency = dependencies.getChild("dependency", moduleRoot.getNamespace());

            // Apply dependency inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify all dependency elements remain (external dependency)
            assertNotNull(dependency.getChild("groupId", moduleRoot.getNamespace()));
            assertNotNull(dependency.getChild("artifactId", moduleRoot.getNamespace()));
            assertNotNull(dependency.getChild("version", moduleRoot.getNamespace()));
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

            Document moduleADoc = saxBuilder.build(new StringReader(moduleAPomXml));
            Document moduleBDoc = saxBuilder.build(new StringReader(moduleBPomXml));

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "module-a", "pom.xml"), moduleADoc);
            pomMap.put(Paths.get("project", "module-b", "pom.xml"), moduleBDoc);

            Element moduleBRoot = moduleBDoc.getRootElement();
            Element dependencies = moduleBRoot.getChild("dependencies", moduleBRoot.getNamespace());
            Element dependency = dependencies.getChild("dependency", moduleBRoot.getNamespace());

            // Apply dependency inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify correct behavior when version doesn't match:
            // - groupId should be removed (can be inferred from project regardless of version)
            // - version should remain (doesn't match project version, so can't be inferred)
            // - artifactId should remain (always required)
            assertNull(dependency.getChild("groupId", moduleBRoot.getNamespace()));
            assertNotNull(dependency.getChild("artifactId", moduleBRoot.getNamespace()));
            assertNotNull(dependency.getChild("version", moduleBRoot.getNamespace()));
        }

        @Test
        @DisplayName("should handle plugin dependencies")
        void shouldHandlePluginDependencies() throws Exception {
            String moduleAPomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <groupId>com.example</groupId>
                    <artifactId>module-a</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            String moduleBPomXml =
                    """
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

            Document moduleADoc = saxBuilder.build(new StringReader(moduleAPomXml));
            Document moduleBDoc = saxBuilder.build(new StringReader(moduleBPomXml));

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "module-a", "pom.xml"), moduleADoc);
            pomMap.put(Paths.get("project", "module-b", "pom.xml"), moduleBDoc);

            Element moduleBRoot = moduleBDoc.getRootElement();
            Element build = moduleBRoot.getChild("build", moduleBRoot.getNamespace());
            Element plugins = build.getChild("plugins", moduleBRoot.getNamespace());
            Element plugin = plugins.getChild("plugin", moduleBRoot.getNamespace());
            Element dependencies = plugin.getChild("dependencies", moduleBRoot.getNamespace());
            Element dependency = dependencies.getChild("dependency", moduleBRoot.getNamespace());

            // Apply dependency inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify version and groupId were removed from plugin dependency
            assertNull(dependency.getChild("version", moduleBRoot.getNamespace()));
            assertNull(dependency.getChild("groupId", moduleBRoot.getNamespace()));
            assertNotNull(dependency.getChild("artifactId", moduleBRoot.getNamespace()));
        }
    }

    @Nested
    @DisplayName("Parent Inference")
    class ParentInferenceTests {

        @Test
        @DisplayName("should remove parent groupId when child doesn't have explicit groupId")
        void shouldRemoveParentGroupIdWhenChildDoesntHaveExplicitGroupId() throws Exception {
            String parentPomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            String childPomXml =
                    """
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

            Document parentDoc = saxBuilder.build(new StringReader(parentPomXml));
            Document childDoc = saxBuilder.build(new StringReader(childPomXml));

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "pom.xml"), parentDoc);
            pomMap.put(Paths.get("project", "child", "pom.xml"), childDoc);

            Element childRoot = childDoc.getRootElement();
            Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

            // Verify parent elements exist before inference
            assertNotNull(parentElement.getChild("groupId", childRoot.getNamespace()));
            assertNotNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
            assertNotNull(parentElement.getChild("version", childRoot.getNamespace()));

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify parent groupId and version were removed (since child doesn't have explicit ones)
            assertNull(parentElement.getChild("groupId", childRoot.getNamespace()));
            assertNull(parentElement.getChild("version", childRoot.getNamespace()));
            // artifactId should also be removed since parent POM is in pomMap
            assertNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
        }

        @Test
        @DisplayName("should keep parent groupId when child has explicit groupId")
        void shouldKeepParentGroupIdWhenChildHasExplicitGroupId() throws Exception {
            String parentPomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                    <modelVersion>4.1.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            String childPomXml =
                    """
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

            Document parentDoc = saxBuilder.build(new StringReader(parentPomXml));
            Document childDoc = saxBuilder.build(new StringReader(childPomXml));

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "pom.xml"), parentDoc);
            pomMap.put(Paths.get("project", "child", "pom.xml"), childDoc);

            Element childRoot = childDoc.getRootElement();
            Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify parent elements are kept (since child has explicit values)
            assertNotNull(parentElement.getChild("groupId", childRoot.getNamespace()));
            assertNotNull(parentElement.getChild("version", childRoot.getNamespace()));
            // artifactId should still be removed since parent POM is in pomMap
            assertNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
        }

        @Test
        @DisplayName("should not trim parent elements when parent is external")
        void shouldNotTrimParentElementsWhenParentIsExternal() throws Exception {
            String childPomXml =
                    """
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

            Document childDoc = saxBuilder.build(new StringReader(childPomXml));

            Map<Path, Document> pomMap = Map.of(Paths.get("project", "pom.xml"), childDoc);

            Element childRoot = childDoc.getRootElement();
            Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify correct behavior for external parent:
            // - groupId should be removed (child doesn't have explicit groupId, can inherit from parent)
            // - version should be removed (child doesn't have explicit version, can inherit from parent)
            // - artifactId should be removed (Maven 4.1.0+ can infer from relativePath even for external parents)
            assertNull(parentElement.getChild("groupId", childRoot.getNamespace()));
            assertNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
            assertNull(parentElement.getChild("version", childRoot.getNamespace()));
        }
    }

    @Nested
    @DisplayName("Maven 4.0.0 Limited Inference")
    class Maven400LimitedInferenceTests {

        @Test
        @DisplayName("should remove child groupId and version when they match parent in 4.0.0")
        void shouldRemoveChildGroupIdAndVersionWhenTheyMatchParentIn400() throws Exception {
            String parentPomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                </project>
                """;

            String childPomXml =
                    """
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

            Document parentDoc = saxBuilder.build(new StringReader(parentPomXml));
            Document childDoc = saxBuilder.build(new StringReader(childPomXml));

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "pom.xml"), parentDoc);
            pomMap.put(Paths.get("project", "child", "pom.xml"), childDoc);

            Element childRoot = childDoc.getRootElement();
            Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

            // Verify child and parent elements exist before inference
            assertNotNull(childRoot.getChild("groupId", childRoot.getNamespace()));
            assertNotNull(childRoot.getChild("version", childRoot.getNamespace()));
            assertNotNull(parentElement.getChild("groupId", childRoot.getNamespace()));
            assertNotNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
            assertNotNull(parentElement.getChild("version", childRoot.getNamespace()));

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify child groupId and version were removed (Maven 4.0.0 can infer these from parent)
            assertNull(childRoot.getChild("groupId", childRoot.getNamespace()));
            assertNull(childRoot.getChild("version", childRoot.getNamespace()));
            // Child artifactId should remain (always required)
            assertNotNull(childRoot.getChild("artifactId", childRoot.getNamespace()));
            // Parent elements should all remain (no relativePath inference in 4.0.0)
            assertNotNull(parentElement.getChild("groupId", childRoot.getNamespace()));
            assertNotNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
            assertNotNull(parentElement.getChild("version", childRoot.getNamespace()));
        }

        @Test
        @DisplayName("should keep child groupId when it differs from parent in 4.0.0")
        void shouldKeepChildGroupIdWhenItDiffersFromParentIn400() throws Exception {
            String parentPomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                </project>
                """;

            String childPomXml =
                    """
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

            Document parentDoc = saxBuilder.build(new StringReader(parentPomXml));
            Document childDoc = saxBuilder.build(new StringReader(childPomXml));

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "pom.xml"), parentDoc);
            pomMap.put(Paths.get("project", "child", "pom.xml"), childDoc);

            Element childRoot = childDoc.getRootElement();
            Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify child elements are kept (since they differ from parent)
            assertNotNull(childRoot.getChild("groupId", childRoot.getNamespace()));
            assertNotNull(childRoot.getChild("version", childRoot.getNamespace()));
            assertNotNull(childRoot.getChild("artifactId", childRoot.getNamespace()));
            // Parent elements should all remain (no relativePath inference in 4.0.0)
            assertNotNull(parentElement.getChild("groupId", childRoot.getNamespace()));
            assertNotNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
            assertNotNull(parentElement.getChild("version", childRoot.getNamespace()));
        }

        @Test
        @DisplayName("should handle partial inheritance in 4.0.0")
        void shouldHandlePartialInheritanceIn400() throws Exception {
            String parentPomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                </project>
                """;

            String childPomXml =
                    """
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

            Document parentDoc = saxBuilder.build(new StringReader(parentPomXml));
            Document childDoc = saxBuilder.build(new StringReader(childPomXml));

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "pom.xml"), parentDoc);
            pomMap.put(Paths.get("project", "child", "pom.xml"), childDoc);

            Element childRoot = childDoc.getRootElement();
            Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify child groupId was removed (matches parent, can be inferred)
            assertNull(childRoot.getChild("groupId", childRoot.getNamespace()));
            // Verify child version was kept (differs from parent, cannot be inferred)
            assertNotNull(childRoot.getChild("version", childRoot.getNamespace()));
            // Verify child artifactId was kept (always required)
            assertNotNull(childRoot.getChild("artifactId", childRoot.getNamespace()));
            // Parent elements should all remain (no relativePath inference in 4.0.0)
            assertNotNull(parentElement.getChild("groupId", childRoot.getNamespace()));
            assertNotNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
            assertNotNull(parentElement.getChild("version", childRoot.getNamespace()));
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

            Document moduleADoc = saxBuilder.build(new StringReader(moduleAPomXml));
            Document moduleBDoc = saxBuilder.build(new StringReader(moduleBPomXml));

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("project", "module-a", "pom.xml"), moduleADoc);
            pomMap.put(Paths.get("project", "module-b", "pom.xml"), moduleBDoc);

            Element moduleBRoot = moduleBDoc.getRootElement();
            Element dependency = moduleBRoot
                    .getChild("dependencies", moduleBRoot.getNamespace())
                    .getChildren("dependency", moduleBRoot.getNamespace())
                    .get(0);

            // Verify dependency elements exist before inference
            assertNotNull(dependency.getChild("groupId", moduleBRoot.getNamespace()));
            assertNotNull(dependency.getChild("artifactId", moduleBRoot.getNamespace()));
            assertNotNull(dependency.getChild("version", moduleBRoot.getNamespace()));

            // Apply inference
            UpgradeContext context = createMockContext();
            strategy.apply(context, pomMap);

            // Verify dependency inference was NOT applied (all elements should remain for 4.0.0)
            assertNotNull(dependency.getChild("groupId", moduleBRoot.getNamespace()));
            assertNotNull(dependency.getChild("artifactId", moduleBRoot.getNamespace()));
            assertNotNull(dependency.getChild("version", moduleBRoot.getNamespace()));
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
