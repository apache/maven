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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultModelProcessor}.
 */
class DefaultModelProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void testDetailedErrorMessageWithMultipleParsers() throws IOException {
        // Create a test POM file
        Path pomFile = tempDir.resolve("pom.yaml");
        Files.writeString(pomFile, "invalid: yaml: content:");

        // Create mock parsers that will fail
        ModelParser yamlParser = mock(ModelParser.class);
        when(yamlParser.locateAndParse(any(), any()))
                .thenThrow(new ModelParserException(
                        "YAML parsing failed", 5, 10, new RuntimeException("Invalid YAML syntax")));

        ModelParser tomlParser = mock(ModelParser.class);
        when(tomlParser.locateAndParse(any(), any()))
                .thenThrow(new ModelParserException("TOML parsing failed", 3, 7, null));

        // Create mock XML factory that will also fail
        ModelXmlFactory xmlFactory = mock(ModelXmlFactory.class);
        when(xmlFactory.read(any(XmlReaderRequest.class)))
                .thenThrow(new XmlReaderException("XML parsing failed", null, null));

        // Create processor with the mock parsers
        DefaultModelProcessor processor =
                new DefaultModelProcessor(xmlFactory, Map.of("yaml", yamlParser, "toml", tomlParser));

        // Create request
        XmlReaderRequest request =
                XmlReaderRequest.builder().path(pomFile).strict(true).build();

        // Execute and verify
        IOException exception = assertThrows(IOException.class, () -> processor.read(request));

        String message = exception.getMessage();

        // Verify the message contains information about all parsers
        assertTrue(message.contains("Unable to parse POM"), "Message should mention unable to parse POM");
        assertTrue(message.contains(pomFile.toString()), "Message should contain the POM file path");
        assertTrue(message.contains("Tried 2 parsers"), "Message should mention 2 parsers were tried");
        assertTrue(message.contains("YAML parsing failed"), "Message should contain YAML parser error");
        assertTrue(message.contains("at line 5, column 10"), "Message should contain YAML line/column info");
        assertTrue(message.contains("Invalid YAML syntax"), "Message should contain YAML cause message");
        assertTrue(message.contains("TOML parsing failed"), "Message should contain TOML parser error");
        assertTrue(message.contains("at line 3, column 7"), "Message should contain TOML line/column info");
        assertTrue(message.contains("default) XML reader also failed"), "Message should mention XML reader failure");
        assertTrue(message.contains("XML parsing failed"), "Message should contain XML error message");

        // Verify suppressed exceptions are still attached
        assertEquals(3, exception.getSuppressed().length, "Should have 3 suppressed exceptions");
    }

    @Test
    void testDetailedErrorMessageWithSingleParser() throws IOException {
        Path pomFile = tempDir.resolve("pom.json");
        Files.writeString(pomFile, "{invalid json}");

        ModelParser jsonParser = mock(ModelParser.class);
        when(jsonParser.locateAndParse(any(), any())).thenThrow(new ModelParserException("JSON parsing failed"));

        ModelXmlFactory xmlFactory = mock(ModelXmlFactory.class);
        when(xmlFactory.read(any(XmlReaderRequest.class)))
                .thenThrow(new XmlReaderException("Not valid XML", null, null));

        DefaultModelProcessor processor = new DefaultModelProcessor(xmlFactory, Map.of("json", jsonParser));

        XmlReaderRequest request =
                XmlReaderRequest.builder().path(pomFile).strict(true).build();

        IOException exception = assertThrows(IOException.class, () -> processor.read(request));

        String message = exception.getMessage();
        assertTrue(message.contains("Tried 1 parser:"), "Message should mention 1 parser (singular)");
        assertTrue(message.contains("JSON parsing failed"), "Message should contain JSON parser error");
        assertTrue(message.contains("Not valid XML"), "Message should contain XML error");
    }

    @Test
    void testSuccessfulParsingDoesNotThrowException() throws Exception {
        Path pomFile = tempDir.resolve("pom.yaml");
        Files.writeString(pomFile, "valid: yaml");

        Model mockModel = mock(Model.class);
        when(mockModel.withPomFile(any())).thenReturn(mockModel);

        ModelParser yamlParser = mock(ModelParser.class);
        when(yamlParser.locateAndParse(any(), any())).thenReturn(Optional.of(mockModel));

        ModelXmlFactory xmlFactory = mock(ModelXmlFactory.class);

        DefaultModelProcessor processor = new DefaultModelProcessor(xmlFactory, Map.of("yaml", yamlParser));

        XmlReaderRequest request =
                XmlReaderRequest.builder().path(pomFile).strict(true).build();

        Model result = processor.read(request);
        assertNotNull(result);
        verify(xmlFactory, never()).read(any(XmlReaderRequest.class));
    }
}
