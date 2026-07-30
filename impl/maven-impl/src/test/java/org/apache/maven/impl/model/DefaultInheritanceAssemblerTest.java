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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.services.xml.XmlWriterRequest;
import org.apache.maven.impl.DefaultModelXmlFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    @Test
    void testPluginConfiguration() throws Exception {
        testInheritance("plugin-configuration");
    }

    /**
     * Check most classical urls inheritance: directory structure where parent POM in parent directory
     * and child directory == artifactId
     */
    @Test
    void testUrls() throws Exception {
        testInheritance("urls");
    }

    /**
     * Flat directory structure: parent & child POMs in sibling directories, child directory == artifactId.
     */
    @Test
    void testFlatUrls() throws Exception {
        testInheritance("flat-urls");
    }

    /**
     * MNG-5951 MNG-6059 child.x.y.inherit.append.path="false" test
     */
    @Test
    void testNoAppendUrls() throws Exception {
        testInheritance("no-append-urls");
    }

    /**
     * MNG-5951 special case test: inherit with partial override
     */
    @Test
    void testNoAppendUrls2() throws Exception {
        testInheritance("no-append-urls2");
    }

    /**
     * MNG-5951 special case test: child.x.y.inherit.append.path="true" in child should not reset content
     */
    @Test
    void testNoAppendUrls3() throws Exception {
        testInheritance("no-append-urls3");
    }

    @Test
    void testWithEmptyUrl() throws Exception {
        testInheritance("empty-urls", false);
    }

    @Test
    void testModulePathNotArtifactId() throws Exception {
        Model parent = getModel("module-path-not-artifactId-parent");
        Model child = getModel("module-path-not-artifactId-child");

        Model assembled = assembler.assembleModelInheritance(child, parent, null, null);

        Path actual = Paths.get("target/test-classes/poms/inheritance/module-path-not-artifactId-actual.xml");
        Files.createDirectories(actual.getParent());
        xmlFactory.write(XmlWriterRequest.<Model>builder()
                .content(assembled)
                .path(actual)
                .build());

        Path expected = getPom("module-path-not-artifactId-expected");

        Diff diff = DiffBuilder.compare(expected.toFile())
                .withTest(actual.toFile())
                .ignoreComments()
                .ignoreWhitespace()
                .build();
        assertFalse(diff.hasDifferences(), "XML files should be identical: " + diff.toString());
    }

    public void testInheritance(String baseName) throws Exception {
        testInheritance(baseName, false);
        testInheritance(baseName, true);
    }

    public void testInheritance(String baseName, boolean fromRepo) throws Exception {
        Model parent = getModel(baseName + "-parent");
        Model child = getModel(baseName + "-child");

        if (!fromRepo) {
            parent = parent.withPomFile(getPom(baseName + "-parent").toAbsolutePath());
            child = child.withPomFile(getPom(baseName + "-child").toAbsolutePath());
        }

        Model assembled = assembler.assembleModelInheritance(child, parent, null, null);

        Path actual = Paths.get(
                "target/test-classes/poms/inheritance/" + baseName + (fromRepo ? "-build" : "-repo") + "-actual.xml");
        Files.createDirectories(actual.getParent());
        xmlFactory.write(XmlWriterRequest.<Model>builder()
                .content(assembled)
                .path(actual)
                .build());

        Path expected = getPom(baseName + "-expected");

        Diff diff = DiffBuilder.compare(expected.toFile())
                .withTest(actual.toFile())
                .ignoreComments()
                .ignoreWhitespace()
                .build();
        assertFalse(diff.hasDifferences(), "XML files should be identical: " + diff.toString());
    }

    @Test
    void testAssembleWithNullArtifactIdDoesNotThrowNpe() {
        Model parent = Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("test")
                .artifactId("parent")
                .version("1.0")
                .build();

        Model child = Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("test")
                .version("1.0")
                .build();

        assertDoesNotThrow(() -> assembler.assembleModelInheritance(child, parent, null, null));
    }

    @Test
    void testAssembleWithRootProjectDirectoryDoesNotThrowNpe() {
        Model parent = Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("test")
                .artifactId("parent")
                .version("1.0")
                .build();

        Model child = Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("test")
                .artifactId("child")
                .version("1.0")
                .pomFile(Paths.get("/pom.xml"))
                .build();

        assertDoesNotThrow(() -> assembler.assembleModelInheritance(child, parent, null, null));
    }

    @Test
    void testAssembleWithNullArtifactIdAndRootProjectDirectoryDoesNotThrowNpe() {
        Model parent = Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("test")
                .artifactId("parent")
                .version("1.0")
                .modules(List.of("../child/pom.xml"))
                .build();

        Model child = Model.newBuilder()
                .modelVersion("4.0.0")
                .groupId("test")
                .version("1.0")
                .pomFile(Paths.get("/pom.xml"))
                .build();

        assertDoesNotThrow(() -> assembler.assembleModelInheritance(child, parent, null, null));
    }
}
