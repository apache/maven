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
package org.apache.maven.toolchain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Session;
import org.apache.maven.api.services.ToolchainFactory;
import org.apache.maven.api.toolchain.ToolchainModel;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultToolchainManagerTest {
    // Mocks to inject into toolchainManager
    @Mock
    private Logger logger;

    @InjectMocks
    private DefaultToolchainManager toolchainManager;

    @Mock
    private ToolchainFactory toolchainFactory_basicType;

    @Mock
    private ToolchainFactory toolchainFactory_rareType;

    @BeforeEach
    void onSetup() throws Exception {
        MockitoAnnotations.initMocks(this);

        Map<String, ToolchainFactory> factories = new HashMap<>();
        factories.put("basic", toolchainFactory_basicType);
        factories.put("rare", toolchainFactory_rareType);
        toolchainManager = new DefaultToolchainManager(
                new org.apache.maven.internal.impl.DefaultToolchainManager(factories, logger) {});
    }

    @Test
    void testNoModels() {
        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        when(session.getRequest()).thenReturn(executionRequest);

        List<Toolchain> toolchains = toolchainManager.getToolchains(session, "unknown", null);

        assertEquals(0, toolchains.size());
    }

    @Test
    void testModelNoFactory() {
        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        List<ToolchainModel> toolchainModels = new ArrayList<>();
        toolchainModels.add(ToolchainModel.newBuilder().type("unknown").build());
        when(session.getRequest()).thenReturn(executionRequest);
        Session sessionv4 = mock(Session.class);
        when(session.getSession()).thenReturn(sessionv4);
        when(sessionv4.getToolchains()).thenReturn(toolchainModels);

        List<Toolchain> toolchains = toolchainManager.getToolchains(session, "unknown", null);

        assertEquals(0, toolchains.size());
        verify(logger).error("Missing toolchain factory for type: unknown. Possibly caused by misconfigured project.");
    }

    @Test
    void testModelAndFactory() {
        MavenSession session = mock(MavenSession.class);
        List<ToolchainModel> toolchainModels = List.of(
                ToolchainModel.newBuilder().type("basic").build(),
                ToolchainModel.newBuilder().type("basic").build(),
                ToolchainModel.newBuilder().type("rare").build());
        Session sessionv4 = mock(Session.class);
        when(session.getSession()).thenReturn(sessionv4);
        when(sessionv4.getToolchains()).thenReturn(toolchainModels);

        org.apache.maven.api.Toolchain rareToolchain = mock(org.apache.maven.api.Toolchain.class);
        when(toolchainFactory_rareType.createToolchain(any())).thenReturn(rareToolchain);

        List<Toolchain> toolchains = toolchainManager.getToolchains(session, "rare", null);

        assertEquals(1, toolchains.size());
    }

    @Test
    void testModelsAndFactory() {
        MavenSession session = mock(MavenSession.class);
        Session sessionv4 = mock(Session.class);
        when(session.getSession()).thenReturn(sessionv4);
        when(sessionv4.getToolchains())
                .thenReturn(List.of(
                        ToolchainModel.newBuilder().type("basic").build(),
                        ToolchainModel.newBuilder().type("basic").build(),
                        ToolchainModel.newBuilder().type("rare").build()));

        org.apache.maven.api.Toolchain basicToolchain = mock(org.apache.maven.api.Toolchain.class);
        when(toolchainFactory_basicType.createToolchain(any())).thenReturn(basicToolchain);
        org.apache.maven.api.Toolchain basicToolchain2 = mock(org.apache.maven.api.Toolchain.class);
        when(toolchainFactory_basicType.createToolchain(any())).thenReturn(basicToolchain2);

        List<Toolchain> toolchains = toolchainManager.getToolchains(session, "basic", null);

        assertEquals(2, toolchains.size());
    }

    @Test
    void testRequirements() throws Exception {
        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        when(session.getRequest()).thenReturn(executionRequest);
        Session sessionv4 = mock(Session.class);
        when(session.getSession()).thenReturn(sessionv4);
        when(sessionv4.getToolchains())
                .thenReturn(List.of(
                        ToolchainModel.newBuilder().type("basic").build(),
                        ToolchainModel.newBuilder().type("rare").build()));

        org.apache.maven.api.Toolchain basicPrivate = mock(org.apache.maven.api.Toolchain.class);
        when(basicPrivate.matchesRequirements(anyMap())).thenReturn(false);
        when(basicPrivate.matchesRequirements(ArgumentMatchers.eq(Map.of("key", "value"))))
                .thenReturn(true);
        when(toolchainFactory_basicType.createToolchain(isA(org.apache.maven.api.toolchain.ToolchainModel.class)))
                .thenReturn(basicPrivate);

        List<Toolchain> toolchains =
                toolchainManager.getToolchains(session, "basic", Collections.singletonMap("key", "value"));

        assertEquals(1, toolchains.size());
    }
}
