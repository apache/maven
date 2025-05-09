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

import java.util.Collections;

import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultToolchainTest {
    private AutoCloseable mocks;
    private final Logger logger = mock(Logger.class);

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
    }

    private DefaultToolchain newDefaultToolchain(ToolchainModel model) {
        return new DefaultToolchain(model, logger) {
            @Override
            public String findTool(String toolName) {
                return null;
            }
        };
    }

    private DefaultToolchain newDefaultToolchain(ToolchainModel model, String type) {
        return new DefaultToolchain(model, type, logger) {
            @Override
            public String findTool(String toolName) {
                return null;
            }
        };
    }

    @Test
    void getModel() {
        ToolchainModel model = new ToolchainModel();
        DefaultToolchain toolchain = newDefaultToolchain(model);
        assertThat(toolchain.getModel()).isEqualTo(model);
    }

    @Test
    void getType() {
        ToolchainModel model = new ToolchainModel();
        DefaultToolchain toolchain = newDefaultToolchain(model, "TYPE");
        assertThat(toolchain.getType()).isEqualTo("TYPE");

        model.setType("MODEL_TYPE");
        toolchain = newDefaultToolchain(model);
        assertThat(toolchain.getType()).isEqualTo("MODEL_TYPE");
    }

    @Test
    void getLogger() {
        ToolchainModel model = new ToolchainModel();
        DefaultToolchain toolchain = newDefaultToolchain(model);
        assertThat(toolchain.getLog()).isEqualTo(logger);
    }

    @Test
    void missingRequirementProperty() {
        ToolchainModel model = new ToolchainModel();
        model.setType("TYPE");
        DefaultToolchain toolchain = newDefaultToolchain(model);

        assertThat(toolchain.matchesRequirements(Collections.singletonMap("name", "John Doe"))).isFalse();
        verify(logger).debug("Toolchain {} is missing required property: {}", toolchain, "name");
    }

    @Test
    void nonMatchingRequirementProperty() {
        ToolchainModel model = new ToolchainModel();
        model.setType("TYPE");
        DefaultToolchain toolchain = newDefaultToolchain(model);
        toolchain.addProvideToken("name", RequirementMatcherFactory.createExactMatcher("Jane Doe"));

        assertThat(toolchain.matchesRequirements(Collections.singletonMap("name", "John Doe"))).isFalse();
        verify(logger).debug("Toolchain {} doesn't match required property: {}", toolchain, "name");
    }

    @Test
    void equals() {
        ToolchainModel tm1 = new ToolchainModel();
        tm1.setType("jdk");
        tm1.addProvide("version", "1.5");
        tm1.addProvide("vendor", "sun");
        Xpp3Dom configuration1 = new Xpp3Dom("configuration");
        Xpp3Dom jdkHome1 = new Xpp3Dom("jdkHome");
        jdkHome1.setValue("${env.JAVA_HOME}");
        configuration1.addChild(jdkHome1);
        tm1.setConfiguration(configuration1);

        ToolchainModel tm2 = new ToolchainModel();
        tm1.setType("jdk");
        tm1.addProvide("version", "1.4");
        tm1.addProvide("vendor", "sun");
        Xpp3Dom configuration2 = new Xpp3Dom("configuration");
        Xpp3Dom jdkHome2 = new Xpp3Dom("jdkHome");
        jdkHome2.setValue("${env.JAVA_HOME}");
        configuration2.addChild(jdkHome2);
        tm2.setConfiguration(configuration2);

        DefaultToolchain tc1 = new DefaultJavaToolChain(tm1, null);
        DefaultToolchain tc2 = new DefaultJavaToolChain(tm2, null);

        assertThat(tc1).isEqualTo(tc1);
        assertThat(tc2).isNotEqualTo(tc1);
        assertThat(tc1).isNotEqualTo(tc2);
        assertThat(tc2).isEqualTo(tc2);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }
}
