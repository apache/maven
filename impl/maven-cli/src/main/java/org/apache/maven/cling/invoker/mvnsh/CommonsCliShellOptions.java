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
package org.apache.maven.cling.invoker.mvnsh;

import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.mvnsh.ShellOptions;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.InterpolatorException;
import org.apache.maven.cling.invoker.CommonsCliOptions;

import static org.apache.maven.cling.invoker.CliUtils.createInterpolator;

/**
 * Implementation of {@link ShellOptions} (base + shell).
 */
public class CommonsCliShellOptions extends CommonsCliOptions implements ShellOptions {
    public static CommonsCliShellOptions parse(String[] args) throws ParseException {
        CLIManager cliManager = new CLIManager();
        return new CommonsCliShellOptions(Options.SOURCE_CLI, cliManager, cliManager.parse(args));
    }

    protected CommonsCliShellOptions(String source, CLIManager cliManager, CommandLine commandLine) {
        super(source, cliManager, commandLine);
    }

    private static CommonsCliShellOptions interpolate(CommonsCliShellOptions options, UnaryOperator<String> callback) {
        try {
            // now that we have properties, interpolate all arguments
            Interpolator interpolator = createInterpolator();
            CommandLine.Builder commandLineBuilder = new CommandLine.Builder();
            commandLineBuilder.setDeprecatedHandler(o -> {});
            for (Option option : options.commandLine.getOptions()) {
                if (!CLIManager.USER_PROPERTY.equals(option.getOpt())) {
                    List<String> values = option.getValuesList();
                    for (ListIterator<String> it = values.listIterator(); it.hasNext(); ) {
                        it.set(interpolator.interpolate(it.next(), callback));
                    }
                }
                commandLineBuilder.addOption(option);
            }
            for (String arg : options.commandLine.getArgList()) {
                commandLineBuilder.addArg(interpolator.interpolate(arg, callback));
            }
            return new CommonsCliShellOptions(
                    options.source, (CLIManager) options.cliManager, commandLineBuilder.build());
        } catch (InterpolatorException e) {
            throw new IllegalArgumentException("Could not interpolate CommonsCliOptions", e);
        }
    }

    @Override
    public ShellOptions interpolate(UnaryOperator<String> callback) {
        return interpolate(this, callback);
    }

    protected static class CLIManager extends CommonsCliOptions.CLIManager {
        @Override
        protected String commandLineSyntax(String command) {
            return command + " [options]";
        }
    }
}
