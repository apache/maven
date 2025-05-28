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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for parent inference functionality in Maven upgrade tools.
 * Tests the logic that determines when parent GAV elements can be removed because
 * they can be inferred from relativePath during Maven model building.
 */
class ParentInferenceTest {

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
    void testCanInferParentArtifactIdWithExplicitRelativePath() throws Exception {
        // Test parent artifactId inference with explicit relativePath
        String parentPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <groupId>com.example</groupId>
                <artifactId>parent-project</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        String childPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <relativePath>../pom.xml</relativePath>
                </parent>
                <artifactId>child-project</artifactId>
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

        boolean canInfer = upgrade.canInferParentArtifactId(null, parentElement, childPath, pomMap);
        assertTrue(canInfer, "Should be able to infer parent artifactId when parent POM is in pomMap");
    }

    @Test
    void testCanInferParentArtifactIdWithDefaultRelativePath() throws Exception {
        // Test parent artifactId inference with default relativePath (no explicit relativePath element)
        String parentPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <groupId>com.example</groupId>
                <artifactId>parent-project</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        String childPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <!-- No relativePath - should default to ../pom.xml -->
                </parent>
                <artifactId>child-project</artifactId>
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

        boolean canInfer = upgrade.canInferParentArtifactId(null, parentElement, childPath, pomMap);
        assertTrue(canInfer, "Should be able to infer parent artifactId with default relativePath");
    }

    @Test
    void testCanInferParentArtifactIdParentNotInPomMap() throws Exception {
        // Test that inference fails when parent POM is not in pomMap
        String childPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>external-parent</artifactId>
                    <version>1.0.0</version>
                    <relativePath>../external-parent/pom.xml</relativePath>
                </parent>
                <artifactId>child-project</artifactId>
            </project>
            """;

        Document childDoc = saxBuilder.build(new StringReader(childPomXml));

        Map<Path, Document> pomMap = new HashMap<>();
        Path childPath = Paths.get("/project/child/pom.xml");
        // Note: parent POM is NOT in pomMap

        Element childRoot = childDoc.getRootElement();
        Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

        boolean canInfer = upgrade.canInferParentArtifactId(null, parentElement, childPath, pomMap);
        assertFalse(canInfer, "Should not be able to infer parent artifactId when parent POM is not in pomMap");
    }

    @Test
    void testResolveParentPomPathExplicitFile() throws Exception {
        Path childPath = Paths.get("/project/child/pom.xml");

        Path parentPath = upgrade.resolveParentPomPath(childPath, "../pom.xml");
        assertEquals(Paths.get("/project/pom.xml"), parentPath);
    }

    @Test
    void testResolveParentPomPathDirectory() throws Exception {
        Path childPath = Paths.get("/project/child/pom.xml");

        Path parentPath = upgrade.resolveParentPomPath(childPath, "..");
        assertEquals(Paths.get("/project/pom.xml"), parentPath);
    }

    @Test
    void testResolveParentPomPathNullRelativePath() throws Exception {
        Path childPath = Paths.get("/project/child/pom.xml");

        // Null relativePath should default to ../pom.xml
        Path parentPath = upgrade.resolveParentPomPath(childPath, null);
        assertEquals(Paths.get("/project/pom.xml"), parentPath);
    }

    @Test
    void testTrimParentElementRemoveGroupIdWhenChildHasNone() throws Exception {
        // Test that parent groupId is removed when child doesn't have explicit groupId
        String parentPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <groupId>com.example</groupId>
                <artifactId>parent-project</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        String childPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
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
        Path parentPath = Paths.get("/project/pom.xml");
        Path childPath = Paths.get("/project/child/pom.xml");
        pomMap.put(parentPath, parentDoc);
        pomMap.put(childPath, childDoc);

        Element childRoot = childDoc.getRootElement();
        Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

        // Verify parent elements exist before trimming
        assertNotNull(parentElement.getChild("groupId", childRoot.getNamespace()));
        assertNotNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
        assertNotNull(parentElement.getChild("version", childRoot.getNamespace()));

        // Apply trimming logic
        UpgradeContext context = createMockContext();
        upgrade.trimParentElement(context, childDoc, childPath, new HashSet<>(), pomMap);

        // Verify parent groupId and version were removed (since child doesn't have explicit ones)
        assertNull(parentElement.getChild("groupId", childRoot.getNamespace()));
        assertNull(parentElement.getChild("version", childRoot.getNamespace()));
        // artifactId should also be removed since parent POM is in pomMap
        assertNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
    }

    @Test
    void testTrimParentElementKeepGroupIdWhenChildHasExplicit() throws Exception {
        // Test that parent groupId is kept when child has explicit groupId
        String parentPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <groupId>com.example</groupId>
                <artifactId>parent-project</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        String childPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
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

        // Apply trimming logic
        UpgradeContext context = createMockContext();
        upgrade.trimParentElement(context, childDoc, childPath, new HashSet<>(), pomMap);

        // Verify parent elements are kept (since child has explicit values)
        assertNotNull(parentElement.getChild("groupId", childRoot.getNamespace()));
        assertNotNull(parentElement.getChild("version", childRoot.getNamespace()));
        // artifactId should still be removed since parent POM is in pomMap
        assertNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
    }

    @Test
    void testTrimParentElementExternalParent() throws Exception {
        // Test that parent elements are not trimmed when parent is external (not in pomMap)
        String childPomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
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

        Map<Path, Document> pomMap = new HashMap<>();
        Path childPath = Paths.get("/project/pom.xml");
        // Note: external parent is not in pomMap

        Element childRoot = childDoc.getRootElement();
        Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

        // Apply trimming logic
        UpgradeContext context = createMockContext();
        upgrade.trimParentElement(context, childDoc, childPath, new HashSet<>(), pomMap);

        // Verify correct behavior for external parent:
        // - groupId should be removed (child doesn't have explicit groupId, can inherit from parent)
        // - version should be removed (child doesn't have explicit version, can inherit from parent)
        // - artifactId should be kept (external parent, cannot infer from relativePath)
        assertNull(parentElement.getChild("groupId", childRoot.getNamespace()));
        assertNotNull(parentElement.getChild("artifactId", childRoot.getNamespace()));
        assertNull(parentElement.getChild("version", childRoot.getNamespace()));
    }
}
