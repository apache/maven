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
package org.apache.maven.toolchain.java;

import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class JavaToolchainFactoryTest {

    private JavaToolchainFactory factory;

    @BeforeEach
    void setup() {
        factory = new JavaToolchainFactory();
    }

    @Test
    void defaultToolchainShouldReturnEmpty() {
        assertNull(factory.createDefaultToolchain());
    }

    @Test
    void missingJdkHomeShouldThrowException() {
        ToolchainModel toolchainModel = new ToolchainModel();
        MisconfiguredToolchainException exception = Assertions.assertThrows(
                MisconfiguredToolchainException.class, () -> factory.createToolchain(toolchainModel));
        assertEquals("Java toolchain without the jdkHome configuration element.", exception.getMessage());
    }

    @Test
    void nonExistingJdkHomeShouldThrowException() {
        ToolchainModel toolchainModel = new ToolchainModel();
        Xpp3Dom jdkHome = new Xpp3Dom("jdkHome");
        jdkHome.setValue("/not-exist/jdk/home");
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        configuration.addChild(jdkHome);
        toolchainModel.setConfiguration(configuration);

        MisconfiguredToolchainException exception = Assertions.assertThrows(
                MisconfiguredToolchainException.class, () -> factory.createToolchain(toolchainModel));
        assertTrue(
                exception.getMessage().contains("Non-existing JDK home configuration at"),
                "Not expected exception message: '" + exception.getMessage());
        assertTrue(
                exception.getMessage().contains("not-exist"),
                "Not expected exception message: '" + exception.getMessage() + "', should contain: 'not-exist'");
    }

    @Test
    void properToolchainShouldReturnJavaToolchain() throws Exception {
        String javaHome = System.getProperty("java.home");
        String javaVersion = System.getProperty("java.version");

        ToolchainModel toolchainModel = new ToolchainModel();
        Xpp3Dom jdkHome = new Xpp3Dom("jdkHome");
        jdkHome.setValue(javaHome);
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        configuration.addChild(jdkHome);
        toolchainModel.setConfiguration(configuration);

        toolchainModel.addProvide("version", javaVersion);

        JavaToolchain toolchain = (JavaToolchain) factory.createToolchain(toolchainModel);
        assertNotNull(toolchain);
        assertEquals(javaHome, toolchain.getJavaHome());
        assertEquals(javaVersion, toolchain.getJavaVersion().toString());
    }
}
