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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void testModelLifecycle() {
        // Test initial state
        assertNull(result.getSource());
        assertNull(result.getFileModel());
        assertNull(result.getRawModel());
        assertNull(result.getEffectiveModel());
        assertEquals(0L, result.getProblemCollector().problems().count());

        // Set and verify source
        result.setSource(source);
        assertSame(source, result.getSource());

        // Set and verify file model
        result.setFileModel(fileModel);
        assertSame(fileModel, result.getFileModel());

        // Set and verify raw model
        result.setRawModel(rawModel);
        assertSame(rawModel, result.getRawModel());

        // Set and verify effective model
        result.setEffectiveModel(effectiveModel);
        assertSame(effectiveModel, result.getEffectiveModel());
    }

    @Test
    void testProblemCollection() {
        ModelProblem problem = mock(ModelProblem.class);
        Mockito.when(problem.getSeverity()).thenReturn(BuilderProblem.Severity.ERROR);
        problemCollector.reportProblem(problem);

        assertEquals(1, result.getProblemCollector().problems().count());
        assertSame(problem, result.getProblemCollector().problems().findFirst().get());
    }

    @Test
    void testChildrenManagement() {
        DefaultModelBuilderResult child1 = new DefaultModelBuilderResult(request, problemCollector);
        DefaultModelBuilderResult child2 = new DefaultModelBuilderResult(request, problemCollector);

        result.getChildren().add(child1);
        result.getChildren().add(child2);

        assertEquals(2, result.getChildren().size());
        assertTrue(result.getChildren().contains(child1));
        assertTrue(result.getChildren().contains(child2));
    }

    @Test
    void testRequestAssociation() {
        assertSame(request, result.getRequest());
    }
}
