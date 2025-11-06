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
package org.apache.maven.model.v4;

import javax.xml.stream.XMLStreamException;

import java.io.StringReader;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenStaxReaderTest {

    @Test
    void testNamespaceReporting() throws Exception {
        String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "</project>";

        Model model = fromXml(xml);
        assertEquals("http://maven.apache.org/POM/4.0.0", model.getNamespaceUri());
    }

    @Test
    void testEmptyNamespaceReporting() throws Exception {
        String xml = "<project>\n" + "  <modelVersion>4.0.0</modelVersion>\n" + "</project>";

        Model model = fromXml(xml);
        assertEquals("", model.getNamespaceUri());
    }

    @Test
    void testNamespaceConsistency() throws XMLStreamException {
        String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "  <build xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "    <plugins>\n"
                + "      <plugin>\n"
                + "        <artifactId>maven-test-plugin</artifactId>\n"
                + "      </plugin>\n"
                + "    </plugins>\n"
                + "  </build>\n"
                + "</project>";

        Model model = fromXml(xml);
        assertEquals("http://maven.apache.org/POM/4.0.0", model.getNamespaceUri());
    }

    @Test
    void testNamespaceInconsistencyThrows() {
        String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "  <build xmlns=\"http://maven.apache.org/POM/4.1.0\">\n"
                + "    <plugins>\n"
                + "      <plugin>\n"
                + "        <artifactId>maven-test-plugin</artifactId>\n"
                + "      </plugin>\n"
                + "    </plugins>\n"
                + "  </build>\n"
                + "</project>";

        XMLStreamException ex = assertThrows(XMLStreamException.class, () -> fromXml(xml));
        assertTrue(ex.getMessage().contains("Unexpected namespace for element 'build'"));
        assertTrue(ex.getMessage().contains("found 'http://maven.apache.org/POM/4.1.0'"));
        assertTrue(ex.getMessage().contains("expected 'http://maven.apache.org/POM/4.0.0'"));
    }

    @Test
    void testEmptyNamespaceConsistency() throws XMLStreamException {
        String xml = "<project>\n"
                + "  <build>\n"
                + "    <plugins>\n"
                + "      <plugin>\n"
                + "        <artifactId>maven-test-plugin</artifactId>\n"
                + "      </plugin>\n"
                + "    </plugins>\n"
                + "  </build>\n"
                + "</project>";

        Model model = fromXml(xml);
        assertEquals("", model.getNamespaceUri());
    }

    @Test
    void testEmptyNamespaceInconsistencyThrows() {
        String xml = "<project>\n"
                + "  <build xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "    <plugins>\n"
                + "      <plugin>\n"
                + "        <artifactId>maven-test-plugin</artifactId>\n"
                + "      </plugin>\n"
                + "    </plugins>\n"
                + "  </build>\n"
                + "</project>";

        XMLStreamException ex = assertThrows(XMLStreamException.class, () -> fromXml(xml));
        assertTrue(ex.getMessage().contains("Unexpected namespace for element 'build'"));
        assertTrue(ex.getMessage().contains("found 'http://maven.apache.org/POM/4.0.0'"));
        assertTrue(ex.getMessage().contains("expected ''"));
    }

    @Test
    void testPluginConfigurationAllowsOtherNamespaces() throws XMLStreamException {
        String xml = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
                + "  <build>\n"
                + "    <plugins>\n"
                + "      <plugin>\n"
                + "        <artifactId>maven-test-plugin</artifactId>\n"
                + "        <configuration>\n"
                + "          <customConfig xmlns:custom=\"http://custom.namespace.org\">\n"
                + "            <custom:element>value</custom:element>\n"
                + "          </customConfig>\n"
                + "        </configuration>\n"
                + "      </plugin>\n"
                + "    </plugins>\n"
                + "  </build>\n"
                + "</project>";

        Model model = fromXml(xml);
        assertNotNull(model);
        assertEquals("http://maven.apache.org/POM/4.0.0", model.getNamespaceUri());
    }

    @Test
    void testLocationReportingForElements() throws Exception {
        String xml = "<project>\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>org.example</groupId>\n"
                + "  <artifactId>test-artifact</artifactId>\n"
                + "  <version>1.0.0</version>\n"
                + "  <dependencies>\n"
                + "    <dependency>\n"
                + "      <groupId>junit</groupId>\n"
                + "      <artifactId>junit</artifactId>\n"
                + "      <version>4.13.2</version>\n"
                + "    </dependency>\n"
                + "  </dependencies>\n"
                + "</project>";

        MavenStaxReader reader = new MavenStaxReader();
        reader.setAddLocationInformation(true);
        Model model = reader.read(new StringReader(xml), true, InputSource.of("test.xml"));

        // Check root element location - should point to <project> tag on line 1, column 1
        InputLocation projectLocation = model.getLocation("");
        assertNotNull(projectLocation, "Project location should not be null");
        assertEquals(1, projectLocation.getLineNumber(), "Project should start at line 1");
        assertEquals(1, projectLocation.getColumnNumber(), "Project should start at column 1");

        // Check modelVersion location - should point to <modelVersion> tag on line 2, column 3
        InputLocation modelVersionLocation = model.getLocation("modelVersion");
        assertNotNull(modelVersionLocation, "ModelVersion location should not be null");
        assertEquals(2, modelVersionLocation.getLineNumber(), "ModelVersion should start at line 2");
        assertEquals(3, modelVersionLocation.getColumnNumber(), "ModelVersion should start at column 3");

        // Check groupId location - should point to <groupId> tag on line 3, column 3
        InputLocation groupIdLocation = model.getLocation("groupId");
        assertNotNull(groupIdLocation, "GroupId location should not be null");
        assertEquals(3, groupIdLocation.getLineNumber(), "GroupId should start at line 3");
        assertEquals(3, groupIdLocation.getColumnNumber(), "GroupId should start at column 3");

        // Check dependencies location - should point to <dependencies> tag on line 6, column 3
        InputLocation dependenciesLocation = model.getLocation("dependencies");
        assertNotNull(dependenciesLocation, "Dependencies location should not be null");
        assertEquals(6, dependenciesLocation.getLineNumber(), "Dependencies should start at line 6");
        assertEquals(3, dependenciesLocation.getColumnNumber(), "Dependencies should start at column 3");

        // Check dependency location - should point to <dependency> tag on line 7, column 5
        Dependency dependency = model.getDependencies().get(0);
        InputLocation dependencyLocation = dependency.getLocation("");
        assertNotNull(dependencyLocation, "Dependency location should not be null");
        assertEquals(7, dependencyLocation.getLineNumber(), "Dependency should start at line 7");
        assertEquals(5, dependencyLocation.getColumnNumber(), "Dependency should start at column 5");

        // Check dependency groupId location - should point to <groupId> tag on line 8, column 7
        InputLocation depGroupIdLocation = dependency.getLocation("groupId");
        assertNotNull(depGroupIdLocation, "Dependency groupId location should not be null");
        assertEquals(8, depGroupIdLocation.getLineNumber(), "Dependency groupId should start at line 8");
        assertEquals(7, depGroupIdLocation.getColumnNumber(), "Dependency groupId should start at column 7");
    }

    @Test
    void testLocationReportingForAttributes() throws Exception {
        String xml = "<project root=\"true\">\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <groupId>org.example</groupId>\n"
                + "  <artifactId>test-artifact</artifactId>\n"
                + "  <version>1.0.0</version>\n"
                + "  <scm child.scm.connection.inherit.append.path=\"false\">\n"
                + "    <connection>scm:git:https://github.com/example/repo.git</connection>\n"
                + "  </scm>\n"
                + "</project>";

        MavenStaxReader reader = new MavenStaxReader();
        reader.setAddLocationInformation(true);
        Model model = reader.read(new StringReader(xml), true, InputSource.of("test.xml"));

        // Check project root attribute - attributes get the location of their containing element
        // since XMLStreamReader doesn't provide individual attribute positions
        InputLocation rootLocation = model.getLocation("root");
        assertNotNull(rootLocation, "Root attribute location should not be null");
        assertEquals(1, rootLocation.getLineNumber(), "Root attribute should be on line 1 (element line)");
        assertEquals(1, rootLocation.getColumnNumber(), "Root attribute should point to column 1 (element column)");
        assertTrue(model.isRoot(), "Root should be true");

        // Check scm element location
        InputLocation scmLocation = model.getScm().getLocation("");
        assertNotNull(scmLocation, "SCM location should not be null");
        assertEquals(6, scmLocation.getLineNumber(), "SCM should start at line 6");
        assertEquals(3, scmLocation.getColumnNumber(), "SCM should start at column 3");

        // Check scm child.scm.connection.inherit.append.path attribute
        // Like all attributes, it gets the location of its containing element
        InputLocation scmInheritLocation = model.getScm().getLocation("child.scm.connection.inherit.append.path");
        assertNotNull(scmInheritLocation, "SCM inherit attribute location should not be null");
        assertEquals(6, scmInheritLocation.getLineNumber(), "SCM inherit attribute should be on line 6 (element line)");
        assertEquals(
                3,
                scmInheritLocation.getColumnNumber(),
                "SCM inherit attribute should point to column 3 (element column)");
        assertEquals("false", model.getScm().getChildScmConnectionInheritAppendPath());
    }

    @Test
    void testLocationReportingForListElements() throws Exception {
        String xml = "<project>\n"
                + "  <modelVersion>4.0.0</modelVersion>\n"
                + "  <modules>\n"
                + "    <module>module1</module>\n"
                + "    <module>module2</module>\n"
                + "    <module>module3</module>\n"
                + "  </modules>\n"
                + "</project>";

        MavenStaxReader reader = new MavenStaxReader();
        reader.setAddLocationInformation(true);
        Model model = reader.read(new StringReader(xml), true, InputSource.of("test.xml"));

        // Check modules location - should point to <modules> tag on line 3, column 3
        InputLocation modulesLocation = model.getLocation("modules");
        assertNotNull(modulesLocation, "Modules location should not be null");
        assertEquals(3, modulesLocation.getLineNumber(), "Modules should start at line 3");
        assertEquals(3, modulesLocation.getColumnNumber(), "Modules should start at column 3");

        // Check individual module locations
        InputLocation module1Location = modulesLocation.getLocation(0);
        assertNotNull(module1Location, "Module 1 location should not be null");
        assertEquals(4, module1Location.getLineNumber(), "Module 1 should start at line 4");
        assertEquals(5, module1Location.getColumnNumber(), "Module 1 should start at column 5");

        InputLocation module2Location = modulesLocation.getLocation(1);
        assertNotNull(module2Location, "Module 2 location should not be null");
        assertEquals(5, module2Location.getLineNumber(), "Module 2 should start at line 5");
        assertEquals(5, module2Location.getColumnNumber(), "Module 2 should start at column 5");

        InputLocation module3Location = modulesLocation.getLocation(2);
        assertNotNull(module3Location, "Module 3 location should not be null");
        assertEquals(6, module3Location.getLineNumber(), "Module 3 should start at line 6");
        assertEquals(5, module3Location.getColumnNumber(), "Module 3 should start at column 5");
    }

    private Model fromXml(String xml) throws XMLStreamException {
        MavenStaxReader reader = new MavenStaxReader();
        return reader.read(new StringReader(xml), true, null);
    }
}
