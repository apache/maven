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
package org.apache.maven.cling.invoker.mvnenc;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.mvnenc.EncryptOptions;
import org.apache.maven.cling.invoker.CommonsCliOptions;
import org.codehaus.plexus.interpolation.BasicInterpolator;
import org.codehaus.plexus.interpolation.InterpolationException;

import static org.apache.maven.cling.invoker.Utils.createInterpolator;

public class CommonsCliEncryptOptions extends CommonsCliOptions implements EncryptOptions {
    public static CommonsCliEncryptOptions parse(String[] args) throws ParseException {
        CLIManager cliManager = new CLIManager();
        return new CommonsCliEncryptOptions(Options.SOURCE_CLI, cliManager, cliManager.parse(args));
    }

    protected CommonsCliEncryptOptions(String source, CLIManager cliManager, CommandLine commandLine) {
        super(source, cliManager, commandLine);
    }

    private static CommonsCliEncryptOptions interpolate(
            CommonsCliEncryptOptions options, Collection<Map<String, String>> properties) {
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
            return new CommonsCliEncryptOptions(
                    options.source, (CLIManager) options.cliManager, commandLineBuilder.build());
        } catch (InterpolationException e) {
            throw new IllegalArgumentException("Could not interpolate CommonsCliOptions", e);
        }
    }

    @Override
    public Optional<String> cipher() {
        if (commandLine.hasOption(CLIManager.CIPHER)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.CIPHER));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> masterSource() {
        if (commandLine.hasOption(CLIManager.MASTER_SOURCE)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.MASTER_SOURCE));
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> dispatcher() {
        if (commandLine.hasOption(CLIManager.DISPATCHER)) {
            return Optional.of(commandLine.getOptionValue(CLIManager.DISPATCHER));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> force() {
        if (commandLine.hasOption(CLIManager.FORCE)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Boolean> yes() {
        if (commandLine.hasOption(CLIManager.YES)) {
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
    public EncryptOptions interpolate(Collection<Map<String, String>> properties) {
        return interpolate(this, properties);
    }

    protected static class CLIManager extends CommonsCliOptions.CLIManager {
        public static final String CIPHER = "c";
        public static final String MASTER_SOURCE = "m";
        public static final String DISPATCHER = "d";
        public static final String FORCE = "f";
        public static final String YES = "y";

        @Override
        protected void prepareOptions(org.apache.commons.cli.Options options) {
            super.prepareOptions(options);
            options.addOption(Option.builder(CIPHER)
                    .longOpt("cipher")
                    .desc("The cipher that user wants to use for non-dispatched encryption")
                    .build());
            options.addOption(Option.builder(MASTER_SOURCE)
                    .longOpt("master-source")
                    .desc("The master source that user wants to use for non-dispatched encryption")
                    .build());
            options.addOption(Option.builder(DISPATCHER)
                    .longOpt("dispatcher")
                    .desc("The dispatcher to use for dispatched encryption")
                    .build());
            options.addOption(Option.builder(FORCE)
                    .longOpt("force")
                    .desc("Should overwrite without asking any configuration, if exist.")
                    .build());
            options.addOption(Option.builder(YES)
                    .longOpt("yes")
                    .desc("Should imply \"yes\" answer to all questions")
                    .build());
        }
    }
}
