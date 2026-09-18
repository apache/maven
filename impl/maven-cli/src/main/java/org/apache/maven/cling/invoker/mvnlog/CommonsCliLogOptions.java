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

import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.mvnlog.LogOptions;
import org.apache.maven.cling.invoker.CommonsCliOptions;

public class CommonsCliLogOptions extends CommonsCliOptions implements LogOptions {

    public static CommonsCliLogOptions parse(String[] args) throws ParseException {
        CLIManager cliManager = new CLIManager();
        return new CommonsCliLogOptions(Options.SOURCE_CLI, cliManager, cliManager.parse(args));
    }

    protected CommonsCliLogOptions(String source, CLIManager cliManager, CommandLine commandLine) {
        super(source, cliManager, commandLine);
    }

    @Override
    @Nonnull
    public Optional<Boolean> web() {
        if (commandLine.hasOption(CLIManager.WEB)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    @Nonnull
    public Optional<Integer> port() {
        if (commandLine.hasOption(CLIManager.PORT)) {
            try {
                return Optional.of(Integer.parseInt(commandLine.getOptionValue(CLIManager.PORT)));
            } catch (NumberFormatException e) {
                // Ignore, will default
            }
        }
        return Optional.empty();
    }

    @Override
    @Nonnull
    public Optional<String> file() {
        if (commandLine.hasOption(CLIManager.FILE)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.FILE));
        }
        // Allow fallback to unflagged first argument
        if (!commandLine.getArgList().isEmpty()) {
            return Optional.of(commandLine.getArgList().get(0));
        }
        return Optional.empty();
    }

    @Override
    public void displayHelp(ParserRequest request, Consumer<String> printStream) {
        super.displayHelp(request, printStream);
        printStream.accept("");
        printStream.accept("Options:");
        printStream.accept("  -w, --web             Start local HTTP server to view build reports interactively");
        printStream.accept("  -p, --port <port>     Set custom HTTP server port (only valid with --web)");
        printStream.accept("  -f, --file <file>     Path to specific build-report.json file to view");
        printStream.accept("  -h, --help            Display this help message");
        printStream.accept("");
    }

    @Override
    protected CommonsCliLogOptions copy(String source, CommonsCliOptions.CLIManager cliManager, CommandLine commandLine) {
        return new CommonsCliLogOptions(source, (CLIManager) cliManager, commandLine);
    }

    protected static class CLIManager extends CommonsCliOptions.CLIManager {
        public static final String WEB = "w";
        public static final String PORT = "p";
        public static final String FILE = "f";

        @Override
        protected void prepareOptions(org.apache.commons.cli.Options options) {
            super.prepareOptions(options);
            options.addOption(Option.builder(WEB)
                    .longOpt("web")
                    .desc("Start local HTTP server for interactive build report viewer")
                    .build());
            options.addOption(Option.builder(PORT)
                    .longOpt("port")
                    .hasArg()
                    .desc("Custom port for the local HTTP server")
                    .build());
            options.addOption(Option.builder(FILE)
                    .longOpt("file")
                    .hasArg()
                    .desc("Specific build-report.json file path")
                    .build());
        }
    }
}
