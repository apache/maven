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
import java.util.HashSet;
import java.util.Map;

import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Maven 4.0.0 limited inference functionality.
 * Tests the logic that applies Maven 4.0.0's limited parent inheritance capabilities.
 */
class Maven400InferenceTest {

    private BaseUpgradeGoal upgrade;
    private SAXBuilder saxBuilder;

    @BeforeEach
    void setUp() {
        upgrade = new Apply();
        saxBuilder = new SAXBuilder();
    }

    private UpgradeContext createMockContext() {
        InvokerRequest request = Mockito.mock(InvokerRequest.class);

        // Mock all required properties for LookupContext constructor
        Mockito.when(request.cwd()).thenReturn(Paths.get("/project"));
        Mockito.when(request.installationDirectory()).thenReturn(Paths.get("/maven"));
        Mockito.when(request.userHomeDirectory()).thenReturn(Paths.get("/home/user"));
        Mockito.when(request.topDirectory()).thenReturn(Paths.get("/project"));
        Mockito.when(request.rootDirectory()).thenReturn(java.util.Optional.empty());
        Mockito.when(request.userProperties()).thenReturn(java.util.Map.of());
        Mockito.when(request.systemProperties()).thenReturn(java.util.Map.of());

        // Mock parserRequest and logger
        org.apache.maven.api.cli.ParserRequest parserRequest =
                Mockito.mock(org.apache.maven.api.cli.ParserRequest.class);
        org.apache.maven.api.cli.Logger logger = Mockito.mock(org.apache.maven.api.cli.Logger.class);
        Mockito.when(request.parserRequest()).thenReturn(parserRequest);
        Mockito.when(parserRequest.logger()).thenReturn(logger);

        UpgradeContext context = new UpgradeContext(request);
        return context;
    }

    @Test
    void testIsModelVersionEligibleForInferenceIncludesMaven400() throws Exception {
        // Test that Maven 4.0.0 is now eligible for limited inference
        assertTrue(upgrade.isModelVersionEligibleForInference("4.0.0"));
        assertTrue(upgrade.isModelVersionEligibleForInference("4.1.0"));
        assertFalse(upgrade.isModelVersionEligibleForInference("3.0.0"));
    }

    @Test
    void testTrimParentElementLimited400RemoveChildGroupIdWhenMatchesParent() throws Exception {
        // Test that child groupId is removed in 4.0.0 when it matches parent groupId
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
        Path parentPath = Paths.get("/project/pom.xml");
        Path childPath = Paths.get("/project/child/pom.xml");
        pomMap.put(parentPath, parentDoc);
        pomMap.put(childPath, childDoc);

        Element childRoot = childDoc.getRootElement();
        Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

        // Verify child and parent elements exist before trimming
        assertNotNull(childRoot.getChild("groupId", childRoot.getNamespace()));
        assertNotNull(childRoot.getChild("version", childRoot.getNamespace()));
        assertNotNull(parentElement.getChild("groupId", childRoot.getNamespace()));
        assertNotNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
        assertNotNull(parentElement.getChild("version", childRoot.getNamespace()));

        // Apply limited 4.0.0 inference
        UpgradeContext context = createMockContext();
        upgrade.trimParentElementLimited400(context, childDoc, childPath, new HashSet<>(), pomMap);

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
    void testTrimParentElementLimited400KeepChildGroupIdWhenDifferentFromParent() throws Exception {
        // Test that child groupId is kept in 4.0.0 when it differs from parent groupId
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
        Path parentPath = Paths.get("/project/pom.xml");
        Path childPath = Paths.get("/project/child/pom.xml");
        pomMap.put(parentPath, parentDoc);
        pomMap.put(childPath, childDoc);

        Element childRoot = childDoc.getRootElement();
        Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

        // Apply limited 4.0.0 inference
        UpgradeContext context = createMockContext();
        upgrade.trimParentElementLimited400(context, childDoc, childPath, new HashSet<>(), pomMap);

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
    void testTrimParentElementLimited400PartialInheritance() throws Exception {
        // Test case where child groupId matches parent but version differs in 4.0.0
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
        Path parentPath = Paths.get("/project/pom.xml");
        Path childPath = Paths.get("/project/child/pom.xml");
        pomMap.put(parentPath, parentDoc);
        pomMap.put(childPath, childDoc);

        Element childRoot = childDoc.getRootElement();
        Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

        // Apply limited 4.0.0 inference
        UpgradeContext context = createMockContext();
        upgrade.trimParentElementLimited400(context, childDoc, childPath, new HashSet<>(), pomMap);

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
    void testApplyLimitedInferenceFor400() throws Exception {
        // Test the complete limited inference workflow for 4.0.0
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
            </project>
            """;

        Document parentDoc = saxBuilder.build(new StringReader(parentPomXml));
        Document childDoc = saxBuilder.build(new StringReader(childPomXml));

        Map<Path, Document> pomMap = new HashMap<>();
        Path parentPath = Paths.get("/project/pom.xml");
        Path childPath = Paths.get("/project/child/pom.xml");
        pomMap.put(parentPath, parentDoc);
        pomMap.put(childPath, childDoc);

        Element childRoot = childDoc.getRootElement();
        Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

        // Apply complete limited inference for 4.0.0
        UpgradeContext context = createMockContext();
        upgrade.applyLimitedInferenceFor400(context, childDoc, childPath, pomMap);

        // Verify limited inference was applied correctly
        assertNull(childRoot.getChild("groupId", childRoot.getNamespace()));
        assertNull(childRoot.getChild("version", childRoot.getNamespace()));
        assertNotNull(childRoot.getChild("artifactId", childRoot.getNamespace()));

        // Verify parent elements remain (no relativePath inference in 4.0.0)
        assertNotNull(parentElement.getChild("groupId", childRoot.getNamespace()));
        assertNotNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
        assertNotNull(parentElement.getChild("version", childRoot.getNamespace()));

        // Verify modelVersion is still present (not removed in 4.0.0)
        assertNotNull(childRoot.getChild("modelVersion", childRoot.getNamespace()));
    }

    @Test
    void testApplyInferenceRoutesToCorrectMethod() throws Exception {
        // Test that applyInference routes to the correct method based on model version
        String maven400PomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        String maven410PomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <modelVersion>4.1.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        Document maven400Doc = saxBuilder.build(new StringReader(maven400PomXml));
        Document maven410Doc = saxBuilder.build(new StringReader(maven410PomXml));

        Map<Path, Document> pomMap = new HashMap<>();
        Path maven400Path = Paths.get("/project/pom.xml");
        Path maven410Path = Paths.get("/project/pom.xml");

        UpgradeContext context = createMockContext();

        // Test 4.0.0 routing
        pomMap.put(maven400Path, maven400Doc);
        upgrade.applyInference(context, maven400Doc, maven400Path, pomMap);

        // Verify modelVersion is still present for 4.0.0 (limited inference)
        Element maven400Root = maven400Doc.getRootElement();
        assertNotNull(maven400Root.getChild("modelVersion", maven400Root.getNamespace()));

        // Test 4.1.0 routing
        pomMap.clear();
        pomMap.put(maven410Path, maven410Doc);
        upgrade.applyInference(context, maven410Doc, maven410Path, pomMap);

        // Verify modelVersion is removed for 4.1.0 (full inference)
        Element maven410Root = maven410Doc.getRootElement();
        assertNull(maven410Root.getChild("modelVersion", maven410Root.getNamespace()));
    }
}
