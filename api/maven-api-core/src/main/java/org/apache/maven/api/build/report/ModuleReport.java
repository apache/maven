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

/**
 * Build results for a single module in a reactor build.
 *
 * @since 4.1.0
 * @see BuildReport#modules()
 */
@Experimental
public interface ModuleReport {

    /**
     * The module's group ID.
     *
     * @return the group ID, never {@code null}
     */
    @Nonnull
    String groupId();

    /**
     * The module's artifact ID.
     *
     * @return the artifact ID, never {@code null}
     */
    @Nonnull
    String artifactId();

    /**
     * The module's version.
     *
     * @return the version string, never {@code null}
     */
    @Nonnull
    String version();

    /**
     * The build outcome for this module.
     *
     * @return the status, never {@code null}
     */
    @Nonnull
    BuildStatus status();

    /**
     * When this module started building (wall-clock time).
     *
     * @return the start instant, never {@code null}
     */
    @Nonnull
    Instant startTime();

    /**
     * How long this module took to build.
     *
     * @return the duration, never {@code null}
     */
    @Nonnull
    Duration duration();

    /**
     * The mojo executions that ran within this module, in execution order.
     *
     * @return the mojo reports, never {@code null}
     */
    @Nonnull
    List<MojoReport> mojos();

    /**
     * Structured log events captured during this module's build lifecycle
     * but outside any mojo execution — dependency resolution messages,
     * resource copying, and other Maven infrastructure output.
     * <p>
     * For per-mojo events see {@link MojoReport#output()}.
     *
     * @return the captured log events, never {@code null}; may be empty
     */
    @Nonnull
    List<LogEvent> output();

    /**
     * The module identifier formatted as {@code "groupId:artifactId:version"}.
     * <p>
     * This matches the format used by {@link FailureReport#module()}, allowing
     * direct lookup from a failure report.
     *
     * @return the GAV string, never {@code null}
     */
    @Nonnull
    default String id() {
        return groupId() + ":" + artifactId() + ":" + version();
    }

    /**
     * Find a mojo execution by its identifier string.
     * <p>
     * The identifier format is {@code "artifactId:version:goal"}, matching
     * the format used by {@link FailureReport#mojo()}.
     *
     * @param mojoId the mojo identifier (e.g. {@code "maven-compiler-plugin:3.15.0:compile"})
     * @return the matching mojo report, or empty if not found
     */
    @Nonnull
    default Optional<MojoReport> findMojo(String mojoId) {
        Objects.requireNonNull(mojoId);
        return mojos().stream().filter(m -> mojoId.equals(m.id())).findFirst();
    }
}
