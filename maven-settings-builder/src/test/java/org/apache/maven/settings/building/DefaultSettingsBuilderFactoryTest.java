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
package org.apache.maven.settings.building;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Benjamin Bentmann
 */
public class DefaultSettingsBuilderFactoryTest {

    private File getSettings(String name) {
        return new File("src/test/resources/settings/factory/" + name + ".xml").getAbsoluteFile();
    }

    @Test
    public void testCompleteWiring() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user.home", "/home/user");

        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setSystemProperties(properties);
        request.setUserSettingsFile(getSettings("simple"));

        SettingsBuildingResult result = builder.build(request);
        assertNotNull(result);
        Settings settings = result.getEffectiveSettings();
        assertNotNull(settings);

        String localRepository = settings.getLocalRepository();
        assertTrue(localRepository.equals("/home/user/.m2/repository")
                || localRepository.endsWith("\\home\\user\\.m2\\repository"));
    }

    @Test
    public void testSettingsWithServers() throws Exception {
        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setSystemProperties(System.getProperties());
        request.setUserSettingsFile(getSettings("settings-servers-1"));

        Settings settings = builder.build(request).getEffectiveSettings();

        List<Server> servers = settings.getServers();
        assertEquals(2, servers.size());

        Server server1 = getServerById(servers, "server-1");
        assertEquals("username1", server1.getUsername());
        assertEquals("password1", server1.getPassword());

        Server server2 = getServerById(servers, "server-2");
        assertEquals("username2", server2.getUsername());
        assertEquals("password2", server2.getPassword());
    }

    @Test
    public void testSettingsWithServersAndAliases() throws Exception {
        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setSystemProperties(System.getProperties());
        request.setUserSettingsFile(getSettings("settings-servers-2"));

        Settings settings = builder.build(request).getEffectiveSettings();

        List<Server> servers = settings.getServers();
        assertEquals(6, servers.size());

        Server server1 = getServerById(servers, "server-1");
        assertEquals("username1", server1.getUsername());
        assertEquals("password1", server1.getPassword());
        assertEquals(Arrays.asList("server-11", "server-12"), server1.getAliases());

        Server server11 = getServerById(servers, "server-11");
        assertEquals("username1", server11.getUsername());
        assertEquals("password1", server11.getPassword());
        assertTrue(server11.getAliases().isEmpty());

        Server server12 = getServerById(servers, "server-12");
        assertEquals("username1", server12.getUsername());
        assertEquals("password1", server12.getPassword());
        assertTrue(server12.getAliases().isEmpty());

        Server server2 = getServerById(servers, "server-2");
        assertEquals("username2", server2.getUsername());
        assertEquals("password2", server2.getPassword());
        assertEquals(Arrays.asList("server-21"), server2.getAliases());

        Server server21 = getServerById(servers, "server-21");
        assertEquals("username2", server21.getUsername());
        assertEquals("password2", server21.getPassword());
        assertTrue(server21.getAliases().isEmpty());

        Server server3 = getServerById(servers, "server-3");
        assertEquals("username3", server3.getUsername());
        assertEquals("password3", server3.getPassword());
        assertTrue(server3.getAliases().isEmpty());
    }

    private Server getServerById(List<Server> servers, String id) {
        return servers.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Server with id " + id + " not found on list: " + servers));
    }

    @Test
    public void testSettingsWithDuplicateServersIds() throws Exception {
        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setSystemProperties(System.getProperties());
        request.setUserSettingsFile(getSettings("settings-servers-3"));

        SettingsBuildingResult result = builder.build(request);

        List<SettingsProblem> problems = result.getProblems();
        assertEquals(1, problems.size());
        assertEquals(
                "'servers.server[0].aliases[0]' for server-1 must be unique across all server ids and aliases but found duplicate alias server-2",
                problems.get(0).getMessage());
    }

    @Test
    public void testNonStringInterpolationHappyPath() throws Exception {
        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull(builder);

        boolean testActive = true;
        int testPort = 2026;
        Properties userProperties = new Properties();
        userProperties.setProperty("test.active", Boolean.toString(testActive));
        userProperties.setProperty("test.port", Integer.toString(testPort));
        userProperties.setProperty("maven.settings.strictParsing", Boolean.TRUE.toString());

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setUserProperties(userProperties);
        request.setSystemProperties(System.getProperties());
        request.setUserSettingsFile(getSettings("proxy"));

        SettingsBuildingResult result = builder.build(request);
        assertNotNull(result);
        assertNotNull(result.getEffectiveSettings());
        assertEquals(
                testActive, result.getEffectiveSettings().getProxies().get(0).isActive());
        assertEquals(testPort, result.getEffectiveSettings().getProxies().get(0).getPort());
    }

    @Test
    public void testNonStringInterpolationNonHappyPath() {
        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull(builder);

        Properties userProperties = new Properties();
        userProperties.setProperty("test.active", "yes");
        userProperties.setProperty("test.port", "foo");
        userProperties.setProperty("maven.settings.strictParsing", Boolean.TRUE.toString());

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setUserProperties(userProperties);
        request.setSystemProperties(System.getProperties());
        request.setUserSettingsFile(getSettings("proxy"));

        assertThrows(SettingsBuildingException.class, () -> builder.build(request));
    }

    @Test
    public void testNonStringInterpolationMissingProperties() {
        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull(builder);

        Properties userProperties = new Properties();
        userProperties.setProperty("maven.settings.strictParsing", Boolean.TRUE.toString());

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setUserProperties(userProperties);
        request.setSystemProperties(System.getProperties());
        request.setUserSettingsFile(getSettings("proxy"));

        assertThrows(SettingsBuildingException.class, () -> builder.build(request));
    }
}
