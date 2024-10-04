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

import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.DeprecatedAttributes;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.cli.CleanArgument;
import org.apache.maven.jline.MessageUtils;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.cling.invoker.Utils.toMap;

public abstract class CommonsCliOptions implements Options {

    protected final String source;
    protected final CLIManager cliManager;
    protected final CommandLine commandLine;

    protected CommonsCliOptions(String source, CLIManager cliManager, CommandLine commandLine) {
        this.source = requireNonNull(source);
        this.cliManager = requireNonNull(cliManager);
        this.commandLine = requireNonNull(commandLine);
    }

    @Override
    public String source() {
        return source;
    }

    @Override
    public Optional<Map<String, String>> userProperties() {
        if (commandLine.hasOption(CLIManager.USER_PROPERTY)) {
            return Optional.of(toMap(commandLine.getOptionProperties(CLIManager.USER_PROPERTY)));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> showVersionAndExit() {
        if (commandLine.hasOption(CLIManager.SHOW_VERSION_AND_EXIT)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> showVersion() {
        if (commandLine.hasOption(CLIManager.SHOW_VERSION)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> quiet() {
        if (commandLine.hasOption(CLIManager.QUIET)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> verbose() {
        if (commandLine.hasOption(CLIManager.VERBOSE) || commandLine.hasOption(CLIManager.DEBUG)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> showErrors() {
        if (commandLine.hasOption(CLIManager.SHOW_ERRORS)) {
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
    public Optional<Boolean> nonInteractive() {
        if (commandLine.hasOption(CLIManager.NON_INTERACTIVE) || commandLine.hasOption(CLIManager.BATCH_MODE)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> forceInteractive() {
        if (commandLine.hasOption(CLIManager.FORCE_INTERACTIVE)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> altUserSettings() {
        if (commandLine.hasOption(CLIManager.ALTERNATE_USER_SETTINGS)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.ALTERNATE_USER_SETTINGS));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> altProjectSettings() {
        if (commandLine.hasOption(CLIManager.ALTERNATE_PROJECT_SETTINGS)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.ALTERNATE_PROJECT_SETTINGS));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> altInstallationSettings() {
        if (commandLine.hasOption(CLIManager.ALTERNATE_INSTALLATION_SETTINGS)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.ALTERNATE_INSTALLATION_SETTINGS));
        }
        if (commandLine.hasOption(CLIManager.ALTERNATE_GLOBAL_SETTINGS)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.ALTERNATE_GLOBAL_SETTINGS));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> altUserToolchains() {
        if (commandLine.hasOption(CLIManager.ALTERNATE_USER_TOOLCHAINS)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.ALTERNATE_USER_TOOLCHAINS));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> altInstallationToolchains() {
        if (commandLine.hasOption(CLIManager.ALTERNATE_INSTALLATION_TOOLCHAINS)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.ALTERNATE_INSTALLATION_TOOLCHAINS));
        }
        if (commandLine.hasOption(CLIManager.ALTERNATE_GLOBAL_TOOLCHAINS)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.ALTERNATE_GLOBAL_TOOLCHAINS));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> logFile() {
        if (commandLine.hasOption(CLIManager.LOG_FILE)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.LOG_FILE));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> color() {
        if (commandLine.hasOption(CLIManager.COLOR)) {
            if (commandLine.getOptionValue(CLIManager.COLOR) != null) {
                return Optional.of(commandLine.getOptionValue(CLIManager.COLOR));
            } else {
                return Optional.of("auto");
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> help() {
        if (commandLine.hasOption(CLIManager.HELP)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public void warnAboutDeprecatedOptions(ParserRequest request, PrintWriter printWriter) {
        if (cliManager.getUsedDeprecatedOptions().isEmpty()) {
            return;
        }
        printWriter.println("Detected deprecated option use in " + source);
        for (Option option : cliManager.getUsedDeprecatedOptions()) {
            StringBuilder sb = new StringBuilder();
            sb.append("The option -").append(option.getOpt());
            if (option.getLongOpt() != null) {
                sb.append(",--").append(option.getLongOpt());
            }
            sb.append(" is deprecated ");
            if (option.getDeprecated().isForRemoval()) {
                sb.append("and will be removed in a future version");
            }
            if (option.getDeprecated().getSince() != null) {
                sb.append("since ")
                        .append(request.commandName())
                        .append(" ")
                        .append(option.getDeprecated().getSince());
            }
            printWriter.println(sb);
        }
    }

    @Override
    public void displayHelp(ParserRequest request, PrintWriter printStream) {
        cliManager.displayHelp(request.command(), printStream);
    }

    protected static class CLIManager {
        public static final String USER_PROPERTY = "D";
        public static final String SHOW_VERSION_AND_EXIT = "v";
        public static final String SHOW_VERSION = "V";
        public static final String QUIET = "q";
        public static final String VERBOSE = "X";
        /** This option is deprecated and may be repurposed as Java debug in a future version.
         * Use {@code -X,--verbose} instead. */
        @Deprecated
        public static final String DEBUG = "debug";

        public static final String SHOW_ERRORS = "e";
        public static final String FAIL_ON_SEVERITY = "fos";
        public static final String NON_INTERACTIVE = "non-interactive";
        public static final String BATCH_MODE = "B";
        public static final String FORCE_INTERACTIVE = "force-interactive";
        public static final String ALTERNATE_USER_SETTINGS = "s";
        public static final String ALTERNATE_PROJECT_SETTINGS = "ps";

        @Deprecated
        public static final String ALTERNATE_GLOBAL_SETTINGS = "gs";

        public static final String ALTERNATE_INSTALLATION_SETTINGS = "is";
        public static final String ALTERNATE_USER_TOOLCHAINS = "t";

        @Deprecated
        public static final String ALTERNATE_GLOBAL_TOOLCHAINS = "gt";

        public static final String ALTERNATE_INSTALLATION_TOOLCHAINS = "it";
        public static final String LOG_FILE = "l";
        public static final String COLOR = "color";
        public static final String HELP = "h";

        protected org.apache.commons.cli.Options options;
        protected final LinkedHashSet<Option> usedDeprecatedOptions = new LinkedHashSet<>();

        @SuppressWarnings("checkstyle:MethodLength")
        protected CLIManager() {
            options = new org.apache.commons.cli.Options();
            prepareOptions(options);
        }

        protected void prepareOptions(org.apache.commons.cli.Options options) {
            options.addOption(Option.builder(HELP)
                    .longOpt("help")
                    .desc("Display help information")
                    .build());
            options.addOption(Option.builder(USER_PROPERTY)
                    .numberOfArgs(2)
                    .valueSeparator('=')
                    .desc("Define a user property")
                    .build());
            options.addOption(Option.builder(SHOW_VERSION_AND_EXIT)
                    .longOpt("version")
                    .desc("Display version information")
                    .build());
            options.addOption(Option.builder(QUIET)
                    .longOpt("quiet")
                    .desc("Quiet output - only show errors")
                    .build());
            options.addOption(Option.builder(VERBOSE)
                    .longOpt("verbose")
                    .desc("Produce execution verbose output")
                    .build());
            options.addOption(Option.builder(SHOW_ERRORS)
                    .longOpt("errors")
                    .desc("Produce execution error messages")
                    .build());
            options.addOption(Option.builder(BATCH_MODE)
                    .longOpt("batch-mode")
                    .desc("Run in non-interactive mode. Alias for --non-interactive (kept for backwards compatability)")
                    .build());
            options.addOption(Option.builder()
                    .longOpt(NON_INTERACTIVE)
                    .desc("Run in non-interactive mode. Alias for --batch-mode")
                    .build());
            options.addOption(Option.builder()
                    .longOpt(FORCE_INTERACTIVE)
                    .desc(
                            "Run in interactive mode. Overrides, if applicable, the CI environment variable and --non-interactive/--batch-mode options")
                    .build());
            options.addOption(Option.builder(ALTERNATE_USER_SETTINGS)
                    .longOpt("settings")
                    .desc("Alternate path for the user settings file")
                    .hasArg()
                    .build());
            options.addOption(Option.builder(ALTERNATE_PROJECT_SETTINGS)
                    .longOpt("project-settings")
                    .desc("Alternate path for the project settings file")
                    .hasArg()
                    .build());
            options.addOption(Option.builder(ALTERNATE_INSTALLATION_SETTINGS)
                    .longOpt("install-settings")
                    .desc("Alternate path for the installation settings file")
                    .hasArg()
                    .build());
            options.addOption(Option.builder(ALTERNATE_USER_TOOLCHAINS)
                    .longOpt("toolchains")
                    .desc("Alternate path for the user toolchains file")
                    .hasArg()
                    .build());
            options.addOption(Option.builder(ALTERNATE_INSTALLATION_TOOLCHAINS)
                    .longOpt("install-toolchains")
                    .desc("Alternate path for the installation toolchains file")
                    .hasArg()
                    .build());
            options.addOption(Option.builder(FAIL_ON_SEVERITY)
                    .longOpt("fail-on-severity")
                    .desc("Configure which severity of logging should cause the build to fail")
                    .hasArg()
                    .build());
            options.addOption(Option.builder(LOG_FILE)
                    .longOpt("log-file")
                    .hasArg()
                    .desc("Log file where all build output will go (disables output color)")
                    .build());
            options.addOption(Option.builder(SHOW_VERSION)
                    .longOpt("show-version")
                    .desc("Display version information WITHOUT stopping build")
                    .build());
            options.addOption(Option.builder()
                    .longOpt(COLOR)
                    .hasArg()
                    .optionalArg(true)
                    .desc("Defines the color mode of the output. Supported are 'auto', 'always', 'never'.")
                    .build());

            // Deprecated
            options.addOption(Option.builder()
                    .longOpt(DEBUG)
                    .desc("<deprecated> Produce execution verbose output.")
                    .deprecated(DeprecatedAttributes.builder()
                            .setForRemoval(true)
                            .setSince("4.0.0")
                            .setDescription("Use -X,--verbose instead.")
                            .get())
                    .build());
            options.addOption(Option.builder(ALTERNATE_GLOBAL_SETTINGS)
                    .longOpt("global-settings")
                    .desc("<deprecated> Alternate path for the global settings file.")
                    .hasArg()
                    .deprecated(DeprecatedAttributes.builder()
                            .setForRemoval(true)
                            .setSince("4.0.0")
                            .setDescription("Use -is,--install-settings instead.")
                            .get())
                    .build());
            options.addOption(Option.builder(ALTERNATE_GLOBAL_TOOLCHAINS)
                    .longOpt("global-toolchains")
                    .desc("<deprecated> Alternate path for the global toolchains file.")
                    .hasArg()
                    .deprecated(DeprecatedAttributes.builder()
                            .setForRemoval(true)
                            .setSince("4.0.0")
                            .setDescription("Use -it,--install-toolchains instead.")
                            .get())
                    .build());
        }

        public CommandLine parse(String[] args) throws ParseException {
            // We need to eat any quotes surrounding arguments...
            String[] cleanArgs = CleanArgument.cleanArgs(args);
            DefaultParser parser = DefaultParser.builder()
                    .setDeprecatedHandler(usedDeprecatedOptions::add)
                    .build();
            CommandLine commandLine = parser.parse(options, cleanArgs);
            // to trigger deprecation handler, so we can report deprecation BEFORE we actually use options
            options.getOptions().forEach(commandLine::hasOption);
            return commandLine;
        }

        public Set<Option> getUsedDeprecatedOptions() {
            return usedDeprecatedOptions;
        }

        public void displayHelp(String command, PrintWriter pw) {
            HelpFormatter formatter = new HelpFormatter();

            int width = MessageUtils.getTerminalWidth();
            if (width <= 0) {
                width = HelpFormatter.DEFAULT_WIDTH;
            }

            pw.println();

            formatter.printHelp(
                    pw,
                    width,
                    command + " [args]",
                    System.lineSeparator() + "Options:",
                    options,
                    HelpFormatter.DEFAULT_LEFT_PAD,
                    HelpFormatter.DEFAULT_DESC_PAD,
                    System.lineSeparator(),
                    false);

            pw.flush();
        }
    }
}
