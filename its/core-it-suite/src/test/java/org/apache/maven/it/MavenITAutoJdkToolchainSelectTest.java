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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Integration tests for automatic JDK toolchain selection.
 * <p>
 * When the running JDK does not support the project's required {@code --source}/{@code --release}
 * level, Maven should automatically search configured toolchains and select a compatible JDK.
 */
class MavenITAutoJdkToolchainSelectTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verifies that Maven auto-selects a JDK toolchain when the running JDK
     * does not support the project's required source level.
     * <p>
     * The project declares {@code maven.compiler.source=6}, which is not supported
     * by JDK 12+ (minimum source level 7 for JDK 12-20, 8 for JDK 21+).
     * A JDK 11 toolchain is configured in toolchains.xml and should be auto-selected.
     */
    @Test
    void testAutoSelectToolchainWhenSourceLevelUnsupported() throws Exception {
        Path testDir = extractResources("auto-jdk-toolchain-select");

        // Create a fake JDK home with bin/javac for the toolchain
        Path javaHome = testDir.resolve("fakeJdk11");
        Path binDir = javaHome.resolve("bin");
        Files.createDirectories(binDir);
        if (!Files.exists(binDir.resolve("javac"))) {
            ItUtils.createFile(binDir.resolve("javac"));
        }
        if (!Files.exists(binDir.resolve("javac.exe"))) {
            ItUtils.createFile(binDir.resolve("javac.exe"));
        }

        Verifier verifier = newVerifier(testDir);
        // Clear the default compiler properties set by newVerifier() — the POM defines
        // maven.compiler.source=6 and we need the effective model to reflect that, not
        // the system property override of 8 from MAVEN_OPTS.
        verifier.getSystemProperties().remove("maven.compiler.source");
        verifier.getSystemProperties().remove("maven.compiler.target");
        verifier.getSystemProperties().remove("maven.compiler.release");

        Map<String, String> filterProps = verifier.newDefaultFilterMap();
        filterProps.put("@javaHome@", javaHome.toString());
        verifier.filterFile("toolchains.xml", "toolchains.xml", filterProps);

        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("--toolchains");
        verifier.addCliArgument("toolchains.xml");
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify the auto-selection warning was logged
        verifier.verifyTextInLog("Automatically selected JDK");

        // Verify the toolchain was auto-selected and find-tool found javac
        verifier.verifyFilePresent("target/tool.properties");
        Properties toolProps = verifier.loadProperties("target/tool.properties");
        assertEquals("jdk", toolProps.getProperty("toolchain.type"), "Auto-selected toolchain type should be 'jdk'");
    }

    /**
     * Verifies that Maven does NOT auto-select a JDK toolchain when the running
     * JDK already supports the project's required source level.
     * <p>
     * The project declares {@code maven.compiler.source=11}, which is supported
     * by JDK 12+ (all current CI JDKs). No auto-selection should occur.
     */
    @Test
    void testNoAutoSelectWhenSourceLevelSupported() throws Exception {
        Path testDir = extractResources("auto-jdk-toolchain-no-select");

        // Create a fake JDK home (needed to create a valid toolchain)
        Path javaHome = testDir.resolve("fakeJdk8");
        Path binDir = javaHome.resolve("bin");
        Files.createDirectories(binDir);
        if (!Files.exists(binDir.resolve("javac"))) {
            ItUtils.createFile(binDir.resolve("javac"));
        }
        if (!Files.exists(binDir.resolve("javac.exe"))) {
            ItUtils.createFile(binDir.resolve("javac.exe"));
        }

        Verifier verifier = newVerifier(testDir);
        // Clear the default compiler properties to let the POM properties be used
        verifier.getSystemProperties().remove("maven.compiler.source");
        verifier.getSystemProperties().remove("maven.compiler.target");
        verifier.getSystemProperties().remove("maven.compiler.release");

        Map<String, String> filterProps = verifier.newDefaultFilterMap();
        filterProps.put("@javaHome@", javaHome.toString());
        verifier.filterFile("toolchains.xml", "toolchains.xml", filterProps);

        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("--toolchains");
        verifier.addCliArgument("toolchains.xml");
        verifier.addCliArgument("initialize");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify no auto-selection warning was logged
        verifier.verifyTextNotInLog("Automatically selected JDK");

        // Verify no toolchain was auto-selected (find-tool returns nothing)
        verifier.verifyFilePresent("target/tool.properties");
        Properties toolProps = verifier.loadProperties("target/tool.properties");
        assertNull(
                toolProps.getProperty("toolchain.type"),
                "No toolchain should be auto-selected when running JDK supports the source level");
    }
}
