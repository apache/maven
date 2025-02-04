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
package org.apache.maven.internal.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.Toolchain;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.ToolchainFactory;
import org.apache.maven.api.toolchain.ToolchainModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultToolchainManagerTest {

    @Mock
    private Session session;

    @Mock
    private Lookup lookup;

    @Mock
    private Project project;

    @Mock
    private ToolchainFactory jdkFactory;

    @Mock
    private Toolchain mockToolchain;

    private ToolchainModel toolchainModel;

    private DefaultToolchainManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultToolchainManager(Map.of("jdk", jdkFactory));
    }

    @Test
    void getToolchains_WithValidTypeAndRequirements() {
        toolchainModel = ToolchainModel.newBuilder().type("jdk").build();
        when(session.getToolchains()).thenReturn(List.of(toolchainModel));
        when(jdkFactory.createToolchain(toolchainModel)).thenReturn(mockToolchain);
        when(jdkFactory.createDefaultToolchain()).thenReturn(Optional.empty());
        when(mockToolchain.matchesRequirements(any())).thenReturn(true);

        List<Toolchain> result = manager.getToolchains(session, "jdk", Map.of("version", "11"));

        assertEquals(1, result.size());
        assertEquals(mockToolchain, result.get(0));
    }

    @Test
    void getToolchains_WithInvalidType() {
        List<Toolchain> result = manager.getToolchains(session, "invalid", null);
        assertTrue(result.isEmpty());
    }

    @Test
    void storeAndRetrieveToolchainFromBuildContext() {
        Map<String, Object> context = new HashMap<>();
        toolchainModel = ToolchainModel.newBuilder().type("jdk").build();
        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.of(project));
        when(session.getPluginContext(project)).thenReturn(context);
        when(mockToolchain.getType()).thenReturn("jdk");
        when(mockToolchain.getModel()).thenReturn(toolchainModel);
        when(jdkFactory.createToolchain(any(ToolchainModel.class))).thenReturn(mockToolchain);

        manager.storeToolchainToBuildContext(session, mockToolchain);
        Optional<Toolchain> result = manager.getToolchainFromBuildContext(session, "jdk");

        assertTrue(result.isPresent());
        assertEquals(mockToolchain, result.get());
    }

    @Test
    void retrieveContext_WithoutProject() {
        when(session.getService(Lookup.class)).thenReturn(lookup);
        when(lookup.lookupOptional(Project.class)).thenReturn(Optional.empty());

        assertTrue(manager.retrieveContext(session).isEmpty());
    }

    @Test
    void getToolchains_WithNullType() {
        assertThrows(NullPointerException.class, () -> manager.getToolchains(session, null, null));
    }
}
