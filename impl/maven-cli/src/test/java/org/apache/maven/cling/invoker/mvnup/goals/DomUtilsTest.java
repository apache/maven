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

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for DomUtils functionality with domtrip backend.
 */
class DomUtilsTest {

    @Test
    void testFindChildElement() throws Exception {
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
                            <version>3.8.1</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();

        Element buildElement = DomUtils.findChildElement(root, "build");
        assertNotNull(buildElement, "Should find build element");

        Element pluginsElement = DomUtils.findChildElement(buildElement, "plugins");
        assertNotNull(pluginsElement, "Should find plugins element");
    }

    @Test
    void testInsertNewElement() throws Exception {
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
                            <version>3.8.1</version>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();
        Element buildElement = DomUtils.findChildElement(root, "build");

        Element pluginManagementElement = DomUtils.insertNewElement("pluginManagement", buildElement);
        assertNotNull(pluginManagementElement, "Should create pluginManagement element");

        // Verify it was added to the document
        String xmlOutput = DomUtils.toXml(doc);
        assertTrue(xmlOutput.contains("<pluginManagement>"), "Should contain pluginManagement element");
    }

    @Test
    void testInsertContentElement() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();

        Element descriptionElement = DomUtils.insertContentElement(root, "description", "Test project description");
        assertNotNull(descriptionElement, "Should create description element");

        // Verify it was added to the document with content
        String xmlOutput = DomUtils.toXml(doc);
        assertTrue(
                xmlOutput.contains("<description>Test project description</description>"),
                "Should contain description element with content");
    }

    @Test
    void testToXml() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        Document doc = Document.of(pomXml);

        String xmlOutput = DomUtils.toXml(doc);
        assertNotNull(xmlOutput, "Should produce XML output");
        assertTrue(xmlOutput.contains("<project"), "Should contain project element");
        assertTrue(xmlOutput.contains("<modelVersion>4.0.0</modelVersion>"), "Should contain modelVersion");
        assertTrue(xmlOutput.contains("<groupId>test</groupId>"), "Should contain groupId");
    }

    @Test
    void testElementOrderingInProject() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();

        // Insert elements that should be ordered according to ELEMENT_ORDER
        DomUtils.insertContentElement(root, "description", "Test description");
        DomUtils.insertContentElement(root, "name", "Test Project");
        DomUtils.insertNewElement("properties", root);

        String xmlOutput = DomUtils.toXml(doc);

        // Verify that elements appear in the correct order according to ELEMENT_ORDER
        int nameIndex = xmlOutput.indexOf("<name>");
        int descriptionIndex = xmlOutput.indexOf("<description>");
        int propertiesIndex = xmlOutput.indexOf("<properties>");

        assertTrue(nameIndex > 0, "Should contain name element");
        assertTrue(descriptionIndex > 0, "Should contain description element");
        assertTrue(propertiesIndex > 0, "Should contain properties element");

        // According to ELEMENT_ORDER: name should come before description, and properties should come much later
        assertTrue(nameIndex < descriptionIndex, "name should come before description");
        assertTrue(descriptionIndex < propertiesIndex, "description should come before properties");
    }

    @Test
    void testInsertElementWithCorrectPositioning() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <properties>
                    <maven.compiler.source>17</maven.compiler.source>
                </properties>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();

        // Insert elements that should be positioned according to ELEMENT_ORDER
        DomUtils.insertContentElement(root, "name", "Test Project");
        DomUtils.insertContentElement(root, "description", "Test description");
        DomUtils.insertContentElement(root, "url", "https://example.com");

        String xmlOutput = DomUtils.toXml(doc);

        // Find positions of all elements
        int modelVersionIndex = xmlOutput.indexOf("<modelVersion>");
        int groupIdIndex = xmlOutput.indexOf("<groupId>");
        int nameIndex = xmlOutput.indexOf("<name>");
        int descriptionIndex = xmlOutput.indexOf("<description>");
        int urlIndex = xmlOutput.indexOf("<url>");
        int propertiesIndex = xmlOutput.indexOf("<properties>");

        // Verify correct ordering according to ELEMENT_ORDER for project
        assertTrue(modelVersionIndex < groupIdIndex, "modelVersion should come before groupId");
        assertTrue(groupIdIndex < nameIndex, "groupId should come before name");
        assertTrue(nameIndex < descriptionIndex, "name should come before description");
        assertTrue(descriptionIndex < urlIndex, "description should come before url");
        assertTrue(urlIndex < propertiesIndex, "url should come before properties");
    }

    @Test
    void testInsertElementBetweenExistingElements() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <name>Test Project</name>
                <url>https://example.com</url>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();

        // Insert description between name and url
        DomUtils.insertContentElement(root, "description", "Test description");

        String xmlOutput = DomUtils.toXml(doc);

        int nameIndex = xmlOutput.indexOf("<name>");
        int descriptionIndex = xmlOutput.indexOf("<description>");
        int urlIndex = xmlOutput.indexOf("<url>");

        // Verify description is inserted between name and url
        assertTrue(nameIndex < descriptionIndex, "name should come before description");
        assertTrue(descriptionIndex < urlIndex, "description should come before url");
    }

    @Test
    void testInsertElementNotInOrdering() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();

        // Insert an element that's not in the ELEMENT_ORDER (should be appended at the end)
        DomUtils.insertContentElement(root, "customElement", "custom value");

        String xmlOutput = DomUtils.toXml(doc);

        int versionIndex = xmlOutput.indexOf("<version>");
        int customElementIndex = xmlOutput.indexOf("<customElement>");

        // Custom element should be appended at the end
        assertTrue(customElementIndex > versionIndex, "customElement should come after version");
        assertTrue(
                xmlOutput.contains("<customElement>custom value</customElement>"),
                "Should contain custom element with content");
    }

    @Test
    void testInsertElementInParentWithoutOrdering() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <customParent>
                    <existingChild>value</existingChild>
                </customParent>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();
        Element customParent = root.child("customParent").orElse(null);
        assertNotNull(customParent, "customParent should exist");

        // Insert element in parent that has no ordering defined
        DomUtils.insertContentElement(customParent, "newChild", "new value");

        String xmlOutput = DomUtils.toXml(doc);

        // Should be appended at the end since no ordering is defined for customParent
        assertTrue(xmlOutput.contains("<newChild>new value</newChild>"), "Should contain new child element");

        int existingChildIndex = xmlOutput.indexOf("<existingChild>");
        int newChildIndex = xmlOutput.indexOf("<newChild>");
        assertTrue(newChildIndex > existingChildIndex, "newChild should come after existingChild");
    }

    @Test
    void testInsertElementInDependency() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter</artifactId>
                        <version>5.9.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();
        Element dependencies = root.child("dependencies").orElse(null);
        assertNotNull(dependencies, "dependencies should exist");
        Element dependency = dependencies.child("dependency").orElse(null);
        assertNotNull(dependency, "dependency should exist");

        // Insert elements in dependency according to dependency ordering
        DomUtils.insertContentElement(dependency, "scope", "test");
        DomUtils.insertContentElement(dependency, "type", "jar");

        String xmlOutput = DomUtils.toXml(doc);

        // Verify dependency element ordering: groupId, artifactId, version, type, scope
        int groupIdIndex = xmlOutput.indexOf("<groupId>org.junit.jupiter</groupId>");
        int artifactIdIndex = xmlOutput.indexOf("<artifactId>junit-jupiter</artifactId>");
        int versionIndex = xmlOutput.indexOf("<version>5.9.0</version>");
        int typeIndex = xmlOutput.indexOf("<type>jar</type>");
        int scopeIndex = xmlOutput.indexOf("<scope>test</scope>");

        assertTrue(groupIdIndex < artifactIdIndex, "groupId should come before artifactId");
        assertTrue(artifactIdIndex < versionIndex, "artifactId should come before version");
        assertTrue(versionIndex < typeIndex, "version should come before type");
        assertTrue(typeIndex < scopeIndex, "type should come before scope");
    }

    @Test
    void testInsertElementInBuild() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <build>
                    <finalName>test-app</finalName>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();
        Element build = root.child("build").orElse(null);
        assertNotNull(build, "build should exist");

        // Insert elements in build according to build ordering
        DomUtils.insertContentElement(build, "directory", "target");
        DomUtils.insertContentElement(build, "sourceDirectory", "src/main/java");

        String xmlOutput = DomUtils.toXml(doc);

        // Verify build element ordering: directory, finalName, sourceDirectory, plugins
        int directoryIndex = xmlOutput.indexOf("<directory>target</directory>");
        int finalNameIndex = xmlOutput.indexOf("<finalName>test-app</finalName>");
        int sourceDirectoryIndex = xmlOutput.indexOf("<sourceDirectory>src/main/java</sourceDirectory>");
        int pluginsIndex = xmlOutput.indexOf("<plugins>");

        assertTrue(directoryIndex < finalNameIndex, "directory should come before finalName");
        assertTrue(finalNameIndex < sourceDirectoryIndex, "finalName should come before sourceDirectory");
        assertTrue(sourceDirectoryIndex < pluginsIndex, "sourceDirectory should come before plugins");
    }

    @Test
    void testInsertElementWithTextContent() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();

        // Insert elements with various text content scenarios
        DomUtils.insertContentElement(root, "name", "Test Project Name");
        DomUtils.insertContentElement(root, "description", ""); // Empty content
        DomUtils.insertContentElement(root, "url", null); // Null content
        DomUtils.insertContentElement(root, "inceptionYear", "2023");

        String xmlOutput = DomUtils.toXml(doc);

        // Verify text content handling
        assertTrue(xmlOutput.contains("<name>Test Project Name</name>"), "Should contain name with text content");
        assertTrue(
                xmlOutput.contains("<description></description>") || xmlOutput.contains("<description/>"),
                "Should contain empty description element");
        assertTrue(
                xmlOutput.contains("<url></url>") || xmlOutput.contains("<url/>"), "Should contain empty url element");
        assertTrue(
                xmlOutput.contains("<inceptionYear>2023</inceptionYear>"),
                "Should contain inceptionYear with text content");
    }

    @Test
    void testInsertNewElementWithoutContent() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();

        // Insert empty elements using insertNewElement
        Element properties = DomUtils.insertNewElement("properties", root);
        Element dependencies = DomUtils.insertNewElement("dependencies", root);

        assertNotNull(properties, "properties element should be created");
        assertNotNull(dependencies, "dependencies element should be created");

        String xmlOutput = DomUtils.toXml(doc);

        // Verify elements are created and positioned correctly
        int versionIndex = xmlOutput.indexOf("<version>1.0.0</version>");
        int propertiesIndex = xmlOutput.indexOf("<properties>");
        int dependenciesIndex = xmlOutput.indexOf("<dependencies>");

        assertTrue(versionIndex < propertiesIndex, "version should come before properties");
        assertTrue(propertiesIndex < dependenciesIndex, "properties should come before dependencies");

        // Verify elements are empty
        assertTrue(
                xmlOutput.contains("<properties></properties>") || xmlOutput.contains("<properties/>"),
                "properties should be empty");
        assertTrue(
                xmlOutput.contains("<dependencies></dependencies>") || xmlOutput.contains("<dependencies/>"),
                "dependencies should be empty");
    }

    @Test
    void testInsertMultipleElementsInCorrectOrder() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();

        // Insert elements in random order - they should be positioned correctly
        DomUtils.insertContentElement(root, "licenses", "");
        DomUtils.insertContentElement(root, "name", "Test Project");
        DomUtils.insertNewElement("dependencies", root);
        DomUtils.insertContentElement(root, "description", "A test project");
        DomUtils.insertNewElement("properties", root);
        DomUtils.insertContentElement(root, "url", "https://example.com");
        DomUtils.insertNewElement("build", root);

        String xmlOutput = DomUtils.toXml(doc);

        // Find all element positions
        int modelVersionIndex = xmlOutput.indexOf("<modelVersion>");
        int groupIdIndex = xmlOutput.indexOf("<groupId>");
        int nameIndex = xmlOutput.indexOf("<name>");
        int descriptionIndex = xmlOutput.indexOf("<description>");
        int urlIndex = xmlOutput.indexOf("<url>");
        int licensesIndex = xmlOutput.indexOf("<licenses>");
        int propertiesIndex = xmlOutput.indexOf("<properties>");
        int dependenciesIndex = xmlOutput.indexOf("<dependencies>");
        int buildIndex = xmlOutput.indexOf("<build>");

        // Verify correct ordering according to ELEMENT_ORDER
        assertTrue(modelVersionIndex < groupIdIndex, "modelVersion should come before groupId");
        assertTrue(groupIdIndex < nameIndex, "groupId should come before name");
        assertTrue(nameIndex < descriptionIndex, "name should come before description");
        assertTrue(descriptionIndex < urlIndex, "description should come before url");
        assertTrue(urlIndex < licensesIndex, "url should come before licenses");
        assertTrue(licensesIndex < propertiesIndex, "licenses should come before properties");
        assertTrue(propertiesIndex < dependenciesIndex, "properties should come before dependencies");
        assertTrue(dependenciesIndex < buildIndex, "dependencies should come before build");
    }

    @Test
    void testRemoveElement() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <name>Test Project</name>
                <description>Test description</description>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();
        Element nameElement = DomUtils.findChildElement(root, "name");

        // Test removing existing element
        DomUtils.removeElement(nameElement);

        String xmlOutput = DomUtils.toXml(doc);
        assertFalse(xmlOutput.contains("<name>Test Project</name>"), "Should not contain removed name element");
        assertTrue(
                xmlOutput.contains("<description>Test description</description>"), "Should still contain description");
    }

    @Test
    void testChildTextContent() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <name>Test Project</name>
                <description></description>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();

        // Test getting text from existing elements
        assertEquals("4.0.0", root.childText("modelVersion"), "Should get modelVersion text");
        assertEquals("test", root.childText("groupId"), "Should get groupId text");
        assertEquals("Test Project", root.childText("name"), "Should get name text");
        assertEquals("", root.childText("description"), "Should get empty description text");

        // Test getting text from non-existing element
        assertNull(root.childText("nonexistent"), "Should return null for non-existing element");
    }

    @Test
    void testAddGAVElements() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <dependencies>
                    <dependency>
                    </dependency>
                </dependencies>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();
        Element dependencies = DomUtils.findChildElement(root, "dependencies");
        Element dependency = DomUtils.findChildElement(dependencies, "dependency");

        // Test adding GAV elements with version
        DomUtils.addGAVElements(dependency, "org.example", "test-artifact", "1.0.0");

        String xmlOutput = DomUtils.toXml(doc);
        assertTrue(xmlOutput.contains("<groupId>org.example</groupId>"), "Should contain groupId");
        assertTrue(xmlOutput.contains("<artifactId>test-artifact</artifactId>"), "Should contain artifactId");
        assertTrue(xmlOutput.contains("<version>1.0.0</version>"), "Should contain version");

        // Test adding GAV elements without version
        Element dependency2 = DomUtils.insertNewElement("dependency", dependencies);
        DomUtils.addGAVElements(dependency2, "org.example", "test-artifact2", null);

        xmlOutput = DomUtils.toXml(doc);
        assertTrue(xmlOutput.contains("<artifactId>test-artifact2</artifactId>"), "Should contain second artifactId");
        assertFalse(
                xmlOutput.contains("test-artifact2</artifactId>\n        <version>"),
                "Should not add version element for null version");
    }

    @Test
    void testCreateDependency() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <dependencies>
                </dependencies>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();
        Element dependencies = DomUtils.findChildElement(root, "dependencies");

        // Test creating dependency with version
        Element dependency = DomUtils.createDependency(dependencies, "org.junit.jupiter", "junit-jupiter", "5.9.0");
        assertNotNull(dependency, "Should create dependency element");

        String xmlOutput = DomUtils.toXml(doc);
        assertTrue(xmlOutput.contains("<dependency>"), "Should contain dependency element");
        assertTrue(xmlOutput.contains("<groupId>org.junit.jupiter</groupId>"), "Should contain groupId");
        assertTrue(xmlOutput.contains("<artifactId>junit-jupiter</artifactId>"), "Should contain artifactId");
        assertTrue(xmlOutput.contains("<version>5.9.0</version>"), "Should contain version");

        // Test creating dependency without version
        Element dependency2 = DomUtils.createDependency(dependencies, "org.example", "test-lib", null);
        assertNotNull(dependency2, "Should create second dependency element");

        xmlOutput = DomUtils.toXml(doc);
        assertTrue(xmlOutput.contains("<artifactId>test-lib</artifactId>"), "Should contain second artifactId");
    }

    @Test
    void testCreatePlugin() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <build>
                    <plugins>
                    </plugins>
                </build>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();
        Element build = DomUtils.findChildElement(root, "build");
        Element plugins = DomUtils.findChildElement(build, "plugins");

        // Test creating plugin with version
        Element plugin = DomUtils.createPlugin(plugins, "org.apache.maven.plugins", "maven-compiler-plugin", "3.11.0");
        assertNotNull(plugin, "Should create plugin element");

        String xmlOutput = DomUtils.toXml(doc);
        assertTrue(xmlOutput.contains("<plugin>"), "Should contain plugin element");
        assertTrue(xmlOutput.contains("<groupId>org.apache.maven.plugins</groupId>"), "Should contain groupId");
        assertTrue(xmlOutput.contains("<artifactId>maven-compiler-plugin</artifactId>"), "Should contain artifactId");
        assertTrue(xmlOutput.contains("<version>3.11.0</version>"), "Should contain version");

        // Test creating plugin without version
        Element plugin2 = DomUtils.createPlugin(plugins, "org.example", "test-plugin", "");
        assertNotNull(plugin2, "Should create second plugin element");

        xmlOutput = DomUtils.toXml(doc);
        assertTrue(xmlOutput.contains("<artifactId>test-plugin</artifactId>"), "Should contain second artifactId");
    }

    @Test
    void testUpdateOrCreateChildElement() throws Exception {
        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0.0</version>
                <name>Old Name</name>
            </project>
            """;

        Document doc = Document.of(pomXml);
        Element root = doc.root();

        // Test updating existing element
        Element updatedName = DomUtils.updateOrCreateChildElement(root, "name", "New Name");
        assertNotNull(updatedName, "Should return updated element");

        String xmlOutput = DomUtils.toXml(doc);
        assertTrue(xmlOutput.contains("<name>New Name</name>"), "Should contain updated name");
        assertFalse(xmlOutput.contains("<name>Old Name</name>"), "Should not contain old name");

        // Test creating new element
        Element description = DomUtils.updateOrCreateChildElement(root, "description", "Test Description");
        assertNotNull(description, "Should return created element");

        xmlOutput = DomUtils.toXml(doc);
        assertTrue(xmlOutput.contains("<description>Test Description</description>"), "Should contain new description");

        // Verify element ordering is maintained
        int nameIndex = xmlOutput.indexOf("<name>");
        int descriptionIndex = xmlOutput.indexOf("<description>");
        assertTrue(nameIndex < descriptionIndex, "name should come before description");
    }
}
