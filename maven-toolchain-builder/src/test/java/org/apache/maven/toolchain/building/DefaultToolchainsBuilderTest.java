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
package org.apache.maven.toolchain.building;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.building.StringSource;
import org.apache.maven.toolchain.discovery.ToolchainDiscoverer;
import org.apache.maven.toolchain.io.DefaultToolchainsReader;
import org.apache.maven.toolchain.io.DefaultToolchainsWriter;
import org.apache.maven.toolchain.io.ToolchainsParseException;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.interpolation.os.OperatingSystemUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

class DefaultToolchainsBuilderTest {
    private static final String LS = System.lineSeparator();

    @Spy
    private DefaultToolchainsReader toolchainsReader;

    @Spy
    private DefaultToolchainsWriter toolchainsWriter;

    @Spy
    private List<ToolchainDiscoverer> toolchainDiscoverers = new ArrayList<>();

    @InjectMocks
    private DefaultToolchainsBuilder toolchainBuilder;

    @BeforeEach
    void onSetup() {
        MockitoAnnotations.initMocks(this);

        Map<String, String> envVarMap = new HashMap<>();
        envVarMap.put("testKey", "testValue");
        envVarMap.put("testSpecialCharactersKey", "<test&Value>");
        OperatingSystemUtils.setEnvVarSource(new TestEnvVarSource(envVarMap));
    }

    @Test
    void testBuildEmptyRequest() throws Exception {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        ToolchainsBuildingResult result = toolchainBuilder.build(request);
        assertNotNull(result.getEffectiveToolchains());
        assertNotNull(result.getProblems());
        assertEquals(0, result.getProblems().size());
    }

    @Test
    void testBuildRequestWithUserToolchains() throws Exception {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource(new StringSource(""));

        Properties props = new Properties();
        props.put("key", "user_value");
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType("TYPE");
        toolchain.setProvides(props);
        PersistedToolchains userResult = new PersistedToolchains();
        userResult.setToolchains(Collections.singletonList(toolchain));
        doReturn(userResult)
                .when(toolchainsReader)
                .read(any(InputStream.class), ArgumentMatchers.<String, Object>anyMap());

        ToolchainsBuildingResult result = toolchainBuilder.build(request);
        assertNotNull(result.getEffectiveToolchains());
        assertEquals(1, result.getEffectiveToolchains().getToolchains().size());
        assertEquals(
                "TYPE", result.getEffectiveToolchains().getToolchains().get(0).getType());
        assertEquals(
                "user_value",
                result.getEffectiveToolchains()
                        .getToolchains()
                        .get(0)
                        .getProvides()
                        .get("key"));
        assertNotNull(result.getProblems());
        assertEquals(0, result.getProblems().size());
    }

    @Test
    void testBuildRequestWithGlobalToolchains() throws Exception {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setGlobalToolchainsSource(new StringSource(""));

        Properties props = new Properties();
        props.put("key", "global_value");
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType("TYPE");
        toolchain.setProvides(props);
        PersistedToolchains globalResult = new PersistedToolchains();
        globalResult.setToolchains(Collections.singletonList(toolchain));
        doReturn(globalResult)
                .when(toolchainsReader)
                .read(any(InputStream.class), ArgumentMatchers.<String, Object>anyMap());

        ToolchainsBuildingResult result = toolchainBuilder.build(request);
        assertNotNull(result.getEffectiveToolchains());
        assertEquals(1, result.getEffectiveToolchains().getToolchains().size());
        assertEquals(
                "TYPE", result.getEffectiveToolchains().getToolchains().get(0).getType());
        assertEquals(
                "global_value",
                result.getEffectiveToolchains()
                        .getToolchains()
                        .get(0)
                        .getProvides()
                        .get("key"));
        assertNotNull(result.getProblems());
        assertEquals(0, result.getProblems().size());
    }

    @Test
    void testBuildRequestWithBothToolchains() throws Exception {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setGlobalToolchainsSource(new StringSource(""));
        request.setUserToolchainsSource(new StringSource(""));

        Properties props = new Properties();
        props.put("key", "user_value");
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType("TYPE");
        toolchain.setProvides(props);
        PersistedToolchains userResult = new PersistedToolchains();
        userResult.setToolchains(Collections.singletonList(toolchain));

        props = new Properties();
        props.put("key", "global_value");
        toolchain = new ToolchainModel();
        toolchain.setType("TYPE");
        toolchain.setProvides(props);
        PersistedToolchains globalResult = new PersistedToolchains();
        globalResult.setToolchains(Collections.singletonList(toolchain));

        doReturn(globalResult)
                .doReturn(userResult)
                .when(toolchainsReader)
                .read(any(InputStream.class), ArgumentMatchers.<String, Object>anyMap());

        ToolchainsBuildingResult result = toolchainBuilder.build(request);
        assertNotNull(result.getEffectiveToolchains());
        assertEquals(2, result.getEffectiveToolchains().getToolchains().size());
        assertEquals(
                "TYPE", result.getEffectiveToolchains().getToolchains().get(0).getType());
        assertEquals(
                "user_value",
                result.getEffectiveToolchains()
                        .getToolchains()
                        .get(0)
                        .getProvides()
                        .get("key"));
        assertEquals(
                "TYPE", result.getEffectiveToolchains().getToolchains().get(1).getType());
        assertEquals(
                "global_value",
                result.getEffectiveToolchains()
                        .getToolchains()
                        .get(1)
                        .getProvides()
                        .get("key"));
        assertNotNull(result.getProblems());
        assertEquals(0, result.getProblems().size());
    }

    @Test
    void testStrictToolchainsParseException() throws Exception {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setGlobalToolchainsSource(new StringSource(""));
        ToolchainsParseException parseException = new ToolchainsParseException("MESSAGE", 4, 2);
        doThrow(parseException)
                .when(toolchainsReader)
                .read(any(InputStream.class), ArgumentMatchers.<String, Object>anyMap());

        try {
            toolchainBuilder.build(request);
        } catch (ToolchainsBuildingException e) {
            assertEquals(
                    "1 problem was encountered while building the effective toolchains" + LS
                            + "[FATAL] Non-parseable toolchains (memory): MESSAGE @ line 4, column 2" + LS,
                    e.getMessage());
        }
    }

    @Test
    void testIOException() throws Exception {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setGlobalToolchainsSource(new StringSource("", "LOCATION"));
        IOException ioException = new IOException("MESSAGE");
        doThrow(ioException)
                .when(toolchainsReader)
                .read(any(InputStream.class), ArgumentMatchers.<String, Object>anyMap());

        try {
            toolchainBuilder.build(request);
        } catch (ToolchainsBuildingException e) {
            assertEquals(
                    "1 problem was encountered while building the effective toolchains" + LS
                            + "[FATAL] Non-readable toolchains LOCATION: MESSAGE" + LS,
                    e.getMessage());
        }
    }

    @Test
    void testEnvironmentVariablesAreInterpolated() throws Exception {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource(new StringSource(""));

        Properties props = new Properties();
        props.put("key", "${env.testKey}");
        Xpp3Dom configurationChild = new Xpp3Dom("jdkHome");
        configurationChild.setValue("${env.testKey}");
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        configuration.addChild(configurationChild);
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType("TYPE");
        toolchain.setProvides(props);
        toolchain.setConfiguration(configuration);
        PersistedToolchains persistedToolchains = new PersistedToolchains();
        persistedToolchains.setToolchains(Collections.singletonList(toolchain));

        doReturn(persistedToolchains)
                .when(toolchainsReader)
                .read(any(InputStream.class), ArgumentMatchers.<String, Object>anyMap());

        ToolchainsBuildingResult result = toolchainBuilder.build(request);
        String interpolatedValue = "testValue";
        assertEquals(
                interpolatedValue,
                result.getEffectiveToolchains()
                        .getToolchains()
                        .get(0)
                        .getProvides()
                        .get("key"));
        org.codehaus.plexus.util.xml.Xpp3Dom toolchainConfiguration = (org.codehaus.plexus.util.xml.Xpp3Dom)
                result.getEffectiveToolchains().getToolchains().get(0).getConfiguration();
        assertEquals(
                interpolatedValue, toolchainConfiguration.getChild("jdkHome").getValue());
        assertNotNull(result.getProblems());
        assertEquals(0, result.getProblems().size());
    }

    @Test
    void testNonExistingEnvironmentVariablesAreNotInterpolated() throws Exception {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource(new StringSource(""));

        Properties props = new Properties();
        props.put("key", "${env.testNonExistingKey}");
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType("TYPE");
        toolchain.setProvides(props);
        PersistedToolchains persistedToolchains = new PersistedToolchains();
        persistedToolchains.setToolchains(Collections.singletonList(toolchain));

        doReturn(persistedToolchains)
                .when(toolchainsReader)
                .read(any(InputStream.class), ArgumentMatchers.<String, Object>anyMap());

        ToolchainsBuildingResult result = toolchainBuilder.build(request);
        assertEquals(
                "${env.testNonExistingKey}",
                result.getEffectiveToolchains()
                        .getToolchains()
                        .get(0)
                        .getProvides()
                        .get("key"));
        assertNotNull(result.getProblems());
        assertEquals(0, result.getProblems().size());
    }

    @Test
    void testEnvironmentVariablesWithSpecialCharactersAreInterpolated() throws Exception {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource(new StringSource(""));

        Properties props = new Properties();
        props.put("key", "${env.testSpecialCharactersKey}");
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType("TYPE");
        toolchain.setProvides(props);
        PersistedToolchains persistedToolchains = new PersistedToolchains();
        persistedToolchains.setToolchains(Collections.singletonList(toolchain));

        doReturn(persistedToolchains)
                .when(toolchainsReader)
                .read(any(InputStream.class), ArgumentMatchers.<String, Object>anyMap());

        ToolchainsBuildingResult result = toolchainBuilder.build(request);
        String interpolatedValue = "<test&Value>";
        assertEquals(
                interpolatedValue,
                result.getEffectiveToolchains()
                        .getToolchains()
                        .get(0)
                        .getProvides()
                        .get("key"));
        assertNotNull(result.getProblems());
        assertEquals(0, result.getProblems().size());
    }

    static class TestEnvVarSource implements OperatingSystemUtils.EnvVarSource {
        private final Map<String, String> envVarMap;

        TestEnvVarSource(Map<String, String> envVarMap) {
            this.envVarMap = envVarMap;
        }

        public Map<String, String> getEnvMap() {
            return envVarMap;
        }
    }
}
