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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link CommonsCliValidateOptions} class.
 * Tests that POM paths arrive as positional arguments and that a bad {@code --format} value is
 * rejected while the options are parsed, rather than later during execution.
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
    @DisplayName("should reject a bad --format while parsing, not while executing")
    void shouldRejectBadFormatWhileParsing() {
        ParseException e = assertThrows(
                ParseException.class,
                () -> CommonsCliValidateOptions.parse(new String[] {"--format", "xml"}),
                "an unknown format must fail during parsing, so the user sees a usage error");
        assertTrue(e.getMessage().contains("xml"), e.getMessage());
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
}
