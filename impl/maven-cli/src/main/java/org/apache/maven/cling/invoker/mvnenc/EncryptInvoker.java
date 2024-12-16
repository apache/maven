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
import java.util.function.Consumer;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.mvnenc.EncryptOptions;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.LookupInvoker;
import org.apache.maven.cling.utils.CLIReportingUtils;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Colors;

/**
 * mvnenc invoker implementation.
 */
public class EncryptInvoker extends LookupInvoker<EncryptContext> {

    public static final int OK = 0; // OK
    public static final int ERROR = 1; // "generic" error
    public static final int BAD_OPERATION = 2; // bad user input or alike
    public static final int CANCELED = 3; // user canceled

    public EncryptInvoker(Lookup protoLookup) {
        this(protoLookup, null);
    }

    public EncryptInvoker(Lookup protoLookup, @Nullable Consumer<LookupContext> contextConsumer) {
        super(protoLookup, contextConsumer);
    }

    @Override
    protected EncryptContext createContext(InvokerRequest invokerRequest) {
        return new EncryptContext(invokerRequest);
    }

    @Override
    protected void lookup(EncryptContext context) throws Exception {
        if (context.goals == null) {
            super.lookup(context);
            context.goals = context.lookup.lookupMap(Goal.class);
        }
    }

    @Override
    protected int execute(EncryptContext context) throws Exception {
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

            context.reader =
                    LineReaderBuilder.builder().terminal(context.terminal).build();

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
            context.logger.error("Goal canceled by user.");
            return CANCELED;
        } catch (Exception e) {
            if (context.invokerRequest.options().showErrors().orElse(false)) {
                context.logger.error(e.getMessage(), e);
            } else {
                context.logger.error(e.getMessage());
            }
            return ERROR;
        } finally {
            context.terminal.writer().flush();
        }
    }

    protected int badGoalsErrorMessage(String message, EncryptContext context) {
        context.logger.error(message);
        context.logger.error("Supported goals are: " + String.join(", ", context.goals.keySet()));
        context.logger.error("Use -h to display help.");
        return BAD_OPERATION;
    }
}
