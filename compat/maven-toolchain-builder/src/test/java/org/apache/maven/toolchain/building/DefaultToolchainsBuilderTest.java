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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.building.Source;
import org.apache.maven.building.StringSource;
import org.apache.maven.impl.DefaultToolchainsXmlFactory;
import org.apache.maven.toolchain.io.DefaultToolchainsReader;
import org.apache.maven.toolchain.io.DefaultToolchainsWriter;
import org.apache.maven.toolchain.io.ToolchainsParseException;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.interpolation.os.OperatingSystemUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DefaultToolchainsBuilderTest {
    private static final String LS = System.lineSeparator();

    @Spy
    private DefaultToolchainsReader toolchainsReader;

    @Spy
    private DefaultToolchainsWriter toolchainsWriter;

    @InjectMocks
    private DefaultToolchainsBuilder toolchainBuilder;

    @BeforeEach
    void onSetup() {
        // MockitoAnnotations.openMocks(this);

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
        Properties props = new Properties();
        props.put("key", "user_value");
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType("TYPE");
        toolchain.setProvides(props);
        PersistedToolchains persistedToolchains = new PersistedToolchains();
        persistedToolchains.setToolchains(Collections.singletonList(toolchain));

        String xml = new DefaultToolchainsXmlFactory().toXmlString(persistedToolchains.getDelegate());
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource(new StringSource(xml));

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
        Properties props = new Properties();
        props.put("key", "global_value");
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType("TYPE");
        toolchain.setProvides(props);
        PersistedToolchains persistedToolchains = new PersistedToolchains();
        persistedToolchains.setToolchains(Collections.singletonList(toolchain));

        String xml = new DefaultToolchainsXmlFactory().toXmlString(persistedToolchains.getDelegate());
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setGlobalToolchainsSource(new StringSource(xml));

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

        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource(
                new StringSource(new DefaultToolchainsXmlFactory().toXmlString(userResult.getDelegate())));
        request.setGlobalToolchainsSource(
                new StringSource(new DefaultToolchainsXmlFactory().toXmlString(globalResult.getDelegate())));

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
        doThrow(parseException).when(toolchainsReader).read(any(InputStream.class), ArgumentMatchers.anyMap());

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
        Source src = mock(Source.class);
        IOException ioException = new IOException("MESSAGE");
        doThrow(ioException).when(src).getInputStream();
        doReturn("LOCATION").when(src).getLocation();

        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setGlobalToolchainsSource(src);

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

        String xml = new DefaultToolchainsXmlFactory().toXmlString(persistedToolchains.getDelegate());
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource(new StringSource(xml));

        ToolchainsBuildingResult result = toolchainBuilder.build(request);
        String interpolatedValue = "testValue";
        assertEquals(
                interpolatedValue,
                result.getEffectiveToolchains()
                        .getToolchains()
                        .get(0)
                        .getProvides()
                        .get("key"));
        Xpp3Dom toolchainConfiguration =
                (Xpp3Dom) result.getEffectiveToolchains().getToolchains().get(0).getConfiguration();
        assertEquals(
                interpolatedValue, toolchainConfiguration.getChild("jdkHome").getValue());
        assertNotNull(result.getProblems());
        assertEquals(0, result.getProblems().size());
    }

    @Test
    void testNonExistingEnvironmentVariablesAreNotInterpolated() throws Exception {
        Properties props = new Properties();
        props.put("key", "${env.testNonExistingKey}");
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType("TYPE");
        toolchain.setProvides(props);
        PersistedToolchains persistedToolchains = new PersistedToolchains();
        persistedToolchains.setToolchains(Collections.singletonList(toolchain));

        String xml = new DefaultToolchainsXmlFactory().toXmlString(persistedToolchains.getDelegate());
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource(new StringSource(xml));

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
        Properties props = new Properties();
        props.put("key", "${env.testSpecialCharactersKey}");
        ToolchainModel toolchain = new ToolchainModel();
        toolchain.setType("TYPE");
        toolchain.setProvides(props);
        PersistedToolchains persistedToolchains = new PersistedToolchains();
        persistedToolchains.setToolchains(Collections.singletonList(toolchain));

        String xml = new DefaultToolchainsXmlFactory().toXmlString(persistedToolchains.getDelegate());
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setUserToolchainsSource(new StringSource(xml));

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
