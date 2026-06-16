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

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.services.xml.XmlWriterRequest;
import org.apache.maven.impl.DefaultModelXmlFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

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
