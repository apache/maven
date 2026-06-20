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
import java.util.List;
import java.util.Map;

import org.apache.maven.api.services.model.ProfileActivationContext;
import org.junit.jupiter.api.Test;
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

    // -----------------------------------------------------------------------
    // candidateNames()
    // -----------------------------------------------------------------------

    @Test
    void candidateNamesUnixNoExtension() {
        List<String> names = ExecutableFinder.candidateNames("musl-gcc", false);
        assertEquals(List.of("musl-gcc"), names);
    }

    @Test
    void candidateNamesWindowsNoExtension() {
        List<String> names = ExecutableFinder.candidateNames("musl-gcc", true);
        assertEquals(List.of("musl-gcc", "musl-gcc.exe", "musl-gcc.cmd", "musl-gcc.bat", "musl-gcc.com"), names);
    }

    @Test
    void candidateNamesWindowsAlreadyHasExtension() {
        // When the name already has a Windows extension, no extras should be added.
        List<String> names = ExecutableFinder.candidateNames("myapp.exe", true);
        assertEquals(List.of("myapp.exe"), names);
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

    @Test
    void getPathValueFallsBackToSystemEnv() {
        // No env.PATH in context -> should fall back to System.getenv("PATH")
        ProfileActivationContext ctx = contextWithPath(null);
        String fromEnv = System.getenv("PATH");
        assertEquals(fromEnv, ExecutableFinder.getPathValue(ctx));
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
    void returnsFalseWhenFileIsNotExecutable() throws Exception {
        Path exec = tempDir.resolve("non-exec-tool");
        Files.createFile(exec);
        exec.toFile().setExecutable(false);

        // Only meaningful on POSIX; on Windows the execute bit is not enforced by the JVM.
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("windows")) {
            assertFalse(ExecutableFinder.isExecutableInPath("non-exec-tool", contextWithPath(tempDir.toString())));
        }
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
}
