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

import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelProblem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Reporter} and its two implementations.
 * Tests that both formats derive a problem's location from its parts rather than from
 * {@link ModelProblem#getLocation()}, and that the JSON document escapes what it must.
 */
@DisplayName("Reporter")
class ReporterTest {

    private static final Path POM = Path.of("bad/pom.xml");

    /** Text output names the POM on the first line and lists the problems under it. */
    private static final int FIRST_PROBLEM_LINE = 1;

    /** The problem line of a rendered text report, without the "  SEVERITY message" prefix. */
    private static String locationOf(ModelProblem problem) {
        String line = TestUtils.render(new TextReporter(), List.of(Report.of(POM, List.of(problem))))
                .get(FIRST_PROBLEM_LINE);
        int at = line.indexOf(" @ ");
        return at < 0 ? "" : line.substring(at + 3);
    }

    @Test
    @DisplayName("should not rely on getLocation, which the model builder leaves empty")
    void shouldNotRelyOnGetLocation() {
        ModelProblem problem = TestUtils.problem(BuilderProblem.Severity.ERROR, "boom", "bad/pom.xml", 13, 17);

        assertEquals(
                "",
                problem.getLocation(),
                "DefaultModelProblem hardcodes getLocation() to empty; the reporter must build it itself");
        assertEquals("line 13, column 17", locationOf(problem), "minus the file already named on the line above");
    }

    @Test
    @DisplayName("should leave out the parts of a location that are unknown")
    void shouldLeaveOutUnknownLocationParts() {
        assertEquals(
                "",
                locationOf(TestUtils.problem(BuilderProblem.Severity.WARNING, "no place", null, -1, -1)),
                "a problem with no location at all should add nothing");
        assertEquals(
                "",
                locationOf(TestUtils.problem(BuilderProblem.Severity.WARNING, "same file", "bad/pom.xml", 0, 0)),
                "a source that is the file being reported on adds nothing");
        assertEquals(
                "line 7",
                locationOf(TestUtils.problem(BuilderProblem.Severity.WARNING, "line only", null, 7, 0)),
                "a problem with only a line should produce just the line");
    }

    @Test
    @DisplayName("should name the source as a path when the problem comes from another file")
    void shouldNameSourceOfAnotherFileAsPath() {
        Path other = Path.of("other/pom.xml").toAbsolutePath().normalize();
        ModelProblem problem = TestUtils.problem(
                BuilderProblem.Severity.ERROR, "elsewhere", other.toUri().toString(), 4, 0);

        assertEquals(
                other + ", line 4",
                locationOf(problem),
                "the model builder reports the source as a file URL; the output uses paths");
    }

    @Test
    @DisplayName("should indent the continuation lines of a multi-line message")
    void shouldIndentContinuationLines() {
        // A parser failure arrives as several lines; unindented, the tail would look like a
        // separate entry rather than part of this problem.
        ModelProblem problem = TestUtils.problem(
                BuilderProblem.Severity.FATAL, "Non-parseable POM" + System.lineSeparator() + "at [1,19]", null, 0, 0);

        List<String> lines = TestUtils.render(new TextReporter(), List.of(Report.of(POM, List.of(problem))));

        assertEquals(
                "  FATAL Non-parseable POM" + System.lineSeparator() + "    at [1,19]", lines.get(FIRST_PROBLEM_LINE));
    }

    @Test
    @DisplayName("should pass a source that is not a file through unchanged")
    void shouldPassNonFileSourceThrough() {
        // On Unix this parses as a relative path, and resolving it would name a file in the
        // working directory that was never involved.
        ModelProblem problem = TestUtils.problem(BuilderProblem.Severity.ERROR, "imported", "org.test:bom:1.0", 0, 0);

        assertEquals("org.test:bom:1.0", Report.of(POM, List.of(problem)).sourceOf(problem));
    }

    @Nested
    @DisplayName("Text")
    class TextTests {

        @Test
        @DisplayName("should report a clean file on one line")
        void shouldReportCleanFileOnOneLine() {
            List<String> lines = TestUtils.render(new TextReporter(), List.of(Report.of(POM, List.of())));
            assertEquals(List.of(POM + ": no problems"), lines);
        }

        @Test
        @DisplayName("should append the location after the message")
        void shouldAppendLocationAfterMessage() {
            List<String> lines = TestUtils.render(
                    new TextReporter(),
                    List.of(Report.of(
                            POM,
                            List.of(TestUtils.problem(BuilderProblem.Severity.ERROR, "boom", "bad/pom.xml", 13, 17)))));

            assertEquals(POM + ":", lines.get(0));
            assertEquals("  ERROR boom @ line 13, column 17", lines.get(FIRST_PROBLEM_LINE));
        }

        @Test
        @DisplayName("should omit the location separator when there is no location")
        void shouldOmitLocationSeparatorWhenNoLocation() {
            List<String> lines = TestUtils.render(
                    new TextReporter(),
                    List.of(Report.of(
                            POM,
                            List.of(TestUtils.problem(BuilderProblem.Severity.WARNING, "no place", null, -1, -1)))));

            assertEquals(
                    "  WARNING no place", lines.get(FIRST_PROBLEM_LINE), "no location means no trailing separator");
        }

        @Test
        @DisplayName("should report a file that could not be read")
        void shouldReportUnreadableFile() {
            List<String> lines =
                    TestUtils.render(new TextReporter(), List.of(Report.failed(POM, "not a readable file")));
            assertEquals(List.of(POM + ": not a readable file"), lines);
        }
    }

    @Nested
    @DisplayName("JSON")
    class JsonTests {

        @Test
        @DisplayName("should emit one object per file in a single array")
        void shouldEmitOneObjectPerFile() {
            List<String> lines = TestUtils.render(
                    new JsonReporter(),
                    List.of(Report.of(Path.of("a.xml"), List.of()), Report.of(Path.of("b.xml"), List.of())));

            assertEquals(1, lines.size(), "the whole document should be written as one line");
            assertEquals("[{\"pom\":\"a.xml\",\"problems\":[]},{\"pom\":\"b.xml\",\"problems\":[]}]", lines.get(0));
        }

        @Test
        @DisplayName("should leave out absent fields rather than emitting the -1 sentinel")
        void shouldLeaveOutAbsentFields() {
            String json = TestUtils.render(
                            new JsonReporter(),
                            List.of(Report.of(
                                    POM,
                                    List.of(TestUtils.problem(
                                            BuilderProblem.Severity.WARNING, "no place", null, -1, -1)))))
                    .get(0);

            assertFalse(json.contains("-1"), "the -1 the model builder uses for \"unknown\" must not leak: " + json);
            assertFalse(json.contains("\"line\""), "an unknown line should be left out entirely: " + json);
            assertFalse(json.contains("\"source\""), "an unknown source should be left out entirely: " + json);
            assertFalse(json.contains("file:"), "a file URL must not reach the document: " + json);
            assertTrue(json.contains("\"severity\":\"WARNING\""), json);
        }

        @Test
        @DisplayName("should escape quotes, backslashes and control characters")
        void shouldEscapeQuotesBackslashesAndControlCharacters() {
            String json = TestUtils.render(
                            new JsonReporter(),
                            List.of(Report.of(
                                    POM,
                                    List.of(TestUtils.problem(
                                            BuilderProblem.Severity.ERROR,
                                            "say \"hi\"\nand\ttab\\end",
                                            "bad/pom.xml",
                                            1,
                                            1)))))
                    .get(0);

            assertTrue(json.contains("say \\\"hi\\\""), "quotes should be escaped: " + json);
            assertTrue(json.contains("\\n"), "newlines should be escaped: " + json);
            assertTrue(json.contains("\\t"), "tabs should be escaped: " + json);
            assertTrue(json.contains("\\\\end"), "backslashes should be escaped: " + json);
        }

        @Test
        @DisplayName("should escape everything outside printable ASCII")
        void shouldEscapeEverythingOutsidePrintableAscii() {
            // A lone high surrogate is reachable from a filename and no charset can encode it; a
            // console that is not UTF-8 would mangle the accented letter. Both are escaped so the
            // document survives whatever the writer's charset turns out to be.
            String json = TestUtils.render(
                            new JsonReporter(),
                            List.of(Report.of(
                                    POM,
                                    List.of(TestUtils.problem(
                                            BuilderProblem.Severity.ERROR, "\u0001 café \ud83d", null, 1, 1)))))
                    .get(0);

            assertTrue(json.contains("\\u0001"), "control characters should be escaped: " + json);
            assertTrue(json.contains("\\u00e9"), "non-ASCII should be escaped: " + json);
            assertTrue(json.contains("\\ud83d"), "an unpaired surrogate should be escaped: " + json);
            assertTrue(json.chars().allMatch(c -> c >= 0x20 && c <= 0x7e), "the document should be plain ASCII");
        }

        @Test
        @DisplayName("should record a file that could not be read as a failure")
        void shouldRecordUnreadableFileAsFailure() {
            String json = TestUtils.render(new JsonReporter(), List.of(Report.failed(POM, "not a readable file")))
                    .get(0);
            assertTrue(json.contains("\"failure\":\"not a readable file\""), json);
        }
    }
}
