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
package org.apache.maven.cling.invoker.mvnenc;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.cli.mvnenc.EncryptInvoker;
import org.apache.maven.api.cli.mvnenc.EncryptInvokerRequest;
import org.apache.maven.api.cli.mvnenc.EncryptOptions;
import org.apache.maven.cling.invoker.LookupInvoker;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.utils.CLIReportingUtils;
import org.jline.consoleui.prompt.ConsolePrompt;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Colors;
import org.jline.utils.OSUtils;

/**
 * Encrypt invoker implementation, when Encrypt CLI is being run. System uses ClassWorld launcher, and class world
 * instance is passed in via "enhanced" main method. Hence, this class expects fully setup ClassWorld via constructor.
 */
public class DefaultEncryptInvoker
        extends LookupInvoker<EncryptOptions, EncryptInvokerRequest, DefaultEncryptInvoker.LocalContext>
        implements EncryptInvoker {

    @SuppressWarnings("VisibilityModifier")
    public static class LocalContext
            extends LookupInvokerContext<EncryptOptions, EncryptInvokerRequest, DefaultEncryptInvoker.LocalContext> {
        protected LocalContext(DefaultEncryptInvoker invoker, EncryptInvokerRequest invokerRequest) {
            super(invoker, invokerRequest);
        }

        public Map<String, Goal> goals;

        public List<AttributedString> header;
        public AttributedStyle style;
        public LineReader reader;
        public ConsolePrompt prompt;

        public void addInHeader(String text) {
            addInHeader(AttributedStyle.DEFAULT, text);
        }

        public void addInHeader(AttributedStyle style, String text) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.style(style).append(text);
            header.add(asb.toAttributedString());
        }
    }

    public DefaultEncryptInvoker(ProtoLookup protoLookup) {
        super(protoLookup);
    }

    @Override
    protected int execute(LocalContext context) throws Exception {
        return doExecute(context);
    }

    @Override
    protected LocalContext createContext(EncryptInvokerRequest invokerRequest) {
        return new LocalContext(this, invokerRequest);
    }

    @Override
    protected void lookup(LocalContext context) {
        context.goals = context.lookup.lookupMap(Goal.class);
    }

    public static final int OK = 0; // OK
    public static final int ERROR = 1; // "generic" error
    public static final int BAD_OPERATION = 2; // bad user input or alike
    public static final int CANCELED = 3; // user canceled

    protected int doExecute(LocalContext context) throws Exception {
        try {
            if (!context.interactive) {
                context.terminal.writer().println("This tool works only in interactive mode!");
                context.terminal
                        .writer()
                        .println("Tool purpose is to configure password management on developer workstations.");
                context.terminal
                        .writer()
                        .println(
                                "Note: Generated configuration can be moved/copied to headless environments, if configured as such.");
                return BAD_OPERATION;
            }

            context.header = new ArrayList<>();
            context.style = new AttributedStyle();
            context.addInHeader(
                    context.style.italic().bold().foreground(Colors.rgbColor("green")),
                    "Maven Encryption " + CLIReportingUtils.showVersionMinimal());
            context.addInHeader("Tool for secure password management on workstations.");
            context.addInHeader("This tool is part of Apache Maven 4 distribution.");
            context.addInHeader("");

            Thread executeThread = Thread.currentThread();
            context.terminal.handle(Terminal.Signal.INT, signal -> executeThread.interrupt());
            ConsolePrompt.UiConfig config;
            if (context.terminal.getType().equals(Terminal.TYPE_DUMB)
                    || context.terminal.getType().equals(Terminal.TYPE_DUMB_COLOR)) {
                context.terminal.writer().println(context.terminal.getName() + ": " + context.terminal.getType());
                throw new IllegalStateException("Dumb terminal detected.\nThis tool requires real terminal to work!\n"
                        + "Note: On Windows Jansi or JNA library must be included in classpath.");
            } else if (OSUtils.IS_WINDOWS) {
                config = new ConsolePrompt.UiConfig(">", "( )", "(x)", "( )");
            } else {
                config = new ConsolePrompt.UiConfig("❯", "◯ ", "◉ ", "◯ ");
            }
            config.setCancellableFirstPrompt(true);

            context.reader =
                    LineReaderBuilder.builder().terminal(context.terminal).build();
            context.prompt = new ConsolePrompt(context.reader, context.terminal, config);

            if (context.invokerRequest.options().goals().isEmpty()
                    || context.invokerRequest.options().goals().get().size() != 1) {
                return badGoalsErrorMessage("No goal or multiple goals specified, specify only one goal.", context);
            }

            String goalName = context.invokerRequest.options().goals().get().get(0);
            Goal goal = context.goals.get(goalName);

            if (goal == null) {
                return badGoalsErrorMessage("Unknown goal: " + goalName, context);
            }

            return goal.execute(context);
        } catch (InterruptedException | InterruptedIOException | UserInterruptException e) {
            context.terminal.writer().println("Goal canceled by user.");
            return CANCELED;
        } catch (Exception e) {
            if (context.invokerRequest.options().showErrors().orElse(false)) {
                context.terminal.writer().println(e.getMessage());
                e.printStackTrace(context.terminal.writer());
            } else {
                context.terminal.writer().println(e.getMessage());
            }
            return ERROR;
        } finally {
            context.terminal.writer().flush();
        }
    }

    protected int badGoalsErrorMessage(String message, LocalContext context) {
        context.terminal.writer().println(message);
        context.terminal.writer().println("Supported goals are: " + String.join(", ", context.goals.keySet()));
        context.terminal.writer().println("Use -h to display help.");
        return BAD_OPERATION;
    }
}
