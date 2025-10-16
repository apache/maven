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
import java.io.StringWriter;
import java.util.function.Function;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.services.xml.XmlWriterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultModelXmlFactoryTest {

    private DefaultModelXmlFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultModelXmlFactory();
    }

    @Test
    void testValidNamespaceWithModelVersion400() throws Exception {
        String xml = """
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
        String xml = """
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
        String xml = """
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
        String xml = """
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
        assertThrows(NullPointerException.class, () -> factory.read((XmlReaderRequest) null));
    }

    @Test
    void testMalformedModelVersion() throws Exception {
        String xml = """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>invalid.version</modelVersion>
                </project>""";

        XmlReaderRequest request =
                XmlReaderRequest.builder().reader(new StringReader(xml)).build();

        Model model = factory.read(request);
        assertEquals("invalid.version", model.getModelVersion());
    }

    @Test
    void testWriteWithoutFormatterDisablesLocationTracking() throws Exception {
        // minimal valid model we can round-trip
        String xml = """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>g</groupId>
                  <artifactId>a</artifactId>
                  <version>1</version>
                </project>""";

        Model model = factory.read(XmlReaderRequest.builder()
                .reader(new StringReader(xml))
                .strict(true)
                .build());

        StringWriter out = new StringWriter();
        factory.write(XmlWriterRequest.<Model>builder()
                .writer(out)
                .content(model)
                // no formatter -> tracking should be OFF
                .build());

        String result = out.toString();
        assertFalse(result.contains("LOC_MARK"), "Unexpected marker found in output");
    }

    @Test
    void testWriteWithFormatterEnablesLocationTracking() throws Exception {
        String xml = """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>g</groupId>
                  <artifactId>a</artifactId>
                  <version>1</version>
                </project>""";

        Model model = factory.read(XmlReaderRequest.builder()
                .reader(new StringReader(xml))
                .strict(true)
                .build());

        StringWriter out = new StringWriter();
        Function<Object, String> formatter = o -> "LOC_MARK";

        factory.write(XmlWriterRequest.<Model>builder()
                .writer(out)
                .content(model)
                .inputLocationFormatter(formatter)
                .build());

        String result = out.toString();
        // Presence of our formatter's output proves tracking was enabled and formatter applied
        assertTrue(result.contains("LOC_MARK"), "Expected formatter marker in output when tracking is enabled");
    }
}
