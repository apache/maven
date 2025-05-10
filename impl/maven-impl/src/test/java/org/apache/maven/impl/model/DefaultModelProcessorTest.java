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
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.ModelParserException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.deleteIfExists;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultModelProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void locateExistingPomShouldHandleRegularFileInput() throws IOException {
        // Create a temporary file
        Path tempFile = Files.createTempFile(tempDir, "pom", ".xml");

        // Test that locateExistingPom returns the file when given a regular file
        DefaultModelProcessor processor = new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of());
        Path result = processor.locateExistingPom(tempFile);
        assertEquals(tempFile, result);

        // Clean up
        Files.deleteIfExists(tempFile);
    }

    @Test
    void locateExistingPomShouldReturnNullForNonPomFile() throws IOException {
        // Create a temporary file that's not a POM
        Path tempFile = Files.createTempFile(tempDir, "test", ".txt");

        // Test that locateExistingPom returns null for non-POM files
        DefaultModelProcessor processor = new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of());
        Path result = processor.locateExistingPom(tempFile);
        assertEquals(result, tempFile);
        // Clean up
        Files.deleteIfExists(tempFile);
    }

    @Test
    void locateExistingPomShouldReturnNullForDirectoryWithoutPom() throws IOException {
        // Create a temporary directory
        Path tempDir = Files.createTempDirectory(this.tempDir, "project");

        // Test that locateExistingPom returns null for directories without pom.xml
        DefaultModelProcessor processor = new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of());
        Path result = processor.locateExistingPom(tempDir);
        assertNull(result);

        // Clean up
        Files.deleteIfExists(tempDir);
    }

    @Test
    void locateExistingPomShouldFindPomInDirectory() throws IOException {
        // Create a temporary directory with a pom.xml
        Path projectDir = Files.createTempDirectory(this.tempDir, "project");
        Path pomFile = projectDir.resolve("pom.xml");
        Files.createFile(pomFile);

        // Test that locateExistingPom finds the pom.xml in the directory
        DefaultModelProcessor processor = new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of());
        Path result = processor.locateExistingPom(projectDir);
        assertEquals(pomFile, result);

        // Clean up
        Files.deleteIfExists(pomFile);
        Files.deleteIfExists(projectDir);
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
        assertEquals(model, new DefaultModelProcessor(factory, List.of(parser)).read(request));
    }

    @Test
    void readNullPomPathShouldUseFactoryDirectly() throws Exception {
        ModelXmlFactory factory = mock(ModelXmlFactory.class);
        XmlReaderRequest request = mock(XmlReaderRequest.class);
        Model model = mock(Model.class);
        when(request.getPath()).thenReturn(null);
        when(factory.read(request)).thenReturn(model);
        assertEquals(model, new DefaultModelProcessor(factory, List.of()).read(request));
    }

    @Test
    void readWithParserExceptionShouldSuppressAndRethrowFactoryException() {
        assertThrows(RuntimeException.class, () -> new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of())
                .locateExistingPom(null));
    }

    @Test
    void testErrorHandlingMustCollectParsingErrorsAndAddAsSuppressedToRethrowException() {
        ModelXmlFactory factory = mock(ModelXmlFactory.class);
        ModelParser parser1 = mock(ModelParser.class);
        ModelParser parser2 = mock(ModelParser.class);
        XmlReaderRequest request = mock(XmlReaderRequest.class);
        when(request.getPath()).thenReturn(Path.of("project/pom.xml"));
        when(request.isStrict()).thenReturn(true);
        IOException ioException = new IOException("Factory failure");
        ModelParserException modelParserException = new ModelParserException("Parser exception 1", ioException);
        when(parser1.locateAndParse(any(), any())).thenThrow(modelParserException);
        when(parser2.locateAndParse(any(), any())).thenReturn(Optional.empty());
        when(factory.read(request)).thenThrow(new XmlReaderException("Factory failure", null, ioException));
        IOException thrown = assertThrows(
                IOException.class, () -> new DefaultModelProcessor(factory, List.of(parser1, parser2)).read(request));
        assertThat(thrown.getSuppressed()).containsExactly(modelParserException);
        assertThat(thrown.getSuppressed()[0].getCause()).isEqualTo(ioException);
    }

    @Test
    void testErrorHandlingCollectsParsingErrorsAndAddsAsSuppressedToRethrownException() {
        ModelXmlFactory factory = mock(ModelXmlFactory.class);
        ModelParser parser1 = mock(ModelParser.class);
        ModelParser parser2 = mock(ModelParser.class);
        XmlReaderRequest request = mock(XmlReaderRequest.class);
        Path pomPath = Path.of("project/pom.xml");
        when(request.getPath()).thenReturn(pomPath);
        when(request.isStrict()).thenReturn(true);
        when(parser1.locateAndParse(any(), any())).thenReturn(Optional.empty());
        when(parser2.locateAndParse(any(), any())).thenReturn(Optional.empty());
        when(factory.read(request)).thenThrow(new RuntimeException(new IOException()));
        assertThat(assertThrows(RuntimeException.class, () -> new DefaultModelProcessor(factory, List.of())
                                .read(request))
                        .getMessage())
                .isEqualTo("java.io.IOException");
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

    @Test
    void readWithParserReturningEmptyShouldTryNextParser() throws Exception {
        ModelXmlFactory factory = mock(ModelXmlFactory.class);
        ModelParser parser1 = mock(ModelParser.class);
        ModelParser parser2 = mock(ModelParser.class);
        XmlReaderRequest request = mock(XmlReaderRequest.class);
        Path pomPath = Path.of("project/pom.xml");
        when(request.getPath()).thenReturn(pomPath);
        when(request.isStrict()).thenReturn(true);
        Model expectedModel = mock(Model.class);
        when(expectedModel.withPomFile(pomPath)).thenReturn(expectedModel);
        when(parser1.locateAndParse(any(), any())).thenReturn(Optional.empty());
        when(parser2.locateAndParse(any(), any())).thenReturn(Optional.of(expectedModel));
        assertSame(expectedModel, new DefaultModelProcessor(factory, List.of(parser1, parser2)).read(request));
    }

    @Test
    void readWithAllParsersReturningEmptyShouldFallbackToFactory() throws Exception {
        ModelXmlFactory factory = mock(ModelXmlFactory.class);
        ModelParser parser1 = mock(ModelParser.class);
        ModelParser parser2 = mock(ModelParser.class);
        XmlReaderRequest request = mock(XmlReaderRequest.class);
        Path pomPath = Path.of("project/pom.xml");
        when(request.getPath()).thenReturn(pomPath);
        when(request.isStrict()).thenReturn(true);
        Model expectedModel = mock(Model.class);
        when(parser1.locateAndParse(any(), any())).thenReturn(Optional.empty());
        when(parser2.locateAndParse(any(), any())).thenReturn(Optional.empty());
        when(factory.read(request)).thenReturn(expectedModel);
        assertSame(expectedModel, new DefaultModelProcessor(factory, List.of(parser1, parser2)).read(request));
    }

    @Test
    void readWithParserReturningModelShouldUseThatModel() throws Exception {
        ModelXmlFactory factory = mock(ModelXmlFactory.class);
        ModelParser parser = mock(ModelParser.class);
        XmlReaderRequest request = mock(XmlReaderRequest.class);
        Path pomPath = Path.of("project/pom.xml");
        when(request.getPath()).thenReturn(pomPath);
        when(request.isStrict()).thenReturn(true);
        Model expectedModel = mock(Model.class);
        when(expectedModel.withPomFile(pomPath)).thenReturn(expectedModel);
        when(parser.locateAndParse(any(), any())).thenReturn(Optional.of(expectedModel));
        assertSame(expectedModel, new DefaultModelProcessor(factory, List.of(parser)).read(request));
        verify(factory, never()).read((Path) any());
    }

    @Test
    void readWithParserReturningModelShouldSetPomFile() throws Exception {
        ModelXmlFactory factory = mock(ModelXmlFactory.class);
        ModelParser parser = mock(ModelParser.class);
        XmlReaderRequest request = mock(XmlReaderRequest.class);
        Path pomPath = Path.of("project/pom.xml");
        when(request.getPath()).thenReturn(pomPath);
        when(request.isStrict()).thenReturn(true);
        Model originalModel = mock(Model.class);
        Model modelWithPom = mock(Model.class);
        when(originalModel.withPomFile(pomPath)).thenReturn(modelWithPom);
        when(parser.locateAndParse(any(), any())).thenReturn(Optional.of(originalModel));
        assertSame(modelWithPom, new DefaultModelProcessor(factory, List.of(parser)).read(request));
        verify(originalModel).withPomFile(pomPath);
    }

    @Nested
    class WithTmpFiles {

        Path testProjectDir, testPomFile;

        @BeforeEach
        void setup() throws IOException {
            testProjectDir = createTempDirectory(tempDir, "testproject");
            testPomFile = testProjectDir.resolve("pom.xml");
        }

        @AfterEach
        void teardown() throws IOException {
            deleteIfExists(testPomFile);
            deleteIfExists(testProjectDir);
        }

        @Test
        void locateExistingPomFallbackWithValidPomShouldReturnPom() throws Exception {
            Files.createFile(testPomFile);
            assertEquals(
                    testPomFile,
                    new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of())
                            .locateExistingPom(testProjectDir));
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
            assertEquals("Parser error", ex.getMessage());
            assertEquals(0, ex.getSuppressed().length);
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
            assertEquals("Parser error", ex.getMessage());
            assertEquals(0, ex.getSuppressed().length);
        }

        @Test
        void locateExistingPomWithDirectoryContainingPom() throws IOException {
            testProjectDir = createTempDirectory(tempDir, "project");
            testPomFile = testProjectDir.resolve("pom.xml");
            Files.createFile(testPomFile);
            assertEquals(
                    testPomFile,
                    new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of())
                            .locateExistingPom(testProjectDir));
        }

        @Test
        void locateExistingPomWithDirectoryWithoutPom() throws IOException {
            testProjectDir = createTempDirectory(tempDir, "project");
            assertNull(new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of())
                    .locateExistingPom(testProjectDir));
        }

        @Test
        void locateExistingPomWithPomFile() throws IOException {
            testPomFile = Files.createTempFile(tempDir, "pom", ".xml");
            assertEquals(
                    testPomFile,
                    new DefaultModelProcessor(mock(ModelXmlFactory.class), List.of()).locateExistingPom(testPomFile));
        }
    }
}
