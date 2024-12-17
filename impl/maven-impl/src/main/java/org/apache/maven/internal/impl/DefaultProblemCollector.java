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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.maven.api.Constants;
import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ProblemCollector;

/**
 * Describes a problem that was encountered during settings building. A problem can either be an exception that was
 * thrown or a simple string message. In addition, a problem carries a hint about its source, e.g. the settings file
 * that exhibits the problem.
 *
 * @param <P> The type of the problem.
 */
public class DefaultProblemCollector<P extends BuilderProblem> implements ProblemCollector<P> {
    /**
     * Creates new instance of pre-configured problem collector.
     */
    public static <P extends BuilderProblem> ProblemCollector<P> create(@Nullable ProtoSession protoSession) {
        if (protoSession != null
                && protoSession.getUserProperties().containsKey(Constants.MAVEN_BUILDER_MAX_PROBLEMS)) {
            return new DefaultProblemCollector<>(
                    Integer.parseInt(protoSession.getUserProperties().get(Constants.MAVEN_BUILDER_MAX_PROBLEMS)));
        } else {
            return new DefaultProblemCollector<>();
        }
    }

    /**
     * Visible for testing only.
     */
    public static <P extends BuilderProblem> ProblemCollector<P> create(int maxCountLimit) {
        return new DefaultProblemCollector<>(maxCountLimit);
    }

    private final int maxCountLimit;
    private final AtomicInteger totalCount;
    private final ConcurrentMap<BuilderProblem.Severity, LongAdder> counters;
    private final ConcurrentMap<BuilderProblem.Severity, Collection<P>> problems;

    private static final List<BuilderProblem.Severity> REVERSED_ORDER = Arrays.stream(BuilderProblem.Severity.values())
            .sorted(Comparator.reverseOrder())
            .toList();

    /**
     * Creates collector collecting up to 100 problems. Do not use this constructor, only when in need.
     */
    private DefaultProblemCollector() {
        this(100);
    }

    private DefaultProblemCollector(int maxCountLimit) {
        if (maxCountLimit <= 0) {
            throw new IllegalArgumentException("maxCountLimit must be greater than 0");
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
