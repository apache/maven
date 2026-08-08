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
package org.apache.maven.api.build.report;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.BuilderProblem;

/**
 * A structured report of a Maven build execution, persisted to
 * {@code target/build-report.json} at the end of every build.
 * <p>
 * The report captures metadata, per-module results (including mojo execution
 * timings), and any failures. It is intended to be consumed by tools, IDEs,
 * CI systems, and LLM agents without having to re-run the build or parse
 * console output.
 *
 * @since 4.1.0
 * @see ModuleReport
 * @see FailureReport
 */
@Experimental
public interface BuildReport {

    /**
     * Schema version of the report format. Consumers should check this
     * to handle forward compatibility.
     *
     * @return the format version, currently {@code 1}
     */
    int formatVersion();

    /**
     * The overall build status.
     *
     * @return the build outcome, never {@code null}
     */
    @Nonnull
    BuildStatus status();

    /**
     * Wall-clock duration of the entire build.
     *
     * @return the total duration, never {@code null}
     */
    @Nonnull
    Duration duration();

    /**
     * When the build started (wall-clock time).
     *
     * @return the start instant, never {@code null}
     */
    @Nonnull
    Instant startTime();

    /**
     * The Maven version that produced this report.
     *
     * @return the Maven version string, never {@code null}
     */
    @Nonnull
    String mavenVersion();

    /**
     * The Java version used for the build.
     *
     * @return the Java version string, never {@code null}
     */
    @Nonnull
    String javaVersion();

    /**
     * The goals or phases that were requested.
     *
     * @return the list of goals, never {@code null}
     */
    @Nonnull
    List<String> goals();

    /**
     * The GAV of the top-level project ({@code groupId:artifactId:version}).
     *
     * @return the project identifier, never {@code null}
     */
    @Nonnull
    String project();

    /**
     * Whether this was a multi-module (reactor) build.
     *
     * @return {@code true} for multi-module builds
     */
    boolean multiModule();

    /**
     * The degree of concurrency ({@code -T} flag), or 1 for sequential builds.
     *
     * @return the thread count
     */
    int threads();

    /**
     * Per-module build results, in reactor execution order.
     *
     * @return the module reports, never {@code null}
     */
    @Nonnull
    List<ModuleReport> modules();

    /**
     * Failures that occurred during the build, if any.
     *
     * @return the failure reports, never {@code null}; empty if the build succeeded
     */
    @Nonnull
    List<FailureReport> failures();

    /**
     * Structured problems (warnings, errors) reported during the build by
     * Maven itself or by plugins.
     *
     * @return the problems, never {@code null}; empty if none were reported
     * @since 4.1.0
     */
    @Nonnull
    List<BuilderProblem> problems();

    /**
     * Structured log events captured outside of any module's lifecycle —
     * Maven startup messages, reactor ordering, and the final reactor summary.
     * <p>
     * For per-module events see {@link ModuleReport#output()}, and for
     * per-mojo events see {@link MojoReport#output()}.
     * <p>
     * Together, {@code BuildReport.output()}, {@code ModuleReport.output()},
     * and {@code MojoReport.output()} form a non-overlapping partition of
     * the full build log.
     *
     * @return the captured log events, never {@code null}; may be empty
     */
    @Nonnull
    List<LogEvent> output();

    /**
     * Find a module report by its GAV identifier.
     * <p>
     * The identifier format is {@code "groupId:artifactId:version"}, matching
     * the format returned by {@link ModuleReport#id()} and used in
     * {@link FailureReport#module()}.
     *
     * @param moduleId the module GAV string
     *     (e.g. {@code "org.apache.maven:maven-core:4.1.0-SNAPSHOT"})
     * @return the matching module report, or empty if not found
     */
    @Nonnull
    default Optional<ModuleReport> findModule(String moduleId) {
        Objects.requireNonNull(moduleId);
        return modules().stream().filter(m -> moduleId.equals(m.id())).findFirst();
    }

    /**
     * Find the module report that corresponds to a given failure.
     *
     * @param failure the failure report
     * @return the matching module report, or empty if not found
     */
    @Nonnull
    default Optional<ModuleReport> findModule(FailureReport failure) {
        Objects.requireNonNull(failure);
        return findModule(failure.module());
    }
}
