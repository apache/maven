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
package org.apache.maven.cling.invoker.mvnlog;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.mvnlog.LogOptions;
import org.apache.maven.cling.invoker.CommonsCliOptions;

/**
 * Implementation of {@link LogOptions} using Commons CLI.
 */
public class CommonsCliLogOptions extends CommonsCliOptions implements LogOptions {

    public static CommonsCliLogOptions parse(String[] args) throws ParseException {
        CLIManager cliManager = new CLIManager();
        return new CommonsCliLogOptions(Options.SOURCE_CLI, cliManager, cliManager.parse(args));
    }

    protected CommonsCliLogOptions(String source, CLIManager cliManager, CommandLine commandLine) {
        super(source, cliManager, commandLine);
    }

    @Override
    public Optional<Boolean> diagnostics() {
        if (commandLine.hasOption(CLIManager.DIAGNOSTICS)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> failures() {
        if (commandLine.hasOption(CLIManager.FAILURES)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> full() {
        if (commandLine.hasOption(CLIManager.FULL)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> list() {
        if (commandLine.hasOption(CLIManager.LIST)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> json() {
        if (commandLine.hasOption(CLIManager.JSON)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> reportFile() {
        List<String> args = commandLine.getArgList();
        if (!args.isEmpty()) {
            return Optional.of(args.get(0));
        }
        return Optional.empty();
    }

    @Override
    public void displayHelp(ParserRequest request, Consumer<String> printStream) {
        super.displayHelp(request, printStream);
        printStream.accept("");
        printStream.accept("Usage: mvnlog [options] [report-file]");
        printStream.accept("");
        printStream.accept("Displays a formatted summary of the last Maven build report.");
        printStream.accept("If no report-file is specified, reads target/build-reports/build-report-latest.json.");
        printStream.accept("");
        printStream.accept("Use --json to output the raw JSON report (e.g. mvnlog --json | jq '.modules').");
        printStream.accept("");
    }

    @Override
    protected CommonsCliLogOptions copy(
            String source, CommonsCliOptions.CLIManager cliManager, CommandLine commandLine) {
        return new CommonsCliLogOptions(source, (CLIManager) cliManager, commandLine);
    }

    protected static class CLIManager extends CommonsCliOptions.CLIManager {
        public static final String DIAGNOSTICS = "d";
        public static final String FAILURES = "f";
        public static final String FULL = "F";
        public static final String LIST = "L";
        public static final String JSON = "j";

        @Override
        protected void prepareOptions(org.apache.commons.cli.Options options) {
            super.prepareOptions(options);
            options.addOption(Option.builder(DIAGNOSTICS)
                    .longOpt("diagnostics")
                    .desc("Show detailed warnings and errors from the build")
                    .get());
            options.addOption(Option.builder(FAILURES)
                    .longOpt("failures")
                    .desc("Show detailed failure information including stack traces")
                    .get());
            options.addOption(Option.builder(FULL)
                    .longOpt("full")
                    .desc("Show full per-mojo timing breakdown")
                    .get());
            options.addOption(Option.builder(LIST)
                    .longOpt("list")
                    .desc("List all available build reports")
                    .get());
            options.addOption(Option.builder(JSON)
                    .longOpt("json")
                    .desc("Output the raw JSON build report (useful for piping to jq)")
                    .get());
        }
    }
}
