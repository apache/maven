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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Report for a single mojo (plugin goal) execution within a module.
 *
 * @since 4.1.0
 * @see ModuleReport#mojos()
 */
@Experimental
public interface MojoReport {

    /**
     * The plugin's group ID.
     *
     * @return the group ID, never {@code null}
     */
    @Nonnull
    String groupId();

    /**
     * The plugin's artifact ID.
     *
     * @return the artifact ID, never {@code null}
     */
    @Nonnull
    String artifactId();

    /**
     * The plugin version.
     *
     * @return the version string, never {@code null}
     */
    @Nonnull
    String version();

    /**
     * The goal that was executed (e.g. {@code "compile"}, {@code "test"}).
     *
     * @return the goal name, never {@code null}
     */
    @Nonnull
    String goal();

    /**
     * The execution ID (e.g. {@code "default-compile"}).
     *
     * @return the execution ID, or {@code null} if not set
     */
    @Nullable
    String executionId();

    /**
     * The lifecycle phase this mojo was bound to (e.g. {@code "compile"}, {@code "test"}).
     *
     * @return the phase name, or {@code null} if invoked directly
     */
    @Nullable
    String phase();

    /**
     * The outcome of this mojo execution.
     *
     * @return the status, never {@code null}
     */
    @Nonnull
    BuildStatus status();

    /**
     * When this mojo execution started (wall-clock time).
     *
     * @return the start instant, never {@code null}
     */
    @Nonnull
    Instant startTime();

    /**
     * How long this mojo execution took.
     *
     * @return the duration, never {@code null}
     */
    @Nonnull
    Duration duration();

    /**
     * Structured log events captured during this mojo's execution.
     * <p>
     * The list may be truncated if the mojo produced excessive output.
     * <p>
     * This captures all SLF4J output that occurred on the mojo's execution
     * thread between the mojo's start and finish events, regardless of
     * whether the mojo used the legacy {@code Mojo.getLog()}, the Maven 4
     * injected {@code Log}, or plain SLF4J.
     *
     * @return the captured log events, never {@code null}; may be empty
     * @since 4.1.0
     */
    @Nonnull
    List<LogEvent> output();

    /**
     * The mojo identifier formatted as {@code "artifactId:version:goal"}.
     * <p>
     * This matches the format used by {@link FailureReport#mojo()}, allowing
     * direct lookup via {@link ModuleReport#findMojo(String)}.
     *
     * @return the mojo identifier string, never {@code null}
     */
    @Nonnull
    default String id() {
        return artifactId() + ":" + version() + ":" + goal();
    }
}
