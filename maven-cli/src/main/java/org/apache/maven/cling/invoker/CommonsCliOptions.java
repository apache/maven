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

import java.io.PrintStream;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.maven.api.cli.Options;
import org.apache.maven.cli.CLIManager;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.cling.invoker.Utils.toMap;

public abstract class CommonsCliOptions implements Options {
    protected final CLIManager cliManager;
    protected final CommandLine commandLine;

    public CommonsCliOptions(CLIManager cliManager, CommandLine commandLine) {
        this.cliManager = requireNonNull(cliManager);
        this.commandLine = requireNonNull(commandLine);
    }

    public Collection<Option> getUsedDeprecatedOptions() {
        return cliManager.getUsedDeprecatedOptions();
    }

    @Override
    public Optional<Map<String, String>> userProperties() {
        if (commandLine.hasOption(CLIManager.SET_USER_PROPERTY)) {
            return Optional.of(toMap(commandLine.getOptionProperties(String.valueOf(CLIManager.SET_USER_PROPERTY))));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> showVersionAndExit() {
        if (commandLine.hasOption(CLIManager.VERSION)) {
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
        if (commandLine.hasOption(CLIManager.ERRORS)) {
            return Optional.of(Boolean.TRUE);
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
    public void displayHelp(PrintStream printStream) {
        cliManager.displayHelp(printStream);
    }
}
