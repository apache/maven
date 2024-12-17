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
package org.apache.maven.api.services;

import java.util.stream.Stream;

import org.apache.maven.api.annotations.Experimental;

/**
 * Collects problems that were encountered during project building.
 *
 * @param <P> The type of the problem.
 * @since 4.0.0
 */
@Experimental
public interface ProblemCollector<P extends BuilderProblem> {

    /**
     * Creates "empty" problem collector.
     */
    static <P extends BuilderProblem> ProblemCollector<P> empty() {
        return new ProblemCollector<P>() {
            @Override
            public int problemsReported(BuilderProblem.Severity... severities) {
                return 0;
            }

            @Override
            public void reportProblem(P problem) {
                throw new IllegalStateException("empty problem collector");
            }

            @Override
            public Stream<P> problems() {
                return Stream.empty();
            }
        };
    }

    /**
     * Returns {@code true} if there is at least one problem collected with severity equal or more severe than
     * {@link org.apache.maven.api.services.BuilderProblem.Severity#ERROR}.
     */
    default boolean hasErrors() {
        return hasProblems(BuilderProblem.Severity.ERROR);
    }

    /**
     * Returns {@code true} if there is at least one problem collected with severity equal or more severe than
     * passed in severity.
     */
    default boolean hasProblems(BuilderProblem.Severity severity) {
        for (BuilderProblem.Severity s : BuilderProblem.Severity.values()) {
            if (s.ordinal() <= severity.ordinal() && problemsReported(s) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns total count of problems reported.
     */
    default int problemsReported() {
        return problemsReported(BuilderProblem.Severity.values());
    }

    /**
     * Returns count of problems reported for given severities.
     */
    int problemsReported(BuilderProblem.Severity... severities);

    /**
     * Reports a problem: always maintains the counters, but whether problem is preserved in memory, depends on
     * implementation and its configuration.
     */
    void reportProblem(P problem);

    /**
     * Returns all reported and preserved problems ordered by severity in decreasing order. Note: counters and
     * element count in this stream does not have to be equal.
     */
    Stream<P> problems();
}
