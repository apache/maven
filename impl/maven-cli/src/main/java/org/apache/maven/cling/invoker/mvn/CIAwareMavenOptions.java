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
package org.apache.maven.cling.invoker.mvn;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.cisupport.CIInfo;
import org.apache.maven.api.cli.mvn.MavenOptions;

/**
 * A wrapper around MavenOptions that provides CI-specific defaults when CI is detected.
 * This class applies the following optimizations for CI environments:
 * <ul>
 *   <li>Enables batch mode (non-interactive) if not explicitly overridden</li>
 *   <li>Enables show-version for better CI build identification</li>
 *   <li>Enables show-errors for better CI debugging</li>
 * </ul>
 *
 * All CI optimizations can be overridden by explicit command-line flags.
 *
 * @since 4.0.0
 */
public class CIAwareMavenOptions implements MavenOptions {
    private final MavenOptions delegate;
    private final CIInfo ciInfo;
    private final boolean hasExplicitShowVersion;
    private final boolean hasExplicitShowErrors;
    private final boolean hasExplicitNonInteractive;

    public CIAwareMavenOptions(MavenOptions delegate, CIInfo ciInfo) {
        this.delegate = delegate;
        this.ciInfo = ciInfo;

        // Check if user has explicitly set these options
        this.hasExplicitShowVersion = delegate.showVersion().isPresent();
        this.hasExplicitShowErrors = delegate.showErrors().isPresent();
        this.hasExplicitNonInteractive = delegate.nonInteractive().isPresent();
    }

    @Override
    public String source() {
        return "CI-aware(" + delegate.source() + ")";
    }

    @Override
    public Optional<Boolean> showVersion() {
        // If user explicitly set show-version, respect their choice
        if (hasExplicitShowVersion) {
            return delegate.showVersion();
        }
        // In CI, default to showing version for better build identification
        return Optional.of(true);
    }

    @Override
    public Optional<Boolean> showErrors() {
        // If user explicitly set show-errors, respect their choice
        if (hasExplicitShowErrors) {
            return delegate.showErrors();
        }
        // In CI, default to showing errors for better debugging
        return Optional.of(true);
    }

    @Override
    public Optional<Boolean> nonInteractive() {
        // If user explicitly set non-interactive/batch-mode, respect their choice
        if (hasExplicitNonInteractive) {
            return delegate.nonInteractive();
        }
        // In CI, default to non-interactive mode (batch mode)
        return Optional.of(true);
    }

    // Delegate all other methods to the wrapped options
    @Override
    public Optional<Map<String, String>> userProperties() {
        return delegate.userProperties();
    }

    @Override
    public Optional<Boolean> showVersionAndExit() {
        return delegate.showVersionAndExit();
    }

    @Override
    public Optional<Boolean> quiet() {
        return delegate.quiet();
    }

    @Override
    public Optional<Boolean> verbose() {
        return delegate.verbose();
    }

    @Override
    public Optional<String> failOnSeverity() {
        return delegate.failOnSeverity();
    }

    @Override
    public Optional<Boolean> forceInteractive() {
        return delegate.forceInteractive();
    }

    @Override
    public Optional<String> altUserSettings() {
        return delegate.altUserSettings();
    }

    @Override
    public Optional<String> altProjectSettings() {
        return delegate.altProjectSettings();
    }

    @Override
    public Optional<String> altInstallationSettings() {
        return delegate.altInstallationSettings();
    }

    @Override
    public Optional<String> altUserToolchains() {
        return delegate.altUserToolchains();
    }

    @Override
    public Optional<String> altInstallationToolchains() {
        return delegate.altInstallationToolchains();
    }

    @Override
    public Optional<String> logFile() {
        return delegate.logFile();
    }

    @Override
    public Optional<Boolean> rawStreams() {
        return delegate.rawStreams();
    }

    @Override
    public Optional<String> color() {
        return delegate.color();
    }

    @Override
    public Optional<Boolean> offline() {
        return delegate.offline();
    }

    @Override
    public Optional<Boolean> help() {
        return delegate.help();
    }

    @Override
    public Optional<String> alternatePomFile() {
        return delegate.alternatePomFile();
    }

    @Override
    public Optional<List<String>> goals() {
        return delegate.goals();
    }

    @Override
    public Optional<List<String>> activatedProfiles() {
        return delegate.activatedProfiles();
    }

    @Override
    public Optional<Boolean> suppressSnapshotUpdates() {
        return delegate.suppressSnapshotUpdates();
    }

    @Override
    public Optional<Boolean> strictChecksums() {
        return delegate.strictChecksums();
    }

    @Override
    public Optional<Boolean> relaxedChecksums() {
        return delegate.relaxedChecksums();
    }

    @Override
    public Optional<Boolean> failFast() {
        return delegate.failFast();
    }

    @Override
    public Optional<Boolean> failAtEnd() {
        return delegate.failAtEnd();
    }

    @Override
    public Optional<Boolean> failNever() {
        return delegate.failNever();
    }

    @Override
    public Optional<Boolean> resume() {
        return delegate.resume();
    }

    @Override
    public Optional<String> resumeFrom() {
        return delegate.resumeFrom();
    }

    @Override
    public Optional<List<String>> projects() {
        return delegate.projects();
    }

    @Override
    public Optional<Boolean> alsoMake() {
        return delegate.alsoMake();
    }

    @Override
    public Optional<Boolean> alsoMakeDependents() {
        return delegate.alsoMakeDependents();
    }

    @Override
    public Optional<String> threads() {
        return delegate.threads();
    }

    @Override
    public Optional<String> builder() {
        return delegate.builder();
    }

    @Override
    public Optional<Boolean> noTransferProgress() {
        return delegate.noTransferProgress();
    }

    @Override
    public Optional<Boolean> cacheArtifactNotFound() {
        return delegate.cacheArtifactNotFound();
    }

    @Override
    public Optional<Boolean> strictArtifactDescriptorPolicy() {
        return delegate.strictArtifactDescriptorPolicy();
    }

    @Override
    public Optional<Boolean> ignoreTransitiveRepositories() {
        return delegate.ignoreTransitiveRepositories();
    }

    @Override
    public Optional<String> atFile() {
        return delegate.atFile();
    }

    @Override
    public Optional<Boolean> updateSnapshots() {
        return delegate.updateSnapshots();
    }

    @Override
    public Optional<Boolean> nonRecursive() {
        return delegate.nonRecursive();
    }

    @Override
    public MavenOptions interpolate(UnaryOperator<String> callback) {
        return new CIAwareMavenOptions((MavenOptions) delegate.interpolate(callback), ciInfo);
    }

    @Override
    public void warnAboutDeprecatedOptions(ParserRequest request, Consumer<String> printWriter) {
        delegate.warnAboutDeprecatedOptions(request, printWriter);
    }

    @Override
    public void displayHelp(ParserRequest request, Consumer<String> printWriter) {
        delegate.displayHelp(request, printWriter);
    }

    /**
     * Returns the CI information that triggered the CI-aware behavior.
     *
     * @return the CI information
     */
    public CIInfo getCIInfo() {
        return ciInfo;
    }

    /**
     * Returns the underlying delegate options.
     *
     * @return the delegate options
     */
    public MavenOptions getDelegate() {
        return delegate;
    }
}
