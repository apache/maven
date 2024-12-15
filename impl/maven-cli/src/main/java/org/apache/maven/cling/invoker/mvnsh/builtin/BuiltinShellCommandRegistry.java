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

import org.apache.maven.api.cli.ParserRequest;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.mvn.MavenParser;
import org.apache.maven.cling.invoker.mvnenc.EncryptParser;
import org.apache.maven.cling.invoker.mvnsh.ShellEncryptInvoker;
import org.apache.maven.cling.invoker.mvnsh.ShellMavenInvoker;
import org.jline.builtins.Completers;
import org.jline.builtins.Options;
import org.jline.console.CmdDesc;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.impl.AbstractCommandRegistry;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;

import static java.util.Objects.requireNonNull;
import static org.jline.console.impl.JlineCommandRegistry.compileCommandOptions;

public class BuiltinShellCommandRegistry extends AbstractCommandRegistry implements AutoCloseable {
    public enum Command {
        MVN,
        MVNENC
    }

    private final LookupContext shellContext;
    private final ShellMavenInvoker shellMavenInvoker;
    private final MavenParser mavenParser;
    private final ShellEncryptInvoker shellEncryptInvoker;
    private final EncryptParser encryptParser;

    public BuiltinShellCommandRegistry(LookupContext shellContext) {
        this.shellContext = requireNonNull(shellContext, "shellContext");
        this.shellMavenInvoker = new ShellMavenInvoker(shellContext);
        this.mavenParser = new MavenParser();
        this.shellEncryptInvoker = new ShellEncryptInvoker(shellContext);
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
