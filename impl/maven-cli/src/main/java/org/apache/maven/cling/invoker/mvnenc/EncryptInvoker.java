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

import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.mvnenc.EncryptOptions;
import org.apache.maven.cling.invoker.LookupInvoker;
import org.apache.maven.cling.invoker.ProtoLookup;
import org.apache.maven.cling.utils.CLIReportingUtils;
import org.jline.consoleui.prompt.ConsolePrompt;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Colors;
import org.jline.utils.OSUtils;

/**
 * mvnenc invoker implementation.
 */
public class EncryptInvoker extends LookupInvoker<EncryptContext> {

    public EncryptInvoker(ProtoLookup protoLookup) {
        super(protoLookup);
    }

    @Override
    protected int execute(EncryptContext context) throws Exception {
        return doExecute(context);
    }

    @Override
    protected EncryptContext createContext(InvokerRequest invokerRequest) {
        return new EncryptContext(invokerRequest);
    }

    @Override
    protected void lookup(EncryptContext context) {
        context.goals = context.lookup.lookupMap(Goal.class);
    }

    public static final int OK = 0; // OK
    public static final int ERROR = 1; // "generic" error
    public static final int BAD_OPERATION = 2; // bad user input or alike
    public static final int CANCELED = 3; // user canceled

    protected int doExecute(EncryptContext context) throws Exception {
        try {
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
            if (OSUtils.IS_WINDOWS) {
                config = new ConsolePrompt.UiConfig(">", "( )", "(x)", "( )");
            } else {
                config = new ConsolePrompt.UiConfig("❯", "◯ ", "◉ ", "◯ ");
            }
            config.setCancellableFirstPrompt(true);

            context.reader =
                    LineReaderBuilder.builder().terminal(context.terminal).build();
            context.prompt = new ConsolePrompt(context.reader, context.terminal, config);

            EncryptOptions options = (EncryptOptions) context.invokerRequest.options();
            if (options.goals().isEmpty() || options.goals().get().size() != 1) {
                return badGoalsErrorMessage("No goal or multiple goals specified, specify only one goal.", context);
            }

            String goalName = options.goals().get().get(0);
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

    protected int badGoalsErrorMessage(String message, EncryptContext context) {
        context.terminal.writer().println(message);
        context.terminal.writer().println("Supported goals are: " + String.join(", ", context.goals.keySet()));
        context.terminal.writer().println("Use -h to display help.");
        return BAD_OPERATION;
    }
}
