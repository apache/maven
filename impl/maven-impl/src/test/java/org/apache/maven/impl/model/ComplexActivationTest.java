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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.maven.api.Session;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.Sources;
import org.apache.maven.impl.standalone.ApiRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(builder).isNotNull();
    }

    @Test
    void andConditionInActivation() throws Exception {
        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .source(Sources.buildSource(getPom("complex")))
                .systemProperties(Map.of("myproperty", "test"))
                .build();
        ModelBuilderResult result = builder.newSession().build(request);
        assertThat(result).isNotNull();
        assertThat(result.getEffectiveModel()).isNotNull();
        assertThat(result.getEffectiveModel().getProperties().get("profile.file")).isEqualTo("activated-1");
        assertThat(result.getEffectiveModel().getProperties().get("profile.miss")).isNull();
    }

    @Test
    void conditionExistingAndMissingInActivation() throws Exception {
        ModelBuilderRequest request = ModelBuilderRequest.builder()
                .session(session)
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .source(Sources.buildSource(getPom("complexExistsAndMissing")))
                .build();
        ModelBuilderResult result = builder.newSession().build(request);
        assertThat(result).isNotNull();
        assertThat(result.getProblemCollector()
                .problems()
                .anyMatch(p -> p.getSeverity() == BuilderProblem.Severity.WARNING
                        && p.getMessage().contains("The 'missing' assertion will be ignored."))).isTrue();
    }

    private Path getPom(String name) {
        return Paths.get("src/test/resources/poms/factory/" + name + ".xml").toAbsolutePath();
    }
}
