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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 */
class DefaultProblemCollectorTest {

    private ProblemCollector<BuilderProblem> collector;

    @BeforeEach
    void setUp() throws Exception {
        collector = new DefaultProblemCollector<>(5);
    }

    @AfterEach
    void tearDown() throws Exception {
        collector = null;
    }

    @Test
    void moreSeverPushOutLeastSevere() {
        assertEquals(0, collector.problemsReported());
        assertEquals(0, collector.problems().count());

        IntStream.range(0, 5)
                .forEach(i -> collector.reportProblem(new DefaultBuilderProblem(
                        "source", 0, 0, null, "message " + i, BuilderProblem.Severity.WARNING)));
        assertEquals(5, collector.problemsReported());
        assertEquals(5, collector.problems().count());

        IntStream.range(0, 5)
                .forEach(i -> collector.reportProblem(new DefaultBuilderProblem(
                        "source", 0, 0, null, "message " + i, BuilderProblem.Severity.ERROR)));
        assertEquals(10, collector.problemsReported());
        assertEquals(5, collector.problems().count());

        IntStream.range(0, 5)
                .forEach(i -> collector.reportProblem(new DefaultBuilderProblem(
                        "source", 0, 0, null, "message " + i, BuilderProblem.Severity.FATAL)));
        assertEquals(15, collector.problemsReported());
        assertEquals(5, collector.problems().count());

        assertEquals(5, collector.problemsReported(BuilderProblem.Severity.WARNING));
        assertEquals(5, collector.problemsReported(BuilderProblem.Severity.ERROR));
        assertEquals(5, collector.problemsReported(BuilderProblem.Severity.FATAL));

        assertEquals(
                0,
                collector
                        .problems()
                        .filter(p -> p.getSeverity() == BuilderProblem.Severity.WARNING)
                        .count());
        assertEquals(
                0,
                collector
                        .problems()
                        .filter(p -> p.getSeverity() == BuilderProblem.Severity.ERROR)
                        .count());
        assertEquals(
                5,
                collector
                        .problems()
                        .filter(p -> p.getSeverity() == BuilderProblem.Severity.FATAL)
                        .count());
    }
}
