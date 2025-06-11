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
package org.apache.maven.cling.invoker.mvnup;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
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
 * mvnup invoker implementation.
 */
public class UpgradeInvoker extends LookupInvoker<UpgradeContext> {

    public static final int OK = 0; // OK
    public static final int ERROR = 1; // "generic" error
    public static final int BAD_OPERATION = 2; // bad user input or alike
    public static final int CANCELED = 3; // user canceled

    public UpgradeInvoker(Lookup protoLookup, @Nullable Consumer<LookupContext> contextConsumer) {
        super(protoLookup, contextConsumer);
    }

    @Override
    protected UpgradeContext createContext(InvokerRequest invokerRequest) {
        return new UpgradeContext(
                invokerRequest, (UpgradeOptions) invokerRequest.options().orElse(null));
    }

    @Override
    protected void lookup(UpgradeContext context) throws Exception {
        if (context.goals == null) {
            super.lookup(context);
            context.goals = context.lookup.lookupMap(Goal.class);
        }
    }

    @Override
    protected int execute(UpgradeContext context) throws Exception {
        try {
            context.header = new ArrayList<>();
            context.style = new AttributedStyle();
            context.addInHeader(
                    context.style.italic().bold().foreground(Colors.rgbColor("green")),
                    "Maven Upgrade " + CLIReportingUtils.showVersionMinimal());
            context.addInHeader("Tool for upgrading Maven projects and dependencies.");
            context.addInHeader("This tool is part of Apache Maven 4 distribution.");
            context.addInHeader("");

            context.terminal.handle(
                    Terminal.Signal.INT, signal -> Thread.currentThread().interrupt());

            context.reader =
                    LineReaderBuilder.builder().terminal(context.terminal).build();

            if (context.options().goals().isEmpty()) {
                return badGoalsErrorMessage("No goals specified.", context);
            }

            String goalName = context.options().goals().get().get(0);
            Goal goal = context.goals.get(goalName);
            if (goal == null) {
                return badGoalsErrorMessage("Unknown goal: " + goalName, context);
            }

            return goal.execute(context);
        } catch (InterruptedException | InterruptedIOException | UserInterruptException e) {
            context.logger.error("Goal canceled by user.");
            return CANCELED;
        } catch (Exception e) {
            if (context.options().showErrors().orElse(false)) {
                context.logger.error(e.getMessage(), e);
            } else {
                context.logger.error(e.getMessage());
            }
            return ERROR;
        }
    }

    protected int badGoalsErrorMessage(String message, UpgradeContext context) {
        context.logger.error(message);
        context.logger.error("Supported goals are: " + String.join(", ", context.goals.keySet()));
        context.logger.error("Use -h to display help.");
        return BAD_OPERATION;
    }
}
