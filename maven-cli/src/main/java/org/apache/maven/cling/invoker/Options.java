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
package org.apache.maven.cling.invoker;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.annotations.Nonnull;

/**
 * Maven options.
 * <p>
 * This is pretty much what Maven CLI surface offers as knobs and switches. Also, {@code maven.config} may contain
 * subset of these (ie no goals).
 */
public interface Options {
    @Nonnull
    Optional<Map<String, String>> userProperties();

    @Nonnull
    Optional<String> alternatePomFile();

    @Nonnull
    Optional<Boolean> offline();

    @Nonnull
    Optional<Boolean> showVersionAndExit();

    @Nonnull
    Optional<Boolean> showVersion();

    @Nonnull
    Optional<Boolean> quiet();

    @Nonnull
    Optional<Boolean> verbose();

    @Nonnull
    Optional<Boolean> showErrors();

    @Nonnull
    Optional<Boolean> nonRecursive();

    @Nonnull
    Optional<Boolean> updateSnapshots();

    @Nonnull
    Optional<List<String>> activatedProfiles();

    @Nonnull
    Optional<Boolean> nonInteractive();

    @Nonnull
    Optional<Boolean> forceInteractive();

    @Nonnull
    Optional<Boolean> suppressSnapshotUpdates();

    @Nonnull
    Optional<Boolean> strictChecksums();

    @Nonnull
    Optional<Boolean> relaxedChecksums();

    @Nonnull
    Optional<String> altUserSettings();

    @Nonnull
    Optional<String> altProjectSettings();

    @Nonnull
    Optional<String> altInstallationSettings();

    @Nonnull
    Optional<String> altUserToolchains();

    @Nonnull
    Optional<String> altInstallationToolchains();

    @Nonnull
    Optional<String> failOnSeverity();

    @Nonnull
    Optional<Boolean> failFast();

    @Nonnull
    Optional<Boolean> failAtEnd();

    @Nonnull
    Optional<Boolean> failNever();

    @Nonnull
    Optional<Boolean> resume();

    @Nonnull
    Optional<String> resumeFrom();

    @Nonnull
    Optional<List<String>> projects();

    @Nonnull
    Optional<Boolean> alsoMake();

    @Nonnull
    Optional<Boolean> alsoMakeDependents();

    @Nonnull
    Optional<String> logFile();

    @Nonnull
    Optional<String> threads();

    @Nonnull
    Optional<String> builder();

    @Nonnull
    Optional<Boolean> noTransferProgress();

    @Nonnull
    Optional<String> color();

    @Nonnull
    Optional<Boolean> cacheArtifactNotFound();

    @Nonnull
    Optional<Boolean> strictArtifactDescriptorPolicy();

    @Nonnull
    Optional<Boolean> ignoreTransitiveRepositories();

    @Nonnull
    Optional<Boolean> help();

    @Nonnull
    Optional<List<String>> goals();

    /**
     * Returns new instance of {@link Options} that is result of interpolating this instance with given collection
     * of properties.
     */
    @Nonnull
    Options interpolate(Collection<Map<String, String>> properties);

    /**
     * Displays help.
     */
    void displayHelp(PrintStream printStream);
}
