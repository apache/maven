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
package org.apache.maven.api.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SourcesTest {
    @TempDir
    Path tempDir;

    @Test
    void testFromPath() {
        Path path = Paths.get("/tmp");
        Source source = Sources.fromPath(path);

        assertNotNull(source);
        assertInstanceOf(Sources.PathSource.class, source);
        assertEquals(path.normalize(), source.getPath());
    }

    @Test
    void testBuildSource() {
        Path path = Paths.get("/tmp");
        ModelSource source = Sources.buildSource(path);

        assertNotNull(source);
        assertInstanceOf(Sources.BuildPathSource.class, source);
        assertEquals(path.normalize(), source.getPath());
    }

    @Test
    void testResolvedSource() {
        Path path = Paths.get("/tmp");
        String location = "custom-location";
        ModelSource source = Sources.resolvedSource(path, location);

        assertNotNull(source);
        assertInstanceOf(Sources.ResolvedPathSource.class, source);
        assertNull(source.getPath());
        assertEquals(location, source.getLocation());
    }

    @Test
    void testPathSourceFunctionality() {
        // Test basic source functionality
        Path path = Paths.get("/tmp");
        Sources.PathSource source = (Sources.PathSource) Sources.fromPath(path);

        assertEquals(path.normalize(), source.getPath());
        assertEquals(path.toString(), source.getLocation());

        Source resolved = source.resolve("subdir");
        assertNotNull(resolved);
        assertEquals(path.resolve("subdir").normalize(), resolved.getPath());
    }

    @Test
    void testBuildPathSourceFunctionality() {
        // Test build source functionality
        Path basePath = Paths.get("/tmp");
        ModelSource.ModelLocator locator = mock(ModelSource.ModelLocator.class);
        Path resolvedPath = Paths.get("/tmp/subproject/pom.xml");
        when(locator.locateExistingPom(any(Path.class))).thenReturn(resolvedPath);

        Sources.BuildPathSource source = (Sources.BuildPathSource) Sources.buildSource(basePath);
        ModelSource resolved = source.resolve(locator, "subproject");

        assertNotNull(resolved);
        assertInstanceOf(Sources.BuildPathSource.class, resolved);
        assertEquals(resolvedPath, resolved.getPath());

        verify(locator).locateExistingPom(any(Path.class));
    }

    @Test
    void testResolvedPathSourceFunctionality() {
        // Test resolved source functionality
        Path path = Paths.get("/tmp");
        String location = "custom-location";
        Sources.ResolvedPathSource source = (Sources.ResolvedPathSource) Sources.resolvedSource(path, location);

        assertNull(source.getPath());
        assertEquals(location, source.getLocation());
        assertNull(source.resolve("subdir"));

        ModelSource.ModelLocator locator = mock(ModelSource.ModelLocator.class);
        assertNull(source.resolve(locator, "subproject"));
        verify(locator, never()).locateExistingPom(any(Path.class));
    }

    @Test
    void testStreamReading() throws IOException {
        // Test stream reading functionality
        Path testFile = tempDir.resolve("test.txt");
        String content = "test content";
        Files.writeString(testFile, content);

        Source source = Sources.fromPath(testFile);
        String readContent = new String(source.openStream().readAllBytes());

        assertEquals(content, readContent);
    }

    @Test
    void testNullHandling() {
        assertThrows(NullPointerException.class, () -> Sources.fromPath(null));
        assertThrows(NullPointerException.class, () -> Sources.buildSource(null));
        assertThrows(NullPointerException.class, () -> Sources.resolvedSource(null, "location"));
    }
}
