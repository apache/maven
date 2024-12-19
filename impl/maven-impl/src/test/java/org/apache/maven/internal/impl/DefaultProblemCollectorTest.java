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
package org.apache.maven.internal.impl;

import java.util.stream.IntStream;

import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ProblemCollector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This UT is for {@link ProblemCollector} but here we have implementations for problems.
 */
class DefaultProblemCollectorTest {
    @Test
    void severityFatalDetection() {
        ProblemCollector<BuilderProblem> collector = ProblemCollector.create(5);

        assertFalse(collector.hasProblemsFor(BuilderProblem.Severity.WARNING));
        assertFalse(collector.hasErrorProblems());
        assertFalse(collector.hasFatalProblems());

        collector.reportProblem(
                new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.FATAL));

        // fatal triggers all
        assertTrue(collector.hasProblemsFor(BuilderProblem.Severity.WARNING));
        assertTrue(collector.hasErrorProblems());
        assertTrue(collector.hasFatalProblems());
    }

    @Test
    void severityErrorDetection() {
        ProblemCollector<BuilderProblem> collector = ProblemCollector.create(5);

        assertFalse(collector.hasProblemsFor(BuilderProblem.Severity.WARNING));
        assertFalse(collector.hasErrorProblems());
        assertFalse(collector.hasFatalProblems());

        collector.reportProblem(
                new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.ERROR));

        // error triggers error + warning
        assertTrue(collector.hasProblemsFor(BuilderProblem.Severity.WARNING));
        assertTrue(collector.hasErrorProblems());
        assertFalse(collector.hasFatalProblems());
    }

    @Test
    void severityWarningDetection() {
        ProblemCollector<BuilderProblem> collector = ProblemCollector.create(5);

        assertFalse(collector.hasProblemsFor(BuilderProblem.Severity.WARNING));
        assertFalse(collector.hasErrorProblems());
        assertFalse(collector.hasFatalProblems());

        collector.reportProblem(
                new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.WARNING));

        // warning triggers warning only
        assertTrue(collector.hasProblemsFor(BuilderProblem.Severity.WARNING));
        assertFalse(collector.hasErrorProblems());
        assertFalse(collector.hasFatalProblems());
    }

    @Test
    void lossy() {
        ProblemCollector<BuilderProblem> collector = ProblemCollector.create(5);
        IntStream.range(0, 5)
                .forEach(i -> collector.reportProblem(new DefaultBuilderProblem(
                        "source", 0, 0, null, "message " + i, BuilderProblem.Severity.WARNING)));

        // collector is "full" of warnings
        assertFalse(collector.reportProblem(
                new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.WARNING)));

        // but collector will drop warning for more severe issues
        assertTrue(collector.reportProblem(
                new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.ERROR)));
        assertTrue(collector.reportProblem(
                new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.FATAL)));

        // collector is full of warnings, errors and fatal (mixed)
        assertFalse(collector.reportProblem(
                new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.WARNING)));

        // fill it up with fatal ones
        IntStream.range(0, 5)
                .forEach(i -> collector.reportProblem(new DefaultBuilderProblem(
                        "source", 0, 0, null, "message " + i, BuilderProblem.Severity.FATAL)));

        // from now on, only counters work, problems are lost
        assertFalse(collector.reportProblem(
                new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.WARNING)));
        assertFalse(collector.reportProblem(
                new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.ERROR)));
        assertFalse(collector.reportProblem(
                new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.FATAL)));

        assertEquals(17, collector.totalProblemsReported());
        assertEquals(8, collector.problemsReportedFor(BuilderProblem.Severity.WARNING));
        assertEquals(2, collector.problemsReportedFor(BuilderProblem.Severity.ERROR));
        assertEquals(7, collector.problemsReportedFor(BuilderProblem.Severity.FATAL));

        // but preserved problems count == capacity
        assertEquals(5, collector.problems().count());
    }

    @Test
    void moreSeverePushOutLeastSevere() {
        ProblemCollector<BuilderProblem> collector = ProblemCollector.create(5);

        assertEquals(0, collector.totalProblemsReported());
        assertEquals(0, collector.problems().count());

        IntStream.range(0, 5)
                .forEach(i -> collector.reportProblem(new DefaultBuilderProblem(
                        "source", 0, 0, null, "message " + i, BuilderProblem.Severity.WARNING)));
        assertEquals(5, collector.totalProblemsReported());
        assertEquals(5, collector.problems().count());

        IntStream.range(0, 5)
                .forEach(i -> collector.reportProblem(new DefaultBuilderProblem(
                        "source", 0, 0, null, "message " + i, BuilderProblem.Severity.ERROR)));
        assertEquals(10, collector.totalProblemsReported());
        assertEquals(5, collector.problems().count());

        IntStream.range(0, 4)
                .forEach(i -> collector.reportProblem(new DefaultBuilderProblem(
                        "source", 0, 0, null, "message " + i, BuilderProblem.Severity.FATAL)));
        assertEquals(14, collector.totalProblemsReported());
        assertEquals(5, collector.problems().count());

        assertEquals(5, collector.problemsReportedFor(BuilderProblem.Severity.WARNING));
        assertEquals(5, collector.problemsReportedFor(BuilderProblem.Severity.ERROR));
        assertEquals(4, collector.problemsReportedFor(BuilderProblem.Severity.FATAL));

        assertEquals(0, collector.problems(BuilderProblem.Severity.WARNING).count());
        assertEquals(1, collector.problems(BuilderProblem.Severity.ERROR).count());
        assertEquals(4, collector.problems(BuilderProblem.Severity.FATAL).count());
    }
}
