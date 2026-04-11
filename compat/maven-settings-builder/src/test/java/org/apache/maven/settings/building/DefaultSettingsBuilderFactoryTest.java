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
import java.util.List;
import java.util.Properties;

import org.apache.maven.api.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
class DefaultSettingsBuilderFactoryTest {

    private File getSettings(String name) {
        return new File("src/test/resources/settings/factory/" + name + ".xml").getAbsoluteFile();
    }

    SettingsBuildingResult execute(String settingsName) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user.home", "/home/user");

        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setSystemProperties(properties);
        request.setUserSettingsFile(getSettings(settingsName));

        SettingsBuildingResult result = builder.build(request);
        assertNotNull(result);
        return result;
    }

    @Test
    void testCompleteWiring() throws Exception {
        Settings settings = execute("simple").getEffectiveSettings();

        String localRepository = settings.getLocalRepository();
        assertTrue(localRepository.equals("/home/user/.m2/repository")
                || localRepository.endsWith("\\home\\user\\.m2\\repository"));
    }

    @Test
    void testSettingsWithServers() throws Exception {
        Settings settings = execute("settings-servers-1").getEffectiveSettings();

        List<Server> servers = settings.getDelegate().getServers();
        assertEquals(2, servers.size());

        Server server1 = getServerById(servers, "server-1");
        assertEquals("username1", server1.getUsername());
        assertEquals("password1", server1.getPassword());

        Server server2 = getServerById(servers, "server-2");
        assertEquals("username2", server2.getUsername());
        assertEquals("password2", server2.getPassword());
    }

    @Test
    void testSettingsWithServersAndAliases() throws Exception {
        Settings settings = execute("settings-servers-2").getEffectiveSettings();

        List<Server> servers = settings.getDelegate().getServers();
        assertEquals(6, servers.size());

        Server server1 = getServerById(servers, "server-1");
        assertEquals("username1", server1.getUsername());
        assertEquals("password1", server1.getPassword());
        assertEquals(List.of("server-11", "server-12"), server1.getAliases());

        Server server11 = getServerById(servers, "server-11");
        assertEquals("username1", server11.getUsername());
        assertEquals("password1", server11.getPassword());
        assertTrue(server11.getAliases().isEmpty());

        Server server12 = getServerById(servers, "server-12");
        assertEquals("username1", server12.getUsername());
        assertEquals("password1", server12.getPassword());
        assertTrue(server11.getAliases().isEmpty());

        Server server2 = getServerById(servers, "server-2");
        assertEquals("username2", server2.getUsername());
        assertEquals("password2", server2.getPassword());
        assertEquals(List.of("server-21"), server2.getAliases());

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
    void testSettingsWithDuplicateServersIds() throws Exception {
        SettingsBuildingResult result = execute("settings-servers-3");

        List<SettingsProblem> problems = result.getProblems();
        assertEquals(1, problems.size());
        assertEquals(
                "'servers.server.id' must be unique but found duplicate server with id server-2",
                problems.get(0).getMessage());
    }
}
