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
package org.apache.maven.cling.invoker.mvnsh;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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

public class ShellCommandRegistry extends AbstractCommandRegistry {
    public enum Command {
        MVN,
        MVNENC
    }

    private final Supplier<Path> cwd;

    public ShellCommandRegistry(Supplier<Path> cwd) {
        this.cwd = requireNonNull(cwd, "cwd");
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
    public List<String> commandInfo(String command) {
        return List.of();
    }

    @Override
    public CmdDesc commandDescription(List<String> args) {
        return null;
    }

    @Override
    public String name() {
        return "Maven Shell commands";
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
            input.out().println("mvn");
        } catch (Exception e) {
            saveException(e);
        }
    }

    private List<Completer> mvnCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(
                NullCompleter.INSTANCE,
                new Completers.OptionCompleter(new Completers.FilesCompleter(cwd), this::commandOptions, 1)));
        return completers;
    }

    private void mvnenc(CommandInput input) {
        try {
            input.out().println("mvnenc");
        } catch (Exception e) {
            saveException(e);
        }
    }

    private List<Completer> mvnencCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(
                NullCompleter.INSTANCE,
                new Completers.OptionCompleter(new Completers.FilesCompleter(cwd), this::commandOptions, 1)));
        return completers;
    }
}
