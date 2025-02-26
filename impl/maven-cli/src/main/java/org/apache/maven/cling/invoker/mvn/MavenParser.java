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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.cling.invoker.BaseParser;

public class MavenParser extends BaseParser {

    @Override
    protected List<Options> parseCliOptions(LocalContext context) {
        ArrayList<Options> result = new ArrayList<>();
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
        // maven.config; if exists
        Path mavenConfig = context.rootDirectory != null ? context.rootDirectory.resolve(".mvn/maven.config") : null;
        if (mavenConfig != null && Files.isRegularFile(mavenConfig)) {
            result.add(parseMavenConfigOptions(mavenConfig));
        }
        return result;
    }

    protected MavenOptions parseMavenCliOptions(List<String> args) {
        try {
            return parseArgs(Options.SOURCE_CLI, args);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse CLI arguments: " + e.getMessage(), e.getCause());
        }
    }

    protected MavenOptions parseMavenAtFileOptions(Path atFile) {
        try (Stream<String> lines = Files.lines(atFile, Charset.defaultCharset())) {
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
        try (Stream<String> lines = Files.lines(configFile, Charset.defaultCharset())) {
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

    @Override
    protected MavenOptions emptyOptions() {
        try {
            return CommonsCliMavenOptions.parse(Options.SOURCE_CLI, new String[0]);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected MavenInvokerRequest getInvokerRequest(LocalContext context) {
        return new MavenInvokerRequest(
                context.parserRequest,
                context.parsingFailed,
                context.cwd,
                context.installationDirectory,
                context.userHomeDirectory,
                context.userProperties,
                context.systemProperties,
                context.topDirectory,
                context.rootDirectory,
                context.extensions,
                (MavenOptions) context.options);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected MavenOptions assembleOptions(List<Options> parsedOptions) {
        return LayeredMavenOptions.layerMavenOptions((List) parsedOptions);
    }
}
