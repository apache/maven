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
package org.apache.maven.cling.invoker.mvn;

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link MavenParser#parseMavenSettingsOptions}: verifies that
 * {@code <options>} inline text and {@code <options><arg>} structured forms
 * are both accepted and produce the expected Maven options.
 */
class MavenSettingsOptionsTest {

    private final MavenParser parser = new MavenParser();

    // ------------------------------------------------------------------ <options> inline text

    @Test
    void inlineOptionsThreads() {
        // <options>-T4</options>
        Settings settings = Settings.newBuilder().options("-T4").build();
        MavenOptions options = parser.parseMavenSettingsOptions(settings, Path.of("settings.xml"));
        assertEquals("4", options.threads().orElseThrow());
    }

    @Test
    void inlineOptionsMultipleFlags() {
        // <options>-T4 --no-transfer-progress</options>
        Settings settings =
                Settings.newBuilder().options("-T4 --no-transfer-progress").build();
        MavenOptions options = parser.parseMavenSettingsOptions(settings, Path.of("settings.xml"));
        assertEquals("4", options.threads().orElseThrow());
        assertTrue(options.noTransferProgress().orElse(false));
    }

    // ------------------------------------------------------------------ <options><arg> structured

    @Test
    void structuredArgSingleToken() {
        // <options><arg>-T4</arg></options>
        Settings settings = Settings.newBuilder().args(List.of("-T4")).build();
        MavenOptions options = parser.parseMavenSettingsOptions(settings, Path.of("settings.xml"));
        assertEquals("4", options.threads().orElseThrow());
    }

    @Test
    void structuredArgMultipleTokens() {
        // <options>
        //   <arg>-T4</arg>
        //   <arg>--no-transfer-progress</arg>
        // </options>
        Settings settings = Settings.newBuilder()
                .args(List.of("-T4", "--no-transfer-progress"))
                .build();
        MavenOptions options = parser.parseMavenSettingsOptions(settings, Path.of("settings.xml"));
        assertEquals("4", options.threads().orElseThrow());
        assertTrue(options.noTransferProgress().orElse(false));
    }

    @Test
    void structuredArgTakesPrecedenceOverInlineOptions() {
        // When both args (structured) and options (inline) are set, args wins.
        // Here the structured form only sets --no-transfer-progress;
        // the inline text -q should be ignored.
        Settings settings = Settings.newBuilder()
                .args(List.of("--no-transfer-progress"))
                .options("-q")
                .build();
        MavenOptions options = parser.parseMavenSettingsOptions(settings, Path.of("settings.xml"));
        // structured form is preferred — only --no-transfer-progress should be active
        assertTrue(options.noTransferProgress().orElse(false));
        assertTrue(options.quiet().isEmpty() || !options.quiet().orElse(false));
    }
}
