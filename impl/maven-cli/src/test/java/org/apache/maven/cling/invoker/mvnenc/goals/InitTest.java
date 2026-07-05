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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.cli.InvokerRequest;
import org.apache.maven.api.cli.mvnenc.EncryptOptions;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.cling.invoker.mvnenc.EncryptContext;
import org.codehaus.plexus.components.secdispatcher.DispatcherMeta;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.codehaus.plexus.components.secdispatcher.model.Config;
import org.codehaus.plexus.components.secdispatcher.model.ConfigProperty;
import org.codehaus.plexus.components.secdispatcher.model.SettingsSecurity;
import org.jline.consoleui.prompt.ConsolePrompt;
import org.jline.consoleui.prompt.PromptResultItemIF;
import org.jline.consoleui.prompt.builder.PromptBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InitTest {

    @Test
    void testPrefixPrependedToUserInput() throws Exception {
        MessageBuilderFactory messageBuilderFactory = mock(MessageBuilderFactory.class, Mockito.RETURNS_DEEP_STUBS);
        SecDispatcher secDispatcher = mock(SecDispatcher.class);

        SettingsSecurity settingsSecurity = new SettingsSecurity();
        when(secDispatcher.readConfiguration(true)).thenReturn(settingsSecurity);

        DispatcherMeta meta = mock(DispatcherMeta.class);
        when(meta.name()).thenReturn("master");
        DispatcherMeta.Field field = mock(DispatcherMeta.Field.class);
        when(field.getKey()).thenReturn("password");
        when(meta.fields()).thenReturn(Collections.singletonList(field));

        when(secDispatcher.availableDispatchers()).thenReturn(Collections.singleton(meta));

        Init init = new Init(messageBuilderFactory, secDispatcher);

        InvokerRequest invokerRequest = mock(InvokerRequest.class, Mockito.RETURNS_DEEP_STUBS);
        when(invokerRequest.cwd()).thenReturn(java.nio.file.Paths.get(""));
        when(invokerRequest.installationDirectory()).thenReturn(java.nio.file.Paths.get(""));
        when(invokerRequest.userHomeDirectory()).thenReturn(java.nio.file.Paths.get(""));
        when(invokerRequest.topDirectory()).thenReturn(java.nio.file.Paths.get(""));
        when(invokerRequest.rootDirectory()).thenReturn(java.util.Optional.empty());
        EncryptOptions options = mock(EncryptOptions.class);
        EncryptContext context = new EncryptContext(invokerRequest, options);
        // avoid null pointers for lists
        context.header = new java.util.ArrayList<>();
        context.style = new org.jline.utils.AttributedStyle();
        org.jline.terminal.Terminal terminal = mock(org.jline.terminal.Terminal.class);
        java.io.PrintWriter printWriter = mock(java.io.PrintWriter.class);
        when(terminal.writer()).thenReturn(printWriter);
        context.terminal = terminal;

        try (MockedConstruction<ConsolePrompt> mockedPrompt =
                Mockito.mockConstruction(ConsolePrompt.class, (mock, ctx) -> {
                    PromptBuilder builderMock = mock(PromptBuilder.class, Mockito.RETURNS_DEEP_STUBS);
                    when(mock.getPromptBuilder()).thenReturn(builderMock);

                    Map<String, PromptResultItemIF> dispatcherResult = new HashMap<>();
                    dispatcherResult.put("defaultDispatcher", createResult("master"));

                    Map<String, PromptResultItemIF> configureResult = new HashMap<>();
                    configureResult.put("password", createResult("env:$MVN_PASSWORD"));

                    Map<String, PromptResultItemIF> editResult = new HashMap<>();
                    editResult.put(
                            "edit", createResult("my_password_var")); // User inputs "my_password_var" without prefix

                    Map<String, PromptResultItemIF> confirmResult = new HashMap<>();
                    org.jline.consoleui.prompt.ConfirmResult confirm =
                            mock(org.jline.consoleui.prompt.ConfirmResult.class);
                    when(confirm.getConfirmed())
                            .thenReturn(org.jline.consoleui.elements.ConfirmChoice.ConfirmationValue.YES);
                    confirmResult.put("confirm", confirm);

                    when(mock.prompt(any(java.util.List.class), any(java.util.List.class)))
                            .thenReturn(dispatcherResult, configureResult, editResult, confirmResult);
                })) {
            init.doExecute(context);
        }

        // Validate that the SettingsSecurity model has the prepended prefix
        List<Config> configs = settingsSecurity.getConfigurations();
        assertEquals(1, configs.size());
        Config config = configs.get(0);
        assertEquals("master", config.getName());
        assertEquals(1, config.getProperties().size());
        ConfigProperty prop = config.getProperties().get(0);
        assertEquals("password", prop.getName());
        // The expected value should be env:my_password_var because the template was env:$MVN_PASSWORD
        assertEquals("env:my_password_var", prop.getValue());
    }

    private PromptResultItemIF createResult(String value) {
        return new PromptResultItemIF() {
            @Override
            public String getResult() {
                return value;
            }
        };
    }
}
