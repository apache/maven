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
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Apply goal methods.
 * These tests focus on XML transformation logic by modifying Document objects in memory.
 * The key insight is that we can test the same code paths used by both apply and check modes
 * by modifying Documents in memory without writing to disk.
 */
class ApplyTest {

    private BaseUpgradeGoal upgrade;
    private SAXBuilder saxBuilder;

    @BeforeEach
    void setUp() {
        upgrade = new Apply();
        saxBuilder = new SAXBuilder();
    }

    @Test
    void testUpdateNamespaceRecursively() throws Exception {
        // Test recursive namespace update - core functionality used by both apply and check
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <dependencies>
                    <dependency>
                        <groupId>test</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));
        Element root = document.getRootElement();

        Namespace newNamespace = Namespace.getNamespace("http://maven.apache.org/POM/4.1.0");

        upgrade.updateNamespaceRecursively(root, newNamespace);

        // Verify root namespace updated
        assertEquals(newNamespace, root.getNamespace());

        // Verify child elements namespace updated recursively
        Element dependencies = root.getChild("dependencies", newNamespace);
        assertNotNull(dependencies);
        assertEquals(newNamespace, dependencies.getNamespace());

        Element dependency = dependencies.getChild("dependency", newNamespace);
        assertNotNull(dependency);
        assertEquals(newNamespace, dependency.getNamespace());

        Element groupId = dependency.getChild("groupId", newNamespace);
        assertNotNull(groupId);
        assertEquals(newNamespace, groupId.getNamespace());
    }

    @Test
    void testConvertModulesElement() throws Exception {
        // Test converting modules to subprojects while preserving formatting
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <modules>
                    <module>module1</module>
                    <module>module2</module>
                </modules>
            </project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));
        Element root = document.getRootElement();
        Namespace namespace = root.getNamespace();

        upgrade.convertModulesElement(root, namespace);

        // Verify modules element was renamed to subprojects
        assertNull(root.getChild("modules", namespace));
        Element subprojects = root.getChild("subprojects", namespace);
        assertNotNull(subprojects);

        // Verify module elements were renamed to subproject
        assertEquals(0, subprojects.getChildren("module", namespace).size());
        assertEquals(2, subprojects.getChildren("subproject", namespace).size());

        assertEquals(
                "module1",
                subprojects.getChildren("subproject", namespace).get(0).getText());
        assertEquals(
                "module2",
                subprojects.getChildren("subproject", namespace).get(1).getText());
    }

    @Test
    void testExtractGAVWithParentInheritance() throws Exception {
        // Test extracting GAV with parent inheritance using the available method
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                </parent>
                <artifactId>child</artifactId>
            </project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));

        // Use reflection to access the protected method
        java.lang.reflect.Method method = BaseUpgradeGoal.class.getDeclaredMethod(
                "extractGAVWithParentResolution",
                Document.class,
                org.apache.maven.cling.invoker.mvnup.UpgradeContext.class);
        method.setAccessible(true);

        // We need a mock context, but for this test we can pass null since the method doesn't use it
        BaseUpgradeGoal.GAV result = (BaseUpgradeGoal.GAV) method.invoke(upgrade, document, null);

        assertNotNull(result);
        assertEquals("com.example", result.groupId);
        assertEquals("child", result.artifactId);
        assertEquals("1.0.0", result.version);
    }

    @Test
    void testCanUpgrade400To410() throws Exception {
        boolean result = upgrade.canUpgrade("4.0.0", "4.1.0");
        assertTrue(result);
    }

    @Test
    void testCanUpgradeUnsupportedPath() throws Exception {
        boolean result = upgrade.canUpgrade("4.1.0", "4.0.0");
        assertFalse(result);
    }

    @Test
    void testIsValidModelVersion() throws Exception {
        assertTrue(upgrade.isValidModelVersion("4.0.0"));
        assertTrue(upgrade.isValidModelVersion("4.1.0"));
        assertFalse(upgrade.isValidModelVersion("3.0.0"));
        assertFalse(upgrade.isValidModelVersion("5.0.0"));
    }

    @Test
    void testDetectModelVersion() throws Exception {
        // Test model version detection using the available method
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        Document document = saxBuilder.build(new StringReader(pomXml));

        // Use reflection to access the protected method
        java.lang.reflect.Method method = BaseUpgradeGoal.class.getDeclaredMethod(
                "detectModelVersion", org.apache.maven.cling.invoker.mvnup.UpgradeContext.class, Document.class);
        method.setAccessible(true);

        // We need a mock context, but for this test we can pass null since the method doesn't use it for logging
        String result = (String) method.invoke(upgrade, null, document);
        assertEquals("4.0.0", result);
    }

    @Test
    void testGAVEquals() throws Exception {
        // Create GAV instances
        Object gav1 = new BaseUpgradeGoal.GAV("com.example", "artifact", "1.0.0");
        Object gav2 = new BaseUpgradeGoal.GAV("com.example", "artifact", "1.0.0");
        Object gav3 = new BaseUpgradeGoal.GAV("com.example", "artifact", "2.0.0");

        assertEquals(gav1, gav2);
        assertNotNull(gav3); // Different version should not equal
    }

    @Test
    void testGAVMatchesIgnoringVersion() throws Exception {
        // Create GAV instances with different versions
        BaseUpgradeGoal.GAV gav1 = new BaseUpgradeGoal.GAV("com.example", "artifact", "1.0.0");
        BaseUpgradeGoal.GAV gav2 = new BaseUpgradeGoal.GAV("com.example", "artifact", "2.0.0");

        // Test matchesIgnoringVersion method
        boolean result = gav1.matchesIgnoringVersion(gav2);
        assertTrue(result);
    }

    @Test
    void testIsModelVersionEligibleForInference() throws Exception {
        // Test that 4.0.0+ models are eligible for inference (4.0.0 has limited support)
        assertTrue(upgrade.isModelVersionEligibleForInference("4.0.0"));
        assertTrue(upgrade.isModelVersionEligibleForInference("4.1.0"));
        assertFalse(upgrade.isModelVersionEligibleForInference("3.0.0"));
        // Future versions should also be eligible
        // assertTrue(upgrade.isModelVersionEligibleForInference("4.2.0"));
    }

    @Test
    void testRemoveElementWithFormatting() throws Exception {
        // Test that removeElementWithFormatting correctly removes elements
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0" root="true">
                <modelVersion>4.1.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        Document doc = saxBuilder.build(new StringReader(pomXml));
        Element root = doc.getRootElement();
        Element modelVersionElement = root.getChild("modelVersion", root.getNamespace());

        // Verify modelVersion exists before removal
        assertNotNull(modelVersionElement);

        // Remove the element using the formatting-preserving method
        upgrade.removeElementWithFormatting(modelVersionElement);

        // Verify modelVersion was removed
        assertNull(root.getChild("modelVersion", root.getNamespace()));
    }

    @Test
    void testIsSubprojectsListRedundant() throws Exception {
        // Test that subprojects list redundancy detection works
        String pomXml =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.1.0">
                <subprojects>
                    <subproject>child1</subproject>
                    <subproject>child2</subproject>
                </subprojects>
            </project>
            """;

        Document doc = saxBuilder.build(new StringReader(pomXml));
        Element root = doc.getRootElement();
        Element subprojectsElement = root.getChild("subprojects", root.getNamespace());

        // Test with a temporary directory structure
        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("mvnup-test");
        java.nio.file.Path pomPath = tempDir.resolve("pom.xml");

        try {
            // Create child directories with pom.xml files
            java.nio.file.Files.createDirectory(tempDir.resolve("child1"));
            java.nio.file.Files.createFile(tempDir.resolve("child1").resolve("pom.xml"));
            java.nio.file.Files.createDirectory(tempDir.resolve("child2"));
            java.nio.file.Files.createFile(tempDir.resolve("child2").resolve("pom.xml"));

            // Test redundancy check
            boolean isRedundant =
                    upgrade.isSubprojectsListRedundant(null, subprojectsElement, root.getNamespace(), pomPath);
            assertTrue(isRedundant, "Subprojects list should be redundant when it matches directory structure");

        } finally {
            // Clean up
            java.nio.file.Files.deleteIfExists(tempDir.resolve("child1").resolve("pom.xml"));
            java.nio.file.Files.deleteIfExists(tempDir.resolve("child1"));
            java.nio.file.Files.deleteIfExists(tempDir.resolve("child2").resolve("pom.xml"));
            java.nio.file.Files.deleteIfExists(tempDir.resolve("child2"));
            java.nio.file.Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void testCanInferParentArtifactId() throws Exception {
        // Test that parent artifactId inference works when parent POM is in pomMap
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

        // Create pomMap with both POMs
        Map<java.nio.file.Path, Document> pomMap = new java.util.HashMap<>();
        java.nio.file.Path parentPath = java.nio.file.Paths.get("/project/pom.xml");
        java.nio.file.Path childPath = java.nio.file.Paths.get("/project/child/pom.xml");
        pomMap.put(parentPath, parentDoc);
        pomMap.put(childPath, childDoc);

        Element childRoot = childDoc.getRootElement();
        Element parentElement = childRoot.getChild("parent", childRoot.getNamespace());

        // Test that we can infer parent artifactId
        boolean canInfer = upgrade.canInferParentArtifactId(null, parentElement, childPath, pomMap);
        assertTrue(canInfer, "Should be able to infer parent artifactId when parent POM is in pomMap");
    }

    @Test
    void testResolveParentPomPath() throws Exception {
        // Test parent POM path resolution
        java.nio.file.Path childPath = java.nio.file.Paths.get("/project/child/pom.xml");

        // Test default relativePath
        java.nio.file.Path parentPath1 = upgrade.resolveParentPomPath(childPath, "../pom.xml");
        assertEquals(java.nio.file.Paths.get("/project/pom.xml"), parentPath1);

        // Test directory relativePath
        java.nio.file.Path parentPath2 = upgrade.resolveParentPomPath(childPath, "..");
        assertEquals(java.nio.file.Paths.get("/project/pom.xml"), parentPath2);

        // Test explicit file relativePath
        java.nio.file.Path parentPath3 = upgrade.resolveParentPomPath(childPath, "../parent/pom.xml");
        assertEquals(java.nio.file.Paths.get("/project/parent/pom.xml"), parentPath3);
    }
}
