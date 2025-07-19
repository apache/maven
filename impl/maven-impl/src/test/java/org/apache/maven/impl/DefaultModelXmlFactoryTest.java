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

import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.services.xml.XmlWriterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultModelXmlFactoryTest {

    private DefaultModelXmlFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultModelXmlFactory();
    }

    @Test
    void testValidNamespaceWithModelVersion400() {
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
    void testValidNamespaceWithModelVersion410() {
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
    void testNoNamespaceWithModelVersion400() {
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
        assertThrows(NullPointerException.class, () -> factory.read((XmlReaderRequest) null));
    }

    @Test
    void testMalformedModelVersion() {
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

    @Test
    void testWriteModelWithoutInputLocationTracking() {
        String model =
                """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                    <groupId>org.example</groupId>
                    <artifactId>example-artifact</artifactId>
                    <version>1.0.0</version>
                    <name>Example Project</name>
                    <properties>
                        <example.property>value</example.property>
                    </properties>
                </project>""";

        String expected =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.example</groupId>
                  <artifactId>example-artifact</artifactId>
                  <version>1.0.0</version>
                  <name>Example Project</name>
                  <properties>
                    <example.property>value</example.property>
                  </properties>
                </project>""";

        final StringWriter writer = new StringWriter();
        XmlWriterRequest<Model> request = XmlWriterRequest.<Model>builder()
                .content(factory.read(XmlReaderRequest.builder()
                        .reader(new StringReader(model))
                        .build()))
                .writer(writer)
                .build();

        factory.write(request);

        final String processedPomAsString = writer.toString();
        assertThat(processedPomAsString).isEqualTo(expected);
    }

    @Test
    void testWriteModelWithInputLocationTracking() {
        String model =
                """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                    <groupId>org.example</groupId>
                    <artifactId>example-artifact</artifactId>
                    <version>1.0.0</version>
                    <name>Example Project</name>
                    <properties>
                        <example.property>value</example.property>
                    </properties>
                </project>""";

        String expected =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion><!--some formatter: n/a @ 2:3-->
                  <groupId>org.example</groupId><!--some formatter: n/a @ 3:5-->
                  <artifactId>example-artifact</artifactId><!--some formatter: n/a @ 4:5-->
                  <version>1.0.0</version><!--some formatter: n/a @ 5:5-->
                  <name>Example Project</name><!--some formatter: n/a @ 6:5-->
                  <properties>
                    <example.property>value</example.property><!--some formatter: n/a @ 8:32-->
                  </properties>
                </project>""";

        final StringWriter writer = new StringWriter();
        XmlWriterRequest<Model> request = XmlWriterRequest.<Model>builder()
                .content(factory.read(XmlReaderRequest.builder()
                        .reader(new StringReader(model))
                        .build()))
                .writer(writer)
                .inputLocationFormatter((x) -> "some formatter: " + (InputLocation) x)
                .build();

        factory.write(request);

        final String processedPomAsString = writer.toString();
        assertThat(processedPomAsString).isEqualTo(expected);
    }
}
