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
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ExecutableFinder}.
 */
class ExecutableFinderTest {

    @TempDir
    Path tempDir;

    /**
     * Minimal stub providing isolated environment properties.
     */
    private static ProfileActivationContext contextWithPath(String pathValue) {
        return contextWithPathAndOs(pathValue, null);
    }

    /**
     * Extended stub that also allows setting {@code os.name} for simulating Windows behaviour.
     */
    private static ProfileActivationContext contextWithPathAndOs(String pathValue, String osName) {
        return contextWithPathAndOs(pathValue, osName, null);
    }

    private static ProfileActivationContext contextWithPathAndOs(String pathValue, String osName, String pathExt) {
        return contextWithPathAndOs(
                pathValue, osName, pathExt, osName != null && osName.startsWith("Windows") ? ";" : File.pathSeparator);
    }

    private static ProfileActivationContext contextWithPathAndOs(
            String pathValue, String osName, String pathExt, String separator) {
        Map<String, String> props = new HashMap<>();
        if (pathValue != null) {
            props.put("env.PATH", pathValue);
        }
        if (osName != null) {
            props.put("os.name", osName);
        }
        if (pathExt != null) {
            props.put("env.PATHEXT", pathExt);
        }
        if (separator != null) {
            props.put("path.separator", separator);
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

            @Override
            public void addProfileProperties(
                    java.util.Collection<org.apache.maven.api.model.Profile> activatedProfiles) {
                // no-op for test context
            }
        };
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
        assertTrue(
                ExecutableFinder.isExecutableInPath(absPath, contextWithPathAndOs("", System.getProperty("os.name"))));
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

    @Test
    void honorsCustomWindowsPathExt() throws Exception {
        Files.createFile(tempDir.resolve("docker.custom"));
        var context = contextWithPathAndOs(tempDir.toString(), "Windows 11", " ; .CuStOm ;.CUSTOM;;");
        assertTrue(ExecutableFinder.isExecutableInPath("docker", context));
        assertTrue(ExecutableFinder.isExecutableInPath("docker.custom", context));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void matchesWindowsFileNamesCaseInsensitively() throws Exception {
        Files.createFile(tempDir.resolve("DOCKER.ExE"));
        assertTrue(ExecutableFinder.isExecutableInPath(
                "docker", contextWithPathAndOs(tempDir.toString(), "Windows 11", ".EXE")));
    }

    @Test
    void respectsExcludedAndEmptyWindowsExtensions() throws Exception {
        Files.createFile(tempDir.resolve("docker.exe"));
        assertFalse(ExecutableFinder.isExecutableInPath(
                "docker", contextWithPathAndOs(tempDir.toString(), "Windows 11", ".CMD")));
        assertFalse(ExecutableFinder.isExecutableInPath(
                "docker", contextWithPathAndOs(tempDir.toString(), "Windows 11", "")));
        assertTrue(ExecutableFinder.isExecutableInPath(
                "docker.exe", contextWithPathAndOs(tempDir.toString(), "Windows 11", "")));
    }

    @Test
    void usesWindowsDelimiterAndQuotedDirectories() throws Exception {
        var directory = Files.createDirectory(tempDir.resolve("directory with spaces"));
        Files.createFile(directory.resolve("docker.cmd"));
        var path = ";" + tempDir.resolve("missing") + ";\"" + directory + "\";;";
        assertTrue(ExecutableFinder.isExecutableInPath("docker", contextWithPathAndOs(path, "Windows 11", ".CMD")));
    }

    @Test
    void usesLiteralSeparatorFromContextIndependentlyOfOsName() throws Exception {
        Files.createFile(tempDir.resolve("docker.cmd"));
        var path = tempDir.resolve("missing") + "|" + tempDir;
        assertTrue(
                ExecutableFinder.isExecutableInPath("docker", contextWithPathAndOs(path, "Windows 11", ".CMD", "|")));
        assertFalse(
                ExecutableFinder.isExecutableInPath("docker", contextWithPathAndOs(path, "Windows 11", ".CMD", ";")));
    }

    @Test
    void fallsBackToNativeSeparatorWhenPropertyIsMissingOrEmpty() throws Exception {
        Files.createFile(tempDir.resolve("docker.cmd"));
        var path = tempDir.resolve("missing") + File.pathSeparator + tempDir;
        assertTrue(
                ExecutableFinder.isExecutableInPath("docker", contextWithPathAndOs(path, "Windows 11", ".CMD", null)));
        assertTrue(ExecutableFinder.isExecutableInPath("docker", contextWithPathAndOs(path, "Windows 11", ".CMD", "")));
    }

    @Test
    void ignoresInvalidPathEntries() throws Exception {
        Files.createFile(tempDir.resolve("docker.cmd"));
        assertTrue(ExecutableFinder.isExecutableInPath(
                "docker", contextWithPathAndOs("bad\0directory;" + tempDir, "Windows 11", ".CMD")));
    }

    @Test
    void rejectsInvalidExplicitPathsAndExtensionEntries() throws Exception {
        Files.createFile(tempDir.resolve("docker.cmd"));
        var context = contextWithPathAndOs(tempDir.toString(), "Windows 11", ".bad\0;.bad/path;.bad\\path;.bad:path");
        assertFalse(ExecutableFinder.isExecutableInPath("docker", context));
        assertFalse(ExecutableFinder.isExecutableInPath("bad\0/path", context));
    }

    @Test
    void probesExtensionsForExplicitPaths() throws Exception {
        Files.createFile(tempDir.resolve("docker.custom"));
        assertTrue(ExecutableFinder.isExecutableInPath(
                tempDir.resolve("docker").toString(), contextWithPathAndOs("", "Windows 11", ".CUSTOM")));
    }

    @Test
    void rejectsDirectoriesAndMissingPath() throws Exception {
        Files.createDirectory(tempDir.resolve("docker.exe"));
        assertFalse(ExecutableFinder.isExecutableInPath(
                "docker", contextWithPathAndOs(tempDir.toString(), "Windows 11", ".EXE")));
        assertFalse(ExecutableFinder.isExecutableInPath("docker", contextWithPath(null)));
        assertFalse(ExecutableFinder.isExecutableInPath("", contextWithPath(tempDir.toString())));
        assertFalse(ExecutableFinder.isExecutableInPath(null, contextWithPath(tempDir.toString())));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void searchesMultiplePosixDirectoriesWithoutTrimmingSpaces() throws Exception {
        var directory = Files.createDirectory(tempDir.resolve(" directory with spaces "));
        var executable = Files.createFile(directory.resolve("docker"));
        assertTrue(executable.toFile().setExecutable(true));
        var path = ":bad\0directory:" + tempDir.resolve("missing") + ":" + directory + "::";
        assertTrue(ExecutableFinder.isExecutableInPath("docker", contextWithPathAndOs(path, "Linux")));
        assertFalse(ExecutableFinder.isExecutableInPath("docker", contextWithPathAndOs(tempDir.toString(), "Linux")));
    }
}
