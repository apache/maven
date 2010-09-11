package org.apache.maven.lifecycle.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import java.util.HashSet;

/**
 * Builds one or more lifecycles for a full module
 * 
 * @since 3.0
 * @author Benjamin Bentmann
 * @author Jason van Zyl
 * @author Kristian Rosenvold (extracted class)
 *         <p/>
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
@Component( role = LifecycleModuleBuilder.class )
public class LifecycleModuleBuilder
{

    @Requirement
    private MojoExecutor mojoExecutor;

    @Requirement
    private BuilderCommon builderCommon;

    @Requirement
    private ExecutionEventCatapult eventCatapult;

    public void buildProject( MavenSession session, ReactorContext reactorContext, MavenProject currentProject,
                              TaskSegment taskSegment )
    {
        buildProject( session, session, reactorContext, currentProject, taskSegment );
    }

    public void buildProject( MavenSession session, MavenSession rootSession, ReactorContext reactorContext,
                              MavenProject currentProject, TaskSegment taskSegment )
    {
        session.setCurrentProject( currentProject );

        long buildStartTime = System.currentTimeMillis();

        try
        {

            if ( reactorContext.getReactorBuildStatus().isHaltedOrBlacklisted( currentProject ) )
            {
                eventCatapult.fire( ExecutionEvent.Type.ProjectSkipped, session, null );
                return;
            }

            eventCatapult.fire( ExecutionEvent.Type.ProjectStarted, session, null );

            BuilderCommon.attachToThread( currentProject );
            MavenExecutionPlan executionPlan =
                builderCommon.resolveBuildPlan( session, currentProject, taskSegment, new HashSet<Artifact>() );

            mojoExecutor.execute( session, executionPlan.getMojoExecutions(), reactorContext.getProjectIndex() );

            long buildEndTime = System.currentTimeMillis();

            reactorContext.getResult().addBuildSummary(
                new BuildSuccess( currentProject, buildEndTime - buildStartTime ) );

            eventCatapult.fire( ExecutionEvent.Type.ProjectSucceeded, session, null );
        }
        catch ( Exception e )
        {
            builderCommon.handleBuildError( reactorContext, rootSession, currentProject, e, buildStartTime );
        }
        finally
        {
            session.setCurrentProject( null );

            Thread.currentThread().setContextClassLoader( reactorContext.getOriginalContextClassLoader() );
        }
    }
}