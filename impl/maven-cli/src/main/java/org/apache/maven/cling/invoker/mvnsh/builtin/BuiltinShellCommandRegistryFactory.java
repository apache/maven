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
package org.apache.maven.cling.invoker.mvnsh.builtin;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.mvn.MavenInvoker;
import org.apache.maven.cling.invoker.mvn.MavenParser;
import org.apache.maven.cling.invoker.mvnenc.EncryptInvoker;
import org.apache.maven.cling.invoker.mvnenc.EncryptParser;
import org.apache.maven.cling.invoker.mvnenc.Goal;
import org.apache.maven.cling.invoker.mvnsh.ShellCommandRegistryFactory;
import org.apache.maven.cling.invoker.mvnsh.ShellContext;
import org.apache.maven.cling.invoker.mvnsh.WorkingDirectory;
import org.apache.maven.impl.util.Os;
import org.jline.builtins.Completers;
import org.jline.console.CmdDesc;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.console.impl.JlineCommandRegistry;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

import static java.util.Objects.requireNonNull;

@Named("builtin")
@Singleton
public class BuiltinShellCommandRegistryFactory implements ShellCommandRegistryFactory {
    public CommandRegistry createShellCommandRegistry(ShellContext context) {
        return new BuiltinShellCommandRegistry(context);
    }

    private static class BuiltinShellCommandRegistry extends JlineCommandRegistry implements AutoCloseable {
        private final ShellContext shellContext;
        private final WorkingDirectory workingDirectory;
        private final MavenInvoker shellMavenInvoker;
        private final MavenParser mavenParser;
        private final EncryptInvoker shellEncryptInvoker;
        private final EncryptParser encryptParser;

        private BuiltinShellCommandRegistry(ShellContext shellContext) {
            this.shellContext = requireNonNull(shellContext, "shellContext");
            this.workingDirectory = requireNonNull(shellContext.workingDirectory, "workingDirectory");
            this.shellMavenInvoker = new MavenInvoker(shellContext.invokerRequest.lookup(), contextCopier());
            this.mavenParser = new MavenParser();
            this.shellEncryptInvoker = new EncryptInvoker(shellContext.invokerRequest.lookup(), contextCopier());
            this.encryptParser = new EncryptParser();
            Map<String, CommandMethods> commandExecute = new HashMap<>();
            commandExecute.put("!", new CommandMethods(this::shell, this::defaultCompleter));
            commandExecute.put("cd", new CommandMethods(this::cd, this::cdCompleter));
            commandExecute.put("ls", new CommandMethods(this::ls, this::defaultCompleter));
            commandExecute.put("pwd", new CommandMethods(this::pwd, this::defaultCompleter));
            commandExecute.put("mvn", new CommandMethods(this::mvn, this::mvnCompleter));
            commandExecute.put("mvnenc", new CommandMethods(this::mvnenc, this::mvnencCompleter));
            registerCommands(commandExecute);
        }

        private Consumer<LookupContext> contextCopier() {
            return result -> {
                result.logger = shellContext.logger;
                result.loggerFactory = shellContext.loggerFactory;
                result.slf4jConfiguration = shellContext.slf4jConfiguration;
                result.loggerLevel = shellContext.loggerLevel;
                result.coloredOutput = shellContext.coloredOutput;
                result.terminal = shellContext.terminal;
                result.writer = shellContext.writer;

                result.installationSettingsPath = shellContext.installationSettingsPath;
                result.projectSettingsPath = shellContext.projectSettingsPath;
                result.userSettingsPath = shellContext.userSettingsPath;
                result.interactive = shellContext.interactive;
                result.localRepositoryPath = shellContext.localRepositoryPath;
                result.effectiveSettings = shellContext.effectiveSettings;

                result.containerCapsule = shellContext.containerCapsule;
                result.lookup = shellContext.lookup;
                result.eventSpyDispatcher = shellContext.eventSpyDispatcher;
            };
        }

        @Override
        public void close() throws Exception {
            shellMavenInvoker.close();
            shellEncryptInvoker.close();
        }

        @Override
        public List<String> commandInfo(String command) {
            return List.of();
        }

        @Override
        public CmdDesc commandDescription(List<String> args) {
            return null;
        }

        @Override
        public String name() {
            return "Builtin Maven Shell commands";
        }

        private void executeCmnd(List<String> args) throws Exception {
            ProcessBuilder builder = new ProcessBuilder();
            List<String> processArgs = new ArrayList<>();
            if (Os.IS_WINDOWS) {
                processArgs.add("cmd.exe");
                processArgs.add("/c");
            } else {
                processArgs.add("sh");
                processArgs.add("-c");
            }
            processArgs.add(String.join(" ", args));
            builder.command(processArgs);
            builder.directory(workingDirectory.get().toFile());
            Process process = builder.start();
            StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
            Thread th = new Thread(streamGobbler);
            th.start();
            int exitCode = process.waitFor();
            th.join();
            if (exitCode != 0) {
                streamGobbler = new StreamGobbler(process.getErrorStream(), System.out::println);
                th = new Thread(streamGobbler);
                th.start();
                th.join();
                throw new Exception("Error occurred in shell!");
            }
        }

        private void shell(CommandInput input) {
            List<String> argv = new ArrayList<>(Arrays.asList(input.args()));
            if (!argv.isEmpty()) {
                try {
                    executeCmnd(argv);
                } catch (Exception e) {
                    saveException(e);
                }
            }
        }

        private void cd(CommandInput input) {
            try {
                if (input.args().length == 1) {
                    workingDirectory.changeDirectory(input.args()[0]);
                } else {
                    shellContext.writer.accept("Error: 'cd' accepts only one argument");
                }
            } catch (Exception e) {
                saveException(e);
            }
        }

        private List<Completer> cdCompleter(String name) {
            return List.of(new ArgumentCompleter(new Completers.DirectoriesCompleter(workingDirectory)));
        }

        private void ls(CommandInput input) {
            try {
                try (Stream<Path> list = Files.list(workingDirectory.get())) {
                    list.forEach(file -> {
                        if (Files.isDirectory(file)) {
                            shellContext.writer.accept(file.getFileName().toString() + "/");
                        } else {
                            shellContext.writer.accept(file.getFileName().toString());
                        }
                    });
                }
            } catch (Exception e) {
                saveException(e);
            }
        }

        private void pwd(CommandInput input) {
            try {
                shellContext.writer.accept(workingDirectory.get().toString());
            } catch (Exception e) {
                saveException(e);
            }
        }

        private void mvn(CommandInput input) {
            try {
                shellMavenInvoker.invoke(mavenParser.parseInvocation(
                        ParserRequest.mvn(input.args(), shellContext.invokerRequest.messageBuilderFactory())
                                .cwd(workingDirectory.get())
                                .build()));
            } catch (Exception e) {
                saveException(e);
            }
        }

        private List<Completer> mvnCompleter(String name) {
            return List.of(new ArgumentCompleter(new StringsCompleter(
                    "clean",
                    "validate",
                    "compile",
                    "test",
                    "package",
                    "verify",
                    "install",
                    "deploy",
                    "wrapper:wrapper")));
        }

        private void mvnenc(CommandInput input) {
            try {
                shellEncryptInvoker.invoke(encryptParser.parseInvocation(
                        ParserRequest.mvnenc(input.args(), shellContext.invokerRequest.messageBuilderFactory())
                                .cwd(workingDirectory.get())
                                .build()));
            } catch (Exception e) {
                saveException(e);
            }
        }

        private List<Completer> mvnencCompleter(String name) {
            return List.of(new ArgumentCompleter(new StringsCompleter(
                    shellContext.lookup.lookupMap(Goal.class).keySet())));
        }
    }

    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

        private StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .forEach(consumer);
        }
    }
}
