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
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.io.TempDir;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultPluginXmlFactoryReadWriteTest {

    private static final String NAME = "sample-plugin-" + randomUUID();
    private static final String SAMPLE_PLUGIN_XML =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <plugin>
              <name>%s</name>
              <groupId>org.example</groupId>
              <artifactId>sample-plugin</artifactId>
              <version>1.0.0</version>
            </plugin>
            """
                    .formatted(NAME);

    private final DefaultPluginXmlFactory defaultPluginXmlFactory = new DefaultPluginXmlFactory();

    @TempDir
    Path tempDir;

    @Test
    void readFromInputStreamParsesPluginDescriptorCorrectly() {
        PluginDescriptor descriptor = defaultPluginXmlFactory.read(XmlReaderRequest.builder()
                .inputStream(new ByteArrayInputStream(SAMPLE_PLUGIN_XML.getBytes()))
                .build());
        assertThat(descriptor.getName()).isEqualTo(NAME);
        assertThat(descriptor.getGroupId()).isEqualTo("org.example");
        assertThat(descriptor.getArtifactId()).isEqualTo("sample-plugin");
        assertThat(descriptor.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void parsePlugin() {
        assertThat(defaultPluginXmlFactory
                        .read(XmlReaderRequest.builder()
                                .reader(new StringReader(SAMPLE_PLUGIN_XML))
                                .build())
                        .getName())
                .isEqualTo(NAME);
    }

    @Test
    void readFromPathParsesPluginDescriptorCorrectly() throws Exception {
        Path xmlFile = tempDir.resolve("plugin.xml");
        Files.write(xmlFile, SAMPLE_PLUGIN_XML.getBytes());
        assertThat(defaultPluginXmlFactory
                        .read(XmlReaderRequest.builder().path(xmlFile).build())
                        .getName())
                .isEqualTo(NAME);
    }

    @Test
    void readWithNoInputThrowsIllegalArgumentException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() ->
                        defaultPluginXmlFactory.read(XmlReaderRequest.builder().build()));
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
        assertThat(output).contains("<name>" + NAME + "</name>");
        assertThat(output).contains("<groupId>org.example</groupId>");
    }

    @Test
    void writeToOutputStreamGeneratesValidXml() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        defaultPluginXmlFactory.write(XmlWriterRequest.<PluginDescriptor>builder()
                .outputStream(outputStream)
                .content(PluginDescriptor.newBuilder().name(NAME).build())
                .build());
        assertThat(outputStream.toString()).contains("<name>" + NAME + "</name>");
    }

    @Test
    void writeToPathGeneratesValidXmlFile() throws Exception {
        Path xmlFile = tempDir.resolve("output-plugin.xml");
        defaultPluginXmlFactory.write(XmlWriterRequest.<PluginDescriptor>builder()
                .path(xmlFile)
                .content(PluginDescriptor.newBuilder().name(NAME).build())
                .build());
        assertThat(Files.readString(xmlFile)).contains("<name>" + NAME + "</name>");
    }

    @Test
    void fromXmlStringParsesValidXml() {
        PluginDescriptor descriptor = defaultPluginXmlFactory.fromXmlString(SAMPLE_PLUGIN_XML);
        assertThat(descriptor.getName()).isEqualTo(NAME);
        assertThat(descriptor.getGroupId()).isEqualTo("org.example");
        assertThat(descriptor.getArtifactId()).isEqualTo("sample-plugin");
        assertThat(descriptor.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void toXmlStringGeneratesValidXml() {
        String xml = defaultPluginXmlFactory.toXmlString(PluginDescriptor.newBuilder()
                .name(NAME)
                .groupId("org.example")
                .artifactId("sample-plugin")
                .version("1.0.0")
                .build());
        assertThat(xml).contains("<name>" + NAME + "</name>");
        assertThat(xml).contains("<groupId>org.example</groupId>");
        assertThat(xml).contains("<artifactId>sample-plugin</artifactId>");
        assertThat(xml).contains("<version>1.0.0</version>");
    }

    @Test
    void staticFromXmlParsesValidXml() {
        PluginDescriptor descriptor = DefaultPluginXmlFactory.fromXml(SAMPLE_PLUGIN_XML);
        assertThat(descriptor.getName()).isEqualTo(NAME);
        assertThat(descriptor.getGroupId()).isEqualTo("org.example");
        assertThat(descriptor.getArtifactId()).isEqualTo("sample-plugin");
        assertThat(descriptor.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void staticToXmlGeneratesValidXml() {
        String xml = DefaultPluginXmlFactory.toXml(PluginDescriptor.newBuilder()
                .name(NAME)
                .groupId("org.example")
                .artifactId("sample-plugin")
                .version("1.0.0")
                .build());
        assertThat(xml).contains("<name>" + NAME + "</name>");
        assertThat(xml).contains("<name>" + NAME + "</name>");
        assertThat(xml).contains("<groupId>org.example</groupId>");
        assertThat(xml).contains("<artifactId>sample-plugin</artifactId>");
        assertThat(xml).contains("<version>1.0.0</version>");
    }

    @Test
    void writeWithFailingWriterThrowsXmlWriterException() {
        String unableToWritePlugin = "Unable to write plugin" + randomUUID();
        String ioEx = "ioEx" + randomUUID();
        XmlWriterException exception = assertThatExceptionOfType(XmlWriterException.class)
                .isThrownBy(() -> defaultPluginXmlFactory.write(XmlWriterRequest.<PluginDescriptor>builder()
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
                        .build()))
                .actual();
        assertThat(exception.getMessage()).contains(unableToWritePlugin);
        assertThat(exception.getCause()).isInstanceOf(XmlWriterException.class);
        assertThat(exception.getCause().getCause().getMessage()).isEqualTo(ioEx);
        assertThat(exception.getCause().getMessage()).isEqualTo(unableToWritePlugin);
    }

    @Test
    void writeWithNoTargetThrowsIllegalArgumentException() {
        assertThat(assertThrows(
                                IllegalArgumentException.class,
                                () -> defaultPluginXmlFactory.write(XmlWriterRequest.<PluginDescriptor>builder()
                                        .content(PluginDescriptor.newBuilder()
                                                .name("No Output Plugin")
                                                .build())
                                        .build()))
                        .getMessage())
                .isEqualTo("writer, outputStream or path must be non null");
    }

    @Test
    void readMalformedXmlThrowsXmlReaderException() {
        XmlReaderException exception = assertThatExceptionOfType(XmlReaderException.class)
                .isThrownBy(() -> defaultPluginXmlFactory.read(XmlReaderRequest.builder()
                        .inputStream(new ByteArrayInputStream("<plugin><name>Broken Plugin".getBytes()))
                        .build()))
                .actual();
        assertThat(exception.getMessage()).contains("Unable to read plugin");
        assertThat(exception.getCause()).isInstanceOf(WstxEOFException.class);
    }

    @Test
    void locateExistingPomWithFilePathShouldReturnSameFileIfRegularFile() throws IOException {
        Path pomFile = Files.createTempFile(tempDir, "pom", ".xml");
        DefaultModelProcessor processor = new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of());
        assertThat(processor.locateExistingPom(pomFile)).isEqualTo(pomFile);
    }

    @Test
    @DisabledOnOs(
            value = WINDOWS,
            disabledReason = "windows related issue https://github.com/apache/maven/pull/2312#issuecomment-2876291814")
    void readFromUrlParsesPluginDescriptorCorrectly() throws Exception {
        Path xmlFile = tempDir.resolve("plugin.xml");
        Files.write(xmlFile, SAMPLE_PLUGIN_XML.getBytes());
        PluginDescriptor descriptor = defaultPluginXmlFactory.read(XmlReaderRequest.builder()
                .inputStream(xmlFile.toUri().toURL().openStream())
                .build());
        assertThat(descriptor.getName()).isEqualTo(NAME);
        assertThat(descriptor.getGroupId()).isEqualTo("org.example");
        assertThat(descriptor.getArtifactId()).isEqualTo("sample-plugin");
        assertThat(descriptor.getVersion()).isEqualTo("1.0.0");
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
