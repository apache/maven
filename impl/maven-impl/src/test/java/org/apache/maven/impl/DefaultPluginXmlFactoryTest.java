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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.services.xml.XmlWriterException;
import org.apache.maven.api.services.xml.XmlWriterRequest;
import org.apache.maven.impl.model.DefaultModelProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DefaultPluginXmlFactoryReadWriteTest {

    private static final String SAMPLE_PLUGIN_XML =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <plugin>
              <name>Sample Plugin</name>
              <groupId>org.example</groupId>
              <artifactId>sample-plugin</artifactId>
              <version>1.0.0</version>
            </plugin>
            """;

    private final DefaultPluginXmlFactory sut = new DefaultPluginXmlFactory();

    @TempDir
    Path tempDir;

    @Test
    void readFromInputStreamParsesPluginDescriptorCorrectly() {
        PluginDescriptor descriptor = sut.read(XmlReaderRequest.builder()
                .inputStream(new ByteArrayInputStream(SAMPLE_PLUGIN_XML.getBytes()))
                .build());
        assertEquals("Sample Plugin", descriptor.getName());
        assertEquals("org.example", descriptor.getGroupId());
        assertEquals("sample-plugin", descriptor.getArtifactId());
        assertEquals("1.0.0", descriptor.getVersion());
    }

    @Test
    void readFromReaderParsesPluginDescriptorCorrectly() {
        assertEquals(
                "Sample Plugin",
                sut.read(XmlReaderRequest.builder()
                                .reader(new StringReader(SAMPLE_PLUGIN_XML))
                                .build())
                        .getName());
    }

    @Test
    void readFromPathParsesPluginDescriptorCorrectly(@TempDir Path tempDir) throws Exception {
        Path xmlFile = tempDir.resolve("plugin.xml");
        Files.write(xmlFile, SAMPLE_PLUGIN_XML.getBytes());
        assertEquals(
                "Sample Plugin",
                sut.read(XmlReaderRequest.builder().path(xmlFile).build()).getName());
    }

    @Test
    void readWithNoInputThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> sut.read(XmlReaderRequest.builder().build()));
    }

    @Test
    void writeToWriterGeneratesValidXml() {
        StringWriter writer = new StringWriter();
        sut.write(XmlWriterRequest.<PluginDescriptor>builder()
                .writer(writer)
                .content(PluginDescriptor.newBuilder()
                        .name("Sample Plugin")
                        .groupId("org.example")
                        .artifactId("sample-plugin")
                        .version("1.0.0")
                        .build())
                .build());
        String output = writer.toString();
        assertTrue(output.contains("<name>Sample Plugin</name>"));
        assertTrue(output.contains("<groupId>org.example</groupId>"));
    }

    @Test
    void writeToOutputStreamGeneratesValidXml() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        sut.write(XmlWriterRequest.<PluginDescriptor>builder()
                .outputStream(outputStream)
                .content(PluginDescriptor.newBuilder().name("Sample Plugin").build())
                .build());
        assertTrue(outputStream.toString().contains("<name>Sample Plugin</name>"));
    }

    @Test
    void writeToPathGeneratesValidXmlFile(@TempDir Path tempDir) throws Exception {
        Path xmlFile = tempDir.resolve("output-plugin.xml");
        sut.write(XmlWriterRequest.<PluginDescriptor>builder()
                .path(xmlFile)
                .content(PluginDescriptor.newBuilder().name("Sample Plugin").build())
                .build());
        assertTrue(Files.readString(xmlFile).contains("<name>Sample Plugin</name>"));
    }

    @Test
    void fromXmlStringParsesValidXml() {
        PluginDescriptor descriptor = sut.fromXmlString(SAMPLE_PLUGIN_XML);
        assertEquals("Sample Plugin", descriptor.getName());
        assertEquals("org.example", descriptor.getGroupId());
        assertEquals("sample-plugin", descriptor.getArtifactId());
        assertEquals("1.0.0", descriptor.getVersion());
    }

    @Test
    void toXmlStringGeneratesValidXml() {
        String xml = sut.toXmlString(PluginDescriptor.newBuilder()
                .name("Sample Plugin")
                .groupId("org.example")
                .artifactId("sample-plugin")
                .version("1.0.0")
                .build());
        assertTrue(xml.contains("<name>Sample Plugin</name>"));
        assertTrue(xml.contains("<groupId>org.example</groupId>"));
        assertTrue(xml.contains("<artifactId>sample-plugin</artifactId>"));
        assertTrue(xml.contains("<version>1.0.0</version>"));
    }

    @Test
    void staticFromXmlParsesValidXml() {
        PluginDescriptor descriptor = DefaultPluginXmlFactory.fromXml(SAMPLE_PLUGIN_XML);
        assertEquals("Sample Plugin", descriptor.getName());
        assertEquals("org.example", descriptor.getGroupId());
        assertEquals("sample-plugin", descriptor.getArtifactId());
        assertEquals("1.0.0", descriptor.getVersion());
    }

    @Test
    void staticToXmlGeneratesValidXml() {
        String xml = DefaultPluginXmlFactory.toXml(PluginDescriptor.newBuilder()
                .name("Sample Plugin")
                .groupId("org.example")
                .artifactId("sample-plugin")
                .version("1.0.0")
                .build());
        assertTrue(xml.contains("<name>Sample Plugin</name>"));
        assertTrue(xml.contains("<groupId>org.example</groupId>"));
        assertTrue(xml.contains("<artifactId>sample-plugin</artifactId>"));
        assertTrue(xml.contains("<version>1.0.0</version>"));
    }

    @Test
    void writeWithFailingWriterThrowsXmlWriterException() {
        XmlWriterException exception = assertThrows(
                XmlWriterException.class,
                () -> sut.write(XmlWriterRequest.<PluginDescriptor>builder()
                        .writer(new Writer() {
                            @Override
                            public void write(char[] cbuf, int off, int len) {
                                throw new RuntimeException("Simulated failure");
                            }

                            @Override
                            public void flush() {}

                            @Override
                            public void close() {}
                        })
                        .content(PluginDescriptor.newBuilder()
                                .name("Failing Plugin")
                                .build())
                        .build()));
        assertTrue(exception.getMessage().contains("Unable to write plugin"));
        assertInstanceOf(RuntimeException.class, exception.getCause());
    }

    @Test
    void writeWithNoTargetThrowsIllegalArgumentException() {
        assertEquals(
                "writer, outputStream or path must be non null",
                assertThrows(
                                IllegalArgumentException.class,
                                () -> sut.write(XmlWriterRequest.<PluginDescriptor>builder()
                                        .content(PluginDescriptor.newBuilder()
                                                .name("No Output Plugin")
                                                .build())
                                        .build()))
                        .getMessage());
    }

    @Test
    void readMalformedXmlThrowsXmlReaderException() {
        XmlReaderException exception = assertThrows(
                XmlReaderException.class,
                () -> sut.read(XmlReaderRequest.builder()
                        .inputStream(new ByteArrayInputStream("<plugin><name>Broken Plugin".getBytes()))
                        .build()));
        assertTrue(exception.getMessage().contains("Unable to read plugin"));
        assertInstanceOf(Exception.class, exception.getCause());
    }

    @Test
    void locateExistingPomWithFilePathShouldReturnSameFileIfRegularFile() throws IOException {
        Path pomFile = Files.createTempFile(tempDir, "pom", ".xml");
        DefaultModelProcessor processor = new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of());
        assertEquals(pomFile, processor.locateExistingPom(pomFile));
    }

    @Test
    void readFromUrlParsesPluginDescriptorCorrectly(@TempDir Path tempDir) throws Exception {
        Path xmlFile = tempDir.resolve("plugin.xml");
        Files.write(xmlFile, SAMPLE_PLUGIN_XML.getBytes());
        URL url = xmlFile.toUri().toURL();

        // Create request with URL using reflection since builder doesn't have url() method
        XmlReaderRequest request =
                XmlReaderRequest.builder().inputStream(url.openStream()).build();

        PluginDescriptor descriptor = sut.read(request);

        assertEquals("Sample Plugin", descriptor.getName());
        assertEquals("org.example", descriptor.getGroupId());
        assertEquals("sample-plugin", descriptor.getArtifactId());
        assertEquals("1.0.0", descriptor.getVersion());
    }
}
