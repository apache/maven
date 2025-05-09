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

import static org.assertj.core.api.Assertions.assertThat;
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
    void buildEmptyRequest() throws Exception {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        ToolchainsBuildingResult result = toolchainBuilder.build(request);
        assertThat(result.getEffectiveToolchains()).isNotNull();
        assertThat(result.getProblems()).isNotNull();
        assertThat(result.getProblems().size()).isEqualTo(0);
    }

    @Test
    void buildRequestWithUserToolchains() throws Exception {
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
        assertThat(result.getEffectiveToolchains()).isNotNull();
        assertThat(result.getEffectiveToolchains().getToolchains().size()).isEqualTo(1);
        assertThat(result.getEffectiveToolchains().getToolchains().get(0).getType()).isEqualTo("TYPE");
        assertThat(result.getEffectiveToolchains()
                .getToolchains()
                .get(0)
                .getProvides()
                .get("key")).isEqualTo("user_value");
        assertThat(result.getProblems()).isNotNull();
        assertThat(result.getProblems().size()).isEqualTo(0);
    }

    @Test
    void buildRequestWithGlobalToolchains() throws Exception {
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
        assertThat(result.getEffectiveToolchains()).isNotNull();
        assertThat(result.getEffectiveToolchains().getToolchains().size()).isEqualTo(1);
        assertThat(result.getEffectiveToolchains().getToolchains().get(0).getType()).isEqualTo("TYPE");
        assertThat(result.getEffectiveToolchains()
                .getToolchains()
                .get(0)
                .getProvides()
                .get("key")).isEqualTo("global_value");
        assertThat(result.getProblems()).isNotNull();
        assertThat(result.getProblems().size()).isEqualTo(0);
    }

    @Test
    void buildRequestWithBothToolchains() throws Exception {
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
        assertThat(result.getEffectiveToolchains()).isNotNull();
        assertThat(result.getEffectiveToolchains().getToolchains().size()).isEqualTo(2);
        assertThat(result.getEffectiveToolchains().getToolchains().get(0).getType()).isEqualTo("TYPE");
        assertThat(result.getEffectiveToolchains()
                .getToolchains()
                .get(0)
                .getProvides()
                .get("key")).isEqualTo("user_value");
        assertThat(result.getEffectiveToolchains().getToolchains().get(1).getType()).isEqualTo("TYPE");
        assertThat(result.getEffectiveToolchains()
                .getToolchains()
                .get(1)
                .getProvides()
                .get("key")).isEqualTo("global_value");
        assertThat(result.getProblems()).isNotNull();
        assertThat(result.getProblems().size()).isEqualTo(0);
    }

    @Test
    void strictToolchainsParseException() throws Exception {
        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setGlobalToolchainsSource(new StringSource(""));
        ToolchainsParseException parseException = new ToolchainsParseException("MESSAGE", 4, 2);
        doThrow(parseException).when(toolchainsReader).read(any(InputStream.class), ArgumentMatchers.anyMap());

        try {
            toolchainBuilder.build(request);
        } catch (ToolchainsBuildingException e) {
            assertThat(e.getMessage()).isEqualTo("1 problem was encountered while building the effective toolchains" + LS
                    + "[FATAL] Non-parseable toolchains (memory): MESSAGE @ line 4, column 2" + LS);
        }
    }

    @Test
    void iOException() throws Exception {
        Source src = mock(Source.class);
        IOException ioException = new IOException("MESSAGE");
        doThrow(ioException).when(src).getInputStream();
        doReturn("LOCATION").when(src).getLocation();

        ToolchainsBuildingRequest request = new DefaultToolchainsBuildingRequest();
        request.setGlobalToolchainsSource(src);

        try {
            toolchainBuilder.build(request);
        } catch (ToolchainsBuildingException e) {
            assertThat(e.getMessage()).isEqualTo("1 problem was encountered while building the effective toolchains" + LS
                    + "[FATAL] Non-readable toolchains LOCATION: MESSAGE" + LS);
        }
    }

    @Test
    void environmentVariablesAreInterpolated() throws Exception {
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
        assertThat(result.getEffectiveToolchains()
                .getToolchains()
                .get(0)
                .getProvides()
                .get("key")).isEqualTo(interpolatedValue);
        org.codehaus.plexus.util.xml.Xpp3Dom toolchainConfiguration = (org.codehaus.plexus.util.xml.Xpp3Dom)
                result.getEffectiveToolchains().getToolchains().get(0).getConfiguration();
        assertThat(toolchainConfiguration.getChild("jdkHome").getValue()).isEqualTo(interpolatedValue);
        assertThat(result.getProblems()).isNotNull();
        assertThat(result.getProblems().size()).isEqualTo(0);
    }

    @Test
    void nonExistingEnvironmentVariablesAreNotInterpolated() throws Exception {
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
        assertThat(result.getEffectiveToolchains()
                .getToolchains()
                .get(0)
                .getProvides()
                .get("key")).isEqualTo("${env.testNonExistingKey}");
        assertThat(result.getProblems()).isNotNull();
        assertThat(result.getProblems().size()).isEqualTo(0);
    }

    @Test
    void environmentVariablesWithSpecialCharactersAreInterpolated() throws Exception {
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
        assertThat(result.getEffectiveToolchains()
                .getToolchains()
                .get(0)
                .getProvides()
                .get("key")).isEqualTo(interpolatedValue);
        assertThat(result.getProblems()).isNotNull();
        assertThat(result.getProblems().size()).isEqualTo(0);
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
