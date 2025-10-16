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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.api.toolchain.PersistedToolchains;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that all XML factories properly use the transformer from XmlReaderRequest.
 */
class XmlFactoryTransformerTest {

    @Test
    void testModelXmlFactoryUsesTransformer() throws Exception {
        // Create a test transformer that tracks what contexts are called
        List<String> calledContexts = new ArrayList<>();
        XmlReaderRequest.Transformer trackingTransformer = (value, context) -> {
            calledContexts.add(context);
            return value;
        };

        String pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                <packaging>jar</packaging>
            </project>
            """;

        DefaultModelXmlFactory factory = new DefaultModelXmlFactory();
        XmlReaderRequest request = XmlReaderRequest.builder()
                .reader(new StringReader(pomXml))
                .transformer(trackingTransformer)
                .build();

        Model model = factory.read(request);

        // Verify the model was parsed correctly
        assertEquals("com.example", model.getGroupId());
        assertEquals("test-project", model.getArtifactId());
        assertEquals("1.0.0", model.getVersion());
        assertEquals("jar", model.getPackaging());

        // Verify that the transformer was called
        assertFalse(calledContexts.isEmpty(), "Transformer should have been called");
        assertTrue(calledContexts.contains("groupId"), "groupId context should be called");
        assertTrue(calledContexts.contains("artifactId"), "artifactId context should be called");
        assertTrue(calledContexts.contains("version"), "version context should be called");
        assertTrue(calledContexts.contains("packaging"), "packaging context should be called");
    }

    @Test
    void testSettingsXmlFactoryUsesTransformer() throws Exception {
        // Create a test transformer that tracks what contexts are called
        List<String> calledContexts = new ArrayList<>();
        XmlReaderRequest.Transformer trackingTransformer = (value, context) -> {
            calledContexts.add(context);
            return value;
        };

        String settingsXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <settings xmlns="http://maven.apache.org/SETTINGS/1.2.0">
                <localRepository>/path/to/local/repo</localRepository>
                <servers>
                    <server>
                        <id>test-server</id>
                        <username>testuser</username>
                        <password>testpass</password>
                    </server>
                </servers>
            </settings>
            """;

        DefaultSettingsXmlFactory factory = new DefaultSettingsXmlFactory();
        XmlReaderRequest request = XmlReaderRequest.builder()
                .reader(new StringReader(settingsXml))
                .transformer(trackingTransformer)
                .build();

        Settings settings = factory.read(request);

        // Verify the settings were parsed correctly
        assertEquals("/path/to/local/repo", settings.getLocalRepository());
        assertEquals(1, settings.getServers().size());
        assertEquals("test-server", settings.getServers().get(0).getId());
        assertEquals("testuser", settings.getServers().get(0).getUsername());
        assertEquals("testpass", settings.getServers().get(0).getPassword());

        // Verify that the transformer was called
        assertFalse(calledContexts.isEmpty(), "Transformer should have been called");
        assertTrue(calledContexts.contains("localRepository"), "localRepository context should be called");
        assertTrue(calledContexts.contains("id"), "id context should be called");
        assertTrue(calledContexts.contains("username"), "username context should be called");
        assertTrue(calledContexts.contains("password"), "password context should be called");
    }

    @Test
    void testToolchainsXmlFactoryUsesTransformer() throws Exception {
        // Create a test transformer that tracks what contexts are called
        List<String> calledContexts = new ArrayList<>();
        XmlReaderRequest.Transformer trackingTransformer = (value, context) -> {
            calledContexts.add(context);
            return value;
        };

        String toolchainsXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0">
                <toolchain>
                    <type>jdk</type>
                    <provides>
                        <version>17</version>
                        <vendor>openjdk</vendor>
                    </provides>
                    <configuration>
                        <jdkHome>/path/to/jdk17</jdkHome>
                    </configuration>
                </toolchain>
            </toolchains>
            """;

        DefaultToolchainsXmlFactory factory = new DefaultToolchainsXmlFactory();
        XmlReaderRequest request = XmlReaderRequest.builder()
                .reader(new StringReader(toolchainsXml))
                .transformer(trackingTransformer)
                .build();

        PersistedToolchains toolchains = factory.read(request);

        // Verify the toolchains were parsed correctly
        assertEquals(1, toolchains.getToolchains().size());
        assertEquals("jdk", toolchains.getToolchains().get(0).getType());
        assertEquals("17", toolchains.getToolchains().get(0).getProvides().get("version"));
        assertEquals("openjdk", toolchains.getToolchains().get(0).getProvides().get("vendor"));
        assertEquals(
                "/path/to/jdk17",
                toolchains
                        .getToolchains()
                        .get(0)
                        .getConfiguration()
                        .child("jdkHome")
                        .value());

        // Verify that the transformer was called
        assertFalse(calledContexts.isEmpty(), "Transformer should have been called");
        assertTrue(calledContexts.contains("type"), "type context should be called");

        // Note: The provides and configuration sections are parsed as Maps/DOM,
        // so individual elements like "version", "vendor", "jdkHome" may not
        // trigger the transformer directly. The important thing is that the
        // transformer is being used by the factory.
    }

    @Test
    void testPluginXmlFactoryUsesTransformer() throws Exception {
        // Create a test transformer that tracks what contexts are called
        List<String> calledContexts = new ArrayList<>();
        XmlReaderRequest.Transformer trackingTransformer = (value, context) -> {
            calledContexts.add(context);
            return value;
        };

        String pluginXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <plugin>
                <name>test-plugin</name>
                <groupId>com.example</groupId>
                <artifactId>test-maven-plugin</artifactId>
                <version>1.0.0</version>
                <goalPrefix>test</goalPrefix>
                <mojos>
                    <mojo>
                        <goal>compile</goal>
                        <phase>compile</phase>
                        <implementation>com.example.TestMojo</implementation>
                    </mojo>
                </mojos>
            </plugin>
            """;

        DefaultPluginXmlFactory factory = new DefaultPluginXmlFactory();
        XmlReaderRequest request = XmlReaderRequest.builder()
                .reader(new StringReader(pluginXml))
                .transformer(trackingTransformer)
                .build();

        PluginDescriptor plugin = factory.read(request);

        // Verify the plugin was parsed correctly
        assertEquals("test-plugin", plugin.getName());
        assertEquals("com.example", plugin.getGroupId());
        assertEquals("test-maven-plugin", plugin.getArtifactId());
        assertEquals("1.0.0", plugin.getVersion());
        assertEquals("test", plugin.getGoalPrefix());
        assertEquals(1, plugin.getMojos().size());
        assertEquals("compile", plugin.getMojos().get(0).getGoal());
        assertEquals("compile", plugin.getMojos().get(0).getPhase());
        assertEquals("com.example.TestMojo", plugin.getMojos().get(0).getImplementation());

        // Verify that the transformer was called
        assertFalse(calledContexts.isEmpty(), "Transformer should have been called");
        assertTrue(calledContexts.contains("name"), "name context should be called");
        assertTrue(calledContexts.contains("groupId"), "groupId context should be called");
        assertTrue(calledContexts.contains("artifactId"), "artifactId context should be called");
        assertTrue(calledContexts.contains("version"), "version context should be called");
        assertTrue(calledContexts.contains("goal"), "goal context should be called");
        assertTrue(calledContexts.contains("phase"), "phase context should be called");
        assertTrue(calledContexts.contains("implementation"), "implementation context should be called");
    }
}
