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
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.ctc.wstx.exc.WstxEOFException;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.services.xml.XmlWriterException;
import org.apache.maven.api.services.xml.XmlWriterRequest;
import org.apache.maven.impl.model.DefaultModelProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultPluginXmlFactoryTest {

    private static final String NAME = "sample-plugin-" + randomUUID();
    private static final String SAMPLE_PLUGIN_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <plugin>
              <name>%s</name>
              <groupId>org.example</groupId>
              <artifactId>sample-plugin</artifactId>
              <version>1.0.0</version>
            </plugin>
            """.formatted(NAME);

    private final DefaultPluginXmlFactory defaultPluginXmlFactory = new DefaultPluginXmlFactory();

    @TempDir
    Path tempDir;

    @Test
    void readFromInputStreamParsesPluginDescriptorCorrectly() {
        PluginDescriptor descriptor = defaultPluginXmlFactory.read(XmlReaderRequest.builder()
                .inputStream(new ByteArrayInputStream(SAMPLE_PLUGIN_XML.getBytes()))
                .build());
        assertEquals(NAME, descriptor.getName());
        assertEquals("org.example", descriptor.getGroupId());
        assertEquals("sample-plugin", descriptor.getArtifactId());
        assertEquals("1.0.0", descriptor.getVersion());
    }

    @Test
    void parsePlugin() {
        String actualName = defaultPluginXmlFactory
                .read(XmlReaderRequest.builder()
                        .reader(new StringReader(SAMPLE_PLUGIN_XML))
                        .build())
                .getName();
        assertEquals(NAME, actualName, "Expected plugin name to be " + NAME + " but was " + actualName);
    }

    @Test
    void readFromPathParsesPluginDescriptorCorrectly() throws Exception {
        Path xmlFile = tempDir.resolve("plugin.xml");
        Files.write(xmlFile, SAMPLE_PLUGIN_XML.getBytes());
        String actualName = defaultPluginXmlFactory
                .read(XmlReaderRequest.builder().path(xmlFile).build())
                .getName();
        assertEquals(NAME, actualName, "Expected plugin name to be " + NAME + " but was " + actualName);
    }

    @Test
    void readWithNoInputThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> defaultPluginXmlFactory.read(XmlReaderRequest.builder().build()));
    }

    @Test
    void writeToWriterGeneratesValidXml() {
        StringWriter writer = new StringWriter();
        defaultPluginXmlFactory.write(XmlWriterRequest.<PluginDescriptor>builder()
                .writer(writer)
                .content(PluginDescriptor.newBuilder()
                        .name(NAME)
                        .groupId("org.example")
                        .artifactId("sample-plugin")
                        .version("1.0.0")
                        .build())
                .build());
        String output = writer.toString();
        assertTrue(
                output.contains("<name>" + NAME + "</name>"),
                "Expected " + output + " to contain " + "<name>" + NAME + "</name>");
        assertTrue(
                output.contains("<groupId>org.example</groupId>"),
                "Expected " + output + " to contain " + "<groupId>org.example</groupId>");
    }

    @Test
    void writeToOutputStreamGeneratesValidXml() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        defaultPluginXmlFactory.write(XmlWriterRequest.<PluginDescriptor>builder()
                .outputStream(outputStream)
                .content(PluginDescriptor.newBuilder().name(NAME).build())
                .build());
        String output = outputStream.toString();
        assertTrue(
                output.contains("<name>" + NAME + "</name>"),
                "Expected output to contain <name>" + NAME + "</name> but was: " + output);
    }

    @Test
    void writeToPathGeneratesValidXmlFile() throws Exception {
        Path xmlFile = tempDir.resolve("output-plugin.xml");
        defaultPluginXmlFactory.write(XmlWriterRequest.<PluginDescriptor>builder()
                .path(xmlFile)
                .content(PluginDescriptor.newBuilder().name(NAME).build())
                .build());
        String fileContent = Files.readString(xmlFile);
        assertTrue(
                fileContent.contains("<name>" + NAME + "</name>"),
                "Expected file content to contain <name>" + NAME + "</name> but was: " + fileContent);
    }

    @Test
    void fromXmlStringParsesValidXml() {
        PluginDescriptor descriptor = defaultPluginXmlFactory.fromXmlString(SAMPLE_PLUGIN_XML);
        assertEquals(
                NAME,
                descriptor.getName(),
                "Expected descriptor name to be " + NAME + " but was " + descriptor.getName());
        assertEquals(
                "org.example",
                descriptor.getGroupId(),
                "Expected descriptor groupId to be org.example but was " + descriptor.getGroupId());
        assertEquals(
                "sample-plugin",
                descriptor.getArtifactId(),
                "Expected descriptor artifactId to be sample-plugin but was " + descriptor.getArtifactId());
        assertEquals(
                "1.0.0",
                descriptor.getVersion(),
                "Expected descriptor version to be 1.0.0 but was " + descriptor.getVersion());
    }

    @Test
    void toXmlStringGeneratesValidXml() {
        String xml = defaultPluginXmlFactory.toXmlString(PluginDescriptor.newBuilder()
                .name(NAME)
                .groupId("org.example")
                .artifactId("sample-plugin")
                .version("1.0.0")
                .build());
        assertTrue(
                xml.contains("<name>" + NAME + "</name>"),
                "Expected " + xml + " to contain " + "<name>" + NAME + "</name>");
        assertTrue(
                xml.contains("<groupId>org.example</groupId>"),
                "Expected " + xml + " to contain " + "<groupId>org.example</groupId>");
        assertTrue(
                xml.contains("<artifactId>sample-plugin</artifactId>"),
                "Expected " + xml + " to contain " + "<artifactId>sample-plugin</artifactId>");
        assertTrue(
                xml.contains("<version>1.0.0</version>"),
                "Expected " + xml + " to contain " + "<version>1.0.0</version>");
    }

    @Test
    void staticFromXmlParsesValidXml() {
        PluginDescriptor descriptor = DefaultPluginXmlFactory.fromXml(SAMPLE_PLUGIN_XML);
        assertEquals(
                NAME,
                descriptor.getName(),
                "Expected descriptor name to be " + NAME + " but was " + descriptor.getName());
        assertEquals(
                "org.example",
                descriptor.getGroupId(),
                "Expected descriptor groupId to be org.example but was " + descriptor.getGroupId());
        assertEquals(
                "sample-plugin",
                descriptor.getArtifactId(),
                "Expected descriptor artifactId to be sample-plugin but was " + descriptor.getArtifactId());
        assertEquals(
                "1.0.0",
                descriptor.getVersion(),
                "Expected descriptor version to be 1.0.0 but was " + descriptor.getVersion());
    }

    @Test
    void staticToXmlGeneratesValidXml() {
        String xml = DefaultPluginXmlFactory.toXml(PluginDescriptor.newBuilder()
                .name(NAME)
                .groupId("org.example")
                .artifactId("sample-plugin")
                .version("1.0.0")
                .build());
        assertTrue(
                xml.contains("<name>" + NAME + "</name>"),
                "Expected " + xml + " to contain " + "<name>" + NAME + "</name>");
        assertTrue(
                xml.contains("<name>" + NAME + "</name>"),
                "Expected " + xml + " to contain " + "<name>" + NAME + "</name>");
        assertTrue(
                xml.contains("<groupId>org.example</groupId>"),
                "Expected " + xml + " to contain " + "<groupId>org.example</groupId>");
        assertTrue(
                xml.contains("<artifactId>sample-plugin</artifactId>"),
                "Expected " + xml + " to contain " + "<artifactId>sample-plugin</artifactId>");
        assertTrue(
                xml.contains("<version>1.0.0</version>"),
                "Expected " + xml + " to contain " + "<version>1.0.0</version>");
    }

    @Test
    void writeWithFailingWriterThrowsXmlWriterException() {
        String unableToWritePlugin = "Unable to write plugin" + randomUUID();
        String ioEx = "ioEx" + randomUUID();
        XmlWriterException exception = assertThrows(
                XmlWriterException.class,
                () -> defaultPluginXmlFactory.write(XmlWriterRequest.<PluginDescriptor>builder()
                        .writer(new Writer() {
                            @Override
                            public void write(char[] cbuf, int off, int len) {
                                throw new XmlWriterException(unableToWritePlugin, null, new IOException(ioEx));
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
        assertTrue(exception.getMessage().contains(unableToWritePlugin));
        assertInstanceOf(XmlWriterException.class, exception.getCause());
        assertEquals(ioEx, exception.getCause().getCause().getMessage());
        assertEquals(unableToWritePlugin, exception.getCause().getMessage());
    }

    @Test
    void writeWithNoTargetThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> defaultPluginXmlFactory.write(XmlWriterRequest.<PluginDescriptor>builder()
                        .content(PluginDescriptor.newBuilder()
                                .name("No Output Plugin")
                                .build())
                        .build()));
        assertEquals(
                "writer, outputStream or path must be non null",
                exception.getMessage(),
                "Expected specific error message but was: " + exception.getMessage());
    }

    @Test
    void readMalformedXmlThrowsXmlReaderException() {
        XmlReaderException exception = assertThrows(
                XmlReaderException.class,
                () -> defaultPluginXmlFactory.read(XmlReaderRequest.builder()
                        .inputStream(new ByteArrayInputStream("<plugin><name>Broken Plugin".getBytes()))
                        .build()));
        assertTrue(exception.getMessage().contains("Unable to read plugin"));
        assertInstanceOf(WstxEOFException.class, exception.getCause());
    }

    @Test
    void locateExistingPomWithFilePathShouldReturnSameFileIfRegularFile() throws IOException {
        Path pomFile = Files.createTempFile(tempDir, "pom", ".xml");
        DefaultModelProcessor processor = new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of());
        assertEquals(
                pomFile, processor.locateExistingPom(pomFile), "Expected locateExistingPom to return the same file");
    }

    @Test
    void readFromUrlParsesPluginDescriptorCorrectly() throws Exception {
        Path xmlFile = tempDir.resolve("plugin.xml");
        Files.write(xmlFile, SAMPLE_PLUGIN_XML.getBytes());
        PluginDescriptor descriptor;
        try (InputStream inputStream = xmlFile.toUri().toURL().openStream()) {
            descriptor = defaultPluginXmlFactory.read(
                    XmlReaderRequest.builder().inputStream(inputStream).build());
        }
        assertEquals(
                NAME,
                descriptor.getName(),
                "Expected descriptor name to be " + NAME + " but was " + descriptor.getName());
        assertEquals(
                "org.example",
                descriptor.getGroupId(),
                "Expected descriptor groupId to be org.example but was " + descriptor.getGroupId());
        assertEquals(
                "sample-plugin",
                descriptor.getArtifactId(),
                "Expected descriptor artifactId to be sample-plugin but was " + descriptor.getArtifactId());
        assertEquals(
                "1.0.0",
                descriptor.getVersion(),
                "Expected descriptor version to be 1.0.0 but was " + descriptor.getVersion());
    }

    @Test
    void testReadWithPath() throws Exception {
        Path tempPath = Files.createTempFile("plugin", ".xml");
        Files.writeString(tempPath, "<plugin/>");
        XmlReaderRequest request = mock(XmlReaderRequest.class);
        when(request.getPath()).thenReturn(tempPath);
        when(request.getInputStream()).thenReturn(null);
        when(request.getReader()).thenReturn(null);
        when(request.getURL()).thenReturn(null);
        when(request.isAddDefaultEntities()).thenReturn(false);
        when(request.isStrict()).thenReturn(false);
        assertNotNull(new DefaultPluginXmlFactory().read(request));
        Files.deleteIfExists(tempPath);
    }

    @Test
    void testReadWithPathUrlDefault() throws Exception {
        Path tempPath = Files.createTempFile("plugin", ".xml");
        Files.writeString(tempPath, "<plugin/>");
        XmlReaderRequest request = mock(XmlReaderRequest.class);
        when(request.getPath()).thenReturn(null);
        when(request.getInputStream()).thenReturn(null);
        when(request.getReader()).thenReturn(null);
        when(request.getURL()).thenReturn(tempPath.toUri().toURL());
        when(request.isAddDefaultEntities()).thenReturn(false);
        when(request.isStrict()).thenReturn(false);
        assertNotNull(new DefaultPluginXmlFactory().read(request));
        Files.deleteIfExists(tempPath);
    }
}
