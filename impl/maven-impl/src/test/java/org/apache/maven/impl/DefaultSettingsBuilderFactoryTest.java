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
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.services.SettingsBuilderRequest;
import org.apache.maven.api.services.SettingsBuilderResult;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(builder).isNotNull();

        SettingsBuilderRequest request = SettingsBuilderRequest.builder()
                .session(session)
                .userSettingsSource(Sources.buildSource(getSettings(settingsName)))
                .build();

        SettingsBuilderResult result = builder.build(request);
        return assertThat(result).isNotNull().actual();
    }

    @Test
    void testCompleteWiring() {
        Settings settings = assertThat(execute("settings-simple"))
                .extracting(SettingsBuilderResult::getEffectiveSettings)
                .actual();

        assertThat(settings.getLocalRepository())
                .satisfiesAnyOf(
                        repo -> assertThat(repo).isEqualTo("${user.home}/.m2/repository"),
                        repo -> assertThat(repo).endsWith("\\${user.home}\\.m2\\repository"));
    }

    @Test
    void testSettingsWithServers() {
        Settings settings = assertThat(execute("settings-servers-1"))
                .extracting(SettingsBuilderResult::getEffectiveSettings)
                .actual();

        assertThat(settings.getServers())
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
    void testSettingsWithServersAndAliases() {
        Settings settings = assertThat(execute("settings-servers-2"))
                .extracting(SettingsBuilderResult::getEffectiveSettings)
                .actual();

        assertThat(settings.getLocalRepository()).isEqualTo("${user.home}/.m2/repository");

        assertThat(settings.getServers())
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
        SettingsBuilderResult result = execute("settings-servers-3");

        assertThat(result.getProblems().problems())
                .hasSize(1)
                .extracting(BuilderProblem::getMessage)
                .containsExactly("'servers.server.id' must be unique but found duplicate server with id server-2");
    }

    private Path getSettings(String name) {
        return Paths.get("src/test/resources/settings/" + name + ".xml").toAbsolutePath();
    }
}
