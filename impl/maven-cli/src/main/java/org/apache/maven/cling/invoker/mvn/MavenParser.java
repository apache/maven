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
package org.apache.maven.cling.invoker.mvn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.cling.invoker.BaseParser;

public class MavenParser extends BaseParser {
    @Override
    protected Options parseCliOptions(LocalContext context) {
        ArrayList<MavenOptions> result = new ArrayList<>();
        // CLI args
        MavenOptions cliOptions = parseMavenCliOptions(context.parserRequest.args());
        result.add(cliOptions);
        // atFile option
        if (cliOptions.atFile().isPresent()) {
            Path file = context.cwd.resolve(cliOptions.atFile().orElseThrow());
            if (Files.isRegularFile(file)) {
                result.add(parseMavenAtFileOptions(file));
            } else {
                throw new IllegalArgumentException("Specified file does not exists (" + file + ")");
            }
        }

        // Project settings.xml <args>/<options> supersede maven.config when present
        java.util.Map<String, String> eff = new java.util.HashMap<>(context.systemProperties);
        if (context.userProperties != null) {
            eff.putAll(context.userProperties);
        }
        Path projectSettingsFile = resolveProjectSettingsFile(context, eff);
        Settings projectSettings = readSettingsEarly(projectSettingsFile, context);
        boolean settingsHasArgs = projectSettings != null
                && (!isNullOrEmpty(projectSettings.getOptions())
                        || (projectSettings.getArgs() != null
                                && !projectSettings.getArgs().isEmpty()));
        if (settingsHasArgs) {
            result.add(parseMavenSettingsOptions(projectSettings, projectSettingsFile));
        } else {
            // legacy fallback: .mvn/maven.config
            Path mavenConfig =
                    context.rootDirectory != null ? context.rootDirectory.resolve(".mvn/maven.config") : null;
            if (mavenConfig != null && Files.isRegularFile(mavenConfig)) {
                result.add(parseMavenConfigOptions(mavenConfig));
            }
        }
        return LayeredMavenOptions.layerMavenOptions(result);
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isBlank();
    }

    protected MavenOptions parseMavenSettingsOptions(Settings settings, Path settingsFile) {
        List<String> args;
        if (settings.getArgs() != null && !settings.getArgs().isEmpty()) {
            args = settings.getArgs();
        } else {
            // tokenize inline options text (shell-style, quote-aware)
            args = ArgumentTokenizer.tokenize(settings.getOptions());
        }
        try {
            MavenOptions options = parseArgs("settings.xml", args);
            if (options.goals().isPresent()) {
                throw new IllegalArgumentException("Unrecognized entries in settings.xml (" + settingsFile
                        + ") <args>/<options>: " + options.goals().get());
            }
            return options;
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Failed to parse arguments from settings.xml (" + settingsFile + "): " + e.getMessage(),
                    e.getCause());
        }
    }

    protected MavenOptions parseMavenCliOptions(List<String> args) {
        try {
            return parseArgs(Options.SOURCE_CLI, args);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse CLI arguments: " + e.getMessage(), e.getCause());
        }
    }

    protected MavenOptions parseMavenAtFileOptions(Path atFile) {
        try (Stream<String> lines = Files.lines(atFile, StandardCharsets.UTF_8)) {
            List<String> args =
                    lines.filter(arg -> !arg.isEmpty() && !arg.startsWith("#")).toList();
            return parseArgs("atFile", args);
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Failed to parse arguments from file (" + atFile + "): " + e.getMessage(), e.getCause());
        } catch (IOException e) {
            throw new IllegalStateException("Error reading config file: " + atFile, e);
        }
    }

    protected MavenOptions parseMavenConfigOptions(Path configFile) {
        try (Stream<String> lines = Files.lines(configFile, StandardCharsets.UTF_8)) {
            List<String> args =
                    lines.filter(arg -> !arg.isEmpty() && !arg.startsWith("#")).toList();
            MavenOptions options = parseArgs("maven.config", args);
            if (options.goals().isPresent()) {
                // This file can only contain options, not args (goals or phases)
                throw new IllegalArgumentException("Unrecognized entries in maven.config (" + configFile + ") file: "
                        + options.goals().get());
            }
            return options;
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Failed to parse arguments from maven.config file (" + configFile + "): " + e.getMessage(),
                    e.getCause());
        } catch (IOException e) {
            throw new IllegalStateException("Error reading config file: " + configFile, e);
        }
    }

    protected MavenOptions parseArgs(String source, List<String> args) throws ParseException {
        return CommonsCliMavenOptions.parse(source, args.toArray(new String[0]));
    }
}
