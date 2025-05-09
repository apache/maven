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
package org.apache.maven.impl;

import java.util.stream.IntStream;

import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ProblemCollector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This UT is for {@link ProblemCollector} but here we have implementations for problems.
 */
class DefaultProblemCollectorTest {
    @Test
    void severityFatalDetection() {
        ProblemCollector<BuilderProblem> collector = ProblemCollector.create(5);

        assertThat(collector.hasProblemsFor(BuilderProblem.Severity.WARNING)).isFalse();
        assertThat(collector.hasErrorProblems()).isFalse();
        assertThat(collector.hasFatalProblems()).isFalse();

        collector.reportProblem(
                new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.FATAL));

        // fatal triggers all
        assertThat(collector.hasProblemsFor(BuilderProblem.Severity.WARNING)).isTrue();
        assertThat(collector.hasErrorProblems()).isTrue();
        assertThat(collector.hasFatalProblems()).isTrue();
    }

    @Test
    void severityErrorDetection() {
        ProblemCollector<BuilderProblem> collector = ProblemCollector.create(5);

        assertThat(collector.hasProblemsFor(BuilderProblem.Severity.WARNING)).isFalse();
        assertThat(collector.hasErrorProblems()).isFalse();
        assertThat(collector.hasFatalProblems()).isFalse();

        collector.reportProblem(
                new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.ERROR));

        // error triggers error + warning
        assertThat(collector.hasProblemsFor(BuilderProblem.Severity.WARNING)).isTrue();
        assertThat(collector.hasErrorProblems()).isTrue();
        assertThat(collector.hasFatalProblems()).isFalse();
    }

    @Test
    void severityWarningDetection() {
        ProblemCollector<BuilderProblem> collector = ProblemCollector.create(5);

        assertThat(collector.hasProblemsFor(BuilderProblem.Severity.WARNING)).isFalse();
        assertThat(collector.hasErrorProblems()).isFalse();
        assertThat(collector.hasFatalProblems()).isFalse();

        collector.reportProblem(
                new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.WARNING));

        // warning triggers warning only
        assertThat(collector.hasProblemsFor(BuilderProblem.Severity.WARNING)).isTrue();
        assertThat(collector.hasErrorProblems()).isFalse();
        assertThat(collector.hasFatalProblems()).isFalse();
    }

    @Test
    void lossy() {
        ProblemCollector<BuilderProblem> collector = ProblemCollector.create(5);
        IntStream.range(0, 5)
                .forEach(i -> collector.reportProblem(new DefaultBuilderProblem(
                        "source", 0, 0, null, "message " + i, BuilderProblem.Severity.WARNING)));

        // collector is "full" of warnings
        assertThat(collector.reportProblem(
                        new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.WARNING)))
                .isFalse();

        // but collector will drop warning for more severe issues
        assertThat(collector.reportProblem(
                        new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.ERROR)))
                .isTrue();
        assertThat(collector.reportProblem(
                        new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.FATAL)))
                .isTrue();

        // collector is full of warnings, errors and fatal (mixed)
        assertThat(collector.reportProblem(
                        new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.WARNING)))
                .isFalse();

        // fill it up with fatal ones
        IntStream.range(0, 5)
                .forEach(i -> collector.reportProblem(new DefaultBuilderProblem(
                        "source", 0, 0, null, "message " + i, BuilderProblem.Severity.FATAL)));

        // from now on, only counters work, problems are lost
        assertThat(collector.reportProblem(
                        new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.WARNING)))
                .isFalse();
        assertThat(collector.reportProblem(
                        new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.ERROR)))
                .isFalse();
        assertThat(collector.reportProblem(
                        new DefaultBuilderProblem("source", 0, 0, null, "message", BuilderProblem.Severity.FATAL)))
                .isFalse();

        assertThat(collector.totalProblemsReported()).isEqualTo(17);
        assertThat(collector.problemsReportedFor(BuilderProblem.Severity.WARNING))
                .isEqualTo(8);
        assertThat(collector.problemsReportedFor(BuilderProblem.Severity.ERROR)).isEqualTo(2);
        assertThat(collector.problemsReportedFor(BuilderProblem.Severity.FATAL)).isEqualTo(7);

        // but preserved problems count == capacity
        assertThat(collector.problems().count()).isEqualTo(5);
    }

    @Test
    void moreSeverePushOutLeastSevere() {
        ProblemCollector<BuilderProblem> collector = ProblemCollector.create(5);

        assertThat(collector.totalProblemsReported()).isEqualTo(0);
        assertThat(collector.problems().count()).isEqualTo(0);

        IntStream.range(0, 5)
                .forEach(i -> collector.reportProblem(new DefaultBuilderProblem(
                        "source", 0, 0, null, "message " + i, BuilderProblem.Severity.WARNING)));
        assertThat(collector.totalProblemsReported()).isEqualTo(5);
        assertThat(collector.problems().count()).isEqualTo(5);

        IntStream.range(0, 5)
                .forEach(i -> collector.reportProblem(new DefaultBuilderProblem(
                        "source", 0, 0, null, "message " + i, BuilderProblem.Severity.ERROR)));
        assertThat(collector.totalProblemsReported()).isEqualTo(10);
        assertThat(collector.problems().count()).isEqualTo(5);

        IntStream.range(0, 4)
                .forEach(i -> collector.reportProblem(new DefaultBuilderProblem(
                        "source", 0, 0, null, "message " + i, BuilderProblem.Severity.FATAL)));
        assertThat(collector.totalProblemsReported()).isEqualTo(14);
        assertThat(collector.problems().count()).isEqualTo(5);

        assertThat(collector.problemsReportedFor(BuilderProblem.Severity.WARNING))
                .isEqualTo(5);
        assertThat(collector.problemsReportedFor(BuilderProblem.Severity.ERROR)).isEqualTo(5);
        assertThat(collector.problemsReportedFor(BuilderProblem.Severity.FATAL)).isEqualTo(4);

        assertThat(collector.problems(BuilderProblem.Severity.WARNING).count()).isEqualTo(0);
        assertThat(collector.problems(BuilderProblem.Severity.ERROR).count()).isEqualTo(1);
        assertThat(collector.problems(BuilderProblem.Severity.FATAL).count()).isEqualTo(4);
    }
}
