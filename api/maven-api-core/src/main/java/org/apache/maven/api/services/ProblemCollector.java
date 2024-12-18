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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.maven.api.Constants;
import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

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
    @Nonnull
    static <P extends BuilderProblem> ProblemCollector<P> empty() {
        return new ProblemCollector<P>() {
            @Override
            public int problemsReportedFor(BuilderProblem.Severity... severities) {
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
     * Creates new instance of problem collector.
     */
    @Nonnull
    static <P extends BuilderProblem> ProblemCollector<P> create(@Nullable ProtoSession protoSession) {
        if (protoSession != null
                && protoSession.getUserProperties().containsKey(Constants.MAVEN_BUILDER_MAX_PROBLEMS)) {
            return new ProblemCollectorImpl<>(
                    Integer.parseInt(protoSession.getUserProperties().get(Constants.MAVEN_BUILDER_MAX_PROBLEMS)));
        } else {
            return create(100);
        }
    }

    /**
     * Creates new instance of problem collector. Visible for testing only.
     */
    @Nonnull
    static <P extends BuilderProblem> ProblemCollector<P> create(int maxCountLimit) {
        return new ProblemCollectorImpl<>(maxCountLimit);
    }

    /**
     * Merges problem collectors by creating new instance of collector. During merge problems may lose.
     */
    @SafeVarargs
    @Nonnull
    static <P extends BuilderProblem> ProblemCollector<P> merge(ProblemCollector<P>... collectors) {
        List<ProblemCollector<P>> reduced = reduce(collectors);
        if (reduced.isEmpty()) {
            return empty();
        } else if (reduced.size() == 1) {
            return reduced.get(0);
        } else {
            ProblemCollector<P> result = create(null);
            for (ProblemCollector<P> p : collectors) {
                p.problems().forEach(result::reportProblem);
            }
            return result;
        }
    }

    /**
     * Concatenates problem collectors by preserving concatenated instances of collectors and offering "unified" view.
     */
    @SafeVarargs
    @Nonnull
    static <P extends BuilderProblem> ProblemCollector<P> concat(ProblemCollector<P>... collectors) {
        List<ProblemCollector<P>> reduced = reduce(collectors);
        if (reduced.isEmpty()) {
            return empty();
        } else if (reduced.size() == 1) {
            return reduced.get(0);
        } else {
            ProblemCollector<P> result = create(null);
            for (ProblemCollector<P> p : collectors) {
                p.problems().forEach(result::reportProblem);
            }
            return result;
        }
    }

    /**
     * Concatenates problem collectors by preserving concatenated instances of collectors and offering "unified" view.
     */
    @SafeVarargs
    @Nonnull
    private static <P extends BuilderProblem> List<ProblemCollector<P>> reduce(ProblemCollector<P>... collectors) {
        return Arrays.stream(collectors)
                .filter(Objects::nonNull)
                .filter(c -> c.totalProblemsReported() > 0)
                .toList();
    }

    /**
     * Returns {@code true} if there is at least one problem collected with severity equal or more severe than
     * {@link org.apache.maven.api.services.BuilderProblem.Severity#ERROR}.
     */
    default boolean hasErrors() {
        return hasProblemsFor(BuilderProblem.Severity.ERROR);
    }

    /**
     * Returns {@code true} if there is at least one problem collected with severity equal or more severe than
     * passed in severity.
     */
    default boolean hasProblemsFor(BuilderProblem.Severity severity) {
        for (BuilderProblem.Severity s : BuilderProblem.Severity.values()) {
            if (s.ordinal() <= severity.ordinal() && problemsReportedFor(s) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns total count of problems reported.
     */
    default int totalProblemsReported() {
        return problemsReportedFor(BuilderProblem.Severity.values());
    }

    /**
     * Returns count of problems reported for given severities.
     */
    int problemsReportedFor(BuilderProblem.Severity... severities);

    /**
     * Reports a problem: always maintains the counters, but whether problem is preserved in memory, depends on
     * implementation and its configuration.
     */
    void reportProblem(P problem);

    /**
     * Returns all reported and preserved problems ordered by severity in decreasing order. Note: counters and
     * element count in this stream does not have to be equal.
     */
    @Nonnull
    Stream<P> problems();

    class ProblemCollectorImpl<P extends BuilderProblem> implements ProblemCollector<P> {

        private final int maxCountLimit;
        private final AtomicInteger totalCount;
        private final ConcurrentMap<BuilderProblem.Severity, LongAdder> counters;
        private final ConcurrentMap<BuilderProblem.Severity, Collection<P>> problems;

        private static final List<BuilderProblem.Severity> REVERSED_ORDER = Arrays.stream(
                        BuilderProblem.Severity.values())
                .sorted(Comparator.reverseOrder())
                .toList();

        private ProblemCollectorImpl(int maxCountLimit) {
            if (maxCountLimit < 0) {
                throw new IllegalArgumentException("maxCountLimit must be non-negative");
            }
            this.maxCountLimit = maxCountLimit;
            this.totalCount = new AtomicInteger();
            this.counters = new ConcurrentHashMap<>();
            this.problems = new ConcurrentHashMap<>();
        }

        private LongAdder getCounter(BuilderProblem.Severity severity) {
            return counters.computeIfAbsent(severity, k -> new LongAdder());
        }

        private Collection<P> getProblems(BuilderProblem.Severity severity) {
            return problems.computeIfAbsent(severity, k -> new CopyOnWriteArrayList<>());
        }

        private boolean dropProblemWithLowerSeverity(BuilderProblem.Severity severity) {
            for (BuilderProblem.Severity s : REVERSED_ORDER) {
                if (s.ordinal() > severity.ordinal()) {
                    Supplier<Collection<P>> problems = () -> getProblems(s);
                    while (!problems.get().isEmpty()) {
                        Collection<P> problemList = problems.get();
                        if (problemList.remove(problemList.iterator().next())) {
                            return true; // try as long you can; due concurrency
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public int problemsReportedFor(BuilderProblem.Severity... severity) {
            int result = 0;
            for (BuilderProblem.Severity s : severity) {
                result += getCounter(s).intValue();
            }
            return result;
        }

        @Override
        public void reportProblem(P problem) {
            int currentCount = totalCount.incrementAndGet();
            getCounter(problem.getSeverity()).increment();
            if (currentCount <= maxCountLimit || dropProblemWithLowerSeverity(problem.getSeverity())) {
                getProblems(problem.getSeverity()).add(problem);
            }
        }

        @Override
        public Stream<P> problems() {
            Stream<P> result = Stream.empty();
            for (BuilderProblem.Severity severity : BuilderProblem.Severity.values()) {
                result = Stream.concat(result, getProblems(severity).stream());
            }
            return result;
        }
    }
}
