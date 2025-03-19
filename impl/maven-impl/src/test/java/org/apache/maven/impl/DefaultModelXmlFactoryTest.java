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
package org.apache.maven.impl;

import java.io.StringReader;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultModelXmlFactoryTest {

    private DefaultModelXmlFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultModelXmlFactory();
    }

    @Test
    void testValidNamespaceWithModelVersion400() throws Exception {
        String xml =
                """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                </project>""";

        XmlReaderRequest request =
                XmlReaderRequest.builder().reader(new StringReader(xml)).build();

        Model model = factory.read(request);
        assertEquals("4.0.0", model.getModelVersion());
        assertEquals("http://maven.apache.org/POM/4.0.0", model.getNamespaceUri());
    }

    @Test
    void testValidNamespaceWithModelVersion410() throws Exception {
        String xml =
                """
                <project xmlns="http://maven.apache.org/POM/4.1.0">
                  <modelVersion>4.1.0</modelVersion>
                </project>""";

        XmlReaderRequest request =
                XmlReaderRequest.builder().reader(new StringReader(xml)).build();

        Model model = factory.read(request);
        assertEquals("4.1.0", model.getModelVersion());
        assertEquals("http://maven.apache.org/POM/4.1.0", model.getNamespaceUri());
    }

    @Test
    void testInvalidNamespaceWithModelVersion410() {
        String xml =
                """
                <project xmlns="http://invalid.namespace/4.1.0">
                  <modelVersion>4.1.0</modelVersion>
                </project>""";

        XmlReaderRequest request =
                XmlReaderRequest.builder().reader(new StringReader(xml)).build();

        XmlReaderException ex = assertThrows(XmlReaderException.class, () -> factory.read(request));
        assertTrue(ex.getMessage().contains("Invalid namespace 'http://invalid.namespace/4.1.0'"));
        assertTrue(ex.getMessage().contains("4.1.0"));
    }

    @Test
    void testNoNamespaceWithModelVersion400() throws Exception {
        String xml =
                """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                </project>""";

        XmlReaderRequest request =
                XmlReaderRequest.builder().reader(new StringReader(xml)).build();

        Model model = factory.read(request);
        assertEquals("4.0.0", model.getModelVersion());
        assertEquals("", model.getNamespaceUri());
    }

    @Test
    void testNullRequest() {
        assertThrows(IllegalArgumentException.class, () -> factory.read((XmlReaderRequest) null));
    }

    @Test
    void testMalformedModelVersion() throws Exception {
        String xml =
                """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>invalid.version</modelVersion>
                </project>""";

        XmlReaderRequest request =
                XmlReaderRequest.builder().reader(new StringReader(xml)).build();

        Model model = factory.read(request);
        assertEquals("invalid.version", model.getModelVersion());
    }
}
