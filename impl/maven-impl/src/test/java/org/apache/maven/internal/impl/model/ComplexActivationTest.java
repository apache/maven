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
package org.apache.maven.internal.impl.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.maven.api.Session;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.internal.impl.standalone.ApiRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class ComplexActivationTest {

    Session session;
    ModelBuilder builder;

    @BeforeEach
    void setup() {
        session = ApiRunner.createSession();
        builder = session.getService(ModelBuilder.class);
        assertNotNull(builder);
    }

    @Test
    void testAndConditionInActivation() throws Exception {
        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .source(ModelSource.fromPath(getPom("complex")))
                .systemProperties(Map.of("myproperty", "test"))
                .build();
        ModelBuilderResult result = builder.newSession().build(request);
        assertNotNull(result);
        assertNotNull(result.getEffectiveModel());
        assertEquals("activated-1", result.getEffectiveModel().getProperties().get("profile.file"));
        assertNull(result.getEffectiveModel().getProperties().get("profile.miss"));
    }

    @Test
    public void testConditionExistingAndMissingInActivation() throws Exception {
        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .source(ModelSource.fromPath(getPom("complexExistsAndMissing")))
                .build();
        ModelBuilderResult result = builder.newSession().build(request);
        assertNotNull(result);
        assertTrue(result.getProblems().stream()
                .anyMatch(p -> p.getSeverity() == BuilderProblem.Severity.WARNING
                        && p.getMessage().contains("The 'missing' assertion will be ignored.")));
    }

    private Path getPom(String name) {
        return Paths.get("src/test/resources/poms/factory/" + name + ".xml").toAbsolutePath();
    }
}
