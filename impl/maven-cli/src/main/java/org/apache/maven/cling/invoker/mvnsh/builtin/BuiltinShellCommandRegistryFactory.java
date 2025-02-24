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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.LifecycleRegistry;
import org.apache.maven.api.services.LookupException;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.mvn.MavenInvoker;
import org.apache.maven.cling.invoker.mvn.MavenParser;
import org.apache.maven.cling.invoker.mvnenc.EncryptInvoker;
import org.apache.maven.cling.invoker.mvnenc.EncryptParser;
import org.apache.maven.cling.invoker.mvnenc.Goal;
import org.apache.maven.cling.invoker.mvnsh.ShellCommandRegistryFactory;
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
    public CommandRegistry createShellCommandRegistry(LookupContext context) {
        return new BuiltinShellCommandRegistry(context);
    }

    private static class BuiltinShellCommandRegistry extends JlineCommandRegistry implements AutoCloseable {
        private final LookupContext shellContext;
        private final MavenInvoker shellMavenInvoker;
        private final MavenParser mavenParser;
        private final EncryptInvoker shellEncryptInvoker;
        private final EncryptParser encryptParser;

        private BuiltinShellCommandRegistry(LookupContext shellContext) {
            this.shellContext = requireNonNull(shellContext, "shellContext");
            this.shellMavenInvoker = new MavenInvoker(shellContext.invokerRequest.lookup(), contextCopier());
            this.mavenParser = new MavenParser();
            this.shellEncryptInvoker = new EncryptInvoker(shellContext.invokerRequest.lookup(), contextCopier());
            this.encryptParser = new EncryptParser();
            Map<String, CommandMethods> commandExecute = new HashMap<>();
            commandExecute.put("!", new CommandMethods(this::shell, this::defaultCompleter));
            commandExecute.put("cd", new CommandMethods(this::cd, this::cdCompleter));
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

        private void shell(CommandInput input) {
            if (input.args().length > 0) {
                try {
                    ProcessBuilder builder = new ProcessBuilder();
                    List<String> processArgs = new ArrayList<>();
                    if (Os.IS_WINDOWS) {
                        processArgs.add("cmd.exe");
                        processArgs.add("/c");
                    } else {
                        processArgs.add("sh");
                        processArgs.add("-c");
                    }
                    processArgs.add(String.join(" ", Arrays.asList(input.args())));
                    builder.command(processArgs);
                    builder.directory(shellContext.cwd.get().toFile());
                    Process process = builder.start();
                    Thread out = new Thread(new StreamGobbler(process.getInputStream(), shellContext.writer));
                    Thread err = new Thread(new StreamGobbler(process.getErrorStream(), shellContext.logger::error));
                    out.start();
                    err.start();
                    int exitCode = process.waitFor();
                    out.join();
                    err.join();
                    if (exitCode != 0) {
                        shellContext.logger.error("Shell command exited with code " + exitCode);
                    }
                } catch (Exception e) {
                    saveException(e);
                }
            }
        }

        private void cd(CommandInput input) {
            try {
                if (input.args().length == 1) {
                    shellContext.cwd.change(input.args()[0]);
                } else {
                    shellContext.writer.accept("Error: 'cd' accepts only one argument");
                }
            } catch (Exception e) {
                saveException(e);
            }
        }

        private List<Completer> cdCompleter(String name) {
            return List.of(new ArgumentCompleter(new Completers.DirectoriesCompleter(shellContext.cwd)));
        }

        private void pwd(CommandInput input) {
            try {
                shellContext.writer.accept(shellContext.cwd.get().toString());
            } catch (Exception e) {
                saveException(e);
            }
        }

        private void mvn(CommandInput input) {
            try {
                shellMavenInvoker.invoke(mavenParser.parseInvocation(
                        ParserRequest.mvn(input.args(), shellContext.invokerRequest.messageBuilderFactory())
                                .cwd(shellContext.cwd.get())
                                .build()));
            } catch (InvokerException.ExitException e) {
                shellContext.logger.error("mvn command exited with exit code " + e.getExitCode());
            } catch (Exception e) {
                saveException(e);
            }
        }

        private List<Completer> mvnCompleter(String name) {
            List<String> names;
            try {
                List<String> phases = shellContext.lookup.lookup(LifecycleRegistry.class).stream()
                        .flatMap(Lifecycle::allPhases)
                        .map(Lifecycle.Phase::name)
                        .toList();
                // TODO: add goals dynamically
                List<String> goals = List.of("wrapper:wrapper");
                names = Stream.concat(phases.stream(), goals.stream()).toList();
            } catch (LookupException e) {
                names = List.of(
                        "clean",
                        "validate",
                        "compile",
                        "test",
                        "package",
                        "verify",
                        "install",
                        "deploy",
                        "wrapper:wrapper");
            }
            return List.of(new ArgumentCompleter(new StringsCompleter(names)));
        }

        private void mvnenc(CommandInput input) {
            try {
                shellEncryptInvoker.invoke(encryptParser.parseInvocation(
                        ParserRequest.mvnenc(input.args(), shellContext.invokerRequest.messageBuilderFactory())
                                .cwd(shellContext.cwd.get())
                                .build()));
            } catch (InvokerException.ExitException e) {
                shellContext.logger.error("mvnenc command exited with exit code " + e.getExitCode());
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
