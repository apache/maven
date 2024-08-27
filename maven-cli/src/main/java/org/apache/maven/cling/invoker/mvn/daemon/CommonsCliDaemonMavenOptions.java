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
package org.apache.maven.cling.invoker.mvn.daemon;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.mvn.daemon.DaemonMavenOptions;
import org.apache.maven.cling.invoker.mvn.CommonsCliMavenOptions;
import org.codehaus.plexus.interpolation.BasicInterpolator;
import org.codehaus.plexus.interpolation.InterpolationException;

import static org.apache.maven.cling.invoker.Utils.createInterpolator;

public class CommonsCliDaemonMavenOptions extends CommonsCliMavenOptions implements DaemonMavenOptions {
    public static CommonsCliDaemonMavenOptions parse(String source, String[] args) throws ParseException {
        CLIManager cliManager = new CLIManager();
        return new CommonsCliDaemonMavenOptions(source, cliManager, cliManager.parse(args));
    }

    protected CommonsCliDaemonMavenOptions(String source, CLIManager cliManager, CommandLine commandLine) {
        super(source, cliManager, commandLine);
    }

    @Override
    public Optional<Boolean> rawStreams() {
        if (commandLine.hasOption(CLIManager.RAW_STREAMS)) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    private static CommonsCliDaemonMavenOptions interpolate(
            CommonsCliDaemonMavenOptions options, Collection<Map<String, String>> properties) {
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
            return new CommonsCliDaemonMavenOptions(
                    options.source, (CLIManager) options.cliManager, commandLineBuilder.build());
        } catch (InterpolationException e) {
            throw new IllegalArgumentException("Could not interpolate CommonsCliOptions", e);
        }
    }

    @Override
    public DaemonMavenOptions interpolate(Collection<Map<String, String>> properties) {
        return interpolate(this, properties);
    }

    protected static class CLIManager extends CommonsCliMavenOptions.CLIManager {
        public static final String RAW_STREAMS = "ras";

        @Override
        protected void prepareOptions(Options options) {
            super.prepareOptions(options);
            options.addOption(Option.builder(RAW_STREAMS)
                    .longOpt("raw-streams")
                    .hasArg()
                    .desc("Use raw-streams for daemon communication")
                    .build());
        }
    }
}
