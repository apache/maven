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
package org.apache.maven.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify NIO2 migration of Verifier class.
 */
public class VerifierNIO2Test {

    @TempDir
    Path tempDir;

    private Verifier verifier;

    @BeforeEach
    void setUp() throws VerificationException {
        verifier = new Verifier(Paths.get(tempDir.toString()), null);
    }

    @Test
    void testLoadPropertiesWithPath() throws IOException, VerificationException {
        // Create a test properties file
        Path propsFile = tempDir.resolve("test.properties");
        Files.writeString(propsFile, "key1=value1\nkey2=value2\n");

        // Test the new Path-based method
        Properties props = verifier.loadProperties(propsFile);
        assertEquals("value1", props.getProperty("key1"));
        assertEquals("value2", props.getProperty("key2"));
    }

    @Test
    void testLoadFileWithPath() throws IOException, VerificationException {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "line1\nline2\n# comment\nline3\n");

        // Test the new Path-based method
        List<String> lines = verifier.loadFile(testFile);
        assertEquals(3, lines.size());
        assertEquals("line1", lines.get(0));
        assertEquals("line2", lines.get(1));
        assertEquals("line3", lines.get(2));
    }

    @Test
    void testFilterFileWithPath() throws IOException {
        // Create source file
        Path srcFile = tempDir.resolve("src.txt");
        Files.writeString(srcFile, "Hello @name@, welcome to @place@!");

        // Create filter map
        Map<String, String> filterMap = new HashMap<>();
        filterMap.put("@name@", "World");
        filterMap.put("@place@", "Maven");

        // Test the new Path-based method
        Path dstFile = tempDir.resolve("dst.txt");
        Path result = verifier.filterFile(srcFile, dstFile, null, filterMap);

        assertEquals(dstFile, result);
        assertTrue(Files.exists(result));
        String content = Files.readString(result);
        assertEquals("Hello World, welcome to Maven!", content);
    }

    @Test
    void testDeleteDirectoryRecursively() throws IOException {
        // Create a directory structure
        Path testDir = tempDir.resolve("testdir");
        Path subDir = testDir.resolve("subdir");
        Files.createDirectories(subDir);
        
        Path file1 = testDir.resolve("file1.txt");
        Path file2 = subDir.resolve("file2.txt");
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        assertTrue(Files.exists(testDir));
        assertTrue(Files.exists(subDir));
        assertTrue(Files.exists(file1));
        assertTrue(Files.exists(file2));

        // Test directory deletion
        verifier.deleteDirectory("testdir");

        assertFalse(Files.exists(testDir));
        assertFalse(Files.exists(subDir));
        assertFalse(Files.exists(file1));
        assertFalse(Files.exists(file2));
    }

    @Test
    void testVerifyFilePresence() throws IOException, VerificationException {
        // Create a test file
        Path testFile = tempDir.resolve("present.txt");
        Files.writeString(testFile, "content");

        // Test file presence verification
        verifier.verifyFilePresent("present.txt");

        // Test file absence verification
        verifier.verifyFileNotPresent("absent.txt");

        // Test exception when expected file is not present
        assertThrows(VerificationException.class, () -> {
            verifier.verifyFilePresent("absent.txt");
        });

        // Test exception when unwanted file is present
        assertThrows(VerificationException.class, () -> {
            verifier.verifyFileNotPresent("present.txt");
        });
    }

    @Test
    void testVerifyFilePresenceWithWildcard() throws IOException, VerificationException {
        // Create test files
        Path testFile1 = tempDir.resolve("test-1.txt");
        Path testFile2 = tempDir.resolve("test-2.txt");
        Files.writeString(testFile1, "content1");
        Files.writeString(testFile2, "content2");

        // Test wildcard file presence verification
        verifier.verifyFilePresent("test-*.txt");

        // Test wildcard file absence verification
        verifier.verifyFileNotPresent("missing-*.txt");
    }
}
