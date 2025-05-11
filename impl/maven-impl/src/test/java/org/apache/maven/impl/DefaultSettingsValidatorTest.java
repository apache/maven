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
    void validate() {
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
}
