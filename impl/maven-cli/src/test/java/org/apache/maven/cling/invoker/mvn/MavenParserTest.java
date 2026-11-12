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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.maven.api.cli.mvn.MavenOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MavenParser#parseMavenConfigOptions(Path)}.
 *
 * <p>The {@code .mvn/maven.config} file uses a <em>one-argument-per-line</em> format, explicitly
 * modelled after Java {@code @argfiles} (introduced by MNG-7131, commit {@code 331c5c3435},
 * July 2021). Each line is a single argv token — whitespace inside a line is never re-split.
 * This allows property values containing spaces to be expressed via the two-line form:
 * <pre>
 *   --define
 *   label=Apache Maven
 * </pre>
 * PR #13093 (commit {@code 2829a73de4}) broke this contract by re-tokenizing each line with
 * {@code CleanArgument.splitLine}; it was reverted in PR #13148. These tests guard against
 * that regression.
 */
class MavenParserTest {

    private final MavenParser parser = new MavenParser();

    /**
     * Core MNG-7131 regression test: a property value containing a space must survive the
     * round-trip through {@code parseMavenConfigOptions} intact when supplied via the two-line
     * {@code --define} / {@code value with spaces} form.
     *
     * <p>PR #13093 would have turned {@code "label=Apache Maven"} into two tokens
     * {@code ["label=Apache", "Maven"]}, making Commons CLI reject the parse. This test fails
     * when that bug is present.
     */
    @Test
    void testMultiLineDefineWithSpacedValue(@TempDir Path tempDir) throws Exception {
        Path config = tempDir.resolve("maven.config");
        Files.writeString(config, """
                        --define
                        label=Apache Maven
                        """, StandardCharsets.UTF_8);

        MavenOptions options = parser.parseMavenConfigOptions(config);

        assertTrue(options.userProperties().isPresent(), "userProperties must be present");
        Map<String, String> props = options.userProperties().get();
        assertEquals("Apache Maven", props.get("label"), "Space in property value must be preserved");
    }

    /**
     * Two-line {@code --define} / {@code value} form without spaces must work correctly.
     */
    @Test
    void testMultiLineDefineSimple(@TempDir Path tempDir) throws Exception {
        Path config = tempDir.resolve("maven.config");
        Files.writeString(config, """
                        --define
                        revision=1.0.0
                        """, StandardCharsets.UTF_8);

        MavenOptions options = parser.parseMavenConfigOptions(config);

        assertTrue(options.userProperties().isPresent(), "userProperties must be present");
        Map<String, String> props = options.userProperties().get();
        assertEquals("1.0.0", props.get("revision"), "Simple --define two-line form must work");
    }

    /**
     * Compact single-line {@code -Dkey=value} form must continue to work alongside the two-line form.
     */
    @Test
    void testSingleLineDefine(@TempDir Path tempDir) throws Exception {
        Path config = tempDir.resolve("maven.config");
        Files.writeString(config, """
                        -Drevision=1.0.0
                        """, StandardCharsets.UTF_8);

        MavenOptions options = parser.parseMavenConfigOptions(config);

        assertTrue(options.userProperties().isPresent(), "userProperties must be present");
        Map<String, String> props = options.userProperties().get();
        assertEquals("1.0.0", props.get("revision"), "Single-line -D form must work");
    }

    /**
     * Comment lines (starting with {@code #}) and blank lines must be ignored; only real option
     * lines are forwarded to Commons CLI.
     */
    @Test
    void testCommentsAndEmptyLinesSkipped(@TempDir Path tempDir) throws Exception {
        Path config = tempDir.resolve("maven.config");
        Files.writeString(config, """
                        # a comment

                        -Drevision=1.0.0
                        """, StandardCharsets.UTF_8);

        MavenOptions options = parser.parseMavenConfigOptions(config);

        assertTrue(options.userProperties().isPresent(), "userProperties must be present");
        Map<String, String> props = options.userProperties().get();
        assertEquals(1, props.size(), "Exactly one property must be parsed");
        assertEquals("1.0.0", props.get("revision"));
    }

    /**
     * Goals and phases are not allowed in {@code maven.config}; the parser must reject them with
     * an {@link IllegalArgumentException}.
     */
    @Test
    void testGoalRejected(@TempDir Path tempDir) throws Exception {
        Path config = tempDir.resolve("maven.config");
        Files.writeString(config, """
                        verify
                        """, StandardCharsets.UTF_8);

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parseMavenConfigOptions(config),
                "Goals must not be allowed in maven.config");
    }
}
