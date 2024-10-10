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
package org.apache.maven.cling.invoker.mvnenc.goals;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Map;
import java.util.Objects;

import org.apache.maven.cling.invoker.mvnenc.DefaultEncryptInvoker;
import org.apache.maven.cling.invoker.mvnenc.Goal;
import org.codehaus.plexus.components.secdispatcher.Meta;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.codehaus.plexus.components.secdispatcher.model.SettingsSecurity;
import org.jline.consoleui.elements.ConfirmChoice;
import org.jline.consoleui.prompt.ConfirmResult;
import org.jline.consoleui.prompt.ConsolePrompt;
import org.jline.consoleui.prompt.PromptResultItemIF;
import org.jline.consoleui.prompt.builder.ListPromptBuilder;
import org.jline.consoleui.prompt.builder.PromptBuilder;
import org.jline.utils.Colors;

import static org.apache.maven.cling.invoker.mvnenc.DefaultEncryptInvoker.BAD_OPERATION;
import static org.apache.maven.cling.invoker.mvnenc.DefaultEncryptInvoker.OK;

/**
 * The "init" goal.
 */
@Singleton
@Named("init")
public class InitGoal implements Goal {
    private final SecDispatcher secDispatcher;

    @Inject
    public InitGoal(SecDispatcher secDispatcher) {
        this.secDispatcher = secDispatcher;
    }

    @Override
    public int execute(DefaultEncryptInvoker.LocalContext context) throws Exception {
        context.addInHeader(context.style.italic().bold().foreground(Colors.rgbColor("yellow")), "init");
        context.addInHeader("");

        ConsolePrompt prompt = context.prompt;
        boolean force = context.invokerRequest.options().force().orElse(false);
        boolean yes = context.invokerRequest.options().yes().orElse(false);

        boolean configExists = secDispatcher.readConfiguration(false) != null;
        if (configExists && !force) {
            System.out.println("Error: cannot init, configuration exist.");
            return BAD_OPERATION;
        }

        SettingsSecurity config = secDispatcher.readConfiguration(true);

        Map<String, ? extends PromptResultItemIF> result =
                prompt.prompt(context.header, prompt(prompt).build());
        if (result == null) {
            throw new InterruptedException();
        }
        config.setDefaultDispatcher(result.get("defaultDispatcher").getResult());
        configureDispatcher(
                context,
                config,
                secDispatcher.availableDispatchers().stream()
                        .filter(d -> Objects.equals(config.getDefaultDispatcher(), d.name()))
                        .findFirst()
                        .orElseThrow());

        if (yes) {
            secDispatcher.writeConfiguration(config);
        } else {
            ConfirmResult confirm = (ConfirmResult) result.get("confirm");
            if (confirm.getConfirmed() == ConfirmChoice.ConfirmationValue.YES) {
                System.out.println("Writing out the configuration...");
                secDispatcher.writeConfiguration(config);
            } else {
                System.out.println("Values not accepted; not saving configuration.");
                return BAD_OPERATION;
            }
        }

        return OK;
    }

    protected PromptBuilder prompt(ConsolePrompt prompt) {
        PromptBuilder promptBuilder = prompt.getPromptBuilder();
        dispatcherPrompt(promptBuilder);
        promptBuilder
                .createConfirmPromp()
                .name("confirm")
                .message("Are values above correct?")
                .defaultValue(ConfirmChoice.ConfirmationValue.YES)
                .addPrompt();
        return promptBuilder;
    }

    protected void dispatcherPrompt(PromptBuilder promptBuilder) {
        ListPromptBuilder listPromptBuilder = promptBuilder
                .createListPrompt()
                .name("defaultDispatcher")
                .message("Which dispatcher you want to use as default?");
        for (Meta meta : secDispatcher.availableDispatchers()) {
            listPromptBuilder
                    .newItem()
                    .name(meta.name())
                    .text(meta.displayName())
                    .add();
        }
        listPromptBuilder.addPrompt();
    }

    private void configureDispatcher(
            DefaultEncryptInvoker.LocalContext context, SettingsSecurity config, Meta dispatcherMeta) throws Exception {
        context.addInHeader(
                context.style.italic().bold().foreground(Colors.rgbColor("yellow")),
                "Configure " + dispatcherMeta.name() + " dispatcher");
        context.addInHeader("");
        PromptBuilder promptBuilder = context.prompt.getPromptBuilder();
        for (Meta.Field fields : dispatcherMeta.fields()) {

        }
        cipherPrompt(promptBuilder);
        promptBuilder
                .createConfirmPromp()
                .name("confirm")
                .message("Are values above correct?")
                .defaultValue(ConfirmChoice.ConfirmationValue.YES)
                .addPrompt();

        Map<String, ? extends PromptResultItemIF> result = context.prompt.prompt(context.header, promptBuilder.build());
    }
}
