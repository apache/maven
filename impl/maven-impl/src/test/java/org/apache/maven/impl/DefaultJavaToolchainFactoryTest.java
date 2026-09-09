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
package org.apache.maven.impl;

import java.util.List;
import java.util.Map;

import org.apache.maven.api.JavaToolchain;
import org.apache.maven.api.services.ToolchainFactoryException;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.toolchain.ToolchainModel;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.impl.resolver.MavenVersionScheme;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultJavaToolchainFactoryTest {

    private DefaultJavaToolchainFactory factory;

    @BeforeEach
    void setUp() {
        VersionParser versionParser = new DefaultVersionParser(new DefaultModelVersionParser(new MavenVersionScheme()));
        factory = new DefaultJavaToolchainFactory(versionParser);
    }

    @Test
    void defaultToolchainShouldReturnEmpty() {
        assertTrue(factory.createDefaultToolchain().isEmpty());
    }

    @Test
    void missingJdkHomeShouldThrowException() {
        ToolchainModel toolchainModel = ToolchainModel.newBuilder().build();
        ToolchainFactoryException exception =
                Assertions.assertThrows(ToolchainFactoryException.class, () -> factory.createToolchain(toolchainModel));
        assertEquals("Java toolchain without the jdkHome configuration element.", exception.getMessage());
    }

    @Test
    void nonExistingJdkHomeShouldThrowException() {
        ToolchainModel toolchainModel = ToolchainModel.newBuilder()
                .configuration(XmlNode.newBuilder()
                        .name("configuration")
                        .children(List.of(XmlNode.newInstance("jdkHome", "/not-exist/jdk/home")))
                        .build())
                .build();

        ToolchainFactoryException exception =
                Assertions.assertThrows(ToolchainFactoryException.class, () -> factory.createToolchain(toolchainModel));
        assertTrue(
                exception.getMessage().contains("Non-existing JDK home configuration at"),
                "Not expected exception message: '" + exception.getMessage());
        assertTrue(
                exception.getMessage().contains("not-exist"),
                "Not expected exception message: '" + exception.getMessage() + "', should contain: 'not-exist'");
    }

    @Test
    void properToolchainShouldReturnJavaToolchain() {
        String javaHome = System.getProperty("java.home");
        String javaVersion = System.getProperty("java.version");
        ToolchainModel toolchainModel = ToolchainModel.newBuilder()
                .provides(Map.of("version", javaVersion))
                .configuration(XmlNode.newBuilder()
                        .name("configuration")
                        .children(List.of(XmlNode.newInstance("jdkHome", javaHome)))
                        .build())
                .build();

        JavaToolchain toolchain = factory.createToolchain(toolchainModel);
        assertNotNull(toolchain);
        assertEquals(javaHome, toolchain.getJavaHome());
        assertEquals(javaVersion, toolchain.getJavaVersion().toString());
    }
}
