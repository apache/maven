package org.apache.maven.lifecycle.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.util.HashSet;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.builder.BuilderCommon;
import org.apache.maven.lifecycle.internal.stub.LifecycleExecutionPlanCalculatorStub;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;
import org.codehaus.plexus.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Kristian Rosenvold
 */
public class BuilderCommonTest
{
    private Logger logger = mock( Logger.class );

    @Test
    public void testResolveBuildPlan()
        throws Exception
    {
        MavenSession original = ProjectDependencyGraphStub.getMavenSession();

        final TaskSegment taskSegment1 = new TaskSegment( false );
        final MavenSession session1 = original.clone();
        session1.setCurrentProject( ProjectDependencyGraphStub.A );

        final BuilderCommon builderCommon = getBuilderCommon();
        final MavenExecutionPlan plan =
            builderCommon.resolveBuildPlan( session1, ProjectDependencyGraphStub.A, taskSegment1,
                    new HashSet<>() );
        assertEquals( LifecycleExecutionPlanCalculatorStub.getProjectAExceutionPlan().size(), plan.size() );
    }

    @Test
    public void testDefaultBindingPluginsWarning()
        throws Exception
    {
        MavenSession original = ProjectDependencyGraphStub.getMavenSession();

        final TaskSegment taskSegment1 = new TaskSegment( false );
        final MavenSession session1 = original.clone();
        session1.setCurrentProject( ProjectDependencyGraphStub.A );

        getBuilderCommon().resolveBuildPlan( session1, ProjectDependencyGraphStub.A, taskSegment1, new HashSet<>() );

        verify( logger ).warn("Version not locked for default bindings plugins ["
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
    public void testHandleBuildError()
        throws Exception
    {
    }

    @Test
    public void testAttachToThread()
        throws Exception
    {
    }

    @Test
    public void testGetKey()
        throws Exception
    {
    }

    public BuilderCommon getBuilderCommon()
    {
        final LifecycleDebugLogger debugLogger = new LifecycleDebugLogger( logger );
        return new BuilderCommon( debugLogger, new LifecycleExecutionPlanCalculatorStub(),
                                  logger );
    }

}
