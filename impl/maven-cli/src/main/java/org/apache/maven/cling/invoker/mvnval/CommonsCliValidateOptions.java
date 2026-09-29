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
package org.apache.maven.cling.invoker.mvnval;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.mvnval.ValidateOptions;
import org.apache.maven.cling.invoker.CommonsCliOptions;

/**
 * Implementation of {@link ValidateOptions} (base + mvnval).
 */
public class CommonsCliValidateOptions extends CommonsCliOptions implements ValidateOptions {

    /**
     * Deliberately does not check {@code --format}. Throwing here marks the whole invocation as
     * unparseable, and the CLI exits 1 before the tool runs, which contradicts the exit code the
     * tool documents for bad usage. {@link ValidateInvoker} checks it instead, which also covers
     * the value only becoming wrong once options are interpolated.
     */
    public static CommonsCliValidateOptions parse(String[] args) throws ParseException {
        CLIManager cliManager = new CLIManager();
        return new CommonsCliValidateOptions(Options.SOURCE_CLI, cliManager, cliManager.parse(args));
    }

    protected CommonsCliValidateOptions(String source, CLIManager cliManager, CommandLine commandLine) {
        super(source, cliManager, commandLine);
    }

    @Override
    @Nonnull
    public Optional<String> format() {
        return Optional.ofNullable(commandLine.getOptionValue(CLIManager.FORMAT));
    }

    @Override
    @Nonnull
    public Optional<String> mode() {
        return Optional.ofNullable(commandLine.getOptionValue(CLIManager.MODE));
    }

    @Override
    @Nonnull
    public Optional<String> localRepository() {
        return Optional.ofNullable(commandLine.getOptionValue(CLIManager.LOCAL_REPOSITORY));
    }

    @Override
    @Nonnull
    public Optional<Boolean> tempLocalRepository() {
        return commandLine.hasOption(CLIManager.TEMP_LOCAL_REPOSITORY) ? Optional.of(true) : Optional.empty();
    }

    @Override
    @Nonnull
    public Optional<List<String>> poms() {
        if (commandLine.getArgList().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(commandLine.getArgList());
    }

    @Override
    public void displayHelp(ParserRequest request, Consumer<String> printStream) {
        super.displayHelp(request, printStream);
        printStream.accept("");
        printStream.accept("Validates the given POM files, or ./pom.xml when none are given. No");
        printStream.accept("build is run: the POMs are read and reported on, never written back.");
        printStream.accept("");
        printStream.accept("--mode raw reads the file and raw models only: it reaches no repository");
        printStream.accept("and works offline, but it cannot see a dependency whose version is");
        printStream.accept("inherited. --mode effective, the default, runs that and then resolves");
        printStream.accept("parents and imported boms so the checks needing them run as well. That");
        printStream.accept("goes to the network, to Central and to any repository the POM itself");
        printStream.accept("declares, and caches what it fetches in the local repository.");
        printStream.accept("");
        printStream.accept("A subproject is read when its parent needs it, and only the POMs named");
        printStream.accept("on the command line are reported on, though a problem inherited from a");
        printStream.accept("parent is reported against the POM that inherited it.");
        printStream.accept("");
        printStream.accept("--local-repository aims resolution at another local repository, and");
        printStream.accept("--temp-local-repository at one created for the run and deleted when it");
        printStream.accept("ends, so that validating a POM leaves the configured one untouched.");
        printStream.accept("Without --local-repository, maven.repo.local is used as elsewhere.");
        printStream.accept("Neither option changes what --mode raw does, since it resolves nothing.");
        printStream.accept("");
        printStream.accept("-o, -s, -ps and -is are refused outside --mode raw: resolution reads the");
        printStream.accept("default settings files and cannot be redirected.");
        printStream.accept("");
        printStream.accept("Exits with 0 when nothing was reported, 1 when anything was rejected,");
        printStream.accept("2 on bad usage, and 4 when only warnings were reported.");
        printStream.accept("");
        printStream.accept("--format json writes one document to standard output, and quietens the");
        printStream.accept("log so that it stays a document. Asking for -e or -X turns the log back");
        printStream.accept("on, and its lines land in front of the document; -V prints the version");
        printStream.accept("banner there whatever the log level.");
        printStream.accept("");
    }

    @Override
    protected CommonsCliValidateOptions copy(
            String source, CommonsCliOptions.CLIManager cliManager, CommandLine commandLine) {
        return new CommonsCliValidateOptions(source, (CLIManager) cliManager, commandLine);
    }

    protected static class CLIManager extends CommonsCliOptions.CLIManager {

        public static final String FORMAT = "format";

        public static final String MODE = "mode";

        public static final String LOCAL_REPOSITORY = "local-repository";

        public static final String TEMP_LOCAL_REPOSITORY = "temp-local-repository";

        @Override
        protected void prepareOptions(org.apache.commons.cli.Options options) {
            super.prepareOptions(options);
            options.addOption(Option.builder()
                    .longOpt(FORMAT)
                    .hasArg()
                    .desc("Output format: text (default) or json.")
                    .get());
            options.addOption(Option.builder()
                    .longOpt(MODE)
                    .hasArg()
                    .desc("How far to validate: effective (default) or raw.")
                    .get());
            options.addOption(Option.builder()
                    .longOpt(LOCAL_REPOSITORY)
                    .hasArg()
                    .desc("Resolve into this local repository instead of the configured one.")
                    .get());
            options.addOption(Option.builder()
                    .longOpt(TEMP_LOCAL_REPOSITORY)
                    .desc("Resolve into a directory created for this run and deleted when it ends.")
                    .get());
        }

        @Override
        protected String commandLineSyntax(String command) {
            // The base syntax offers goals and phases. This tool runs neither.
            return command + " [options] [<pom> ...]";
        }
    }
}
