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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Session;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ProblemCollector;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.services.SettingsBuilderRequest;
import org.apache.maven.api.services.SettingsBuilderResult;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
@ExtendWith(MockitoExtension.class)
class DefaultSettingsBuilderFactoryTest {

    @Mock
    Session session;

    @BeforeEach
    void setup() {
        Mockito.lenient()
                .when(session.getService(SettingsXmlFactory.class))
                .thenReturn(new DefaultSettingsXmlFactory());
    }

    SettingsBuilderResult execute(String settingsName) {
        SettingsBuilder builder =
                new DefaultSettingsBuilder(new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of());
        assertNotNull(builder);

        SettingsBuilderRequest request = SettingsBuilderRequest.builder()
                .session(session)
                .userSettingsSource(Sources.buildSource(getSettings(settingsName)))
                .build();

        SettingsBuilderResult result = builder.build(request);
        assertNotNull(result);
        return result;
    }

    @Test
    void testCompleteWiring() {
        Settings settings = execute("settings-simple").getEffectiveSettings();

        String localRepository = settings.getLocalRepository();
        assertTrue(localRepository.equals("${user.home}/.m2/repository")
                || localRepository.endsWith("\\${user.home}\\.m2\\repository"));
    }

    @Test
    void testSettingsWithServers() {
        Settings settings = execute("settings-servers-1").getEffectiveSettings();

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
    void testSettingsWithServersAndAliases() {
        Settings settings = execute("settings-servers-2").getEffectiveSettings();

        assertEquals("${user.home}/.m2/repository", settings.getLocalRepository());

        List<Server> servers = settings.getServers();
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
        SettingsBuilderResult result = execute("settings-servers-3");

        ProblemCollector<BuilderProblem> problems = result.getProblems();
        assertEquals(1, problems.problems().count());
        assertEquals(
                "'servers.server.id' must be unique but found duplicate server with id server-2",
                problems.problems().findFirst().orElseThrow().getMessage());
    }

    private Path getSettings(String name) {
        return Paths.get("src/test/resources/settings/" + name + ".xml").toAbsolutePath();
    }
}
