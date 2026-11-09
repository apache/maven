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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelProblem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link Report} record.
 * Tests the invariants the compact constructor enforces and the clean/dirty distinction.
 */
@DisplayName("Report")
class ReportTest {

    private static final Path POM = Path.of("pom.xml");

    @Nested
    @DisplayName("Problems")
    class ProblemTests {

        @Test
        @DisplayName("should report a problem once when two validation phases both found it")
        void shouldReportRepeatedProblemOnce() {
            // The parent version check sits in both validateFileModel and validateRawModel, so
            // the model builder hands the same finding over twice.
            ModelProblem problem =
                    TestUtils.problem(BuilderProblem.Severity.WARNING, "'parent.version' is LATEST", "pom.xml", 3, 66);
            ModelProblem again =
                    TestUtils.problem(BuilderProblem.Severity.WARNING, "'parent.version' is LATEST", "pom.xml", 3, 66);

            assertEquals(1, Report.of(POM, List.of(problem, again)).problems().size());
        }

        @Test
        @DisplayName("should keep two problems that differ only in where they sit")
        void shouldKeepProblemsAtDifferentPlaces() {
            ModelProblem first = TestUtils.problem(BuilderProblem.Severity.WARNING, "same words", "pom.xml", 3, 1);
            ModelProblem second = TestUtils.problem(BuilderProblem.Severity.WARNING, "same words", "pom.xml", 9, 1);

            assertEquals(2, Report.of(POM, List.of(first, second)).problems().size());
        }
    }

    @Nested
    @DisplayName("State")
    class StateTests {

        @Test
        @DisplayName("should be clean when there is neither a failure nor a problem")
        void shouldBeCleanWhenNeitherFailureNorProblem() {
            assertTrue(Report.of(POM, List.of()).clean(), "a report with no problems should be clean");
        }

        @Test
        @DisplayName("should not be clean when a problem was reported")
        void shouldNotBeCleanWhenProblemReported() {
            Report report = Report.of(
                    POM, List.of(TestUtils.problem(BuilderProblem.Severity.WARNING, "warn", "pom.xml", 1, 1)));
            assertFalse(report.clean(), "a report carrying a problem should not be clean");
        }

        @Test
        @DisplayName("should not be clean when the file could not be validated")
        void shouldNotBeCleanWhenValidationFailed() {
            assertFalse(Report.failed(POM, "not a readable file").clean(), "a failed report should not be clean");
        }
    }

    @Nested
    @DisplayName("Invariants")
    class InvariantTests {

        @Test
        @DisplayName("should reject a failure that also carries problems")
        void shouldRejectFailureCarryingProblems() {
            List<ModelProblem> problems =
                    List.of(TestUtils.problem(BuilderProblem.Severity.ERROR, "boom", "pom.xml", 1, 1));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Report(POM, problems, "not a readable file"),
                    "a failed report must not also carry problems");
        }

        @Test
        @DisplayName("should copy the problem list so later changes cannot leak in")
        void shouldCopyProblemList() {
            List<ModelProblem> mutable = new ArrayList<>();
            mutable.add(TestUtils.problem(BuilderProblem.Severity.ERROR, "first", "pom.xml", 1, 1));
            Report report = Report.of(POM, mutable);

            mutable.add(TestUtils.problem(BuilderProblem.Severity.ERROR, "second", "pom.xml", 2, 2));

            assertEquals(1, report.problems().size(), "the report should not see additions made after it was built");
        }
    }
}
