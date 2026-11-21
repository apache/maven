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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that executable() profile activation searches the forked Maven process's PATH without running the command.
 */
class MavenITgh10115ProfileActivationByExecutableTest extends AbstractMavenIntegrationTestCase {
    private static final String COMMAND = "gh10115-profile-tool";
    private static final String BIN = "fake bin with spaces";

    @Test
    void testMissingExecutable() throws Exception {
        var project = createProject();
        verifyActivation(project, false);
    }

    @Test
    void testDirectoryIsNotExecutable() throws Exception {
        var project = createProject();
        Files.createDirectory(project.resolve(BIN).resolve(COMMAND));
        verifyActivation(project, false);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testPosixExecutable() throws Exception {
        var project = createProject();
        createFakeExecutable(project, COMMAND, true);
        verifyActivation(project, true);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testPosixNonExecutable() throws Exception {
        var project = createProject();
        createFakeExecutable(project, COMMAND, false);
        verifyActivation(project, false);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testWindowsExeSuffix() throws Exception {
        var project = createProject();
        createFakeExecutable(project, COMMAND + ".EXE", true);
        verifyActivation(project, true);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testWindowsCmdSuffix() throws Exception {
        var project = createProject();
        createFakeExecutable(project, COMMAND + ".CMD", true);
        verifyActivation(project, true);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testWindowsCustomPathExtIsNotUsed() throws Exception {
        var project = createProject();
        createFakeExecutable(project, COMMAND + ".GH10115", true);
        verifyActivation(project, false);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testWindowsCustomPathExtIsUsed() throws Exception {
        var project = createProject();
        createFakeExecutable(project, COMMAND + ".GH10115", true);
        verifyActivation(project, true, ".EXE;.CMD;.GH10115");
    }

    private Path createProject() throws Exception {
        var fixture = extractResources("gh-10115-profile-activation-by-executable");
        // Resource extraction returns a shared directory. Never reuse its PATH entries or build output between cases.
        var project = Files.createTempDirectory(fixture, getName() + "-");
        Files.copy(fixture.resolve("pom.xml"), project.resolve("pom.xml"));
        Files.createDirectory(project.resolve(BIN));
        return project;
    }

    private void createFakeExecutable(Path project, String name, boolean executable) throws Exception {
        var file = project.resolve(BIN).resolve(name);
        // These are detection-only files, not real tools. An accidental invocation leaves evidence and fails.
        if (File.separatorChar == '\\') {
            Files.writeString(file, "@echo executed>\"%~dp0..\\executed\"\r\n@exit /b 97\r\n");
        } else {
            Files.writeString(file, "#!/bin/sh\n: > \"${0%/*}/../executed\"\nexit 97\n");
            Files.setPosixFilePermissions(
                    file, PosixFilePermissions.fromString(executable ? "rwxr-xr-x" : "rw-r--r--"));
            assertEquals(executable, Files.isExecutable(file), "Fixture must have the requested execute permissions");
        }
    }

    private void verifyActivation(Path project, boolean active) throws Exception {
        verifyActivation(project, active, ".EXE;.CMD");
    }

    private void verifyActivation(Path project, boolean active, String pathExt) throws Exception {
        var support = Files.createDirectory(project.resolve("launcher-tools"));
        if (File.separatorChar == '\\') {
            // mvn.cmd uses findstr for compact option parsing. Do not expose all of System32 on PATH.
            // SystemRoot is optional in the environment, so never dereference it without a fallback.
            var systemRoot = System.getenv("SystemRoot");
            var findstr = Path.of(
                    systemRoot == null || systemRoot.isBlank() ? "C:\\Windows" : systemRoot, "System32", "findstr.exe");
            assertTrue(Files.isRegularFile(findstr), "Missing Maven launcher prerequisite: " + findstr);
            Files.copy(findstr, support.resolve("findstr.exe"));
        } else {
            // The executor invokes bin/mvn, not java directly. Supply only its required external utilities.
            // Resolve these from fixed OS locations, never from the inherited PATH.
            for (var tool : new String[] {"uname", "dirname"}) {
                var usrBin = Path.of("/usr/bin", tool);
                var source = Files.isExecutable(usrBin) ? usrBin : Path.of("/bin", tool);
                assertTrue(Files.isExecutable(source), "Missing Maven launcher prerequisite: " + source);
                Files.createSymbolicLink(support.resolve(tool), source);
            }
        }
        var path = project.resolve(BIN) + File.pathSeparator + support;
        var verifier = newVerifier(project);
        verifier.setForkJvm(true);
        verifier.setAutoclean(false);
        verifier.setEnvironmentVariable("JAVA_HOME", System.getProperty("java.home"));
        verifier.setEnvironmentVariable("MAVEN_SKIP_RC", "true");
        // Replace both spellings, including on case-sensitive hosts. Never append the inherited PATH.
        verifier.setEnvironmentVariable("PATH", path);
        verifier.setEnvironmentVariable("Path", path);
        verifier.setEnvironmentVariable("PATHEXT", pathExt);
        verifier.addCliArgument("validate");
        try {
            verifier.execute();
            verifier.verifyErrorFreeLog();
            var properties = verifier.loadProperties("target/profile.properties");
            assertEquals(
                    active ? "active" : "inactive", properties.getProperty("project.properties.executable.profile"));
        } finally {
            assertFalse(Files.exists(project.resolve("executed")), "Profile activation must not execute the fake tool");
        }
    }
}
