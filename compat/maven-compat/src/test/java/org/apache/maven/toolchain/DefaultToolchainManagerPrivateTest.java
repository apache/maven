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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Session;
import org.apache.maven.api.Toolchain;
import org.apache.maven.api.services.ToolchainFactory;
import org.apache.maven.api.toolchain.ToolchainModel;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultToolchainManagerPrivateTest {
    // Mocks to inject into toolchainManager
    @Mock
    private Logger logger;

    @InjectMocks
    private DefaultToolchainManagerPrivate toolchainManager;

    @Mock
    private ToolchainFactory toolchainFactory_basicType;

    @Mock
    private ToolchainFactory toolchainFactory_rareType;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.initMocks(this);

        Map<String, ToolchainFactory> factories = new HashMap<>();
        factories.put("basic", toolchainFactory_basicType);
        factories.put("rare", toolchainFactory_rareType);
        toolchainManager = new DefaultToolchainManagerPrivate(
                new org.apache.maven.internal.impl.DefaultToolchainManager(factories, logger) {});
    }

    @Test
    void testToolchainsForAvailableType() throws Exception {
        // prepare
        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest req = new DefaultMavenExecutionRequest();
        when(session.getRequest()).thenReturn(req);
        Session sessionv4 = mock(Session.class);
        when(session.getSession()).thenReturn(sessionv4);

        Toolchain basicToolchain = mock(Toolchain.class);
        when(toolchainFactory_basicType.createDefaultToolchain()).thenReturn(Optional.of(basicToolchain));
        Toolchain rareToolchain = mock(Toolchain.class);
        when(toolchainFactory_rareType.createDefaultToolchain()).thenReturn(Optional.of(rareToolchain));

        // execute
        ToolchainPrivate[] toolchains = toolchainManager.getToolchainsForType("basic", session);

        // verify
        verify(logger, never()).error(anyString());
        assertEquals(1, toolchains.length);
    }

    @Test
    void testToolchainsForUnknownType() throws Exception {
        // prepare
        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest req = new DefaultMavenExecutionRequest();
        when(session.getRequest()).thenReturn(req);

        Toolchain basicToolchain = mock(Toolchain.class);
        when(toolchainFactory_basicType.createDefaultToolchain()).thenReturn(Optional.of(basicToolchain));
        Toolchain rareToolchain = mock(Toolchain.class);
        when(toolchainFactory_rareType.createDefaultToolchain()).thenReturn(Optional.of(rareToolchain));

        // execute
        ToolchainPrivate[] toolchains = toolchainManager.getToolchainsForType("unknown", session);

        // verify
        verify(logger).error("Missing toolchain factory for type: unknown. Possibly caused by misconfigured project.");
        assertEquals(0, toolchains.length);
    }

    @Test
    void testToolchainsForConfiguredType() throws Exception {
        // prepare
        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest req = new DefaultMavenExecutionRequest();
        when(session.getRequest()).thenReturn(req);
        Session sessionv4 = mock(Session.class);
        when(session.getSession()).thenReturn(sessionv4);
        List<ToolchainModel> toolchainModels = new ArrayList<>();
        when(sessionv4.getToolchains()).thenReturn(toolchainModels);

        ToolchainModel basicToolchainModel =
                ToolchainModel.newBuilder().type("basic").build();
        toolchainModels.add(basicToolchainModel);
        toolchainModels.add(basicToolchainModel);

        ToolchainModel rareToolchainModel =
                ToolchainModel.newBuilder().type("rare").build();
        toolchainModels.add(rareToolchainModel);

        Toolchain basic = mock(Toolchain.class);
        when(toolchainFactory_basicType.createToolchain(basicToolchainModel)).thenReturn(basic);

        // execute
        ToolchainPrivate[] toolchains = toolchainManager.getToolchainsForType("basic", session);

        // verify
        verify(logger, never()).error(anyString());
        assertEquals(2, toolchains.length);
    }

    @Test
    void testMisconfiguredToolchain() throws Exception {
        // prepare
        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest req = new DefaultMavenExecutionRequest();
        when(session.getRequest()).thenReturn(req);
        Session sessionv4 = mock(Session.class);
        when(session.getSession()).thenReturn(sessionv4);

        // execute
        ToolchainPrivate[] basics = toolchainManager.getToolchainsForType("basic", session);

        // verify
        assertEquals(0, basics.length);
    }
}
