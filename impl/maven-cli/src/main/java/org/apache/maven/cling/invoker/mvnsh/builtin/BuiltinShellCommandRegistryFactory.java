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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.maven.api.cli.InvokerException;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.mvn.MavenContext;
import org.apache.maven.cling.invoker.mvn.MavenInvoker;
import org.apache.maven.cling.invoker.mvn.MavenParser;
import org.apache.maven.cling.invoker.mvnenc.EncryptContext;
import org.apache.maven.cling.invoker.mvnenc.EncryptInvoker;
import org.apache.maven.cling.invoker.mvnenc.EncryptParser;
import org.apache.maven.cling.invoker.mvnsh.ShellCommandRegistryFactory;
import org.jline.builtins.Completers;
import org.jline.builtins.Options;
import org.jline.console.CmdDesc;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.console.impl.AbstractCommandRegistry;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;

import static java.util.Objects.requireNonNull;
import static org.jline.console.impl.JlineCommandRegistry.compileCommandOptions;

@Named("builtin")
@Singleton
public class BuiltinShellCommandRegistryFactory implements ShellCommandRegistryFactory {
    public CommandRegistry createShellCommandRegistry(LookupContext context) {
        return new BuiltinShellCommandRegistry(context);
    }

    private static class BuiltinShellCommandRegistry extends AbstractCommandRegistry implements AutoCloseable {
        public enum Command {
            MVN,
            MVNENC
        }

        private final LookupContext shellContext;
        private final ShellMavenInvoker shellMavenInvoker;
        private final MavenParser mavenParser;
        private final ShellEncryptInvoker shellEncryptInvoker;
        private final EncryptParser encryptParser;

        private BuiltinShellCommandRegistry(LookupContext shellContext) {
            this.shellContext = requireNonNull(shellContext, "shellContext");
            this.shellMavenInvoker = new ShellMavenInvoker(shellContext.invokerRequest.lookup(), contextCopier());
            this.mavenParser = new MavenParser();
            this.shellEncryptInvoker = new ShellEncryptInvoker(shellContext.invokerRequest.lookup(), contextCopier());
            this.encryptParser = new EncryptParser();
            Set<Command> commands = new HashSet<>(EnumSet.allOf(Command.class));
            Map<Command, String> commandName = new HashMap<>();
            Map<Command, CommandMethods> commandExecute = new HashMap<>();
            for (Command c : commands) {
                commandName.put(c, c.name().toLowerCase());
            }
            commandExecute.put(Command.MVN, new CommandMethods(this::mvn, this::mvnCompleter));
            commandExecute.put(Command.MVNENC, new CommandMethods(this::mvnenc, this::mvnencCompleter));
            registerCommands(commandName, commandExecute);
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

        private List<Completers.OptDesc> commandOptions(String command) {
            try {
                invoke(new CommandSession(), command, "--help");
            } catch (Options.HelpException e) {
                return compileCommandOptions(e.getMessage());
            } catch (Exception e) {
                // ignore
            }
            return null;
        }

        private void mvn(CommandInput input) {
            try {
                shellMavenInvoker.invoke(mavenParser.parseInvocation(ParserRequest.mvn(
                                input.args(),
                                shellContext.invokerRequest.logger(),
                                shellContext.invokerRequest.messageBuilderFactory())
                        .build()));
            } catch (Exception e) {
                saveException(e);
            }
        }

        private List<Completer> mvnCompleter(String name) {
            List<Completer> completers = new ArrayList<>();
            completers.add(new ArgumentCompleter(
                    NullCompleter.INSTANCE,
                    new Completers.OptionCompleter(
                            new Completers.FilesCompleter(shellContext.invokerRequest::cwd), this::commandOptions, 1)));
            return completers;
        }

        private void mvnenc(CommandInput input) {
            try {
                shellEncryptInvoker.invoke(encryptParser.parseInvocation(ParserRequest.mvnenc(
                                input.args(),
                                shellContext.invokerRequest.logger(),
                                shellContext.invokerRequest.messageBuilderFactory())
                        .build()));
            } catch (Exception e) {
                saveException(e);
            }
        }

        private List<Completer> mvnencCompleter(String name) {
            List<Completer> completers = new ArrayList<>();
            completers.add(new ArgumentCompleter(
                    NullCompleter.INSTANCE,
                    new Completers.OptionCompleter(
                            new Completers.FilesCompleter(shellContext.invokerRequest::cwd), this::commandOptions, 1)));
            return completers;
        }
    }

    /**
     * Shell Encrypt invoker: passes over relevant context bits.
     */
    private static class ShellEncryptInvoker extends EncryptInvoker {
        private final Consumer<LookupContext> contextCopier;

        private ShellEncryptInvoker(Lookup lookup, Consumer<LookupContext> contextCopier) {
            super(lookup);
            this.contextCopier = contextCopier;
        }

        @Override
        protected EncryptContext createContext(InvokerRequest invokerRequest) throws InvokerException {
            EncryptContext result = new EncryptContext(invokerRequest, false);
            contextCopier.accept(result);
            return result;
        }
    }

    /**
     * Shell Maven invoker: passes over relevant context bits.
     */
    private static class ShellMavenInvoker extends MavenInvoker<MavenContext> {
        private final Consumer<LookupContext> contextCopier;

        private ShellMavenInvoker(Lookup lookup, Consumer<LookupContext> contextCopier) {
            super(lookup);
            this.contextCopier = contextCopier;
        }

        @Override
        protected MavenContext createContext(InvokerRequest invokerRequest) throws InvokerException {
            MavenContext result = new MavenContext(invokerRequest, false);
            contextCopier.accept(result);
            return result;
        }
    }
}
