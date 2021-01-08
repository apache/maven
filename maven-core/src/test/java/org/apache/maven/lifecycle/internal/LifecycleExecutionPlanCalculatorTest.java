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

import org.apache.maven.AbstractCoreMavenComponentTestCase;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.stub.BuildPluginManagerStub;
import org.apache.maven.lifecycle.internal.stub.DefaultLifecyclesStub;
import org.apache.maven.lifecycle.internal.stub.PluginPrefixResolverStub;
import org.apache.maven.lifecycle.internal.stub.PluginVersionResolverStub;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Kristian Rosenvold
 */
public class LifecycleExecutionPlanCalculatorTest
    extends AbstractCoreMavenComponentTestCase
{

    @Test
    public void testCalculateExecutionPlanWithGoalTasks()
        throws Exception
    {
        MojoDescriptorCreator mojoDescriptorCreator = createMojoDescriptorCreator();
        LifecycleExecutionPlanCalculator lifecycleExecutionPlanCalculator =
            createExecutionPlaceCalculator( mojoDescriptorCreator );

        final GoalTask goalTask1 = new GoalTask( "compiler:compile" );
        final GoalTask goalTask2 = new GoalTask( "surefire:test" );
        final TaskSegment taskSegment1 = new TaskSegment( false, goalTask1, goalTask2 );
        final MavenSession session1 = ProjectDependencyGraphStub.getMavenSession( ProjectDependencyGraphStub.A );

        MavenExecutionPlan executionPlan =
            lifecycleExecutionPlanCalculator.calculateExecutionPlan( session1, ProjectDependencyGraphStub.A,
                                                                     taskSegment1.getTasks() );
        assertEquals( 2, executionPlan.size() );

        final GoalTask goalTask3 = new GoalTask( "surefire:test" );
        final TaskSegment taskSegment2 = new TaskSegment( false, goalTask1, goalTask2, goalTask3 );
        MavenExecutionPlan executionPlan2 =
            lifecycleExecutionPlanCalculator.calculateExecutionPlan( session1, ProjectDependencyGraphStub.A,
                                                                     taskSegment2.getTasks() );
        assertEquals( 3, executionPlan2.size() );
    }

    // Maybe also make one with LifeCycleTasks

    public static LifecycleExecutionPlanCalculator createExecutionPlaceCalculator( MojoDescriptorCreator mojoDescriptorCreator )
    {
        LifecyclePluginResolver lifecyclePluginResolver = new LifecyclePluginResolver( new PluginVersionResolverStub() );
        return new DefaultLifecycleExecutionPlanCalculator( new BuildPluginManagerStub(),
                                                            DefaultLifecyclesStub.createDefaultLifecycles(),
                                                            mojoDescriptorCreator, lifecyclePluginResolver );
    }

    public static MojoDescriptorCreator createMojoDescriptorCreator()
    {
        return new MojoDescriptorCreator( new PluginVersionResolverStub(), new BuildPluginManagerStub(),
                                          new PluginPrefixResolverStub(),
                                          new LifecyclePluginResolver( new PluginVersionResolverStub() ) );
    }

    @Override
    protected String getProjectsDirectory()
    {
        return "src/test/projects/lifecycle-executor";
    }

}
