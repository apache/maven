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
package org.apache.maven.lifecycle.internal.builder;

import java.util.HashSet;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.LifecycleDebugLogger;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Kristian Rosenvold
 */
class BuilderCommonTest {
    private Logger logger = mock(Logger.class);

    @Test
    void testResolveBuildPlan() throws Exception {
        MavenSession original = ProjectDependencyGraphStub.getMavenSession();

        final TaskSegment taskSegment1 = new TaskSegment(false);
        final MavenSession session1 = original.clone();
        session1.setCurrentProject(ProjectDependencyGraphStub.A);

        final BuilderCommon builderCommon = getBuilderCommon(logger);
        final MavenExecutionPlan plan =
                builderCommon.resolveBuildPlan(session1, ProjectDependencyGraphStub.A, taskSegment1, new HashSet<>());
        assertEquals(
                LifecycleExecutionPlanCalculatorStub.getProjectAExecutionPlan().size(), plan.size());
    }

    @Test
    void testDefaultBindingPluginsWarning() throws Exception {
        MavenSession original = ProjectDependencyGraphStub.getMavenSession();

        final TaskSegment taskSegment1 = new TaskSegment(false);
        final MavenSession session1 = original.clone();
        session1.setCurrentProject(ProjectDependencyGraphStub.A);

        getBuilderCommon(logger)
                .resolveBuildPlan(session1, ProjectDependencyGraphStub.A, taskSegment1, new HashSet<>());

        verify(logger)
                .warn("Version not locked for default bindings plugins ["
                        + "stub-plugin-initialize, "
                        + "stub-plugin-process-resources, "
                        + "stub-plugin-compile, "
                        + "stub-plugin-process-test-resources, "
                        + "stub-plugin-test-compile, "
                        + "stub-plugin-test, "
                        + "stub-plugin-package, "
                        + "stub-plugin-install], "
                        + "you should define versions in pluginManagement section of your pom.xml or parent");
    }

    @Test
    void testHandleBuildError() throws Exception {}

    @Test
    void testAttachToThread() throws Exception {}

    @Test
    void testGetKey() throws Exception {}

    public BuilderCommon getBuilderCommon(Logger logger) {
        final LifecycleDebugLogger debugLogger = new LifecycleDebugLogger();
        return new BuilderCommon(
                debugLogger, new LifecycleExecutionPlanCalculatorStub(), mock(ExecutionEventCatapult.class), logger);
    }
}
