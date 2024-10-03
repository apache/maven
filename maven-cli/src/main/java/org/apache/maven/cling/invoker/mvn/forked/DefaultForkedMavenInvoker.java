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
package org.apache.maven.cling.invoker.mvn.forked;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.cli.mvn.forked.ForkedMavenInvoker;
import org.apache.maven.api.cli.mvn.forked.ForkedMavenInvokerRequest;
import org.apache.maven.utils.Os;

import static java.util.Objects.requireNonNull;

/**
 * Forked invoker implementation, it spawns a subprocess with Maven.
 */
public class DefaultForkedMavenInvoker implements ForkedMavenInvoker {
    @Override
    public int invoke(ForkedMavenInvokerRequest invokerRequest) throws InvokerException {
        requireNonNull(invokerRequest);
        validate(invokerRequest);

        ArrayList<String> cmdAndArguments = new ArrayList<>();
        cmdAndArguments.add(invokerRequest
                .installationDirectory()
                .resolve("bin")
                .resolve(Os.IS_WINDOWS ? invokerRequest.command() + ".cmd" : invokerRequest.command())
                .toString());

        MavenOptions mavenOptions = invokerRequest.options();
        if (mavenOptions.userProperties().isPresent()) {
            for (Map.Entry<String, String> entry :
                    mavenOptions.userProperties().get().entrySet()) {
                cmdAndArguments.add("-D" + entry.getKey() + "=" + entry.getValue());
            }
        }
        if (mavenOptions.showVersionAndExit().orElse(false)) {
            cmdAndArguments.add("--version");
        }
        if (mavenOptions.showVersion().orElse(false)) {
            cmdAndArguments.add("--show-version");
        }
        if (mavenOptions.quiet().orElse(false)) {
            cmdAndArguments.add("--quiet");
        }
        if (mavenOptions.verbose().orElse(false)) {
            cmdAndArguments.add("--verbose");
        }
        if (mavenOptions.showErrors().orElse(false)) {
            cmdAndArguments.add("--errors");
        }
        if (mavenOptions.failOnSeverity().isPresent()) {
            cmdAndArguments.add("--fail-on-severity");
            cmdAndArguments.add(mavenOptions.failOnSeverity().get());
        }
        if (mavenOptions.nonInteractive().orElse(false)) {
            cmdAndArguments.add("--non-interactive");
        }
        if (mavenOptions.forceInteractive().orElse(false)) {
            cmdAndArguments.add("--force-interactive");
        }
        if (mavenOptions.altUserSettings().isPresent()) {
            cmdAndArguments.add("--settings");
            cmdAndArguments.add(mavenOptions.altUserSettings().get());
        }
        if (mavenOptions.altProjectSettings().isPresent()) {
            cmdAndArguments.add("--project-settings");
            cmdAndArguments.add(mavenOptions.altProjectSettings().get());
        }
        if (mavenOptions.altInstallationSettings().isPresent()) {
            cmdAndArguments.add("--install-settings");
            cmdAndArguments.add(mavenOptions.altInstallationSettings().get());
        }
        if (mavenOptions.altUserToolchains().isPresent()) {
            cmdAndArguments.add("--toolchains");
            cmdAndArguments.add(mavenOptions.altUserToolchains().get());
        }
        if (mavenOptions.altInstallationToolchains().isPresent()) {
            cmdAndArguments.add("--install-toolchains");
            cmdAndArguments.add(mavenOptions.altInstallationToolchains().get());
        }
        if (mavenOptions.logFile().isPresent()) {
            cmdAndArguments.add("--log-file");
            cmdAndArguments.add(mavenOptions.logFile().get());
        }
        if (mavenOptions.color().isPresent()) {
            cmdAndArguments.add("--color");
            cmdAndArguments.add(mavenOptions.color().get());
        }
        if (mavenOptions.help().orElse(false)) {
            cmdAndArguments.add("--help");
        }
        if (mavenOptions.alternatePomFile().isPresent()) {
            cmdAndArguments.add("--file");
            cmdAndArguments.add(mavenOptions.alternatePomFile().get());
        }
        if (mavenOptions.offline().orElse(false)) {
            cmdAndArguments.add("--offline");
        }
        if (mavenOptions.nonRecursive().orElse(false)) {
            cmdAndArguments.add("--non-recursive");
        }
        if (mavenOptions.updateSnapshots().orElse(false)) {
            cmdAndArguments.add("--update-snapshots");
        }
        if (mavenOptions.activatedProfiles().isPresent()) {
            cmdAndArguments.add("--activate-profiles");
            cmdAndArguments.add(String.join(",", mavenOptions.activatedProfiles().get()));
        }
        if (mavenOptions.suppressSnapshotUpdates().orElse(false)) {
            cmdAndArguments.add("--no-snapshot-updates");
        }
        if (mavenOptions.strictChecksums().orElse(false)) {
            cmdAndArguments.add("--strict-checksums");
        }
        if (mavenOptions.relaxedChecksums().orElse(false)) {
            cmdAndArguments.add("--lax-checksums");
        }
        if (mavenOptions.failFast().orElse(false)) {
            cmdAndArguments.add("--fail-fast");
        }
        if (mavenOptions.failAtEnd().orElse(false)) {
            cmdAndArguments.add("--fail-at-end");
        }
        if (mavenOptions.failNever().orElse(false)) {
            cmdAndArguments.add("--fail-never");
        }
        if (mavenOptions.resume().orElse(false)) {
            cmdAndArguments.add("--resume");
        }
        if (mavenOptions.resumeFrom().isPresent()) {
            cmdAndArguments.add("--resume-from");
            cmdAndArguments.add(mavenOptions.resumeFrom().get());
        }
        if (mavenOptions.projects().isPresent()) {
            cmdAndArguments.add("--projects");
            cmdAndArguments.add(String.join(",", mavenOptions.projects().get()));
        }
        if (mavenOptions.alsoMake().orElse(false)) {
            cmdAndArguments.add("--also-make");
        }
        if (mavenOptions.alsoMakeDependents().orElse(false)) {
            cmdAndArguments.add("--also-make-dependents");
        }
        if (mavenOptions.threads().isPresent()) {
            cmdAndArguments.add("--threads");
            cmdAndArguments.add(mavenOptions.threads().get());
        }
        if (mavenOptions.builder().isPresent()) {
            cmdAndArguments.add("--builder");
            cmdAndArguments.add(mavenOptions.builder().get());
        }
        if (mavenOptions.noTransferProgress().orElse(false)) {
            cmdAndArguments.add("--no-transfer-progress");
        }
        if (mavenOptions.cacheArtifactNotFound().isPresent()) {
            cmdAndArguments.add("--cache-artifact-not-found");
            cmdAndArguments.add(mavenOptions.cacheArtifactNotFound().get().toString());
        }
        if (mavenOptions.strictArtifactDescriptorPolicy().isPresent()) {
            cmdAndArguments.add("--strict-artifact-descriptor-policy");
            cmdAndArguments.add(mavenOptions.strictArtifactDescriptorPolicy().get().toString());
        }
        if (mavenOptions.ignoreTransitiveRepositories().isPresent()) {
            cmdAndArguments.add("--ignore-transitive-repositories");
        }

        // last the goals
        cmdAndArguments.addAll(mavenOptions.goals().orElse(Collections.emptyList()));

        try {
            ProcessBuilder pb = new ProcessBuilder()
                    .directory(invokerRequest.cwd().toFile())
                    .command(cmdAndArguments);

            if (invokerRequest.jvmArguments().isPresent()) {
                pb.environment()
                        .put(
                                "MAVEN_OPTS",
                                String.join(" ", invokerRequest.jvmArguments().get()));
            }

            return pb.start().waitFor();
        } catch (IOException e) {
            invokerRequest.logger().error("IO problem while executing command: " + cmdAndArguments, e);
            return 127;
        } catch (InterruptedException e) {
            invokerRequest.logger().error("Interrupted while executing command: " + cmdAndArguments, e);
            return 127;
        }
    }

    protected void validate(ForkedMavenInvokerRequest invokerRequest) throws InvokerException {}
}
