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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.api.cli.mvnenc.EncryptOptions;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.cling.invoker.mvnenc.EncryptContext;
import org.codehaus.plexus.components.secdispatcher.DispatcherMeta;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.codehaus.plexus.components.secdispatcher.model.Config;
import org.codehaus.plexus.components.secdispatcher.model.ConfigProperty;
import org.codehaus.plexus.components.secdispatcher.model.SettingsSecurity;
import org.jline.consoleui.elements.ConfirmChoice;
import org.jline.consoleui.prompt.ConfirmResult;
import org.jline.consoleui.prompt.ConsolePrompt;
import org.jline.consoleui.prompt.PromptResultItemIF;
import org.jline.consoleui.prompt.builder.ListPromptBuilder;
import org.jline.consoleui.prompt.builder.PromptBuilder;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.utils.Colors;
import org.jline.utils.OSUtils;

import static org.apache.maven.cling.invoker.mvnenc.EncryptInvoker.BAD_OPERATION;
import static org.apache.maven.cling.invoker.mvnenc.EncryptInvoker.OK;

/**
 * The "init" goal.
 */
@Singleton
@Named("init")
public class Init extends InteractiveGoalSupport {
    private static final String NONE = "__none__";

    @Inject
    public Init(MessageBuilderFactory messageBuilderFactory, SecDispatcher secDispatcher) {
        super(messageBuilderFactory, secDispatcher);
    }

    @Override
    public int doExecute(EncryptContext context) throws Exception {
        EncryptOptions options = (EncryptOptions) context.invokerRequest.options();
        boolean force = options.force().orElse(false);
        boolean yes = options.yes().orElse(false);

        if (configExists() && !force) {
            context.logger.error(messageBuilderFactory
                    .builder()
                    .error("Error: configuration exist. Use --force if you want to reset existing configuration.")
                    .build());
            return BAD_OPERATION;
        }

        context.addInHeader(context.style.italic().bold().foreground(Colors.rgbColor("yellow")), "goal: init");
        context.addInHeader("");

        ConsolePrompt.UiConfig promptConfig;
        if (OSUtils.IS_WINDOWS) {
            promptConfig = new ConsolePrompt.UiConfig(">", "( )", "(x)", "( )");
        } else {
            promptConfig = new ConsolePrompt.UiConfig("❯", "◯ ", "◉ ", "◯ ");
        }
        promptConfig.setCancellableFirstPrompt(true);
        ConsolePrompt prompt = new ConsolePrompt(context.reader, context.terminal, promptConfig);

        SettingsSecurity config = secDispatcher.readConfiguration(true);

        // reset config
        config.setDefaultDispatcher(null);
        config.getConfigurations().clear();

        Map<String, PromptResultItemIF> result = prompt.prompt(
                context.header, dispatcherPrompt(prompt.getPromptBuilder()).build());
        if (result == null) {
            throw new InterruptedException();
        }
        if (NONE.equals(result.get("defaultDispatcher").getResult())) {
            context.terminal
                    .writer()
                    .println(messageBuilderFactory
                            .builder()
                            .warning(
                                    "Maven4 SecDispatcher disabled; Maven3 fallback may still work, use `mvnenc diag` to check")
                            .build());
            secDispatcher.writeConfiguration(config);
            return OK;
        }
        config.setDefaultDispatcher(result.get("defaultDispatcher").getResult());

        DispatcherMeta meta = secDispatcher.availableDispatchers().stream()
                .filter(d -> Objects.equals(config.getDefaultDispatcher(), d.name()))
                .findFirst()
                .orElseThrow();
        if (!meta.fields().isEmpty()) {
            result = prompt.prompt(
                    context.header,
                    configureDispatcher(context, meta, prompt.getPromptBuilder())
                            .build());
            if (result == null) {
                throw new InterruptedException();
            }

            List<Map.Entry<String, PromptResultItemIF>> editables = result.entrySet().stream()
                    .filter(e -> e.getValue().getResult().contains("$"))
                    .toList();
            if (!editables.isEmpty()) {
                context.addInHeader("");
                context.addInHeader("Please customize the editable value:");
                Map<String, PromptResultItemIF> editMap;
                for (Map.Entry<String, PromptResultItemIF> editable : editables) {
                    String template = editable.getValue().getResult();
                    String prefix = template.substring(0, template.indexOf("$"));
                    editMap = prompt.prompt(
                            context.header,
                            prompt.getPromptBuilder()
                                    .createInputPrompt()
                                    .name("edit")
                                    .message(template)
                                    .addCompleter(new Completer() {
                                        @Override
                                        public void complete(
                                                LineReader reader, ParsedLine line, List<Candidate> candidates) {
                                            if (!line.line().startsWith(prefix)) {
                                                candidates.add(
                                                        new Candidate(prefix, prefix, null, null, null, null, false));
                                            }
                                        }
                                    })
                                    .addPrompt()
                                    .build());
                    if (editMap == null) {
                        throw new InterruptedException();
                    }
                    result.put(editable.getKey(), editMap.get("edit"));
                }
            }

            Config dispatcherConfig = new Config();
            dispatcherConfig.setName(meta.name());
            for (DispatcherMeta.Field field : meta.fields()) {
                ConfigProperty property = new ConfigProperty();
                property.setName(field.getKey());
                property.setValue(result.get(field.getKey()).getResult());
                dispatcherConfig.addProperty(property);
            }
            if (!dispatcherConfig.getProperties().isEmpty()) {
                config.addConfiguration(dispatcherConfig);
            }
        }

        if (yes) {
            secDispatcher.writeConfiguration(config);
        } else {
            context.addInHeader("");
            context.addInHeader("Values set:");
            context.addInHeader("defaultDispatcher=" + config.getDefaultDispatcher());
            for (Config c : config.getConfigurations()) {
                context.addInHeader("  dispatcherName=" + c.getName());
                for (ConfigProperty cp : c.getProperties()) {
                    context.addInHeader("    " + cp.getName() + "=" + cp.getValue());
                }
            }

            result = prompt.prompt(
                    context.header, confirmPrompt(prompt.getPromptBuilder()).build());
            ConfirmResult confirm = (ConfirmResult) result.get("confirm");
            if (confirm.getConfirmed() == ConfirmChoice.ConfirmationValue.YES) {
                context.terminal
                        .writer()
                        .println(messageBuilderFactory
                                .builder()
                                .info("Writing out the configuration...")
                                .build());
                secDispatcher.writeConfiguration(config);
            } else {
                context.terminal
                        .writer()
                        .println(messageBuilderFactory
                                .builder()
                                .warning("Values not accepted; not saving configuration.")
                                .build());
                return BAD_OPERATION;
            }
        }

        return OK;
    }

    protected PromptBuilder confirmPrompt(PromptBuilder promptBuilder) {
        promptBuilder
                .createConfirmPromp()
                .name("confirm")
                .message("Are values above correct?")
                .defaultValue(ConfirmChoice.ConfirmationValue.YES)
                .addPrompt();
        return promptBuilder;
    }

    protected PromptBuilder dispatcherPrompt(PromptBuilder promptBuilder) {
        ListPromptBuilder listPromptBuilder = promptBuilder
                .createListPrompt()
                .name("defaultDispatcher")
                .message("Which dispatcher you want to use as default?");
        listPromptBuilder
                .newItem()
                .name(NONE)
                .text("None (disable MavenSecDispatcher)")
                .add();
        for (DispatcherMeta meta : secDispatcher.availableDispatchers()) {
            if (!meta.isHidden()) {
                listPromptBuilder
                        .newItem()
                        .name(meta.name())
                        .text(meta.displayName())
                        .add();
            }
        }
        listPromptBuilder.addPrompt();
        return promptBuilder;
    }

    private PromptBuilder configureDispatcher(
            EncryptContext context, DispatcherMeta dispatcherMeta, PromptBuilder promptBuilder) throws Exception {
        context.addInHeader(
                context.style.italic().bold().foreground(Colors.rgbColor("yellow")),
                "Configure " + dispatcherMeta.displayName());
        context.addInHeader("");

        for (DispatcherMeta.Field field : dispatcherMeta.fields()) {
            String fieldKey = field.getKey();
            String fieldDescription = "Configure " + fieldKey + ": " + field.getDescription();
            if (field.getOptions().isPresent()) {
                // list options
                ListPromptBuilder listPromptBuilder =
                        promptBuilder.createListPrompt().name(fieldKey).message(fieldDescription);
                for (DispatcherMeta.Field option : field.getOptions().get()) {
                    listPromptBuilder
                            .newItem()
                            .name(
                                    option.getDefaultValue().isPresent()
                                            ? option.getDefaultValue().get()
                                            : option.getKey())
                            .text(option.getDescription())
                            .add();
                }
                listPromptBuilder.addPrompt();
            } else if (field.getDefaultValue().isPresent()) {
                // input w/ def value
                promptBuilder
                        .createInputPrompt()
                        .name(fieldKey)
                        .message(fieldDescription)
                        .defaultValue(field.getDefaultValue().get())
                        .addPrompt();
            } else {
                // ? plain input?
                promptBuilder
                        .createInputPrompt()
                        .name(fieldKey)
                        .message(fieldDescription)
                        .addPrompt();
            }
        }
        return promptBuilder;
    }
}
