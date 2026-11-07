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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Integration tests for {@code <options>} in project-scoped {@code .mvn/settings.xml}.
 *
 * <ul>
 *   <li>Inline text form: {@code <options>--no-transfer-progress -q</options>}
 *   <li>Structured form: {@code <options><arg>--no-transfer-progress</arg><arg>-q</arg></options>}
 *   <li>Precedence over {@code .mvn/maven.config} when both are present
 * </ul>
 */
class MavenITSettingsOptionsTest extends AbstractMavenIntegrationTestCase {

    /**
     * {@code <options>} inline text form suppresses transfer progress and quiets output.
     */
    @Test
    void optionsInlineTextApplied() throws Exception {
        Path testDir = extractResources("settings-options");

        Verifier verifier = newVerifier(testDir.resolve("inline"));
        verifier.setLogFileName("inline.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // -q suppresses [INFO] lines
        assertFalse(
                verifier.loadFile(verifier.getBasedir(), "inline.txt", false).stream()
                        .anyMatch(l -> l.contains("[INFO] Building")),
                "Quiet mode from <options> must suppress [INFO] Building line");
    }

    /**
     * {@code <options>} structured {@code <arg>} form applies the same flags.
     */
    @Test
    void optionsStructuredArgApplied() throws Exception {
        Path testDir = extractResources("settings-options");

        Verifier verifier = newVerifier(testDir.resolve("structured"));
        verifier.setLogFileName("structured.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertFalse(
                verifier.loadFile(verifier.getBasedir(), "structured.txt", false).stream()
                        .anyMatch(l -> l.contains("[INFO] Building")),
                "Quiet mode from <options><arg> must suppress [INFO] Building line");
    }

    /**
     * When both {@code <options>} in settings and {@code .mvn/maven.config} are present,
     * settings {@code <options>} wins and {@code maven.config} is ignored.
     * We verify that only the flag from settings took effect (not the conflicting one from maven.config).
     */
    @Test
    void settingsOptionsPrecedesOverMavenConfig() throws Exception {
        Path testDir = extractResources("settings-options");

        Verifier verifier = newVerifier(testDir.resolve("precedence"));
        verifier.setLogFileName("precedence.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // settings.xml has -q (quiet); maven.config has --debug
        // If maven.config were honoured, [DEBUG] lines would appear
        assertFalse(
                verifier.loadFile(verifier.getBasedir(), "precedence.txt", false).stream()
                        .anyMatch(l -> l.startsWith("[DEBUG]")),
                "maven.config must be ignored when <options> is present in settings");
        assertFalse(
                verifier.loadFile(verifier.getBasedir(), "precedence.txt", false).stream()
                        .anyMatch(l -> l.contains("[INFO] Building")),
                "Quiet mode from settings <options> must suppress [INFO] Building");
    }
}
