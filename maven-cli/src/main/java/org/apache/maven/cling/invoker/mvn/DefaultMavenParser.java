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
import java.util.Map;
import java.util.stream.Stream;

import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.ParserException;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.cli.extensions.CoreExtension;
import org.apache.maven.api.cli.mvn.MavenInvokerRequest;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.cli.mvn.MavenParser;
import org.apache.maven.cling.invoker.BaseParser;

public abstract class DefaultMavenParser<O extends MavenOptions, R extends MavenInvokerRequest<O>>
        extends BaseParser<O, R> implements MavenParser<R> {
    @SuppressWarnings("ParameterNumber")
    @Override
    protected abstract R getInvokerRequest(
            ParserRequest parserRequest,
            Path cwd,
            Path installationDirectory,
            Path userHomeDirectory,
            Map<String, String> userProperties,
            Map<String, String> systemProperties,
            Path topDirectory,
            Path rootDirectory,
            ArrayList<CoreExtension> extensions,
            Options options);

    @Override
    protected List<O> parseCliOptions(Path rootDirectory, String[] args) throws ParserException, IOException {
        ArrayList<O> result = new ArrayList<>();
        // CLI args
        result.add(parseMavenCliOptions(args));
        // maven.config; if exists
        Path mavenConfig = rootDirectory.resolve(".mvn/maven.config");
        if (Files.isRegularFile(mavenConfig)) {
            result.add(parseMavenConfigOptions(mavenConfig));
        }
        return result;
    }

    protected O parseMavenCliOptions(String[] args) throws ParserException {
        return parseArgs(Options.SOURCE_CLI, args);
    }

    protected O parseMavenConfigOptions(Path configFile) throws ParserException, IOException {
        try (Stream<String> lines = Files.lines(configFile, Charset.defaultCharset())) {
            String[] args =
                    lines.filter(arg -> !arg.isEmpty() && !arg.startsWith("#")).toArray(String[]::new);
            O options = parseArgs("maven.config", args);
            if (options.goals().isPresent()) {
                // This file can only contain options, not args (goals or phases)
                throw new ParserException("Unrecognized maven.config file entries: "
                        + options.goals().get());
            }
            return options;
        }
    }

    protected abstract O parseArgs(String source, String[] args) throws ParserException;
}
