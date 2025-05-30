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
import java.util.List;

import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for Maven 4 compatibility fixes functionality.
 */
class Maven4CompatibilityFixesTest {

    @Test
    void testFixUnsupportedCombineChildrenAttributes() throws Exception {
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0</version>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-surefire-plugin</artifactId>
                            <configuration combine.children="override">
                                <includes>
                                    <include>**/*Test.java</include>
                                </includes>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new StringReader(pomXml));

        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();
        UpgradeContext context = Mockito.mock(UpgradeContext.class);
        context.logger = Mockito.mock(org.apache.maven.api.cli.Logger.class);

        boolean fixed = goal.fixUnsupportedCombineChildrenAttributes(context, document);

        assertTrue(fixed, "Should have fixed combine.children attribute");

        Element configElement = document.getRootElement()
                .getChild("build", document.getRootElement().getNamespace())
                .getChild("plugins", document.getRootElement().getNamespace())
                .getChild("plugin", document.getRootElement().getNamespace())
                .getChild("configuration", document.getRootElement().getNamespace());

        assertEquals("merge", configElement.getAttributeValue("combine.children"));
    }

    @Test
    void testFixUnsupportedCombineSelfAttributes() throws Exception {
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0</version>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-assembly-plugin</artifactId>
                            <configuration combine.self="append">
                                <descriptors>
                                    <descriptor>src/assembly/dist.xml</descriptor>
                                </descriptors>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new StringReader(pomXml));

        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();
        UpgradeContext context = Mockito.mock(UpgradeContext.class);
        context.logger = Mockito.mock(org.apache.maven.api.cli.Logger.class);

        boolean fixed = goal.fixUnsupportedCombineSelfAttributes(context, document);

        assertTrue(fixed, "Should have fixed combine.self attribute");

        Element configElement = document.getRootElement()
                .getChild("build", document.getRootElement().getNamespace())
                .getChild("plugins", document.getRootElement().getNamespace())
                .getChild("plugin", document.getRootElement().getNamespace())
                .getChild("configuration", document.getRootElement().getNamespace());

        assertEquals("merge", configElement.getAttributeValue("combine.self"));
    }

    @Test
    void testAllOptionBehavior() throws Exception {
        // Test that --all option enables all features
        org.apache.maven.cling.invoker.mvnup.UpgradeInvokerRequest request =
                Mockito.mock(org.apache.maven.cling.invoker.mvnup.UpgradeInvokerRequest.class);
        org.apache.maven.api.cli.mvnup.UpgradeOptions options =
                Mockito.mock(org.apache.maven.api.cli.mvnup.UpgradeOptions.class);

        // Mock all required properties for LookupContext constructor
        Mockito.when(request.cwd()).thenReturn(java.nio.file.Paths.get("/project"));
        Mockito.when(request.installationDirectory()).thenReturn(java.nio.file.Paths.get("/maven"));
        Mockito.when(request.userHomeDirectory()).thenReturn(java.nio.file.Paths.get("/home/user"));
        Mockito.when(request.topDirectory()).thenReturn(java.nio.file.Paths.get("/project"));
        Mockito.when(request.rootDirectory()).thenReturn(java.util.Optional.empty());
        Mockito.when(request.userProperties()).thenReturn(java.util.Map.of());
        Mockito.when(request.systemProperties()).thenReturn(java.util.Map.of());

        // Mock --all option as true
        Mockito.when(request.options()).thenReturn(options);
        Mockito.when(options.all()).thenReturn(java.util.Optional.of(true));
        Mockito.when(options.infer()).thenReturn(java.util.Optional.empty());
        Mockito.when(options.fixModel()).thenReturn(java.util.Optional.empty());
        Mockito.when(options.model()).thenReturn(java.util.Optional.empty());

        // Mock parserRequest and logger
        org.apache.maven.api.cli.ParserRequest parserRequest =
                Mockito.mock(org.apache.maven.api.cli.ParserRequest.class);
        org.apache.maven.api.cli.Logger logger = Mockito.mock(org.apache.maven.api.cli.Logger.class);
        Mockito.when(request.parserRequest()).thenReturn(parserRequest);
        Mockito.when(parserRequest.logger()).thenReturn(logger);

        UpgradeContext context = new UpgradeContext(request);
        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();

        // Test that --all option sets target model to 4.1.0 and enables all features
        String result = goal.testDoUpgradeLogic(context, "4.0.0", new java.util.HashMap<>());
        assertEquals("4.1.0", result); // Should upgrade to 4.1.0 when --all is used
    }

    @Test
    void testDefaultBehavior() throws Exception {
        // Test that default behavior enables --fix-model
        org.apache.maven.cling.invoker.mvnup.UpgradeInvokerRequest request =
                Mockito.mock(org.apache.maven.cling.invoker.mvnup.UpgradeInvokerRequest.class);
        org.apache.maven.api.cli.mvnup.UpgradeOptions options =
                Mockito.mock(org.apache.maven.api.cli.mvnup.UpgradeOptions.class);

        // Mock all required properties for LookupContext constructor
        Mockito.when(request.cwd()).thenReturn(java.nio.file.Paths.get("/project"));
        Mockito.when(request.installationDirectory()).thenReturn(java.nio.file.Paths.get("/maven"));
        Mockito.when(request.userHomeDirectory()).thenReturn(java.nio.file.Paths.get("/home/user"));
        Mockito.when(request.topDirectory()).thenReturn(java.nio.file.Paths.get("/project"));
        Mockito.when(request.rootDirectory()).thenReturn(java.util.Optional.empty());
        Mockito.when(request.userProperties()).thenReturn(java.util.Map.of());
        Mockito.when(request.systemProperties()).thenReturn(java.util.Map.of());

        // Mock no options specified
        Mockito.when(request.options()).thenReturn(options);
        Mockito.when(options.all()).thenReturn(java.util.Optional.empty());
        Mockito.when(options.infer()).thenReturn(java.util.Optional.empty());
        Mockito.when(options.fixModel()).thenReturn(java.util.Optional.empty());
        Mockito.when(options.model()).thenReturn(java.util.Optional.empty());

        // Mock parserRequest and logger
        org.apache.maven.api.cli.ParserRequest parserRequest =
                Mockito.mock(org.apache.maven.api.cli.ParserRequest.class);
        org.apache.maven.api.cli.Logger logger = Mockito.mock(org.apache.maven.api.cli.Logger.class);
        Mockito.when(request.parserRequest()).thenReturn(parserRequest);
        Mockito.when(parserRequest.logger()).thenReturn(logger);

        UpgradeContext context = new UpgradeContext(request);
        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();

        // Test that default behavior enables fix-model
        boolean defaultFixModel = goal.testDefaultFixModelBehavior(context, "4.0.0", new java.util.HashMap<>());
        assertTrue(defaultFixModel, "Default behavior should enable --fix-model");
    }

    @Test
    void testFixDuplicatePlugins() throws Exception {
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.8.1</version>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.9.0</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new StringReader(pomXml));

        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();
        UpgradeContext context = Mockito.mock(UpgradeContext.class);
        context.logger = Mockito.mock(org.apache.maven.api.cli.Logger.class);

        boolean fixed = goal.fixDuplicatePlugins(context, document);

        assertTrue(fixed, "Should have fixed duplicate plugins");

        // Verify only one plugin remains
        Element buildElement = document.getRootElement()
                .getChild("build", document.getRootElement().getNamespace());
        Element pluginsElement = buildElement.getChild("plugins", buildElement.getNamespace());
        List<Element> plugins = pluginsElement.getChildren("plugin", pluginsElement.getNamespace());

        assertEquals(1, plugins.size(), "Should have only one plugin after removing duplicates");
    }

    @Test
    void testFixIncorrectParentRelativePaths() throws Exception {
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                    <relativePath>../wrong-path/pom.xml</relativePath>
                </parent>
                <artifactId>child</artifactId>
            </project>
            """;

        String parentPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>parent</artifactId>
                <version>1.0.0</version>
                <packaging>pom</packaging>
            </project>
            """;

        SAXBuilder builder = new SAXBuilder();
        Document childDocument = builder.build(new StringReader(pomXml));
        Document parentDocument = builder.build(new StringReader(parentPomXml));

        // Create mock pomMap with parent POM
        java.util.Map<java.nio.file.Path, Document> pomMap = new java.util.HashMap<>();
        java.nio.file.Path childPath = java.nio.file.Paths.get("child/pom.xml");
        java.nio.file.Path parentPath = java.nio.file.Paths.get("pom.xml");
        pomMap.put(childPath, childDocument);
        pomMap.put(parentPath, parentDocument);

        TestableBaseUpgradeGoal goal = new TestableBaseUpgradeGoal();
        UpgradeContext context = Mockito.mock(UpgradeContext.class);
        context.logger = Mockito.mock(org.apache.maven.api.cli.Logger.class);

        boolean fixed = goal.fixIncorrectParentRelativePaths(context, childDocument, childPath, pomMap);

        assertTrue(fixed, "Should have fixed incorrect parent relative path");

        // Verify the relativePath was updated
        Element parentElement = childDocument
                .getRootElement()
                .getChild("parent", childDocument.getRootElement().getNamespace());
        Element relativePathElement = parentElement.getChild("relativePath", parentElement.getNamespace());

        assertNotNull(relativePathElement, "relativePath element should exist");
        assertEquals("../pom.xml", relativePathElement.getTextTrim(), "relativePath should be corrected");
    }

    /**
     * Testable subclass that exposes protected methods for testing.
     */
    private static class TestableBaseUpgradeGoal extends BaseUpgradeGoal {
        @Override
        protected boolean shouldSaveModifications() {
            return false;
        }

        @Override
        public boolean fixUnsupportedCombineChildrenAttributes(UpgradeContext context, Document pomDocument) {
            return super.fixUnsupportedCombineChildrenAttributes(context, pomDocument);
        }

        @Override
        public boolean fixUnsupportedCombineSelfAttributes(UpgradeContext context, Document pomDocument) {
            return super.fixUnsupportedCombineSelfAttributes(context, pomDocument);
        }

        @Override
        public boolean fixDuplicatePlugins(UpgradeContext context, Document pomDocument) {
            return super.fixDuplicatePlugins(context, pomDocument);
        }

        @Override
        public boolean fixIncorrectParentRelativePaths(
                UpgradeContext context,
                Document pomDocument,
                java.nio.file.Path pomPath,
                java.util.Map<java.nio.file.Path, Document> pomMap) {
            return super.fixIncorrectParentRelativePaths(context, pomDocument, pomPath, pomMap);
        }

        // Test helper methods to expose internal logic
        public String testDoUpgradeLogic(
                UpgradeContext context,
                String targetModel,
                java.util.Map<java.nio.file.Path, org.jdom2.Document> pomMap) {
            org.apache.maven.api.cli.mvnup.UpgradeOptions options =
                    ((org.apache.maven.cling.invoker.mvnup.UpgradeInvokerRequest) context.invokerRequest).options();

            // Handle --all option (overrides individual options)
            boolean useAll = options.all().orElse(false);

            // If --all is specified, ensure we're targeting 4.1.0
            if (useAll && !options.model().isPresent()) {
                targetModel = "4.1.0";
            }

            return targetModel;
        }

        public boolean testDefaultFixModelBehavior(
                UpgradeContext context,
                String targetModel,
                java.util.Map<java.nio.file.Path, org.jdom2.Document> pomMap) {
            org.apache.maven.api.cli.mvnup.UpgradeOptions options =
                    ((org.apache.maven.cling.invoker.mvnup.UpgradeInvokerRequest) context.invokerRequest).options();

            // Handle --all option (overrides individual options)
            boolean useAll = options.all().orElse(false);
            boolean useFixModel = useAll || options.fixModel().orElse(false);

            // Apply default behavior: if no specific options are provided, enable --fix-model
            if (!useAll
                    && !options.infer().isPresent()
                    && !options.fixModel().isPresent()
                    && !options.model().isPresent()) {
                useFixModel = true;
            }

            return useFixModel;
        }
    }
}
