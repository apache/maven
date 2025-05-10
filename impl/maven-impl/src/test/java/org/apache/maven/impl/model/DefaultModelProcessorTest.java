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
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultModelProcessorTest {

    @TempDir
    Path tempDir;

    Path testProjectDir, testPomFile;

    @AfterEach
    void cleanup() throws IOException {
        if (testPomFile != null && Files.exists(testPomFile)) {
            Files.deleteIfExists(testPomFile);
        }
        if (testProjectDir != null && Files.exists(testProjectDir)) {
            Files.deleteIfExists(testProjectDir);
        }
    }

    @Test
    void readWithValidParserShouldReturnModel() throws Exception {
        ModelXmlFactory factory = mock(ModelXmlFactory.class);
        ModelParser parser = mock(ModelParser.class);
        XmlReaderRequest request = mock(XmlReaderRequest.class);
        Model model = mock(Model.class);
        Path path = Path.of("project/pom.xml");
        when(request.getPath()).thenReturn(path);
        when(request.isStrict()).thenReturn(true);
        when(model.withPomFile(path)).thenReturn(model);
        when(parser.locateAndParse(any(), any())).thenReturn(Optional.of(model));
        Model result = new DefaultModelProcessor(factory, List.of(parser)).read(request);
        assertNotNull(result);
        assertEquals(model, result);
    }

    @Test
    void readNullPomPathShouldUseFactoryDirectly() throws Exception {
        ModelXmlFactory factory = mock(ModelXmlFactory.class);
        XmlReaderRequest request = mock(XmlReaderRequest.class);
        Model model = mock(Model.class);
        when(request.getPath()).thenReturn(null);
        when(factory.read(request)).thenReturn(model);
        Model result = new DefaultModelProcessor(factory, List.of()).read(request);
        assertNotNull(result);
        assertEquals(model, result);
    }

    @Test
    void locateExistingPomWithParsersShouldReturnFirstValid() {
        Path expectedPom = Path.of("project/pom.xml");
        Source mockSource = mock(Source.class);
        when(mockSource.getPath()).thenReturn(expectedPom);
        ModelParser parser = mock(ModelParser.class);
        when(parser.locate(any())).thenReturn(Optional.of(mockSource));
        assertEquals(
                expectedPom,
                new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of(parser))
                        .locateExistingPom(Path.of("project")));
    }

    @Test
    void locateExistingPomParserReturnsPathOutsideProjectShouldThrow() {
        Source mockSource = mock(Source.class);
        when(mockSource.getPath()).thenReturn(Path.of("other/pom.xml"));
        ModelParser parser = mock(ModelParser.class);
        when(parser.locate(any())).thenReturn(Optional.of(mockSource));
        assertThat(assertThrows(IllegalArgumentException.class, () -> new DefaultModelProcessor(
                                        mock(ModelXmlFactory.class), List.of(parser))
                                .locateExistingPom(Path.of("project")))
                        .getMessage())
                .contains("does not belong to the given directory");
    }

    @Test
    void locateExistingPomFallbackWithValidPomShouldReturnPom() throws Exception {
        testProjectDir = Files.createTempDirectory(tempDir, "testproject");
        testPomFile = testProjectDir.resolve("pom.xml");
        Files.createFile(testPomFile);
        assertEquals(
                testPomFile,
                new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of()).locateExistingPom(testProjectDir));
    }

    @Test
    void locateExistingPomFallbackWithFileAsPathShouldReturnThatFile() throws Exception {
        testPomFile = Files.createTempFile(tempDir, "pom", ".xml");
        assertEquals(
                testPomFile,
                new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of()).locateExistingPom(testPomFile));
    }

    @Test
    void readWithParserThrowingExceptionShouldCollectException() {
        ModelXmlFactory factory = mock(ModelXmlFactory.class);
        ModelParser parser = mock(ModelParser.class);
        XmlReaderRequest request = mock(XmlReaderRequest.class);
        when(request.getPath()).thenReturn(Path.of("project/pom.xml"));
        when(request.isStrict()).thenReturn(true);
        when(parser.locateAndParse(any(), any())).thenThrow(new RuntimeException("Parser error"));
        when(factory.read(request)).thenThrow(new RuntimeException("Factory error"));
        RuntimeException ex = assertThrows(
                RuntimeException.class, () -> new DefaultModelProcessor(factory, List.of(parser)).read(request));
        assertEquals("Factory error", ex.getMessage());
        assertEquals(1, ex.getSuppressed().length);
        assertInstanceOf(ModelParserException.class, ex.getSuppressed()[0]);
        assertEquals("Parser error", ex.getSuppressed()[0].getCause().getMessage());
    }

    @Test
    void readWithFactoryThrowingExceptionShouldRethrowWithSuppressed() {
        ModelXmlFactory factory = mock(ModelXmlFactory.class);
        XmlReaderRequest request = mock(XmlReaderRequest.class);
        when(request.getPath()).thenReturn(Path.of("project/pom.xml"));
        when(factory.read(request)).thenThrow(new RuntimeException("Factory error"));
        ModelParser parser = mock(ModelParser.class);
        when(parser.locateAndParse(any(), any())).thenThrow(new RuntimeException("Parser error"));
        RuntimeException ex = assertThrows(
                RuntimeException.class, () -> new DefaultModelProcessor(factory, List.of(parser)).read(request));
        assertEquals("Factory error", ex.getMessage());
        assertEquals(1, ex.getSuppressed().length);
        assertInstanceOf(ModelParserException.class, ex.getSuppressed()[0]);
        assertEquals("Parser error", ex.getSuppressed()[0].getCause().getMessage());
    }

    @Test
    void locateExistingPomWithDirectoryContainingPom() throws IOException {
        testProjectDir = Files.createTempDirectory(tempDir, "project");
        testPomFile = testProjectDir.resolve("pom.xml");
        Files.createFile(testPomFile);
        assertEquals(
                testPomFile,
                new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of()).locateExistingPom(testProjectDir));
    }

    @Test
    void locateExistingPomWithDirectoryWithoutPom() throws IOException {
        testProjectDir = Files.createTempDirectory(tempDir, "project");
        assertNull(new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of()).locateExistingPom(testProjectDir));
    }

    @Test
    void locateExistingPomWithPomFile() throws IOException {
        testPomFile = Files.createTempFile(tempDir, "pom", ".xml");
        assertEquals(
                testPomFile,
                new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of()).locateExistingPom(testPomFile));
    }

    @Test
    void locateExistingPomWithNonExistentPath() {
        assertNotNull(new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of())
                .locateExistingPom(tempDir.resolve("nonexistent")));
    }

    @Test
    void locateExistingPomWithNullProjectAndNoPomInUserDirShouldReturnNull() {
        System.setProperty("user.dir", tempDir.toString());
        assertNull(new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of()).locateExistingPom(null));
    }

    @Test
    void locateExistingPomShouldAcceptPomInProjectDirectory() {
        Path projectDir = Path.of("project");
        Path pomInDir = projectDir.resolve("pom.xml");
        Source mockSource = mock(Source.class);
        when(mockSource.getPath()).thenReturn(pomInDir);
        ModelParser parser = mock(ModelParser.class);
        when(parser.locate(any())).thenReturn(Optional.of(mockSource));
        assertEquals(
                pomInDir,
                new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of(parser)).locateExistingPom(projectDir));
    }

    @Test
    void locateExistingPomShouldAcceptPomAsProjectDirectory() {
        Path pomFile = Path.of("pom.xml");
        Source mockSource = mock(Source.class);
        when(mockSource.getPath()).thenReturn(pomFile);
        ModelParser parser = mock(ModelParser.class);
        when(parser.locate(any())).thenReturn(Optional.of(mockSource));
        assertEquals(
                pomFile,
                new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of(parser)).locateExistingPom(pomFile));
    }

    @Test
    void locateExistingPomShouldRejectPomInDifferentDirectory() {
        Source mockSource = mock(Source.class);
        when(mockSource.getPath()).thenReturn(Path.of("other/pom.xml"));
        ModelParser parser = mock(ModelParser.class);
        when(parser.locate(any())).thenReturn(Optional.of(mockSource));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new DefaultModelProcessor(
                                mock(ModelXmlFactory.class), List.of(parser))
                        .locateExistingPom(Path.of("project")))
                .getMessage()
                .contains("does not belong to the given directory"));
    }
}
