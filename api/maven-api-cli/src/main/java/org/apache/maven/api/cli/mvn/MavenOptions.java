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
package org.apache.maven.api.cli.mvn;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.Options;

/**
 * Defines the options specific to Maven operations.
 * This interface extends the general {@link Options} interface, adding Maven-specific configuration options.
 *
 * <p>These options represent the various flags and settings available through the Maven CLI,
 * as well as those that can be specified in the {@code maven.config} file. They provide fine-grained
 * control over Maven's behavior during the build process.</p>
 *
 * @since 4.0.0
 */
@Experimental
public interface MavenOptions extends Options {

    /**
     * Returns the path to an alternate POM file.
     *
     * @return an {@link Optional} containing the path to the alternate POM file, or empty if not specified
     */
    @Nonnull
    Optional<String> alternatePomFile();

    /**
     * Indicates whether Maven should operate in non-recursive mode (i.e., not build child modules).
     *
     * @return an {@link Optional} containing true if non-recursive mode is enabled, false if disabled, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> nonRecursive();

    /**
     * Indicates whether Maven should force a check for updated snapshots on remote repositories.
     *
     * @return an {@link Optional} containing true if snapshot updates should be forced, false if not, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> updateSnapshots();

    /**
     * Returns the list of profiles to activate.
     *
     * @return an {@link Optional} containing the list of profile names to activate, or empty if not specified
     */
    @Nonnull
    Optional<List<String>> activatedProfiles();

    /**
     * Indicates whether Maven should suppress SNAPSHOT updates.
     *
     * @return an {@link Optional} containing true if SNAPSHOT updates should be suppressed, false if not, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> suppressSnapshotUpdates();

    /**
     * Indicates whether Maven should use strict checksum verification.
     *
     * @return an {@link Optional} containing true if strict checksum verification is enabled, false if not, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> strictChecksums();

    /**
     * Indicates whether Maven should use relaxed checksum verification.
     *
     * @return an {@link Optional} containing true if relaxed checksum verification is enabled, false if not, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> relaxedChecksums();

    /**
     * Indicates whether Maven should stop at the first failure in a multi-module build.
     *
     * @return an {@link Optional} containing true if Maven should stop at the first failure, false if not, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> failFast();

    /**
     * Indicates whether Maven should run all builds but defer error reporting to the end.
     *
     * @return an {@link Optional} containing true if error reporting should be deferred to the end, false if not, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> failAtEnd();

    /**
     * Indicates whether Maven should never fail the build, regardless of project result.
     *
     * @return an {@link Optional} containing true if the build should never fail, false if it should fail normally, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> failNever();

    /**
     * Indicates whether Maven should resume from the last failed project in a previous build.
     *
     * @return an {@link Optional} containing true if Maven should resume from the last failure, false if not, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> resume();

    /**
     * Returns the project to resume the build from.
     *
     * @return an {@link Optional} containing the project name to resume from, or empty if not specified
     */
    @Nonnull
    Optional<String> resumeFrom();

    /**
     * Returns the list of specified reactor projects to build instead of all projects.
     *
     * @return an {@link Optional} containing the list of project names to build, or empty if not specified
     */
    @Nonnull
    Optional<List<String>> projects();

    /**
     * Indicates whether Maven should also build the specified projects' dependencies.
     *
     * @return an {@link Optional} containing true if dependencies should also be built, false if not, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> alsoMake();

    /**
     * Indicates whether Maven should also build the specified projects' dependents.
     *
     * @return an {@link Optional} containing true if dependents should also be built, false if not, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> alsoMakeDependents();

    /**
     * Returns the number of threads used for parallel builds.
     *
     * @return an {@link Optional} containing the number of threads (or "1C" for one thread per CPU core), or empty if not specified
     */
    @Nonnull
    Optional<String> threads();

    /**
     * Returns the id of the build strategy to use.
     *
     * @return an {@link Optional} containing the id of the build strategy, or empty if not specified
     */
    @Nonnull
    Optional<String> builder();

    /**
     * Indicates whether Maven should not display transfer progress when downloading or uploading.
     *
     * @return an {@link Optional} containing true if transfer progress should not be displayed, false if it should, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> noTransferProgress();

    /**
     * Indicates whether Maven should cache the "not found" status of artifacts that were not found in remote repositories.
     *
     * @return an {@link Optional} containing true if "not found" status should be cached, false if not, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> cacheArtifactNotFound();

    /**
     * Indicates whether Maven should use strict artifact descriptor policy.
     *
     * @return an {@link Optional} containing true if strict artifact descriptor policy should be used, false if not, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> strictArtifactDescriptorPolicy();

    /**
     * Indicates whether Maven should ignore transitive repositories.
     *
     * @return an {@link Optional} containing true if transitive repositories should be ignored, false if not, or empty if not specified
     */
    @Nonnull
    Optional<Boolean> ignoreTransitiveRepositories();

    /**
     * Specifies "@file"-like file, to load up command line from. It may contain goals as well. Format is one parameter
     * per line (similar to {@code maven.conf}) and {@code '#'} (hash) marked comment lines are allowed. Goals, if
     * present, are appended, to those specified on CLI input, if any.
     */
    Optional<String> atFile();

    /**
     * Returns the list of goals and phases to execute.
     *
     * @return an {@link Optional} containing the list of goals and phases to execute, or empty if not specified
     */
    @Nonnull
    Optional<List<String>> goals();

    /**
     * Returns a new instance of {@link MavenOptions} with values interpolated using the given callback.
     *
     * @param callback a callback to use for interpolation
     * @return a new MavenOptions instance with interpolated values
     */
    @Nonnull
    @Override
    MavenOptions interpolate(@Nonnull UnaryOperator<String> callback);
}
