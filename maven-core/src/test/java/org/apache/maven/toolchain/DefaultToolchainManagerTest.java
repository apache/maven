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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.toolchain.java.JavaToolchainFactory;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultToolchainManagerTest {
    // Mocks to inject into toolchainManager
    @Mock
    private Logger logger;

    @InjectMocks
    private DefaultToolchainManager toolchainManager;

    @Mock
    private ToolchainFactory toolchainFactoryBasicType;

    @Mock
    private ToolchainFactory toolchainFactoryRareType;

    @BeforeEach
    public void onSetup() throws Exception {
        toolchainManager = new DefaultToolchainManager();

        MockitoAnnotations.initMocks(this);

        toolchainManager.factories = new HashMap<>();
        toolchainManager.factories.put("basic", toolchainFactoryBasicType);
        toolchainManager.factories.put("rare", toolchainFactoryRareType);
    }

    @Test
    public void testNoModels() {
        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        when(session.getRequest()).thenReturn(executionRequest);

        List<Toolchain> toolchains = toolchainManager.getToolchains(session, "unknown", null);

        assertEquals(0, toolchains.size());
    }

    @Test
    public void testModelNoFactory() {
        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        Map<String, List<ToolchainModel>> toolchainModels = new HashMap<>();
        toolchainModels.put("unknown", Collections.singletonList(new ToolchainModel()));
        executionRequest.setToolchains(toolchainModels);
        when(session.getRequest()).thenReturn(executionRequest);

        List<Toolchain> toolchains = toolchainManager.getToolchains(session, "unknown", null);

        assertEquals(0, toolchains.size());
        verify(logger).error("Missing toolchain factory for type: unknown. Possibly caused by misconfigured project.");
    }

    @Test
    public void testModelAndFactory() {
        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        Map<String, List<ToolchainModel>> toolchainModels = new HashMap<>();
        toolchainModels.put("basic", Arrays.asList(new ToolchainModel(), new ToolchainModel()));
        toolchainModels.put("rare", Collections.singletonList(new ToolchainModel()));
        executionRequest.setToolchains(toolchainModels);
        when(session.getRequest()).thenReturn(executionRequest);

        List<Toolchain> toolchains = toolchainManager.getToolchains(session, "rare", null);

        assertEquals(1, toolchains.size());
    }

    @Test
    public void testModelsAndFactory() {
        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        Map<String, List<ToolchainModel>> toolchainModels = new HashMap<>();
        toolchainModels.put("basic", Arrays.asList(new ToolchainModel(), new ToolchainModel()));
        toolchainModels.put("rare", Collections.singletonList(new ToolchainModel()));
        executionRequest.setToolchains(toolchainModels);
        when(session.getRequest()).thenReturn(executionRequest);

        List<Toolchain> toolchains = toolchainManager.getToolchains(session, "basic", null);

        assertEquals(2, toolchains.size());
    }

    @Test
    public void testRequirements() throws Exception {
        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        Map<String, List<ToolchainModel>> toolchainModels = new HashMap<>();
        toolchainModels.put("basic", Arrays.asList(new ToolchainModel(), new ToolchainModel()));
        toolchainModels.put("rare", Collections.singletonList(new ToolchainModel()));
        executionRequest.setToolchains(toolchainModels);
        when(session.getRequest()).thenReturn(executionRequest);
        ToolchainPrivate basicPrivate = mock(ToolchainPrivate.class);
        when(basicPrivate.matchesRequirements(ArgumentMatchers.<String, String>anyMap()))
                .thenReturn(false)
                .thenReturn(true);
        when(toolchainFactoryBasicType.createToolchain(isA(ToolchainModel.class)))
                .thenReturn(basicPrivate);

        List<Toolchain> toolchains =
                toolchainManager.getToolchains(session, "basic", Collections.singletonMap("key", "value"));

        assertEquals(1, toolchains.size());
    }

    @Test
    public void testMatchingMisconfiguredJdkToolchain(@TempDir Path temporaryDirectory) {
        Path invalidJdkHome = temporaryDirectory.resolve("non-existing-jdk");
        ToolchainModel model = new ToolchainModel();
        model.setType("jdk");
        model.addProvide("version", "17");

        Xpp3Dom jdkHome = new Xpp3Dom("jdkHome");
        jdkHome.setValue(invalidJdkHome.toString());
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        configuration.addChild(jdkHome);
        model.setConfiguration(configuration);

        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        executionRequest.setToolchains(Collections.singletonMap("jdk", Collections.singletonList(model)));
        when(session.getRequest()).thenReturn(executionRequest);
        toolchainManager.factories.put("jdk", new JavaToolchainFactory());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> toolchainManager.getToolchains(session, "jdk", Collections.singletonMap("version", "17")));

        assertTrue(exception.getMessage().contains("Misconfigured toolchain of type jdk"));
        assertTrue(exception.getMessage().contains(invalidJdkHome.toString()));
        assertInstanceOf(MisconfiguredToolchainException.class, exception.getCause());
    }

    @Test
    public void testValidNonmatchingToolchain() throws Exception {
        ToolchainModel model = new ToolchainModel();
        model.setType("basic");

        MavenSession session = mock(MavenSession.class);
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        executionRequest.setToolchains(Collections.singletonMap("basic", Collections.singletonList(model)));
        when(session.getRequest()).thenReturn(executionRequest);
        ToolchainPrivate toolchain = mock(ToolchainPrivate.class);
        when(toolchainFactoryBasicType.createToolchain(model)).thenReturn(toolchain);
        when(toolchain.matchesRequirements(Collections.singletonMap("version", "21")))
                .thenReturn(false);

        List<Toolchain> toolchains =
                toolchainManager.getToolchains(session, "basic", Collections.singletonMap("version", "21"));

        assertEquals(0, toolchains.size());
    }
}
