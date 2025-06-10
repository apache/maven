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
package org.apache.maven.cling.invoker.mvnup;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.CommonsCliOptions;

/**
 * Implementation of {@link UpgradeOptions} (base + mvnup).
 */
public class CommonsCliUpgradeOptions extends CommonsCliOptions implements UpgradeOptions {
    public static CommonsCliUpgradeOptions parse(String[] args) throws ParseException {
        CLIManager cliManager = new CLIManager();
        return new CommonsCliUpgradeOptions(Options.SOURCE_CLI, cliManager, cliManager.parse(args));
    }

    protected CommonsCliUpgradeOptions(String source, CLIManager cliManager, CommandLine commandLine) {
        super(source, cliManager, commandLine);
    }

    @Override
    @Nonnull
    public Optional<Boolean> force() {
        if (commandLine.hasOption(CLIManager.FORCE)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    @Nonnull
    public Optional<Boolean> yes() {
        if (commandLine.hasOption(CLIManager.YES)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    @Nonnull
    public Optional<List<String>> goals() {
        if (!commandLine.getArgList().isEmpty()) {
            return Optional.of(commandLine.getArgList());
        }
        return Optional.empty();
    }

    @Override
    @Nonnull
    public Optional<String> modelVersion() {
        if (commandLine.hasOption(CLIManager.MODEL_VERSION)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.MODEL_VERSION));
        }
        return Optional.empty();
    }

    @Override
    @Nonnull
    public Optional<String> directory() {
        if (commandLine.hasOption(CLIManager.DIRECTORY)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.DIRECTORY));
        }
        return Optional.empty();
    }

    @Override
    @Nonnull
    public Optional<Boolean> infer() {
        if (commandLine.hasOption(CLIManager.INFER)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    @Nonnull
    public Optional<Boolean> model() {
        if (commandLine.hasOption(CLIManager.MODEL)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    @Nonnull
    public Optional<Boolean> plugins() {
        if (commandLine.hasOption(CLIManager.PLUGINS)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    @Nonnull
    public Optional<Boolean> all() {
        if (commandLine.hasOption(CLIManager.ALL)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public void displayHelp(ParserRequest request, Consumer<String> printStream) {
        super.displayHelp(request, printStream);
        printStream.accept("");
        // we have no DI here (to discover)
        printStream.accept("Goals:");
        printStream.accept("  help  - display this help message");
        printStream.accept("  check - check for available upgrades");
        printStream.accept("  apply - apply available upgrades");
        printStream.accept("");
        printStream.accept("Options:");
        printStream.accept("  -m, --model-version <version> Target POM model version (4.0.0 or 4.1.0)");
        printStream.accept("  -d, --directory <path> Directory to use as starting point for POM discovery");
        printStream.accept("  -i, --infer           Remove redundant information that can be inferred by Maven");
        printStream.accept("      --model           Fix Maven 4 compatibility issues in POM files");
        printStream.accept("      --plugins         Upgrade plugins known to fail with Maven 4");
        printStream.accept(
                "  -a, --all             Apply all upgrades (equivalent to --model-version 4.1.0 --infer --model --plugins)");
        printStream.accept("  -f, --force           Overwrite files without asking for confirmation");
        printStream.accept("  -y, --yes             Answer \"yes\" to all prompts automatically");
        printStream.accept("");
        printStream.accept("Default behavior: --model and --plugins are applied if no other options are specified");
        printStream.accept("");
    }

    @Override
    protected CommonsCliUpgradeOptions copy(
            String source, CommonsCliOptions.CLIManager cliManager, CommandLine commandLine) {
        return new CommonsCliUpgradeOptions(source, (CLIManager) cliManager, commandLine);
    }

    protected static class CLIManager extends CommonsCliOptions.CLIManager {
        public static final String FORCE = "f";
        public static final String YES = "y";
        public static final String MODEL_VERSION = "m";
        public static final String DIRECTORY = "d";
        public static final String INFER = "i";
        public static final String MODEL = "model";
        public static final String PLUGINS = "plugins";
        public static final String ALL = "a";

        @Override
        protected void prepareOptions(org.apache.commons.cli.Options options) {
            super.prepareOptions(options);
            options.addOption(Option.builder(FORCE)
                    .longOpt("force")
                    .desc("Should overwrite without asking any configuration?")
                    .build());
            options.addOption(Option.builder(YES)
                    .longOpt("yes")
                    .desc("Should imply user answered \"yes\" to all incoming questions?")
                    .build());
            options.addOption(Option.builder(MODEL_VERSION)
                    .longOpt("model-version")
                    .hasArg()
                    .argName("version")
                    .desc("Target POM model version (4.0.0 or 4.1.0)")
                    .build());
            options.addOption(Option.builder(DIRECTORY)
                    .longOpt("directory")
                    .hasArg()
                    .argName("path")
                    .desc("Directory to use as starting point for POM discovery")
                    .build());
            options.addOption(Option.builder(INFER)
                    .longOpt("infer")
                    .desc("Use inference when upgrading (remove redundant information)")
                    .build());
            options.addOption(Option.builder(MODEL)
                    .longOpt("model")
                    .desc("Fix Maven 4 compatibility issues in POM files")
                    .build());
            options.addOption(Option.builder(PLUGINS)
                    .longOpt("plugins")
                    .desc("Upgrade plugins known to fail with Maven 4 to their minimum compatible versions")
                    .build());
            options.addOption(Option.builder(ALL)
                    .longOpt("all")
                    .desc("Apply all upgrades (equivalent to --model-version 4.1.0 --infer --model --plugins)")
                    .build());
        }
    }
}
