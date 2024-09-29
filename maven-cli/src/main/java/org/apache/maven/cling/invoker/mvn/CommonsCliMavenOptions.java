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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.cli.CLIManager;
import org.apache.maven.cling.invoker.CommonsCliOptions;
import org.codehaus.plexus.interpolation.BasicInterpolator;
import org.codehaus.plexus.interpolation.InterpolationException;

import static org.apache.maven.cling.invoker.Utils.createInterpolator;

public class CommonsCliMavenOptions extends CommonsCliOptions implements MavenOptions {
    public CommonsCliMavenOptions(CLIManager cliManager, CommandLine commandLine) {
        super(cliManager, commandLine);
    }

    private static CommonsCliMavenOptions interpolate(
            CommonsCliMavenOptions options, Collection<Map<String, String>> properties) {
        try {
            // now that we have properties, interpolate all arguments
            BasicInterpolator interpolator = createInterpolator(properties);
            CommandLine.Builder commandLineBuilder = new CommandLine.Builder();
            commandLineBuilder.setDeprecatedHandler(o -> {});
            for (Option option : options.commandLine.getOptions()) {
                if (!String.valueOf(CLIManager.SET_USER_PROPERTY).equals(option.getOpt())) {
                    List<String> values = option.getValuesList();
                    for (ListIterator<String> it = values.listIterator(); it.hasNext(); ) {
                        it.set(interpolator.interpolate(it.next()));
                    }
                }
                commandLineBuilder.addOption(option);
            }
            for (String arg : options.commandLine.getArgList()) {
                commandLineBuilder.addArg(interpolator.interpolate(arg));
            }
            return new CommonsCliMavenOptions(options.cliManager, commandLineBuilder.build());
        } catch (InterpolationException e) {
            throw new IllegalArgumentException("Could not interpolate CommonsCliOptions", e);
        }
    }

    @Override
    public Optional<String> alternatePomFile() {
        if (commandLine.hasOption(CLIManager.ALTERNATE_POM_FILE)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.ALTERNATE_POM_FILE));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> offline() {
        if (commandLine.hasOption(CLIManager.OFFLINE)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> nonRecursive() {
        if (commandLine.hasOption(CLIManager.NON_RECURSIVE)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> updateSnapshots() {
        if (commandLine.hasOption(CLIManager.UPDATE_SNAPSHOTS)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> activatedProfiles() {
        if (commandLine.hasOption(CLIManager.ACTIVATE_PROFILES)) {
            return Optional.of(Arrays.asList(commandLine.getOptionValues(CLIManager.ACTIVATE_PROFILES)));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> suppressSnapshotUpdates() {
        if (commandLine.hasOption(CLIManager.SUPPRESS_SNAPSHOT_UPDATES)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> strictChecksums() {
        if (commandLine.hasOption(CLIManager.CHECKSUM_FAILURE_POLICY)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> relaxedChecksums() {
        if (commandLine.hasOption(CLIManager.CHECKSUM_WARNING_POLICY)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> failOnSeverity() {
        if (commandLine.hasOption(CLIManager.FAIL_ON_SEVERITY)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.FAIL_ON_SEVERITY));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> failFast() {
        if (commandLine.hasOption(CLIManager.FAIL_FAST)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> failAtEnd() {
        if (commandLine.hasOption(CLIManager.FAIL_AT_END)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> failNever() {
        if (commandLine.hasOption(CLIManager.FAIL_NEVER)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> resume() {
        if (commandLine.hasOption(CLIManager.RESUME)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> resumeFrom() {
        if (commandLine.hasOption(CLIManager.RESUME_FROM)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.RESUME_FROM));
        }
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> projects() {
        if (commandLine.hasOption(CLIManager.PROJECT_LIST)) {
            return Optional.of(Arrays.asList(commandLine.getOptionValues(CLIManager.PROJECT_LIST)));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> alsoMake() {
        if (commandLine.hasOption(CLIManager.ALSO_MAKE)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> alsoMakeDependents() {
        if (commandLine.hasOption(CLIManager.ALSO_MAKE_DEPENDENTS)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> threads() {
        if (commandLine.hasOption(CLIManager.THREADS)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.THREADS));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> builder() {
        if (commandLine.hasOption(CLIManager.BUILDER)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.BUILDER));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> noTransferProgress() {
        if (commandLine.hasOption(CLIManager.NO_TRANSFER_PROGRESS)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> cacheArtifactNotFound() {
        if (commandLine.hasOption(CLIManager.CACHE_ARTIFACT_NOT_FOUND)) {
            return Optional.of(Boolean.parseBoolean(commandLine.getOptionValue(CLIManager.CACHE_ARTIFACT_NOT_FOUND)));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> strictArtifactDescriptorPolicy() {
        if (commandLine.hasOption(CLIManager.STRICT_ARTIFACT_DESCRIPTOR_POLICY)) {
            return Optional.of(
                    Boolean.parseBoolean(commandLine.getOptionValue(CLIManager.STRICT_ARTIFACT_DESCRIPTOR_POLICY)));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> ignoreTransitiveRepositories() {
        if (commandLine.hasOption(CLIManager.IGNORE_TRANSITIVE_REPOSITORIES)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> goals() {
        if (!commandLine.getArgList().isEmpty()) {
            return Optional.of(commandLine.getArgList());
        }
        return Optional.empty();
    }

    @Override
    public MavenOptions interpolate(Collection<Map<String, String>> properties) {
        return interpolate(this, properties);
    }
}
