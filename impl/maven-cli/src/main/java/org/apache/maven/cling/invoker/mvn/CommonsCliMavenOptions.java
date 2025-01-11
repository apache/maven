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
import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.cling.invoker.CommonsCliOptions;
import org.codehaus.plexus.interpolation.BasicInterpolator;
import org.codehaus.plexus.interpolation.InterpolationException;

import static org.apache.maven.cling.invoker.InvokerUtils.createInterpolator;

public class CommonsCliMavenOptions extends CommonsCliOptions implements MavenOptions {
    public static CommonsCliMavenOptions parse(String source, String[] args) throws ParseException {
        CLIManager cliManager = new CLIManager();
        return new CommonsCliMavenOptions(source, cliManager, cliManager.parse(args));
    }

    protected CommonsCliMavenOptions(String source, CLIManager cliManager, CommandLine commandLine) {
        super(source, cliManager, commandLine);
    }

    private static CommonsCliMavenOptions interpolate(
            CommonsCliMavenOptions options, Collection<Map<String, String>> properties) {
        try {
            // now that we have properties, interpolate all arguments
            BasicInterpolator interpolator = createInterpolator(properties);
            CommandLine.Builder commandLineBuilder = new CommandLine.Builder();
            commandLineBuilder.setDeprecatedHandler(o -> {});
            for (Option option : options.commandLine.getOptions()) {
                if (!CLIManager.USER_PROPERTY.equals(option.getOpt())) {
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
            return new CommonsCliMavenOptions(
                    options.source, (CLIManager) options.cliManager, commandLineBuilder.build());
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

    protected static class CLIManager extends CommonsCliOptions.CLIManager {
        public static final String ALTERNATE_POM_FILE = "f";
        public static final String NON_RECURSIVE = "N";
        public static final String UPDATE_SNAPSHOTS = "U";
        public static final String ACTIVATE_PROFILES = "P";
        public static final String SUPPRESS_SNAPSHOT_UPDATES = "nsu";
        public static final String CHECKSUM_FAILURE_POLICY = "C";
        public static final String CHECKSUM_WARNING_POLICY = "c";
        public static final String FAIL_FAST = "ff";
        public static final String FAIL_AT_END = "fae";
        public static final String FAIL_NEVER = "fn";
        public static final String RESUME = "r";
        public static final String RESUME_FROM = "rf";
        public static final String PROJECT_LIST = "pl";
        public static final String ALSO_MAKE = "am";
        public static final String ALSO_MAKE_DEPENDENTS = "amd";
        public static final String THREADS = "T";
        public static final String BUILDER = "b";
        public static final String NO_TRANSFER_PROGRESS = "ntp";
        public static final String CACHE_ARTIFACT_NOT_FOUND = "canf";
        public static final String STRICT_ARTIFACT_DESCRIPTOR_POLICY = "sadp";
        public static final String IGNORE_TRANSITIVE_REPOSITORIES = "itr";

        @Override
        protected void prepareOptions(org.apache.commons.cli.Options options) {
            super.prepareOptions(options);
            options.addOption(Option.builder(ALTERNATE_POM_FILE)
                    .longOpt("file")
                    .hasArg()
                    .desc("Force the use of an alternate POM file (or directory with pom.xml)")
                    .build());
            options.addOption(Option.builder(NON_RECURSIVE)
                    .longOpt("non-recursive")
                    .desc(
                            "Do not recurse into sub-projects. When used together with -pl, do not recurse into sub-projects of selected aggregators")
                    .build());
            options.addOption(Option.builder(UPDATE_SNAPSHOTS)
                    .longOpt("update-snapshots")
                    .desc("Forces a check for missing releases and updated snapshots on remote repositories")
                    .build());
            options.addOption(Option.builder(ACTIVATE_PROFILES)
                    .longOpt("activate-profiles")
                    .desc(
                            "Comma-delimited list of profiles to activate. Prefixing a profile with ! excludes it, and ? marks it as optional")
                    .hasArg()
                    .build());
            options.addOption(Option.builder(SUPPRESS_SNAPSHOT_UPDATES)
                    .longOpt("no-snapshot-updates")
                    .desc("Suppress SNAPSHOT updates")
                    .build());
            options.addOption(Option.builder(CHECKSUM_FAILURE_POLICY)
                    .longOpt("strict-checksums")
                    .desc("Fail the build if checksums don't match")
                    .build());
            options.addOption(Option.builder(CHECKSUM_WARNING_POLICY)
                    .longOpt("lax-checksums")
                    .desc("Warn if checksums don't match")
                    .build());
            options.addOption(Option.builder(FAIL_FAST)
                    .longOpt("fail-fast")
                    .desc("Stop at first failure in reactorized builds")
                    .build());
            options.addOption(Option.builder(FAIL_AT_END)
                    .longOpt("fail-at-end")
                    .desc("Only fail the build afterwards; allow all non-impacted builds to continue")
                    .build());
            options.addOption(Option.builder(FAIL_NEVER)
                    .longOpt("fail-never")
                    .desc("NEVER fail the build, regardless of project result")
                    .build());
            options.addOption(Option.builder(RESUME)
                    .longOpt("resume")
                    .desc(
                            "Resume reactor from the last failed project, using the resume.properties file in the build directory")
                    .build());
            options.addOption(Option.builder(RESUME_FROM)
                    .longOpt("resume-from")
                    .hasArg()
                    .desc("Resume reactor from specified project")
                    .build());
            options.addOption(Option.builder(PROJECT_LIST)
                    .longOpt("projects")
                    .desc(
                            "Comma-delimited list of specified reactor projects to build instead of all projects. A project can be specified by [groupId]:artifactId or by its relative path. Prefixing a project with ! excludes it, and ? marks it as optional")
                    .hasArg()
                    .build());
            options.addOption(Option.builder(ALSO_MAKE)
                    .longOpt("also-make")
                    .desc("If project list is specified, also build projects required by the list")
                    .build());
            options.addOption(Option.builder(ALSO_MAKE_DEPENDENTS)
                    .longOpt("also-make-dependents")
                    .desc("If project list is specified, also build projects that depend on projects on the list")
                    .build());
            options.addOption(Option.builder(THREADS)
                    .longOpt("threads")
                    .hasArg()
                    .desc("Thread count, for instance 4 (int) or 2C/2.5C (int/float) where C is core multiplied")
                    .build());
            options.addOption(Option.builder(BUILDER)
                    .longOpt("builder")
                    .hasArg()
                    .desc("The id of the build strategy to use")
                    .build());
            options.addOption(Option.builder(NO_TRANSFER_PROGRESS)
                    .longOpt("no-transfer-progress")
                    .desc("Do not display transfer progress when downloading or uploading")
                    .build());
            options.addOption(Option.builder(CACHE_ARTIFACT_NOT_FOUND)
                    .longOpt("cache-artifact-not-found")
                    .hasArg()
                    .desc(
                            "Defines caching behaviour for 'not found' artifacts. Supported values are 'true' (default), 'false'.")
                    .build());
            options.addOption(Option.builder(STRICT_ARTIFACT_DESCRIPTOR_POLICY)
                    .longOpt("strict-artifact-descriptor-policy")
                    .hasArg()
                    .desc(
                            "Defines 'strict' artifact descriptor policy. Supported values are 'true', 'false' (default).")
                    .build());
            options.addOption(Option.builder(IGNORE_TRANSITIVE_REPOSITORIES)
                    .longOpt("ignore-transitive-repositories")
                    .desc("If set, Maven will ignore remote repositories introduced by transitive dependencies.")
                    .build());
        }
    }
}
