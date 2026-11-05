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
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        String location = "/tmp";
        Path path = Paths.get(location);
        String modelId = "org.example:test:1.0.0";
        ModelSource source = Sources.resolvedSource(path, modelId);

        assertNotNull(source);
        assertInstanceOf(Sources.ResolvedPathSource.class, source);
        assertNull(source.getPath());
        assertEquals(path.toString(), source.getLocation());
        assertEquals(modelId, source.getModelId());
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

    /**
     * A path source resolves against its own path, for a file as well as for a directory: the result is a path below
     * it, not a sibling. Resolution against the parent directory is the job of the model source.
     */
    @Test
    void testPathSourceResolvesAgainstItsOwnPath() {
        Path pom = Paths.get("/tmp/x/pom.xml");
        Source source = Sources.fromPath(pom);

        assertEquals(
                Paths.get("/tmp/x/pom.xml/child.xml"),
                source.resolve("child.xml").getPath());
        assertEquals(
                Paths.get("/tmp/x/other/pom.xml"),
                source.resolve("../other/pom.xml").getPath());
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
        String location = "/tmp";
        Path path = Paths.get(location);
        String modelId = "org.example:test:1.0.0";
        Sources.ResolvedPathSource source = (Sources.ResolvedPathSource) Sources.resolvedSource(path, modelId);

        assertNull(source.getPath());
        assertEquals(path.toString(), source.getLocation());
        assertEquals(modelId, source.getModelId());
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
        try (InputStream inputStream = source.openStream()) {
            String readContent = new String(inputStream.readAllBytes());
            assertEquals(content, readContent);
        }
    }

    @Test
    void testNullHandling() {
        assertThrows(NullPointerException.class, () -> Sources.fromPath(null));
        assertThrows(NullPointerException.class, () -> Sources.buildSource(null));
        assertThrows(NullPointerException.class, () -> Sources.resolvedSource(null, "modelId"));
    }

    /**
     * Tests that BuildPathSource.resolve() gracefully handles relative paths
     * that are not valid filesystem paths (e.g. containing ':' which is illegal
     * on Windows) by returning null instead of throwing InvalidPathException.
     * This reproduces MNG-8129.
     */
    @Test
    void testBuildPathSourceResolveWithInvalidPath() throws IOException {
        Path pomFile = tempDir.resolve("pom.xml");
        Files.writeString(pomFile, "<project/>");

        Sources.BuildPathSource source = (Sources.BuildPathSource) Sources.buildSource(pomFile);
        ModelSource.ModelLocator locator = mock(ModelSource.ModelLocator.class);
        when(locator.locateExistingPom(any(Path.class))).thenReturn(null);

        // Must not throw InvalidPathException on any platform (MNG-8129)
        ModelSource result = source.resolve(locator, "org.apache:apache");
        assertNull(result);
    }

    // -------------------------------------------------------------------------
    // UrlModelSource tests
    // -------------------------------------------------------------------------

    @Test
    void testUrlSourceNullThrows() {
        assertThrows(NullPointerException.class, () -> Sources.urlSource(null, null));
    }

    @Test
    void testUrlSourceBasicProperties() throws Exception {
        URL url = new URI("file:///tmp/pom.xml").toURL();
        ModelSource source = Sources.urlSource(url, "org.example:test:1.0");

        assertNotNull(source);
        assertInstanceOf(Sources.UrlModelSource.class, source);
        assertNull(source.getPath(), "UrlModelSource.getPath() must always return null");
        assertEquals(url.toExternalForm(), source.getLocation());
        assertEquals("org.example:test:1.0", source.getModelId());
    }

    @Test
    void testUrlSourceNullModelId() throws Exception {
        URL url = new URI("file:///tmp/pom.xml").toURL();
        ModelSource source = Sources.urlSource(url, null);
        assertNull(source.getModelId());
    }

    /**
     * A file: URL resolves siblings via URI path resolution.
     */
    @Test
    void testUrlSourceResolveFileUrl() throws Exception {
        URL base = new URI("file:///projects/myproject/pom.xml").toURL();
        Sources.UrlModelSource source = (Sources.UrlModelSource) Sources.urlSource(base, null);

        Source resolved = source.resolve("../sibling/pom.xml");
        assertNotNull(resolved, "Sibling resolution must work for file: URLs");
        assertInstanceOf(Sources.UrlModelSource.class, resolved);
        // The resolved URL should point to /projects/sibling/pom.xml
        String resolvedLocation = resolved.getLocation();
        assertTrue(resolvedLocation.contains("sibling/pom.xml"), "Expected sibling path in: " + resolvedLocation);
    }

    /**
     * A file: URL with a plain sibling name (no directory traversal).
     */
    @Test
    void testUrlSourceResolveFileSiblingRelative() throws Exception {
        URL base = new URI("file:///projects/myproject/pom.xml").toURL();
        Sources.UrlModelSource source = (Sources.UrlModelSource) Sources.urlSource(base, null);

        Source resolved = source.resolve("child");
        assertNotNull(resolved, "Sibling resolution must work for file: URLs");
        String loc = resolved.getLocation();
        assertTrue(loc.endsWith("myproject/child"), "Expected resolved child path, got: " + loc);
    }

    /**
     * An opaque JAR URL (jar:file:/path/to/jar!/resource) must return null — siblings are not meaningful.
     */
    @Test
    void testUrlSourceResolveJarUrlReturnsNull() throws Exception {
        URL jarUrl = new URI("jar:file:/opt/m2/repo/org/apache/maven/super-pom.jar!/super-pom.xml").toURL();
        Sources.UrlModelSource source = (Sources.UrlModelSource) Sources.urlSource(jarUrl, "super-pom");

        assertNull(source.resolve("sibling.xml"), "Opaque jar: URL must not resolve siblings");
    }

    /**
     * An http: URL resolves siblings via URI path resolution (no locator needed for plain resolve()).
     */
    @Test
    void testUrlSourceResolveHttpUrl() throws Exception {
        URL base = new URI("https://example.com/repo/org/apache/maven/pom.xml").toURL();
        Sources.UrlModelSource source = (Sources.UrlModelSource) Sources.urlSource(base, null);

        Source resolved = source.resolve("../parent/pom.xml");
        assertNotNull(resolved, "Sibling resolution must work for https: URLs");
        String loc = resolved.getLocation();
        assertTrue(loc.contains("parent/pom.xml"), "Expected parent path in: " + loc);
    }

    /**
     * resolve(ModelLocator, relative) for a file: URL delegates to the locator, mirroring BuildPathSource.
     */
    @Test
    void testUrlSourceResolveModelLocatorFileUrl() throws Exception {
        Path siblingPom = tempDir.resolve("sibling").resolve("pom.xml");
        Files.createDirectories(siblingPom.getParent());
        Files.writeString(siblingPom, "<project/>");

        URL base = tempDir.resolve("myproject").resolve("pom.xml").toUri().toURL();
        Sources.UrlModelSource source = (Sources.UrlModelSource) Sources.urlSource(base, null);

        ModelSource.ModelLocator locator = mock(ModelSource.ModelLocator.class);
        when(locator.locateExistingPom(any(Path.class))).thenReturn(siblingPom);

        ModelSource resolved = source.resolve(locator, "sibling");
        assertNotNull(resolved, "ModelLocator-based resolution must work for file: URLs");
        assertInstanceOf(Sources.UrlModelSource.class, resolved);
        String loc = resolved.getLocation();
        assertTrue(loc.contains("sibling"), "Expected sibling in resolved location: " + loc);
        verify(locator).locateExistingPom(any(Path.class));
    }

    /**
     * resolve(ModelLocator, relative) for a file: URL returns null when the locator finds no POM.
     */
    @Test
    void testUrlSourceResolveModelLocatorFileUrlNoMatch() throws Exception {
        URL base = tempDir.resolve("myproject").resolve("pom.xml").toUri().toURL();
        Sources.UrlModelSource source = (Sources.UrlModelSource) Sources.urlSource(base, null);

        ModelSource.ModelLocator locator = mock(ModelSource.ModelLocator.class);
        when(locator.locateExistingPom(any(Path.class))).thenReturn(null);

        ModelSource resolved = source.resolve(locator, "nonexistent");
        assertNull(resolved, "Must return null when locator finds no POM");
    }

    /**
     * resolve(ModelLocator, relative) for an opaque jar: URL must return null.
     */
    @Test
    void testUrlSourceResolveModelLocatorJarUrlReturnsNull() throws Exception {
        URL jarUrl = new URI("jar:file:/opt/m2/repo/org/apache/maven/super-pom.jar!/super-pom.xml").toURL();
        Sources.UrlModelSource source = (Sources.UrlModelSource) Sources.urlSource(jarUrl, null);

        ModelSource.ModelLocator locator = mock(ModelSource.ModelLocator.class);
        assertNull(source.resolve(locator, "sibling.xml"), "Opaque jar: URL must not resolve siblings");
        verify(locator, never()).locateExistingPom(any(Path.class));
    }

    /**
     * resolve(ModelLocator, relative) for an https: URL wraps the resolved URL directly
     * (no filesystem locator involved for non-file: schemes).
     */
    @Test
    void testUrlSourceResolveModelLocatorHttpUrl() throws Exception {
        URL base = new URI("https://example.com/repo/org/apache/maven/pom.xml").toURL();
        Sources.UrlModelSource source = (Sources.UrlModelSource) Sources.urlSource(base, null);

        ModelSource.ModelLocator locator = mock(ModelSource.ModelLocator.class);
        ModelSource resolved = source.resolve(locator, "../parent/pom.xml");
        assertNotNull(resolved, "Non-file: hierarchical URL must resolve without locator");
        String loc = resolved.getLocation();
        assertTrue(loc.contains("parent/pom.xml"), "Expected parent path in: " + loc);
        // locator must NOT be called for non-file: URLs
        verify(locator, never()).locateExistingPom(any(Path.class));
    }

    /**
     * Stream reading from a file: URL works correctly.
     */
    @Test
    void testUrlSourceStreamReading() throws Exception {
        Path testFile = tempDir.resolve("test.xml");
        String content = "<project/>";
        Files.writeString(testFile, content);

        ModelSource source = Sources.urlSource(testFile.toUri().toURL(), null);
        try (InputStream is = source.openStream()) {
            assertEquals(content, new String(is.readAllBytes()));
        }
    }

    /**
     * equals/hashCode contract for UrlModelSource.
     */
    @Test
    void testUrlSourceEqualsHashCode() throws Exception {
        URL url1 = new URI("file:///tmp/pom.xml").toURL();
        URL url2 = new URI("file:///tmp/pom.xml").toURL();
        URL url3 = new URI("file:///tmp/other.xml").toURL();

        ModelSource s1 = Sources.urlSource(url1, "g:a:1");
        ModelSource s2 = Sources.urlSource(url2, "g:a:1");
        ModelSource s3 = Sources.urlSource(url3, "g:a:1");
        ModelSource s4 = Sources.urlSource(url1, "g:a:2");

        assertEquals(s1, s2, "Same URL and modelId must be equal");
        assertEquals(s1.hashCode(), s2.hashCode());
        assertNotEquals(s1, s3, "Different URL must not be equal");
        assertNotEquals(s1, s4, "Different modelId must not be equal");
    }

    @Test
    void testUrlSourceToString() throws Exception {
        URL url = new URI("file:///tmp/pom.xml").toURL();
        ModelSource source = Sources.urlSource(url, "g:a:1");
        String str = source.toString();
        // URL.toExternalForm() normalises file:///tmp → file:/tmp on some JDKs; check both forms
        assertTrue(
                str.contains("file:/tmp/pom.xml") || str.contains("file:///tmp/pom.xml"),
                "toString must include URL: " + str);
        assertTrue(str.contains("g:a:1"), "toString must include modelId: " + str);
    }
}
