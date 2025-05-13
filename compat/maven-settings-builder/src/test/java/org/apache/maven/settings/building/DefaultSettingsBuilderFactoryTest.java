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
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
class DefaultSettingsBuilderFactoryTest {

    private File getSettings(String name) {
        return new File("src/test/resources/settings/factory/" + name + ".xml").getAbsoluteFile();
    }

    private org.apache.maven.settings.Server asServer(Server delegate) {
        return new org.apache.maven.settings.Server(delegate);
    }

    SettingsBuildingResult execute(String settingsName) throws Exception {
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
    void testCompleteWiring() throws Exception {
        Settings settings = assertThat(execute("simple"))
                .extracting(SettingsBuildingResult::getEffectiveSettings)
                .actual();

        assertThat(settings.getLocalRepository())
                .satisfiesAnyOf(
                        repo -> assertThat(repo).isEqualTo("/home/user/.m2/repository"),
                        repo -> assertThat(repo).endsWith("\\home\\user\\.m2\\repository"));
    }

    @Test
    void testSettingsWithServers() throws Exception {
        Settings settings = assertThat(execute("settings-servers-1"))
                .extracting(SettingsBuildingResult::getEffectiveSettings)
                .actual();

        assertThat(settings.getDelegate().getServers())
                .hasSize(2)
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoredFields("locations")
                        .build())
                .containsExactlyInAnyOrder(
                        Server.newBuilder()
                                .id("server-1")
                                .username("username1")
                                .password("password1")
                                .build(),
                        Server.newBuilder()
                                .id("server-2")
                                .username("username2")
                                .password("password2")
                                .build());
    }

    @Test
    void testSettingsWithServersAndAliases() throws Exception {
        Settings settings = assertThat(execute("settings-servers-2"))
                .extracting(SettingsBuildingResult::getEffectiveSettings)
                .actual();

        assertThat(settings.getDelegate().getServers())
                .hasSize(6)
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoredFields("locations")
                        .build())
                .containsExactlyInAnyOrder(
                        Server.newBuilder()
                                .id("server-1")
                                .username("username1")
                                .password("password1")
                                .ids(List.of("server-11", "server-12"))
                                .build(),
                        Server.newBuilder()
                                .id("server-11")
                                .username("username1")
                                .password("password1")
                                .build(),
                        Server.newBuilder()
                                .id("server-12")
                                .username("username1")
                                .password("password1")
                                .build(),
                        Server.newBuilder()
                                .id("server-2")
                                .username("username2")
                                .password("password2")
                                .ids(List.of("server-21"))
                                .build(),
                        Server.newBuilder()
                                .id("server-21")
                                .username("username2")
                                .password("password2")
                                .build(),
                        Server.newBuilder()
                                .id("server-3")
                                .username("username3")
                                .password("password3")
                                .build());
    }

    @Test
    void testSettingsWithDuplicateServersIds() throws Exception {
        SettingsBuildingResult result = execute("settings-servers-3");

        assertThat(result.getProblems())
                .hasSize(1)
                .extracting(SettingsProblem::getMessage)
                .containsExactly("'servers.server.id' must be unique but found duplicate server with id server-2");
    }
}
