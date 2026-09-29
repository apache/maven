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
package org.apache.maven.cling.invoker.mvnval;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link CommonsCliValidateOptions} class.
 * Tests that POM paths arrive as positional arguments, that the options carrying a value take
 * one, and that a bad {@code --format} value reaches the invoker rather than being rejected
 * while the options are parsed.
 */
@DisplayName("CommonsCliValidateOptions")
class CommonsCliValidateOptionsTest {

    @Test
    @DisplayName("should report no poms and no format when given no arguments")
    void shouldReportNothingWhenGivenNoArguments() throws ParseException {
        CommonsCliValidateOptions options = CommonsCliValidateOptions.parse(new String[0]);

        assertTrue(options.poms().isEmpty(), "no positional arguments means no poms");
        assertTrue(options.format().isEmpty(), "no --format means no format");
    }

    @Test
    @DisplayName("should take poms from the positional arguments, in order")
    void shouldTakePomsFromPositionalArguments() throws ParseException {
        CommonsCliValidateOptions options = CommonsCliValidateOptions.parse(new String[] {"a/pom.xml", "b/pom.xml"});

        assertEquals(
                List.of("a/pom.xml", "b/pom.xml"),
                options.poms().orElseThrow(),
                "positional arguments should be the poms to validate");
    }

    @Test
    @DisplayName("should read the format from --format")
    void shouldReadFormatFromOption() throws ParseException {
        assertEquals(
                "json",
                CommonsCliValidateOptions.parse(new String[] {"--format", "json"})
                        .format()
                        .orElseThrow());
    }

    @Test
    @DisplayName("should carry a bad --format through to the invoker rather than failing to parse")
    void shouldNotRejectBadFormatWhileParsing() throws ParseException {
        // Failing here would mark the invocation unparseable and exit 1, while the tool documents
        // 2 for bad usage. ValidateInvoker rejects the value and returns that code.
        CommonsCliValidateOptions options = CommonsCliValidateOptions.parse(new String[] {"--format", "xml"});

        assertEquals("xml", options.format().orElseThrow());
    }

    @Test
    @DisplayName("should accept the --val flag the launcher script passes through")
    void shouldAcceptLauncherFlag() throws ParseException {
        // The mvn script selects the tool with --val but does not remove the flag, so the
        // whole argument list, --val included, reaches this parser.
        CommonsCliValidateOptions options = CommonsCliValidateOptions.parse(new String[] {"--val", "a/pom.xml"});

        assertEquals(
                List.of("a/pom.xml"),
                options.poms().orElseThrow(),
                "--val is the launcher's own flag and must not be taken for a pom");
    }

    @Test
    @DisplayName("should keep poms and format independent")
    void shouldKeepPomsAndFormatIndependent() throws ParseException {
        CommonsCliValidateOptions options =
                CommonsCliValidateOptions.parse(new String[] {"--format", "text", "only/pom.xml"});

        assertEquals("text", options.format().orElseThrow());
        assertEquals(List.of("only/pom.xml"), options.poms().orElseThrow());
    }

    @Test
    @DisplayName("should not offer goals or phases in the usage line")
    void shouldNotOfferGoalsInUsage() throws ParseException {
        List<String> help = new ArrayList<>();
        CommonsCliValidateOptions.parse(new String[] {"--help"})
                .displayHelp(
                        ParserRequest.mvnval(new String[0], new JLineMessageBuilderFactory())
                                .build(),
                        help::add);

        String usage =
                help.stream().filter(l -> l.contains("usage:")).findFirst().orElseThrow();
        assertTrue(usage.contains("<pom>"), "mvnval takes POM files: " + usage);
        assertFalse(usage.contains("goal"), "mvnval runs no goals or phases: " + usage);
    }

    @Test
    @DisplayName("should take a value for --mode without swallowing the pom")
    void shouldTakeModeValue() throws ParseException {
        CommonsCliValidateOptions options = CommonsCliValidateOptions.parse(new String[] {"--mode", "raw", "pom.xml"});

        assertEquals("raw", options.mode().orElseThrow());
        assertEquals(List.of("pom.xml"), options.poms().orElseThrow(), "the value must not swallow the pom");
    }

    @Test
    @DisplayName("should take the local repository path")
    void shouldTakeLocalRepositoryPath() throws ParseException {
        CommonsCliValidateOptions options =
                CommonsCliValidateOptions.parse(new String[] {"--local-repository", "/tmp/repo", "pom.xml"});

        assertEquals("/tmp/repo", options.localRepository().orElseThrow());
        assertEquals(List.of("pom.xml"), options.poms().orElseThrow(), "the value must not swallow the pom");
        assertTrue(options.tempLocalRepository().isEmpty(), "the two options are separate");
    }

    @Test
    @DisplayName("should take the throwaway local repository as a flag with no value")
    void shouldTakeTempLocalRepository() throws ParseException {
        CommonsCliValidateOptions options =
                CommonsCliValidateOptions.parse(new String[] {"--temp-local-repository", "pom.xml"});

        assertEquals(true, options.tempLocalRepository().orElseThrow());
        assertEquals(List.of("pom.xml"), options.poms().orElseThrow(), "the flag takes no argument");
    }

    @Test
    @DisplayName("should leave the mode empty when it is not given")
    void shouldLeaveModeEmptyWhenNotGiven() throws ParseException {
        CommonsCliValidateOptions options = CommonsCliValidateOptions.parse(new String[] {"pom.xml"});

        assertTrue(options.mode().isEmpty(), "empty, not a default, so the invoker decides what the default is");
    }
}
