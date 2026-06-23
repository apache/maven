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
package org.apache.maven.impl.model.profile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.api.services.model.ProfileActivationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ExecutableFinder}.
 */
class ExecutableFinderTest {

    @TempDir
    Path tempDir;

    /**
     * Minimal stub – only {@link ProfileActivationContext#getSystemProperty(String)} is used
     * by {@link ExecutableFinder#getPathValue}.
     */
    private static ProfileActivationContext contextWithPath(String pathValue) {
        Map<String, String> props = pathValue != null ? Map.of("env.PATH", pathValue) : Map.of();
        return new ProfileActivationContext() {
            @Override
            public boolean isProfileActive(String profileId) {
                return false;
            }

            @Override
            public boolean isProfileInactive(String profileId) {
                return false;
            }

            @Override
            public String getSystemProperty(String key) {
                return props.get(key);
            }

            @Override
            public String getUserProperty(String key) {
                return null;
            }

            @Override
            public String getModelProperty(String key) {
                return null;
            }

            @Override
            public String getModelArtifactId() {
                return null;
            }

            @Override
            public String getModelPackaging() {
                return null;
            }

            @Override
            public String getModelRootDirectory() {
                return null;
            }

            @Override
            public String getModelBaseDirectory() {
                return null;
            }

            @Override
            public String interpolatePath(String path) {
                return path;
            }

            @Override
            public boolean exists(String path, boolean glob) {
                return false;
            }
        };
    }

    /**
     * Extended stub that also allows setting {@code os.name} for simulating Windows behaviour.
     */
    private static ProfileActivationContext contextWithPathAndOs(String pathValue, String osName) {
        Map<String, String> props = new HashMap<>();
        if (pathValue != null) {
            props.put("env.PATH", pathValue);
        }
        if (osName != null) {
            props.put("os.name", osName);
        }
        return new ProfileActivationContext() {
            @Override
            public boolean isProfileActive(String profileId) {
                return false;
            }

            @Override
            public boolean isProfileInactive(String profileId) {
                return false;
            }

            @Override
            public String getSystemProperty(String key) {
                return props.get(key);
            }

            @Override
            public String getUserProperty(String key) {
                return null;
            }

            @Override
            public String getModelProperty(String key) {
                return null;
            }

            @Override
            public String getModelArtifactId() {
                return null;
            }

            @Override
            public String getModelPackaging() {
                return null;
            }

            @Override
            public String getModelRootDirectory() {
                return null;
            }

            @Override
            public String getModelBaseDirectory() {
                return null;
            }

            @Override
            public String interpolatePath(String path) {
                return path;
            }

            @Override
            public boolean exists(String path, boolean glob) {
                return false;
            }
        };
    }

    // -----------------------------------------------------------------------
    // getPathValue()
    // -----------------------------------------------------------------------

    @Test
    void getPathValueFromContext() {
        String expected = "/usr/bin" + File.pathSeparator + "/usr/local/bin";
        ProfileActivationContext ctx = contextWithPath(expected);
        assertEquals(expected, ExecutableFinder.getPathValue(ctx));
    }

    // -----------------------------------------------------------------------
    // isExecutableInPath() - plain name
    // -----------------------------------------------------------------------

    @Test
    void findsExecutableByName() throws Exception {
        Path exec = tempDir.resolve("my-tool");
        Files.createFile(exec);
        exec.toFile().setExecutable(true);

        assertTrue(ExecutableFinder.isExecutableInPath("my-tool", contextWithPath(tempDir.toString())));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void returnsFalseWhenFileIsNotExecutable() throws Exception {
        Path exec = tempDir.resolve("non-exec-tool");
        Files.createFile(exec);
        exec.toFile().setExecutable(false);

        // Only meaningful on POSIX; on Windows the execute bit is not enforced by the JVM.
        assertFalse(ExecutableFinder.isExecutableInPath("non-exec-tool", contextWithPath(tempDir.toString())));
    }

    @Test
    void returnsFalseWhenToolNotInPath() {
        assertFalse(ExecutableFinder.isExecutableInPath(
                "this-tool-definitely-does-not-exist-anywhere-12345", contextWithPath(tempDir.toString())));
    }

    @Test
    void returnsFalseForEmptyPath() {
        assertFalse(ExecutableFinder.isExecutableInPath("any-tool", contextWithPath("")));
    }

    // -----------------------------------------------------------------------
    // isExecutableInPath() - absolute / relative path
    // -----------------------------------------------------------------------

    @Test
    void findsExecutableByAbsolutePath() throws Exception {
        Path exec = tempDir.resolve("abs-exec");
        Files.createFile(exec);
        exec.toFile().setExecutable(true);

        String absPath = exec.toAbsolutePath().toString();
        // Absolute paths contain path separators -> direct check
        assertTrue(ExecutableFinder.isExecutableInPath(absPath, contextWithPath("")));
    }

    @Test
    void returnsFalseForAbsolutePathThatDoesNotExist() {
        assertFalse(ExecutableFinder.isExecutableInPath("/no/such/path/to/some/binary/42", contextWithPath("")));
    }

    @Test
    void returnsFalseForDirectoryPath() throws Exception {
        // Directories must not be accepted even if they exist.
        assertFalse(ExecutableFinder.isExecutableInPath(tempDir.toAbsolutePath().toString(), contextWithPath("")));
    }

    // -----------------------------------------------------------------------
    // Windows extension probing (simulated via os.name in context)
    // -----------------------------------------------------------------------

    @Test
    void findsExecutableWithWindowsExtensionInPath() throws Exception {
        // Simulate Windows: create my-tool.exe and search for "my-tool"
        Path exec = tempDir.resolve("my-tool.exe");
        Files.createFile(exec);

        ProfileActivationContext ctx = contextWithPathAndOs(tempDir.toString(), "Windows 10");
        assertTrue(ExecutableFinder.isExecutableInPath("my-tool", ctx));
    }

    @Test
    void findsExecutableWithWindowsExtensionByDirectPath() throws Exception {
        // Simulate Windows: create tool.exe and search for the direct path without extension
        Path exec = tempDir.resolve("tool.exe");
        Files.createFile(exec);

        String directPath = tempDir.resolve("tool").toAbsolutePath().toString();
        ProfileActivationContext ctx = contextWithPathAndOs("", "Windows 10");
        assertTrue(ExecutableFinder.isExecutableInPath(directPath, ctx));
    }

    @Test
    void doesNotAppendExtensionOnNonWindows() throws Exception {
        // On non-Windows, searching for "my-tool" must NOT find "my-tool.exe"
        Path exec = tempDir.resolve("my-tool.exe");
        Files.createFile(exec);

        ProfileActivationContext ctx = contextWithPathAndOs(tempDir.toString(), "Linux");
        assertFalse(ExecutableFinder.isExecutableInPath("my-tool", ctx));
    }
}
