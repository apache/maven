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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Benjamin Bentmann
 */
public class DefaultSettingsBuilderFactoryTest {

    private File getSettings(String name) {
        return new File("src/test/resources/settings/factory/" + name + ".xml").getAbsoluteFile();
    }

    private SettingsBuildingResult execute(String settingsName) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user.home", "/home/user");

        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertThat(builder).isNotNull();

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setSystemProperties(properties);
        request.setUserSettingsFile(getSettings(settingsName));

        SettingsBuildingResult result = builder.build(request);
        return assertThat(result).isNotNull().actual();
    }

    @Test
    public void testCompleteWiring() throws Exception {
        Settings settings = assertThat(execute("simple"))
                .extracting(SettingsBuildingResult::getEffectiveSettings)
                .actual();

        assertThat(settings.getLocalRepository())
                .satisfiesAnyOf(
                        repo -> assertThat(repo).isEqualTo("/home/user/.m2/repository"),
                        repo -> assertThat(repo).endsWith("\\home\\user\\.m2\\repository"));
    }

    @Test
    public void testSettingsWithServers() throws Exception {
        Settings settings = assertThat(execute("settings-servers-1"))
                .extracting(SettingsBuildingResult::getEffectiveSettings)
                .actual();

        assertThat(settings.getServers())
                .hasSize(2)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        aServer("server-1", "username1", "password1"), aServer("server-2", "username2", "password2"));
    }

    @Test
    public void testSettingsWithServersAndAliases() throws Exception {
        Settings settings = assertThat(execute("settings-servers-2"))
                .extracting(SettingsBuildingResult::getEffectiveSettings)
                .actual();

        assertThat(settings.getServers())
                .hasSize(6)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(
                        aServer("server-1", "username1", "password1", Arrays.asList("server-11", "server-12")),
                        aServer("server-11", "username1", "password1"),
                        aServer("server-12", "username1", "password1"),
                        aServer("server-2", "username2", "password2", Collections.singletonList("server-21")),
                        aServer("server-21", "username2", "password2"),
                        aServer("server-3", "username3", "password3"));
    }

    private Server aServer(String id, String username, String password) {
        Server server = new Server();
        server.setId(id);
        server.setUsername(username);
        server.setPassword(password);
        return server;
    }

    private Server aServer(String id, String username, String password, List<String> ids) {
        Server server = aServer(id, username, password);
        server.setIds(ids);
        return server;
    }

    @Test
    public void testSettingsWithDuplicateServersIds() throws Exception {
        SettingsBuildingResult result = execute("settings-servers-3");

        assertThat(result.getProblems())
                .hasSize(1)
                .extracting(SettingsProblem::getMessage)
                .containsExactly("'servers.server.id' must be unique but found duplicate server with id server-2");
    }
}
