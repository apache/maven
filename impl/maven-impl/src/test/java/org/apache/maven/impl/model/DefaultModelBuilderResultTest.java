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

import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.ProblemCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DefaultModelBuilderResultTest {

    private ModelBuilderRequest request;
    private ProblemCollector<ModelProblem> problemCollector;
    private DefaultModelBuilderResult result;
    private ModelSource source;
    private Model fileModel;
    private Model rawModel;
    private Model effectiveModel;

    @BeforeEach
    void setUp() {
        request = mock(ModelBuilderRequest.class);
        problemCollector = ProblemCollector.create(10);
        result = new DefaultModelBuilderResult(request, problemCollector);

        source = mock(ModelSource.class);
        fileModel = mock(Model.class);
        rawModel = mock(Model.class);
        effectiveModel = mock(Model.class);
    }

    @Test
    void modelLifecycle() {
        // Test initial state
        assertThat(result.getSource()).isNull();
        assertThat(result.getFileModel()).isNull();
        assertThat(result.getRawModel()).isNull();
        assertThat(result.getEffectiveModel()).isNull();
        assertThat(result.getProblemCollector().problems().count()).isEqualTo(0L);

        // Set and verify source
        result.setSource(source);
        assertThat(result.getSource()).isSameAs(source);

        // Set and verify file model
        result.setFileModel(fileModel);
        assertThat(result.getFileModel()).isSameAs(fileModel);

        // Set and verify raw model
        result.setRawModel(rawModel);
        assertThat(result.getRawModel()).isSameAs(rawModel);

        // Set and verify effective model
        result.setEffectiveModel(effectiveModel);
        assertThat(result.getEffectiveModel()).isSameAs(effectiveModel);
    }

    @Test
    void problemCollection() {
        ModelProblem problem = mock(ModelProblem.class);
        Mockito.when(problem.getSeverity()).thenReturn(BuilderProblem.Severity.ERROR);
        problemCollector.reportProblem(problem);

        assertThat(result.getProblemCollector().problems().count()).isEqualTo(1);
        assertThat(result.getProblemCollector().problems().findFirst().get()).isSameAs(problem);
    }

    @Test
    void childrenManagement() {
        DefaultModelBuilderResult child1 = new DefaultModelBuilderResult(request, problemCollector);
        DefaultModelBuilderResult child2 = new DefaultModelBuilderResult(request, problemCollector);

        result.getChildren().add(child1);
        result.getChildren().add(child2);

        assertThat(result.getChildren().size()).isEqualTo(2);
        assertThat(result.getChildren().contains(child1)).isTrue();
        assertThat(result.getChildren().contains(child2)).isTrue();
    }

    @Test
    void requestAssociation() {
        assertThat(result.getRequest()).isSameAs(request);
    }
}
