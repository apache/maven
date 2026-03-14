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
import java.nio.file.PathMatcher;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathSelectorTest {
    /**
     * Creates a temporary directory and checks its list of content based on patterns.
     *
     * @param directory temporary directory where to create a tree
     * @throws IOException if an error occurred while creating a temporary file or directory
     */
    @Test
    public void testTree(final @TempDir Path directory) throws IOException {
        Path foo = Files.createDirectory(directory.resolve("foo"));
        Path bar = Files.createDirectory(foo.resolve("bar"));
        Path baz = Files.createDirectory(directory.resolve("baz"));
        Files.createFile(directory.resolve("root.txt"));
        Files.createFile(bar.resolve("leaf.txt"));
        Files.createFile(baz.resolve("excluded.txt"));
        assertFilteredFilesContains(directory, "", "root.txt", "foo/bar/leaf.txt");
        assertFilteredFilesContains(directory, "glob:", "foo/bar/leaf.txt");
    }

    /**
     * Asserts that the filtered set of paths contains the given items and nothing more.
     *
     * @param directory the temporary directory containing the files to test
     * @param syntax syntax to test, either an empty string of {@code "glob:"}
     * @param expected the expected paths
     * @throws IOException if an error occurred while listing the files
     */
    private static void assertFilteredFilesContains(final Path directory, final String syntax, final String... expected)
            throws IOException {
        List<String> includes = List.of(syntax + "**/*.txt");
        List<String> excludes = List.of(syntax + "baz/**");
        PathMatcher matcher = PathSelector.of(directory, includes, excludes, false);
        Set<Path> filtered =
                new HashSet<>(Files.walk(directory).filter(matcher::matches).toList());
        for (String path : expected) {
            assertTrue(filtered.remove(directory.resolve(path)), path);
        }
        assertTrue(filtered.isEmpty(), filtered.toString());
    }

    /**
     * Tests the omission of unnecessary excludes.
     *
     * Note: at the time of writing this test (April 2025), the list of excludes goes down from 40 to 17 elements.
     * This is not bad, but we could do better with, for example, a special treatment of the excludes that are
     * for excluding an entire directory.
     */
    @Test
    public void testExcludeOmission() {
        Path directory = Path.of("dummy");
        List<String> includes = List.of("**/*.java");
        List<String> excludes = List.of("baz/**");
        PathMatcher matcher = PathSelector.of(directory, includes, excludes, true);
        String s = matcher.toString();
        assertTrue(
                s.contains("glob:**/*.java") || s.contains("glob:{**/,}*.java"),
                "Expected " + s + " to contain " + "glob:**/*.java" + " or " + "glob:{**/,}*.java");
        assertFalse(
                s.contains("project.pj"),
                "Expected " + s + " to not contain " + "project.pj"); // Unnecessary exclusion should have been omitted.
        assertFalse(s.contains(".DS_Store"), "Expected " + s + " to not contain " + ".DS_Store");
    }

    /**
     * Test to verify the current behavior of ** patterns before implementing brace expansion improvement.
     * This test documents the expected behavior that must be preserved after the optimization.
     */
    @Test
    public void testDoubleAsteriskPatterns(final @TempDir Path directory) throws IOException {
        // Create a nested directory structure to test ** behavior
        Path src = Files.createDirectory(directory.resolve("src"));
        Path main = Files.createDirectory(src.resolve("main"));
        Path java = Files.createDirectory(main.resolve("java"));
        Path test = Files.createDirectory(src.resolve("test"));
        Path testJava = Files.createDirectory(test.resolve("java"));

        // Create files at different levels
        Files.createFile(directory.resolve("root.java"));
        Files.createFile(src.resolve("src.java"));
        Files.createFile(main.resolve("main.java"));
        Files.createFile(java.resolve("deep.java"));
        Files.createFile(test.resolve("test.java"));
        Files.createFile(testJava.resolve("testdeep.java"));

        // Test that ** matches zero or more directories (POSIX behavior)
        PathMatcher matcher = PathSelector.of(directory, List.of("src/**/test/**/*.java"), null, false);

        // Should match files in src/test/java/ (** matches zero dirs before test, zero dirs after test)
        assertTrue(matcher.matches(testJava.resolve("testdeep.java")));

        // Should also match files directly in src/test/ (** matches zero dirs after test)
        assertTrue(matcher.matches(test.resolve("test.java")));

        // Should NOT match files in other paths
        assertFalse(matcher.matches(directory.resolve("root.java")));
        assertFalse(matcher.matches(src.resolve("src.java")));
        assertFalse(matcher.matches(main.resolve("main.java")));
        assertFalse(matcher.matches(java.resolve("deep.java")));
    }

    @Test
    public void testLiteralBracesAreEscapedInMavenSyntax(@TempDir Path directory) throws IOException {
        // Create a file with literal braces in the name
        Files.createDirectories(directory.resolve("dir"));
        Path file = directory.resolve("dir/foo{bar}.txt");
        Files.createFile(file);

        // In Maven syntax (no explicit glob:), user-provided braces must be treated literally
        PathMatcher matcher = PathSelector.of(directory, List.of("**/foo{bar}.txt"), null, false);

        assertTrue(matcher.matches(file));
    }

    @Test
    public void testBraceAlternationOnlyWithExplicitGlob(@TempDir Path directory) throws IOException {
        // Create src/main/java and src/test/java with files
        Path mainJava = Files.createDirectories(directory.resolve("src/main/java"));
        Path testJava = Files.createDirectories(directory.resolve("src/test/java"));
        Path mainFile = Files.createFile(mainJava.resolve("Main.java"));
        Path testFile = Files.createFile(testJava.resolve("Test.java"));

        // Without explicit glob:, braces from user input are escaped and treated literally -> no matches
        PathMatcher mavenSyntax = PathSelector.of(directory, List.of("src/{main,test}/**/*.java"), null, false);
        assertFalse(mavenSyntax.matches(mainFile));
        assertFalse(mavenSyntax.matches(testFile));

        // With explicit glob:, braces should act as alternation and match both
        PathMatcher explicitGlob = PathSelector.of(directory, List.of("glob:src/{main,test}/**/*.java"), null, false);
        assertTrue(explicitGlob.matches(mainFile));
        assertTrue(explicitGlob.matches(testFile));
    }
}
