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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.cli.ParseException;
import org.apache.maven.api.cli.Options;
import org.apache.maven.api.cli.mvn.MavenOptions;
import org.apache.maven.api.reactor.Alias;
import org.apache.maven.api.reactor.ReactorConfig;
import org.apache.maven.cling.invoker.BaseParser;

public class MavenParser extends BaseParser {
    @Override
    protected Options parseCliOptions(LocalContext context) {
        ArrayList<MavenOptions> result = new ArrayList<>();

        // Expand alias from reactor.xml before parsing, if applicable
        List<String> effectiveArgs = expandAlias(context.parserRequest.args(), context.reactorConfig);

        // CLI args (after alias expansion)
        MavenOptions cliOptions = parseMavenCliOptions(effectiveArgs);
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

        // reactor.xml <options> — used instead of maven.config when reactor.xml is present
        if (context.reactorConfig != null) {
            List<String> optionArgs = resolveReactorOptions(context.reactorConfig);
            if (!optionArgs.isEmpty()) {
                result.add(parseMavenReactorOptions(optionArgs));
            }
        } else {
            // legacy maven.config; if exists
            Path mavenConfig =
                    context.rootDirectory != null ? context.rootDirectory.resolve(".mvn/maven.config") : null;
            if (mavenConfig != null && Files.isRegularFile(mavenConfig)) {
                result.add(parseMavenConfigOptions(mavenConfig));
            }
        }

        return LayeredMavenOptions.layerMavenOptions(result);
    }

    /**
     * Resolves the {@code <options>} or {@code <optionArgs>} block of a {@link ReactorConfig}
     * to a flat list of argument strings.
     *
     * <p>When the config has structured {@code <arg>} children ({@code optionArgs}), those are used as-is.
     * When it has inline text content ({@code options}), it is tokenized with {@link ArgumentTokenizer}.
     */
    private static List<String> resolveReactorOptions(ReactorConfig rc) {
        List<String> optionArgs = rc.getOptionArgs();
        if (optionArgs != null && !optionArgs.isEmpty()) {
            return optionArgs;
        }
        String options = rc.getOptions();
        if (options != null && !options.isBlank()) {
            return ArgumentTokenizer.tokenize(options);
        }
        return List.of();
    }

    /**
     * Expands alias arguments in {@code args} using aliases defined in {@code reactorConfig}.
     *
     * <p>Alias expansion is a pure string manipulation — it happens before any Maven DI or session setup.
     * The argument list is rebuilt on the fly: each argument is checked against the alias map in order;
     * if it matches an alias name, it is replaced by the alias expansion tokens; otherwise it is kept as-is.
     * Expansion is not recursive — tokens produced by an alias are passed through as-is.
     *
     * @param args          original argument list
     * @param reactorConfig parsed reactor config, or {@code null} if reactor.xml is absent
     * @return the (possibly modified) argument list
     */
    static List<String> expandAlias(List<String> args, ReactorConfig reactorConfig) {
        if (reactorConfig == null
                || reactorConfig.getAliases() == null
                || reactorConfig.getAliases().isEmpty()) {
            return args;
        }

        // Build a name → expansion map for O(1) lookup per arg
        Map<String, List<String>> aliasMap = new LinkedHashMap<>();
        for (Alias alias : reactorConfig.getAliases()) {
            String name = alias.getName();
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Alias name must not be blank in reactor.xml");
            }
            aliasMap.put(name, resolveAlias(alias));
        }

        // Rebuild the arg list, expanding every arg that matches an alias
        List<String> expanded = null; // allocated lazily — avoids a copy when nothing matches
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            List<String> expansion = aliasMap.get(arg);
            if (expansion != null) {
                if (expanded == null) {
                    expanded = new ArrayList<>(args.subList(0, i));
                }
                expanded.addAll(expansion);
            } else if (expanded != null) {
                expanded.add(arg);
            }
        }

        return expanded != null ? List.copyOf(expanded) : args;
    }

    private static List<String> resolveAlias(Alias alias) {
        List<String> args = alias.getArgs();
        if (args != null && !args.isEmpty()) {
            return args;
        }
        String content = alias.getContent();
        if (content != null && !content.isBlank()) {
            return ArgumentTokenizer.tokenize(content);
        }
        return List.of();
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

    protected MavenOptions parseMavenReactorOptions(List<String> args) {
        try {
            MavenOptions options = parseArgs("reactor.xml", args);
            if (options.goals().isPresent()) {
                // <options> can only contain options, not goals/phases
                throw new IllegalArgumentException("Unrecognized entries in reactor.xml <options>: "
                        + options.goals().get());
            }
            return options;
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Failed to parse arguments from reactor.xml <options>: " + e.getMessage(), e);
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
