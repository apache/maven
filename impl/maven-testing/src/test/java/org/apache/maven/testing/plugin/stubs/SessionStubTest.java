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
package org.apache.maven.testing.plugin.stubs;

import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ProblemCollector;
import org.apache.maven.impl.model.DefaultModelProblem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionStubTest {

    @Test
    void modelProblemsAreRetainedAcrossCollectorAccesses() {
        SessionStub session = new SessionStub();
        ProblemCollector<ModelProblem> collector = session.getModelProblemCollector();
        ModelProblem problem = modelProblem();

        assertTrue(collector.reportProblem(problem));

        assertSame(collector, session.getModelProblemCollector());
        assertEquals(1, session.getModelProblemCollector().totalProblemsReported());
        assertTrue(session.getModelProblemCollector().hasWarningProblems());
        assertSame(
                problem,
                session.getModelProblemCollector().problems().findFirst().orElseThrow());
    }

    @Test
    void modelProblemsAreIsolatedBetweenInstances() {
        SessionStub session = new SessionStub();
        SessionStub otherSession = new SessionStub();

        session.getModelProblemCollector().reportProblem(modelProblem());

        assertNotSame(session.getModelProblemCollector(), otherSession.getModelProblemCollector());
        assertEquals(0, otherSession.getModelProblemCollector().totalProblemsReported());
        assertFalse(otherSession.getModelProblemCollector().hasWarningProblems());
        assertEquals(0, otherSession.getModelProblemCollector().problems().count());
    }

    private static ModelProblem modelProblem() {
        return new DefaultModelProblem(
                "model warning",
                BuilderProblem.Severity.WARNING,
                ModelProblem.Version.BASE,
                "pom.xml",
                -1,
                -1,
                "org.example:project:1",
                null);
    }
}
