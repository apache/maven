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
package org.apache.maven.internal.impl;

import java.util.List;

import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ProblemCollector;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.settings.Profile;
import org.apache.maven.api.settings.Repository;
import org.apache.maven.api.settings.Settings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
class DefaultSettingsValidatorTest {

    private SettingsBuilder validator;

    @BeforeEach
    void setUp() throws Exception {
        validator = new DefaultSettingsBuilder();
    }

    @AfterEach
    void tearDown() throws Exception {
        validator = null;
    }

    private void assertContains(String msg, String substring) {
        assertTrue(msg.contains(substring), "\"" + substring + "\" was not found in: " + msg);
    }

    @Test
    void testValidate() {
        Profile prof = Profile.newBuilder().id("xxx").build();
        Settings model = Settings.newBuilder().profiles(List.of(prof)).build();
        ProblemCollector<BuilderProblem> problems = validator.validate(model);
        assertEquals(0, problems.problemsReported());

        Repository repo = org.apache.maven.api.settings.Repository.newInstance(false);
        Settings model2 = Settings.newBuilder()
                .profiles(List.of(prof.withRepositories(List.of(repo))))
                .build();
        problems = validator.validate(model2);
        assertEquals(2, problems.problemsReported());

        repo = repo.withUrl("http://xxx.xxx.com");
        model2 = Settings.newBuilder()
                .profiles(List.of(prof.withRepositories(List.of(repo))))
                .build();
        problems = validator.validate(model2);
        assertEquals(1, problems.problemsReported());

        repo = repo.withId("xxx");
        model2 = Settings.newBuilder()
                .profiles(List.of(prof.withRepositories(List.of(repo))))
                .build();
        problems = validator.validate(model2);
        assertEquals(0, problems.problemsReported());
    }

    /*
    @Test
    void testValidateMirror() throws Exception {
        Settings settings = new Settings();
        Mirror mirror = new Mirror();
        mirror.setId("local");
        settings.addMirror(mirror);
        mirror = new Mirror();
        mirror.setId("illegal\\:/chars");
        mirror.setUrl("http://void");
        mirror.setMirrorOf("void");
        settings.addMirror(mirror);

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate(settings, problems);
        assertEquals(4, problems.messages.size());
        assertContains(problems.messages.get(0), "'mirrors.mirror.id' must not be 'local'");
        assertContains(problems.messages.get(1), "'mirrors.mirror.url' for local is missing");
        assertContains(problems.messages.get(2), "'mirrors.mirror.mirrorOf' for local is missing");
        assertContains(problems.messages.get(3), "'mirrors.mirror.id' must not contain any of these characters");
    }

    @Test
    void testValidateRepository() throws Exception {
        Profile profile = new Profile();
        Repository repo = new Repository();
        repo.setId("local");
        profile.addRepository(repo);
        repo = new Repository();
        repo.setId("illegal\\:/chars");
        repo.setUrl("http://void");
        profile.addRepository(repo);
        Settings settings = new Settings();
        settings.addProfile(profile);

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate(settings, problems);
        assertEquals(3, problems.messages.size());
        assertContains(
                problems.messages.get(0), "'profiles.profile[default].repositories.repository.id' must not be 'local'");
        assertContains(
                problems.messages.get(1),
                "'profiles.profile[default].repositories.repository.url' for local is missing");
        assertContains(
                problems.messages.get(2),
                "'profiles.profile[default].repositories.repository.id' must not contain any of these characters");
    }

    @Test
    void testValidateUniqueServerId() throws Exception {
        Settings settings = new Settings();
        Server server1 = new Server();
        server1.setId("test");
        settings.addServer(server1);
        Server server2 = new Server();
        server2.setId("test");
        settings.addServer(server2);

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate(settings, problems);
        assertEquals(1, problems.messages.size());
        assertContains(
                problems.messages.get(0), "'servers.server.id' must be unique but found duplicate server with id test");
    }

    @Test
    void testValidateUniqueProfileId() throws Exception {
        Settings settings = new Settings();
        Profile profile1 = new Profile();
        profile1.setId("test");
        settings.addProfile(profile1);
        Profile profile2 = new Profile();
        profile2.setId("test");
        settings.addProfile(profile2);

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate(settings, problems);
        assertEquals(1, problems.messages.size());
        assertContains(
                problems.messages.get(0),
                "'profiles.profile.id' must be unique but found duplicate profile with id test");
    }

    @Test
    void testValidateUniqueRepositoryId() throws Exception {
        Settings settings = new Settings();
        Profile profile = new Profile();
        profile.setId("pro");
        settings.addProfile(profile);
        Repository repo1 = new Repository();
        repo1.setUrl("http://apache.org/");
        repo1.setId("test");
        profile.addRepository(repo1);
        Repository repo2 = new Repository();
        repo2.setUrl("http://apache.org/");
        repo2.setId("test");
        profile.addRepository(repo2);

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate(settings, problems);
        assertEquals(1, problems.messages.size());
        assertContains(
                problems.messages.get(0),
                "'profiles.profile[pro].repositories.repository.id' must be unique"
                        + " but found duplicate repository with id test");
    }

    @Test
    void testValidateUniqueProxyId() throws Exception {
        Settings settings = new Settings();
        Proxy proxy = new Proxy();
        String id = "foo";
        proxy.setId(id);
        proxy.setHost("www.example.com");
        settings.addProxy(proxy);
        settings.addProxy(proxy);

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate(settings, problems);
        assertEquals(1, problems.messages.size());
        assertContains(
                problems.messages.get(0),
                "'proxies.proxy.id' must be unique" + " but found duplicate proxy with id " + id);
    }

    @Test
    void testValidateProxy() throws Exception {
        Settings settings = new Settings();
        Proxy proxy1 = new Proxy();
        settings.addProxy(proxy1);

        SimpleProblemCollector problems = new SimpleProblemCollector();
        validator.validate(settings, problems);
        assertEquals(1, problems.messages.size());
        assertContains(problems.messages.get(0), "'proxies.proxy.host' for default is missing");
    }

    private static class SimpleProblemCollector implements SettingsProblemCollector {

        public List<String> messages = new ArrayList<>();

        public void add(Severity severity, String message, int line, int column, Exception cause) {
            messages.add(message);
        }
    }

     */
}
