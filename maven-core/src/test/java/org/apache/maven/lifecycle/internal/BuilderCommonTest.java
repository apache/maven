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
package org.apache.maven.lifecycle.internal;

import java.util.HashSet;

import junit.framework.TestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.builder.BuilderCommon;
import org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub;
import org.apache.maven.lifecycle.internal.stub.LoggerStub;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;

/**
 * @author Kristian Rosenvold
 */
public class BuilderCommonTest extends TestCase {
    public void testResolveBuildPlan() throws Exception {
        MavenSession original = ProjectDependencyGraphStub.getMavenSession();

        final TaskSegment taskSegment1 = new TaskSegment(false);
        final MavenSession session1 = original.clone();
        session1.setCurrentProject(ProjectDependencyGraphStub.A);

        final BuilderCommon builderCommon = getBuilderCommon();
        final MavenExecutionPlan plan = builderCommon.resolveBuildPlan(
                session1, ProjectDependencyGraphStub.A, taskSegment1, new HashSet<Artifact>());
        assertEquals(
                LifecycleExecutionPlanCalculatorStub.getProjectAExceutionPlan().size(), plan.size());
    }

    public void testHandleBuildError() throws Exception {}

    public void testAttachToThread() throws Exception {}

    public void testGetKey() throws Exception {}

    public static BuilderCommon getBuilderCommon() {
        final LifecycleDebugLogger logger = new LifecycleDebugLogger(new LoggerStub());
        return new BuilderCommon(logger, new LifecycleExecutionPlanCalculatorStub(), new LoggerStub());
    }
}
