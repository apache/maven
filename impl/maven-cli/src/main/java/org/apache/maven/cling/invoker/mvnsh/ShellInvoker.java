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
import java.util.Map;

import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.LookupInvoker;
import org.apache.maven.cling.utils.CLIReportingUtils;
import org.jline.builtins.ConfigurationPath;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SimpleSystemRegistryImpl;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;
import org.jline.widget.TailTipWidgets;

/**
 * mvnsh invoker implementation.
 */
public class ShellInvoker extends LookupInvoker<LookupContext> {

    public ShellInvoker(Lookup protoLookup) {
        super(protoLookup, null);
    }

    @Override
    protected LookupContext createContext(InvokerRequest invokerRequest) {
        return new LookupContext(invokerRequest);
    }

    public static final int OK = 0; // OK
    public static final int ERROR = 1; // "generic" error

    @Override
    protected int execute(LookupContext context) throws Exception {
        // set up JLine built-in commands
        ConfigurationPath configPath = new ConfigurationPath(context.cwd.get(), context.cwd.get());
        Builtins builtins = new Builtins(context.cwd, configPath, null);
        builtins.rename(Builtins.Command.TTOP, "top");
        builtins.alias("zle", "widget");
        builtins.alias("bindkey", "keymap");

        ShellCommandRegistryHolder holder = new ShellCommandRegistryHolder();
        holder.addCommandRegistry(builtins);

        // gather commands
        Map<String, ShellCommandRegistryFactory> factories =
                context.lookup.lookupMap(ShellCommandRegistryFactory.class);
        for (Map.Entry<String, ShellCommandRegistryFactory> entry : factories.entrySet()) {
            holder.addCommandRegistry(entry.getValue().createShellCommandRegistry(context));
        }

        DefaultParser parser = new DefaultParser();
        parser.setRegexCommand("[:]{0,1}[a-zA-Z!]{1,}\\S*"); // change default regex to support shell commands

        String banner =
                """

                ░▒▓██████████████▓▒░ ░▒▓█▓▒░░▒▓█▓▒░░▒▓███████▓▒░  ░▒▓███████▓▒░░▒▓█▓▒░░▒▓█▓▒░\s
                ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░       ░▒▓█▓▒░░▒▓█▓▒░\s
                ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░ ░▒▓█▓▒▒▓█▓▒░ ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░       ░▒▓█▓▒░░▒▓█▓▒░\s
                ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░ ░▒▓█▓▒▒▓█▓▒░ ░▒▓█▓▒░░▒▓█▓▒░ ░▒▓██████▓▒░ ░▒▓████████▓▒░\s
                ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░  ░▒▓█▓▓█▓▒░  ░▒▓█▓▒░░▒▓█▓▒░       ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░\s
                ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░  ░▒▓█▓▓█▓▒░  ░▒▓█▓▒░░▒▓█▓▒░       ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░\s
                ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░   ░▒▓██▓▒░   ░▒▓█▓▒░░▒▓█▓▒░░▒▓███████▓▒░ ░▒▓█▓▒░░▒▓█▓▒░""";
        context.writer.accept(banner);
        if (!context.invokerRequest.options().showVersion().orElse(false)) {
            context.writer.accept(CLIReportingUtils.showVersionMinimal());
        }
        context.writer.accept("");

        try (holder) {
            SimpleSystemRegistryImpl systemRegistry =
                    new SimpleSystemRegistryImpl(parser, context.terminal, context.cwd, configPath) {
                        @Override
                        public boolean isCommandOrScript(String command) {
                            return command.startsWith("!") || super.isCommandOrScript(command);
                        }
                    };
            systemRegistry.setCommandRegistries(holder.getCommandRegistries());

            Path history = context.userDirectory.apply(".mvnsh_history");
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(context.terminal)
                    .history(new DefaultHistory())
                    .highlighter(new ReplHighlighter())
                    .completer(systemRegistry.completer())
                    .parser(parser)
                    .variable(LineReader.LIST_MAX, 50) // max tab completion candidates
                    .variable(LineReader.HISTORY_FILE, history)
                    .variable(LineReader.OTHERS_GROUP_NAME, "Others")
                    .variable(LineReader.COMPLETION_STYLE_GROUP, "fg:blue,bold")
                    .variable("HELP_COLORS", "ti=1;34:co=38:ar=3:op=33:de=90")
                    .option(LineReader.Option.GROUP_PERSIST, true)
                    .build();
            builtins.setLineReader(reader);
            systemRegistry.setLineReader(reader);
            new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);
            KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
            keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

            // start the shell and process input until the user quits with Ctrl-D
            String line;
            while (true) {
                try {
                    systemRegistry.cleanUp();
                    line = reader.readLine(
                            context.cwd.get().getFileName().toString() + " mvnsh> ",
                            null,
                            (MaskingCallback) null,
                            null);
                    systemRegistry.execute(line);
                } catch (UserInterruptException e) {
                    // Ignore
                    // return CANCELED;
                } catch (EndOfFileException e) {
                    return OK;
                } catch (SystemRegistryImpl.UnknownCommandException e) {
                    context.writer.accept(context.invokerRequest
                            .messageBuilderFactory()
                            .builder()
                            .error(e.getMessage())
                            .build());
                } catch (Exception e) {
                    systemRegistry.trace(e);
                    context.writer.accept(context.invokerRequest
                            .messageBuilderFactory()
                            .builder()
                            .error("Error:" + e.getMessage())
                            .build());
                    if (context.invokerRequest.options().showErrors().orElse(false)) {
                        e.printStackTrace(context.terminal.writer());
                    }
                    return ERROR;
                }
            }
        }
    }

    private static class ReplHighlighter extends DefaultHighlighter {
        @Override
        protected void commandStyle(LineReader reader, AttributedStringBuilder sb, boolean enable) {
            if (enable) {
                if (reader.getTerminal().getNumericCapability(InfoCmp.Capability.max_colors) >= 256) {
                    sb.style(AttributedStyle.DEFAULT.bold().foreground(69));
                } else {
                    sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
                }
            } else {
                sb.style(AttributedStyle.DEFAULT.boldOff().foregroundOff());
            }
        }
    }
}
