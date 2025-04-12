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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathSelectorTest {
    /**
     * The temporary directory containing the files to test.
     */
    private Path directory;

    /**
     * The filtered set of paths. Created by {@link #filter()}.
     */
    private Set<Path> filtered;

    /**
     * Creates a temporary directory and checks its list of content based on patterns.
     *
     * @param tempDir temporary directory where to create a tree
     * @throws IOException if an error occurred while creating a temporary file or directory
     */
    @Test
    public void testTree(final @TempDir Path tempDir) throws IOException {
        directory = tempDir;
        Path foo = Files.createDirectory(tempDir.resolve("foo"));
        Path bar = Files.createDirectory(foo.resolve("bar"));
        Path biz = Files.createDirectory(tempDir.resolve("biz"));
        Files.createFile(tempDir.resolve("root.txt"));
        Files.createFile(bar.resolve("leaf.txt"));
        Files.createFile(biz.resolve("excluded.txt"));

        filter("");
        assertFilteredFilesContains("root.txt");
        assertFilteredFilesContains("foo/bar/leaf.txt");
        assertTrue(filtered.isEmpty(), filtered.toString());

        filter("glob:");
        assertFilteredFilesContains("foo/bar/leaf.txt");
        assertTrue(filtered.isEmpty(), filtered.toString());
    }

    /**
     * Creates the filtered paths in a modifiable set.
     * The result is assigned to {@link #filtered}.
     *
     * @param syntax syntax to test, either an empty string of {@code "glob:"}
     * @throws IOException if an error occurred while listing the files.
     */
    private void filter(final String syntax) throws IOException {
        var includes = List.of(syntax + "**/*.txt");
        var excludes = List.of(syntax + "biz/**");
        var matcher = new PathSelector(directory, includes, excludes, false);
        filtered = new HashSet<>(Files.walk(directory).filter(matcher::matches).toList());
    }

    /**
     * Asserts that the filtered set of paths contains the given item.
     * If present, the path is removed from the collection of filtered files.
     * It allows caller to verify that there are no unexpected elements remaining
     * after all expected elements have been removed.
     *
     * @param path the path to test
     */
    private void assertFilteredFilesContains(String path) {
        assertTrue(filtered.remove(directory.resolve(path)), path);
    }

    /**
     * Tests the omission of unnecessary excludes.
     *
     * Note: at the time of writing this test (April 2025), the list of excludes go down from 40 to 17 elements.
     * This is not bad, but we could do better with, for example, a special treatment of the excludes that are
     * for excluding an entire directory.
     */
    @Test
    public void testExcludeOmission() {
        directory = Path.of("dummy");
        var includes = List.of("**/*.java");
        var excludes = List.of("biz/**");
        var matcher = new PathSelector(directory, includes, excludes, true);
        String s = matcher.toString();
        assertTrue(s.contains("glob:**/*.java"));
        assertFalse(s.contains("project.pj")); // Unnecessary exclusion should have been omitted.
        assertFalse(s.contains(".DS_Store"));
    }
}
