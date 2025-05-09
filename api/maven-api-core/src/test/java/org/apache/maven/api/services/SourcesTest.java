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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SourcesTest {
    @TempDir
    Path tempDir;

    @Test
    void fromPath() {
        Path path = Paths.get("/tmp");
        Source source = Sources.fromPath(path);

        assertThat(source).isNotNull();
        assertThat(source).isInstanceOf(Sources.PathSource.class);
        assertThat(source.getPath()).isEqualTo(path.normalize());
    }

    @Test
    void buildSource() {
        Path path = Paths.get("/tmp");
        ModelSource source = Sources.buildSource(path);

        assertThat(source).isNotNull();
        assertThat(source).isInstanceOf(Sources.BuildPathSource.class);
        assertThat(source.getPath()).isEqualTo(path.normalize());
    }

    @Test
    void resolvedSource() {
        Path path = Paths.get("/tmp");
        String location = "custom-location";
        ModelSource source = Sources.resolvedSource(path, location);

        assertThat(source).isNotNull();
        assertThat(source).isInstanceOf(Sources.ResolvedPathSource.class);
        assertThat(source.getPath()).isNull();
        assertThat(source.getLocation()).isEqualTo(location);
    }

    @Test
    void pathSourceFunctionality() {
        // Test basic source functionality
        Path path = Paths.get("/tmp");
        Sources.PathSource source = (Sources.PathSource) Sources.fromPath(path);

        assertThat(source.getPath()).isEqualTo(path.normalize());
        assertThat(source.getLocation()).isEqualTo(path.toString());

        Source resolved = source.resolve("subdir");
        assertThat(resolved).isNotNull();
        assertThat(resolved.getPath()).isEqualTo(path.resolve("subdir").normalize());
    }

    @Test
    void buildPathSourceFunctionality() {
        // Test build source functionality
        Path basePath = Paths.get("/tmp");
        ModelSource.ModelLocator locator = mock(ModelSource.ModelLocator.class);
        Path resolvedPath = Paths.get("/tmp/subproject/pom.xml");
        when(locator.locateExistingPom(any(Path.class))).thenReturn(resolvedPath);

        Sources.BuildPathSource source = (Sources.BuildPathSource) Sources.buildSource(basePath);
        ModelSource resolved = source.resolve(locator, "subproject");

        assertThat(resolved).isNotNull();
        assertThat(resolved).isInstanceOf(Sources.BuildPathSource.class);
        assertThat(resolved.getPath()).isEqualTo(resolvedPath);

        verify(locator).locateExistingPom(any(Path.class));
    }

    @Test
    void resolvedPathSourceFunctionality() {
        // Test resolved source functionality
        Path path = Paths.get("/tmp");
        String location = "custom-location";
        Sources.ResolvedPathSource source = (Sources.ResolvedPathSource) Sources.resolvedSource(path, location);

        assertThat(source.getPath()).isNull();
        assertThat(source.getLocation()).isEqualTo(location);
        assertThat(source.resolve("subdir")).isNull();

        ModelSource.ModelLocator locator = mock(ModelSource.ModelLocator.class);
        assertThat(source.resolve(locator, "subproject")).isNull();
        verify(locator, never()).locateExistingPom(any(Path.class));
    }

    @Test
    void streamReading() throws IOException {
        // Test stream reading functionality
        Path testFile = tempDir.resolve("test.txt");
        String content = "test content";
        Files.writeString(testFile, content);

        Source source = Sources.fromPath(testFile);
        try (InputStream inputStream = source.openStream()) {
            String readContent = new String(inputStream.readAllBytes());
            assertThat(readContent).isEqualTo(content);
        }
    }

    @Test
    void nullHandling() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> Sources.fromPath(null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> Sources.buildSource(null));
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> Sources.resolvedSource(null, "location"));
    }
}
