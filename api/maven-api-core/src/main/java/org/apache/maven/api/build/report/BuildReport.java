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

import org.apache.maven.api.BuildInvocation;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.services.BuilderProblem;

/**
 * A structured report of a Maven build execution, persisted to
 * {@code target/build-report.json} at the end of every build.
 * <p>
 * The report captures metadata, per-subproject results (including mojo execution
 * timings), and any failures. It is intended to be consumed by tools, IDEs,
 * CI systems, and LLM agents without having to re-run the build or parse
 * console output.
 *
 * @since 4.2.0
 * @see SubprojectReport
 * @see FailureReport
 */
@Experimental
@Immutable
public interface BuildReport {

    /**
     * Semantic version of the report format (e.g. {@code "1.0"}).
     * <p>
     * Versioning policy:
     * <ul>
     *   <li><b>Minor</b> increments (1.0 &rarr; 1.1) are purely additive &mdash;
     *       new fields may appear, but existing fields keep their name, type, and
     *       meaning. Consumers that ignore unknown fields are forward-compatible.</li>
     *   <li><b>Major</b> increments (1.x &rarr; 2.0) signal a breaking change &mdash;
     *       fields may be removed, renamed, or change type. Consumers should check
     *       the major version and refuse to parse an unsupported major.</li>
     * </ul>
     *
     * @return the semver report version string, never {@code null}
     * @since 4.2.0
     */
    @Nonnull
    String reportVersion();

    /**
     * The invocation context of this build: flags, properties, and platform settings
     * that were active when the session started.
     *
     * @return the build invocation, never {@code null}
     * @since 4.2.0
     */
    @Nonnull
    BuildInvocation invocation();

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
     * Whether this was a multi-subproject (reactor) build.
     *
     * @return {@code true} for multi-subproject builds
     */
    boolean multiSubproject();

    /**
     * The degree of concurrency ({@code -T} flag), or 1 for sequential builds.
     *
     * @return the thread count
     */
    int threads();

    /**
     * Per-subproject build results, in reactor execution order.
     *
     * @return the subproject reports, never {@code null}
     */
    @Nonnull
    List<SubprojectReport> subprojects();

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
     * <p>
     * <b>Note (4.2.0):</b> this list is always empty in the current release —
     * the wiring between Maven's problem reporting infrastructure and the
     * build report collector is not yet implemented. It is reserved for a
     * future release.
     *
     * @return the problems, never {@code null}; empty if none were reported
     * @since 4.2.0
     */
    @Nonnull
    List<BuilderProblem> problems();

    /**
     * Structured log events captured outside of any subproject's lifecycle —
     * Maven startup messages, reactor ordering, and the final reactor summary.
     * <p>
     * For per-subproject events see {@link SubprojectReport#output()}, and for
     * per-mojo events see {@link MojoReport#output()}.
     * <p>
     * Together, {@code BuildReport.output()}, {@code SubprojectReport.output()},
     * and {@code MojoReport.output()} form a non-overlapping partition of
     * the full build log. Each scope is capped independently at
     * {@value org.apache.maven.api.Constants#MAVEN_BUILD_REPORT_MAX_LOG_EVENTS}
     * events (configurable via the {@code maven.buildReport.maxLogEvents} property);
     * events beyond the cap are silently dropped.
     *
     * @return the captured log events, never {@code null}; may be empty
     */
    @Nonnull
    List<LogEvent> output();

    /**
     * Find a subproject report by its GAV identifier.
     * <p>
     * The identifier format is {@code "groupId:artifactId:version"}, matching
     * the format returned by {@link SubprojectReport#id()} and used in
     * {@link FailureReport#subproject()}.
     *
     * @param subprojectId the subproject GAV string
     *     (e.g. {@code "org.apache.maven:maven-core:4.1.0-SNAPSHOT"})
     * @return the matching subproject report, or empty if not found
     */
    @Nonnull
    default Optional<SubprojectReport> findSubproject(String subprojectId) {
        Objects.requireNonNull(subprojectId);
        return subprojects().stream().filter(m -> subprojectId.equals(m.id())).findFirst();
    }

    /**
     * Find the subproject report that corresponds to a given failure.
     *
     * @param failure the failure report
     * @return the matching subproject report, or empty if not found
     */
    @Nonnull
    default Optional<SubprojectReport> findSubproject(FailureReport failure) {
        Objects.requireNonNull(failure);
        return findSubproject(failure.subproject());
    }
}
