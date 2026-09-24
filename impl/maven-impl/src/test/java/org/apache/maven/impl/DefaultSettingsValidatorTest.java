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

import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ProblemCollector;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.settings.Profile;
import org.apache.maven.api.settings.Repository;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultSettingsValidatorTest {

    private SettingsBuilder validator;

    @BeforeEach
    void setUp() throws Exception {
        validator = new DefaultSettingsBuilder(new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of());
    }

    @Test
    void testValidate() {
        Profile prof = Profile.newBuilder().id("xxx").build();
        Settings model = Settings.newBuilder().profiles(List.of(prof)).build();
        ProblemCollector<BuilderProblem> problems = validator.validate(model);
        assertEquals(0, problems.totalProblemsReported());

        Repository repo = org.apache.maven.api.settings.Repository.newInstance(false);
        Settings model2 = Settings.newBuilder()
                .profiles(List.of(prof.withRepositories(List.of(repo))))
                .build();
        problems = validator.validate(model2);
        assertEquals(2, problems.totalProblemsReported());

        repo = repo.withUrl("http://xxx.xxx.com");
        model2 = Settings.newBuilder()
                .profiles(List.of(prof.withRepositories(List.of(repo))))
                .build();
        problems = validator.validate(model2);
        assertEquals(1, problems.totalProblemsReported());

        repo = repo.withId("xxx");
        model2 = Settings.newBuilder()
                .profiles(List.of(prof.withRepositories(List.of(repo))))
                .build();
        problems = validator.validate(model2);
        assertEquals(0, problems.totalProblemsReported());
    }

    @Test
    void testValidateServerIdAlias() {
        Server server =
                Server.newBuilder().id("server-1").aliases(List.of("server-1")).build();

        Settings settings = Settings.newBuilder().servers(List.of(server)).build();

        ProblemCollector<BuilderProblem> problems = validator.validate(settings);
        assertEquals(1, problems.totalProblemsReported());
        assertEquals(
                "'servers.server[0].aliases[0]' for server-1 must be unique across all server ids and aliases but found duplicate alias server-1",
                problems.problems().findFirst().orElseThrow().getMessage());
    }

    @Test
    void testMultipleUsageOfAliases() {

        Server server1 =
                Server.newBuilder().id("server-1").aliases(List.of("alias-1")).build();

        Server server2 =
                Server.newBuilder().id("server-2").aliases(List.of("alias-1")).build();

        Settings settings =
                Settings.newBuilder().servers(List.of(server1, server2)).build();

        ProblemCollector<BuilderProblem> problems = validator.validate(settings);
        assertEquals(1, problems.totalProblemsReported());
        assertEquals(
                "'servers.server[1].aliases[0]' for server-2 must be unique across all server ids and aliases but found duplicate alias alias-1",
                problems.problems().findFirst().orElseThrow().getMessage());
    }

    @Test
    void testValidateServerIdAliasesWithEmptyValue() {
        Server server = Server.newBuilder().id("server-1").aliases(List.of("")).build();

        Settings settings = Settings.newBuilder().servers(List.of(server)).build();

        ProblemCollector<BuilderProblem> problems = validator.validate(settings);
        assertEquals(1, problems.totalProblemsReported());
        assertEquals(
                "'servers.server[0].aliases[0]' for server-1 is missing",
                problems.problems().findFirst().orElseThrow().getMessage());
    }

    @Test
    void testValidateServerRepositoryOrigins() {
        Server server = Server.newBuilder()
                .id("server-1")
                .repositoryOrigins(List.of(
                        "https://repo.example.org",
                        "https://mirror.example.org:8443",
                        "HTTP://Repo.Example.Org:80",
                        "https://repo.example.org/"))
                .build();

        Settings settings = Settings.newBuilder().servers(List.of(server)).build();

        ProblemCollector<BuilderProblem> problems = validator.validate(settings);
        assertEquals(0, problems.totalProblemsReported());
    }

    @Test
    void testValidateServerRepositoryOriginWithoutScheme() {
        ProblemCollector<BuilderProblem> problems = validateRepositoryOrigin("repo.example.org");
        assertEquals(1, problems.totalProblemsReported());
        assertEquals(
                "'servers.server[0].repositoryOrigins[0]' for server-1 must start with a scheme,"
                        + " for example https://repo.example.org, but found 'repo.example.org'",
                problems.problems().findFirst().orElseThrow().getMessage());
    }

    @Test
    void testValidateServerRepositoryOriginWithoutHost() {
        ProblemCollector<BuilderProblem> problems = validateRepositoryOrigin("file:/tmp/repo");
        assertEquals(1, problems.totalProblemsReported());
        assertEquals(
                "'servers.server[0].repositoryOrigins[0]' for server-1 must name a host,"
                        + " for example https://repo.example.org, but found 'file:/tmp/repo'",
                problems.problems().findFirst().orElseThrow().getMessage());
    }

    @Test
    void testValidateServerRepositoryOriginWithUserInfo() {
        ProblemCollector<BuilderProblem> problems = validateRepositoryOrigin("https://user:pwd@repo.example.org");
        assertEquals(1, problems.totalProblemsReported());
        assertEquals(
                "'servers.server[0].repositoryOrigins[0]' for server-1 must not carry user information"
                        + " but found 'https://user:pwd@repo.example.org'",
                problems.problems().findFirst().orElseThrow().getMessage());
    }

    @Test
    void testValidateServerRepositoryOriginWithPlaceholder() {
        ProblemCollector<BuilderProblem> problems = validateRepositoryOrigin("${env.REPO_URL}");
        assertEquals(1, problems.totalProblemsReported());
        assertEquals(
                "'servers.server[0].repositoryOrigins[0]' for server-1 contains an unresolved property"
                        + " placeholder: '${env.REPO_URL}'",
                problems.problems().findFirst().orElseThrow().getMessage());
    }

    @Test
    void testValidateServerRepositoryOriginEmpty() {
        ProblemCollector<BuilderProblem> problems = validateRepositoryOrigin("");
        assertEquals(1, problems.totalProblemsReported());
        assertEquals(
                "'servers.server[0].repositoryOrigins[0]' for server-1 is missing",
                problems.problems().findFirst().orElseThrow().getMessage());
    }

    @Test
    void testValidateServerRepositoryOriginWithPathIsOnlyWarned() {
        ProblemCollector<BuilderProblem> problems = validateRepositoryOrigin("https://repo.example.org/releases/");
        assertEquals(1, problems.totalProblemsReported());
        BuilderProblem problem = problems.problems().findFirst().orElseThrow();
        assertEquals(BuilderProblem.Severity.WARNING, problem.getSeverity());
        assertEquals(
                "'servers.server[0].repositoryOrigins[0]' for server-1 is a repository origin,"
                        + " not a repository URL; only 'https://repo.example.org' of"
                        + " 'https://repo.example.org/releases/' is used",
                problem.getMessage());
    }

    @Test
    void testValidateServerRepositoryOriginsOnProjectSettings() {
        Server server = Server.newBuilder()
                .id("server-1")
                .repositoryOrigins(List.of("https://repo.example.org"))
                .build();

        Settings settings = Settings.newBuilder().servers(List.of(server)).build();

        ProblemCollector<BuilderProblem> problems = validator.validate(settings, true);
        assertEquals(1, problems.totalProblemsReported());
        assertEquals(
                "'servers.server[0].repositoryOrigins' are not supported on project settings.",
                problems.problems().findFirst().orElseThrow().getMessage());
    }

    private ProblemCollector<BuilderProblem> validateRepositoryOrigin(String repositoryOrigin) {
        Server server = Server.newBuilder()
                .id("server-1")
                .repositoryOrigins(List.of(repositoryOrigin))
                .build();

        Settings settings = Settings.newBuilder().servers(List.of(server)).build();

        return validator.validate(settings);
    }
}
