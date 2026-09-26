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

    public static CommonsCliValidateOptions parse(String[] args) throws ParseException {
        CLIManager cliManager = new CLIManager();
        CommonsCliValidateOptions options =
                new CommonsCliValidateOptions(Options.SOURCE_CLI, cliManager, cliManager.parse(args));
        // Reject a bad --format here, so the user gets a usage error rather than a failure
        // after the container has been built.
        try {
            options.format().ifPresent(OutputFormat::parse);
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage());
        }
        return options;
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
        printStream.accept("Validates the given POM files, or ./pom.xml when none are given.");
        printStream.accept("Parents are not resolved and no build is run, so the verdict depends");
        printStream.accept("on the files alone. Subprojects are not validated unless named.");
        printStream.accept("");
        printStream.accept("Exits with 0 when nothing was reported, 1 when anything was rejected,");
        printStream.accept("2 on bad usage, and 3 when only warnings were reported.");
        printStream.accept("");
    }

    @Override
    protected CommonsCliValidateOptions copy(
            String source, CommonsCliOptions.CLIManager cliManager, CommandLine commandLine) {
        return new CommonsCliValidateOptions(source, (CLIManager) cliManager, commandLine);
    }

    protected static class CLIManager extends CommonsCliOptions.CLIManager {

        public static final String FORMAT = "format";

        @Override
        protected void prepareOptions(org.apache.commons.cli.Options options) {
            super.prepareOptions(options);
            options.addOption(Option.builder()
                    .longOpt(FORMAT)
                    .hasArg()
                    .desc("Output format: text (default) or json.")
                    .get());
        }

        @Override
        protected String commandLineSyntax(String command) {
            // The base syntax offers goals and phases. This tool runs neither.
            return command + " [options] [<pom> ...]";
        }
    }
}
