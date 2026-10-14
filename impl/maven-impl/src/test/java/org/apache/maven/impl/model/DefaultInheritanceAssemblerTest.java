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
package org.apache.maven.impl.model;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.ReportPlugin;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.services.xml.XmlWriterRequest;
import org.apache.maven.impl.DefaultModelXmlFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultInheritanceAssemblerTest {

    private DefaultModelXmlFactory xmlFactory;

    private DefaultInheritanceAssembler assembler;

    @BeforeEach
    void setUp() {
        xmlFactory = new DefaultModelXmlFactory();
        assembler = new DefaultInheritanceAssembler();
    }

    private Path getPom(String name) {
        return Paths.get("../../compat/maven-model-builder/src/test/resources/poms/inheritance/" + name + ".xml");
    }

    private Model getModel(String name) throws Exception {
        return xmlFactory.read(XmlReaderRequest.builder().path(getPom(name)).build());
    }

    /**
     * An inherited plugin must still report the POM that declares it, as the location of the plugin element
     * itself is what tools use to resolve paths in the plugin configuration against the declaring POM.
     */
    @Test
    void testInheritedPluginKeepsItsLocation() throws Exception {
        Model parent = readWithLocations(
                "<project><modelVersion>4.0.0</modelVersion><groupId>g</groupId><artifactId>parent</artifactId>"
                        + "<version>1</version><packaging>pom</packaging><build><plugins><plugin>"
                        + "<artifactId>some-plugin</artifactId></plugin></plugins></build>"
                        + "<reporting><plugins><plugin><artifactId>some-report</artifactId></plugin></plugins>"
                        + "</reporting></project>",
                "parent.xml");
        Model child = readWithLocations(
                "<project><modelVersion>4.0.0</modelVersion><parent><groupId>g</groupId>"
                        + "<artifactId>parent</artifactId><version>1</version></parent>"
                        + "<artifactId>child</artifactId></project>",
                "child.xml");

        Model assembled = assembler.assembleModelInheritance(child, parent, null, null);

        Plugin plugin = assembled.getBuild().getPlugins().get(0);
        InputLocation location = plugin.getLocation("");
        assertNotNull(location);
        assertEquals(parent.getBuild().getPlugins().get(0).getLocation(""), location);
        assertEquals("g:parent:1", location.getSource().getModelId());

        ReportPlugin reportPlugin = assembled.getReporting().getPlugins().get(0);
        location = reportPlugin.getLocation("");
        assertNotNull(location);
        assertEquals(parent.getReporting().getPlugins().get(0).getLocation(""), location);
        assertEquals("g:parent:1", location.getSource().getModelId());
    }

    /**
     * A plugin that the child declares itself keeps the location of the child declaration, the location of the
     * inherited one must not replace it.
     */
    @Test
    void testChildPluginKeepsItsOwnLocation() throws Exception {
        Model parent = readWithLocations(
                "<project><modelVersion>4.0.0</modelVersion><groupId>g</groupId><artifactId>parent</artifactId>"
                        + "<version>1</version><packaging>pom</packaging><build><plugins><plugin>"
                        + "<artifactId>some-plugin</artifactId></plugin></plugins></build></project>",
                "parent.xml");
        Model child = readWithLocations(
                "<project><modelVersion>4.0.0</modelVersion><parent><groupId>g</groupId>"
                        + "<artifactId>parent</artifactId><version>1</version></parent>"
                        + "<artifactId>child</artifactId><build><plugins><plugin>"
                        + "<artifactId>some-plugin</artifactId></plugin></plugins></build></project>",
                "child.xml");

        Model assembled = assembler.assembleModelInheritance(child, parent, null, null);

        InputLocation location = assembled.getBuild().getPlugins().get(0).getLocation("");
        assertNotNull(location);
        assertEquals(child.getBuild().getPlugins().get(0).getLocation(""), location);
        assertEquals("g:child:1", location.getSource().getModelId());
    }

    private Model readWithLocations(String xml, String location) throws Exception {
        return xmlFactory.read(XmlReaderRequest.builder()
                .reader(new StringReader(xml))
                .location(location)
                .addLocationInformation(true)
                .build());
    }

    @Test
    void testPluginConfiguration() throws Exception {
        testInheritance("plugin-configuration");
    }

    public void testInheritance(String baseName) throws Exception {
        testInheritance(baseName, false);
        testInheritance(baseName, true);
    }

    public void testInheritance(String baseName, boolean fromRepo) throws Exception {
        Model parent = getModel(baseName + "-parent");
        Model child = getModel(baseName + "-child");

        if (!fromRepo) {
            // when model is built from disk, pomFile is set
            // (has consequences in inheritance algorithm since getProjectDirectory() returns non-null)
            parent = parent.withPomFile(getPom(baseName + "-parent").toAbsolutePath());
            child = child.withPomFile(getPom(baseName + "-child").toAbsolutePath());
        }

        Model assembled = assembler.assembleModelInheritance(child, parent, null, null);

        // write baseName + "-actual"
        Path actual = Paths.get(
                "target/test-classes/poms/inheritance/" + baseName + (fromRepo ? "-build" : "-repo") + "-actual.xml");
        Files.createDirectories(actual.getParent());
        xmlFactory.write(XmlWriterRequest.<Model>builder()
                .content(assembled)
                .path(actual)
                .build());

        // check with getPom( baseName + "-expected" )
        Path expected = getPom(baseName + "-expected");

        Diff diff = DiffBuilder.compare(expected.toFile())
                .withTest(actual.toFile())
                .ignoreComments()
                .ignoreWhitespace()
                .build();
        assertFalse(diff.hasDifferences(), "XML files should be identical: " + diff.toString());
    }
}
