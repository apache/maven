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
package org.apache.maven.api;

import java.util.List;
import java.util.Map;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Describes the invocation context of a Maven build: the flags, properties, and
 * environment settings that were active when the build started.
 *
 * <p>An instance is available via {@link Session#buildEnvironment()} during the build,
 * and is also recorded in the structured build report for post-mortem analysis and
 * reproducibility.
 *
 * <h2>What is captured</h2>
 * <ul>
 *   <li>Goals and lifecycle phases requested ({@link #goals()})</li>
 *   <li>User properties passed via {@code -Dkey=value} ({@link #userProperties()}),
 *       with sensitive keys redacted — see {@link #userProperties()} for the denylist</li>
 *   <li>A curated subset of system properties relevant to reproducibility
 *       ({@link #systemInfo()}): OS name/arch/version, Java vendor and VM name/version,
 *       Maven home, and available processors</li>
 *   <li>Local repository path ({@link #localRepository()})</li>
 *   <li>Explicitly activated or deactivated profiles ({@link #activeProfiles()})</li>
 *   <li>Selected projects ({@link #selectedProjects()}) and resume-from
 *       ({@link #resumeFrom()})</li>
 *   <li>Reactor failure behavior ({@link #reactorFailureBehavior()})</li>
 *   <li>Offline mode ({@link #offline()}) and snapshot update policy
 *       ({@link #updateSnapshots()})</li>
 *   <li>Degree of concurrency ({@link #threads()})</li>
 *   <li>Transfer-progress suppression ({@link #noTransferProgress()}) and batch mode ({@link #batchMode()})</li>
 * </ul>
 *
 * <h2>What is not yet captured</h2>
 * <p>The following information is not currently available through the Maven 4 API
 * and therefore cannot be recorded here. It may be added in future versions as
 * the API evolves:
 * <ul>
 *   <li><b>Raw command-line arguments</b> ({@code args[]}): the CLI bootstrap layer
 *       does not surface these through {@code MavenExecutionRequest} or the session</li>
 *   <li><b>Implicitly activated profiles</b>: profiles activated by OS, JDK, or
 *       property conditions rather than by explicit {@code -P} flag; computing
 *       these requires per-project activation and is not available at session level</li>
 * </ul>
 *
 * @since 4.1.0
 * @see Session#buildEnvironment()
 */
@Experimental
@Immutable
public interface BuildEnvironment {

    /**
     * The goals or lifecycle phases that were requested on the command line.
     *
     * @return the requested goals/phases, never {@code null}
     */
    @Nonnull
    List<String> goals();

    /**
     * User properties passed via {@code -Dkey=value} on the command line or via
     * {@code --define}.
     *
     * <p>Sensitive keys are redacted and replaced with {@code "***"}. A key is
     * considered sensitive if its lower-case form contains any of:
     * {@code password}, {@code passwd}, {@code secret}, {@code token},
     * {@code apikey}, {@code api_key}, {@code credential}, {@code passphrase}.
     *
     * @return the user properties, never {@code null}; values of sensitive keys
     *         are replaced with {@code "***"}
     */
    @Nonnull
    Map<String, String> userProperties();

    /**
     * A curated subset of system properties capturing platform and JVM identity,
     * relevant for build reproducibility analysis.
     *
     * <p>The following keys are included when present:
     * {@code os.name}, {@code os.arch}, {@code os.version},
     * {@code java.vendor}, {@code java.vm.name}, {@code java.vm.version},
     * {@code maven.home}, {@code user.home}, {@code user.name},
     * and {@code available.processors} (from {@code Runtime.getRuntime()}).
     *
     * <p>The full {@code System.getProperties()} map is intentionally not captured:
     * it is large, mostly irrelevant, and may contain sensitive values.
     *
     * @return the curated system info map, never {@code null}
     */
    @Nonnull
    Map<String, String> systemInfo();

    /**
     * The path to the local repository used for this build.
     *
     * @return the local repository path string, never {@code null}
     */
    @Nonnull
    String localRepository();

    /**
     * Profiles explicitly activated ({@code -P profileId}) or deactivated
     * ({@code -P !profileId}) on the command line.
     *
     * <p>This list reflects only explicit {@code -P} selections. Profiles activated
     * implicitly by OS, JDK, or property conditions are not included here — see
     * the class-level javadoc for details.
     *
     * @return the explicitly selected profiles, never {@code null}; may be empty
     */
    @Nonnull
    List<String> activeProfiles();

    /**
     * Projects explicitly selected via {@code -pl} / {@code --projects}.
     *
     * @return the selected project selectors (e.g. {@code ":my-module"}),
     *         never {@code null}; empty for a full reactor build
     */
    @Nonnull
    List<String> selectedProjects();

    /**
     * The project to resume from, as specified via {@code -rf} / {@code --resume-from}.
     *
     * @return the resume-from selector, or {@code null} if not specified
     */
    String resumeFrom();

    /**
     * The reactor failure behavior, corresponding to the {@code -ff} / {@code -fae} /
     * {@code -fn} flags.
     *
     * <p>Possible values (matching {@code MavenExecutionRequest} constants):
     * <ul>
     *   <li>{@code "FAIL_FAST"} — stop at first failure ({@code -ff}, default)</li>
     *   <li>{@code "FAIL_AT_END"} — build all, report failures at end ({@code -fae})</li>
     *   <li>{@code "FAIL_NEVER"} — always exit with success ({@code -fn})</li>
     * </ul>
     *
     * @return the failure behavior string, never {@code null}
     */
    @Nonnull
    String reactorFailureBehavior();

    /**
     * Whether the build was invoked in offline mode ({@code -o} / {@code --offline}).
     *
     * @return {@code true} if offline mode was active
     */
    boolean offline();

    /**
     * Whether snapshot updates were forced ({@code -U} / {@code --update-snapshots}).
     *
     * @return {@code true} if snapshot updates were forced
     */
    boolean updateSnapshots();

    /**
     * Whether transfer progress output was suppressed ({@code --no-transfer-progress} / {@code -ntp}).
     *
     * @return {@code true} if transfer progress was disabled
     */
    boolean noTransferProgress();

    /**
     * Whether the build was invoked in non-interactive (batch) mode ({@code --batch-mode} / {@code -B}).
     * Equivalent to {@code !isInteractiveMode()} on {@code MavenExecutionRequest}.
     *
     * @return {@code true} if batch mode was active
     */
    boolean batchMode();

    /**
     * The degree of concurrency ({@code -T} flag), or {@code 1} for sequential builds.
     *
     * @return the thread count
     */
    int threads();
}
