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
package org.apache.maven.cling;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MavenClappCling}.
 */
class MavenClappClingTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // collectJarUrls
    // -------------------------------------------------------------------------

    @Test
    void collectJarUrlsReturnsEmptyListWhenDirectoryDoesNotExist() throws IOException {
        Path nonExistent = tempDir.resolve("does-not-exist");
        List<URL> urls = MavenClappCling.collectJarUrls(nonExistent, "mytool");
        assertTrue(urls.isEmpty(), "Expected empty list for non-existent CLAPP lib directory");
    }

    @Test
    void collectJarUrlsReturnsJarsWhenDirectoryContainsJars() throws IOException {
        Path clappDir = Files.createDirectory(tempDir.resolve("mytool"));
        Files.createFile(clappDir.resolve("a.jar"));
        Files.createFile(clappDir.resolve("b.jar"));
        // non-jar file – must be ignored
        Files.createFile(clappDir.resolve("readme.txt"));

        List<URL> urls = MavenClappCling.collectJarUrls(clappDir, "mytool");
        assertEquals(2, urls.size(), "Expected exactly 2 jar URLs");
        assertTrue(urls.stream().allMatch(u -> u.toString().endsWith(".jar")), "All URLs should end with .jar");
    }

    @Test
    void collectJarUrlsReturnsEmptyListWhenDirectoryIsEmpty() throws IOException {
        Path clappDir = Files.createDirectory(tempDir.resolve("empty-tool"));
        List<URL> urls = MavenClappCling.collectJarUrls(clappDir, "empty-tool");
        assertTrue(urls.isEmpty(), "Expected empty list for empty CLAPP lib directory");
    }

    @Test
    void collectJarUrlsThrowsIOExceptionWhenPathIsNotDirectory() throws IOException {
        Path file = Files.createFile(tempDir.resolve("not-a-dir"));
        assertThrows(
                IOException.class,
                () -> MavenClappCling.collectJarUrls(file, "badtool"),
                "Expected IOException when clapp lib path is a file, not a directory");
    }

    // -------------------------------------------------------------------------
    // launchClapp – error paths (without actually launching a JVM/ClassWorld)
    // -------------------------------------------------------------------------

    @Test
    void launchClappThrowsClappExceptionWhenMavenHomeNotSet() {
        String saved = System.getProperty("maven.home");
        try {
            System.clearProperty("maven.home");
            assertThrows(
                    MavenClappCling.ClappException.class,
                    () -> MavenClappCling.launchClapp("mytool", "com.example.Main", new String[0], null),
                    "Expected ClappException when maven.home is not set");
        } finally {
            if (saved != null) {
                System.setProperty("maven.home", saved);
            }
        }
    }

    @Test
    void launchClappThrowsClappExceptionWhenMainClassNotFound() {
        System.setProperty("maven.home", tempDir.toString());
        try {
            // No jars in lib/clapp/mytool/, main class definitely not on classpath
            assertThrows(
                    MavenClappCling.ClappException.class,
                    () -> MavenClappCling.launchClapp(
                            "mytool", "com.example.NonExistentMain", new String[0], null),
                    "Expected ClappException when CLAPP main class cannot be found");
        } finally {
            System.clearProperty("maven.home");
        }
    }

    @Test
    void launchClappThrowsIllegalArgumentExceptionWhenToolNameInvalid() {
        System.setProperty("maven.home", tempDir.toString());
        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MavenClappCling.launchClapp(
                            "../badtool", "com.example.Main", new String[0], null),
                    "Expected IllegalArgumentException when CLAPP tool name contains invalid path characters");
        } finally {
            System.clearProperty("maven.home");
        }
    }
}
