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
package org.apache.maven.impl.model;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.Sources;
import org.apache.maven.impl.standalone.ApiRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
class DefaultModelBuilderTest {

    Session session;
    ModelBuilder builder;

    @BeforeEach
    void setup() {
        session = ApiRunner.createSession();
        builder = session.getService(ModelBuilder.class);
        assertNotNull(builder);
    }

    @Test
    public void testPropertiesAndProfiles() {
        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .source(Sources.buildSource(getPom("props-and-profiles")))
                .build();
        ModelBuilderResult result = builder.newSession().build(request);
        assertNotNull(result);
        assertEquals("21", result.getEffectiveModel().getProperties().get("maven.compiler.release"));
    }

    @Test
    public void testMergeRepositories() throws Exception {
        // this is here only to trigger mainSession creation; unrelated
        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .userProperties(Map.of("firstParentRepo", "https://some.repo"))
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .source(Sources.buildSource(getPom("props-and-profiles")))
                .build();
        ModelBuilder.ModelBuilderSession session = builder.newSession();
        session.build(request); // ignored result value; just to trigger mainSession creation

        Field mainSessionField = DefaultModelBuilder.ModelBuilderSessionImpl.class.getDeclaredField("mainSession");
        mainSessionField.setAccessible(true);
        DefaultModelBuilder.ModelBuilderSessionState state =
                (DefaultModelBuilder.ModelBuilderSessionState) mainSessionField.get(session);
        Field repositoriesField = DefaultModelBuilder.ModelBuilderSessionState.class.getDeclaredField("repositories");
        repositoriesField.setAccessible(true);

        List<RemoteRepository> repositories;
        // before merge
        repositories = (List<RemoteRepository>) repositoriesField.get(state);
        assertEquals(1, repositories.size()); // central

        Model model = Model.newBuilder()
                .repositories(Arrays.asList(
                        Repository.newBuilder()
                                .id("first")
                                .url("${firstParentRepo}")
                                .build(),
                        Repository.newBuilder()
                                .id("second")
                                .url("${secondParentRepo}")
                                .build()))
                .build();
        state.mergeRepositories(model, false);

        // after merge
        repositories = (List<RemoteRepository>) repositoriesField.get(state);
        assertEquals(3, repositories.size());
        assertEquals(repositories.get(0).getId(), "first");
        assertEquals(repositories.get(0).getUrl(), "https://some.repo"); // interpolated
        assertEquals(repositories.get(1).getId(), "second");
        assertEquals(repositories.get(1).getUrl(), "${secondParentRepo}"); // un-interpolated (no source)
        assertEquals(repositories.get(2).getId(), "central"); // default
    }

    private Path getPom(String name) {
        return Paths.get("src/test/resources/poms/factory/" + name + ".xml").toAbsolutePath();
    }
}
