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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Exercises executable() through the real POSIX launcher with a native Windows JVM.
 * Optional shell locations can be supplied as JVM properties maven.it.gitBash and maven.it.cygwinBash.
 */
@EnabledOnOs(OS.WINDOWS)
class MavenITgh10115ExecutableWindowsShellTest extends AbstractMavenIntegrationTestCase {
    private static final String COMMAND = "gh10115-profile-tool";
    private static final String DEFAULT_PATHEXT = ".COM;.EXE;.BAT;.CMD";
    // Written to a temp file to avoid Windows CreateProcess argument-quoting issues
    // when passing multi-line scripts with double quotes via ProcessBuilder + -c.
    private static final String SCRIPT = """
            set -eu
            # Never expose the inherited host PATH, even in the missing-command case.
            export PATH=/usr/bin:/bin
            fake_path=$(cygpath --unix --path "${IT_FAKE_PATH}")
            export PATH="/usr/bin:/bin:${fake_path}"
            export JAVA_HOME=$(cygpath --unix "${IT_JAVA_HOME}")
            export MAVEN_HOME=$(cygpath --unix "${IT_MAVEN_HOME}")
            export MAVEN_SKIP_RC=true
            unset MAVEN_OPTS MAVEN_ARGS MAVEN_CONFIG MAVEN_PROJECTBASEDIR
            exec "${MAVEN_HOME}/bin/mvn" -B -e --install-settings "${IT_SETTINGS}" "-Dmaven.repo.local=${IT_REPO}" "-Duser.home=${IT_USER_HOME}" validate
            """;

    @Test
    void testGitBash() throws Exception {
        var candidates = new ArrayList<Path>();
        for (var variable : new String[] {"ProgramFiles", "ProgramFiles(x86)", "LOCALAPPDATA"}) {
            var root = System.getenv(variable);
            if (root != null && !root.isBlank()) {
                candidates.add(Path.of(root, variable.equals("LOCALAPPDATA") ? "Programs/Git" : "Git", "bin/bash.exe"));
            }
        }
        candidates.add(Path.of("C:/Program Files/Git/bin/bash.exe"));
        verifyShell(findShell("maven.it.gitBash", candidates));
    }

    @Test
    void testCygwin() throws Exception {
        var drive = System.getenv().getOrDefault("SystemDrive", "C:");
        verifyShell(findShell(
                "maven.it.cygwinBash",
                List.of(Path.of(drive + "/cygwin64/bin/bash.exe"), Path.of(drive + "/cygwin/bin/bash.exe"))));
    }

    private Path findShell(String property, Iterable<Path> candidates) {
        var configured = System.getProperty(property);
        if (configured != null) {
            assertFalse(configured.isBlank(), property + " must name a bash.exe file");
            var shell = Path.of(configured).toAbsolutePath();
            assertTrue(Files.isRegularFile(shell) && Files.isExecutable(shell), property + " is invalid: " + shell);
            return shell;
        }
        for (var candidate : candidates) {
            if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                return candidate.toAbsolutePath();
            }
        }
        assumeTrue(
                false, "Shell not installed; set -D" + property + "=<absolute path to bash.exe> to enable this test");
        throw new IllegalStateException("Unreachable after failed assumption");
    }

    private void verifyShell(Path shell) throws Exception {
        verifyActivation(shell, ".cmd", DEFAULT_PATHEXT, true);
        verifyActivation(shell, ".exe", DEFAULT_PATHEXT, true);
        verifyActivation(shell, ".gh10115", DEFAULT_PATHEXT + ";.GH10115", true);
        verifyActivation(shell, ".gh10115", DEFAULT_PATHEXT, false);
        verifyActivation(shell, null, DEFAULT_PATHEXT + ";.GH10115", false);
    }

    private void verifyActivation(Path shell, String suffix, String pathExt, boolean active) throws Exception {
        var fixture = extractResources("gh-10115-profile-activation-by-executable");
        var project = Files.createTempDirectory(fixture, getName() + "-").toAbsolutePath();
        // The unique command name avoids collisions with tools installed in the shell's own bin directories.
        Files.copy(fixture.resolve("pom.xml"), project.resolve("pom.xml"));
        var fakeBin = Files.createDirectory(project.resolve("fake bin with spaces"));
        if (suffix != null) {
            // Detection only: an accidental shell/batch invocation leaves evidence and exits unsuccessfully.
            var content = suffix.equals(".cmd")
                    ? "@echo executed>\"%~dp0..\\executed\"\r\n@exit /b 97\r\n"
                    : "#!/bin/sh\n: > \"${0%/*}/../executed\"\nexit 97\n";
            Files.writeString(fakeBin.resolve(COMMAND + suffix), content);
        }

        var log = project.resolve("mvn-output.txt");
        // Write the script to a file to avoid Windows CreateProcess argument-quoting
        // issues when passing multi-line scripts with double quotes via -c.
        var scriptFile = project.resolve("run-maven.sh");
        Files.writeString(scriptFile, SCRIPT, StandardCharsets.UTF_8);
        var builder = new ProcessBuilder(shell.toString(), "--noprofile", "--norc", scriptFile.toString())
                .directory(project.toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        var env = builder.environment();
        // Noninteractive bash reads BASH_ENV even with --noprofile/--norc. Also discard inherited MSYS overrides.
        for (var name : new String[] {
            "BASH_ENV",
            "ENV",
            "MAVEN_OPTS",
            "MAVEN_ARGS",
            "MAVEN_CONFIG",
            "MSYS2_ENV_CONV_EXCL",
            "MSYS2_ARG_CONV_EXCL",
            "MSYS_NO_PATHCONV",
            "JAVA_TOOL_OPTIONS",
            "JDK_JAVA_OPTIONS",
            "_JAVA_OPTIONS"
        }) {
            env.remove(name);
        }
        env.put("PATH", shell.getParent().toString());
        env.put("PATHEXT", pathExt);
        env.put("MAVEN_SKIP_RC", "true");
        env.put("IT_FAKE_PATH", fakeBin.toString());
        env.put(
                "IT_JAVA_HOME",
                Path.of(System.getProperty("java.home")).toAbsolutePath().toString());
        env.put(
                "IT_MAVEN_HOME",
                Path.of(System.getProperty("maven.home")).toAbsolutePath().toString());
        env.put(
                "IT_REPO",
                Path.of(System.getProperty(
                                "maven.test.repo.local",
                                project.resolve("repository").toString()))
                        .toAbsolutePath()
                        .toString());
        env.put(
                "IT_USER_HOME",
                Path.of(System.getProperty(
                                "maven.test.user.home",
                                project.resolve("user-home").toString()))
                        .toAbsolutePath()
                        .toString());
        env.put(
                "IT_SETTINGS",
                Path.of(System.getProperty("maven.it.global-settings.dir", "target/test-classes"))
                        .resolve("settings-remote.xml")
                        .toAbsolutePath()
                        .toString());

        var process = builder.start();
        try {
            var ended = process.waitFor(2, TimeUnit.MINUTES);
            if (!ended) {
                // The launcher can still be waiting for Java: do not leave that child running on CI.
                try (var descendants = process.descendants()) {
                    descendants.forEach(ProcessHandle::destroyForcibly);
                }
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
            var output = Files.readString(log, StandardCharsets.UTF_8);
            var diagnostic = shell + " (suffix=" + suffix + ", PATHEXT=" + pathExt + "), log: " + log + "\n" + output;
            assertTrue(ended, "Maven did not end in time: " + diagnostic);
            assertEquals(0, process.exitValue(), diagnostic);
            assertTrue(output.contains("BUILD SUCCESS"), diagnostic);
            var marker = project.resolve("target/profile.properties");
            assertTrue(Files.isRegularFile(marker), "No profile marker: " + diagnostic);
            var properties = new Properties();
            try (var input = Files.newInputStream(marker)) {
                properties.load(input);
            }
            assertEquals(
                    active ? "active" : "inactive",
                    properties.getProperty("project.properties.executable.profile"),
                    diagnostic);
        } finally {
            if (process.isAlive()) {
                try (var descendants = process.descendants()) {
                    descendants.forEach(ProcessHandle::destroyForcibly);
                }
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
            assertFalse(Files.exists(project.resolve("executed")), "Activation executed the fake tool; see " + log);
        }
    }
}
