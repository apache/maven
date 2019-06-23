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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.builder.BuilderCommon;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

/**
 * <p>
 * Logs debug output from the various lifecycle phases.
 * </p>
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 * @author Jason van Zyl
 * @author Kristian Rosenvold (extracted class only)
 */
@Named
@Singleton
public class LifecycleDebugLogger
{
    @Inject
    private Logger logger;


    public LifecycleDebugLogger()
    {
    }

    public LifecycleDebugLogger( Logger logger )
    {
        this.logger = logger;
    }


    public void debug( String s )
    {
        logger.debug( s );
    }

    public void info( String s )
    {
        logger.info( s );
    }

    public void debugReactorPlan( ProjectBuildList projectBuilds )
    {
        if ( !logger.isDebugEnabled() )
        {
            return;
        }

        logger.debug( "=== REACTOR BUILD PLAN ================================================" );

        for ( Iterator<ProjectSegment> it = projectBuilds.iterator(); it.hasNext(); )
        {
            ProjectSegment projectBuild = it.next();

            logger.debug( "Project: " + projectBuild.getProject().getId() );
            logger.debug( "Tasks:   " + projectBuild.getTaskSegment().getTasks() );
            logger.debug( "Style:   " + ( projectBuild.getTaskSegment().isAggregating() ? "Aggregating" : "Regular" ) );

            if ( it.hasNext() )
            {
                logger.debug( "-----------------------------------------------------------------------" );
            }
        }

        logger.debug( "=======================================================================" );
    }


    public void debugProjectPlan( MavenProject currentProject, MavenExecutionPlan executionPlan )
    {
        if ( !logger.isDebugEnabled() )
        {
            return;
        }

        logger.debug( "=== PROJECT BUILD PLAN ================================================" );
        logger.debug( "Project:       " + BuilderCommon.getKey( currentProject ) );

        debugDependencyRequirements( executionPlan.getMojoExecutions() );

        logger.debug( "Repositories (dependencies): " + currentProject.getRemoteProjectRepositories() );
        logger.debug( "Repositories (plugins)     : " + currentProject.getRemotePluginRepositories() );

        for ( ExecutionPlanItem mojoExecution : executionPlan )
        {
            debugMojoExecution( mojoExecution.getMojoExecution() );
        }

        logger.debug( "=======================================================================" );
    }

    private void debugMojoExecution( MojoExecution mojoExecution )
    {
        String mojoExecId =
            mojoExecution.getGroupId() + ':' + mojoExecution.getArtifactId() + ':' + mojoExecution.getVersion() + ':'
                + mojoExecution.getGoal() + " (" + mojoExecution.getExecutionId() + ')';

        Map<String, List<MojoExecution>> forkedExecutions = mojoExecution.getForkedExecutions();
        if ( !forkedExecutions.isEmpty() )
        {
            for ( Map.Entry<String, List<MojoExecution>> fork : forkedExecutions.entrySet() )
            {
                logger.debug( "--- init fork of " + fork.getKey() + " for " + mojoExecId + " ---" );

                debugDependencyRequirements( fork.getValue() );

                for ( MojoExecution forkedExecution : fork.getValue() )
                {
                    debugMojoExecution( forkedExecution );
                }

                logger.debug( "--- exit fork of " + fork.getKey() + " for " + mojoExecId + " ---" );
            }
        }

        logger.debug( "-----------------------------------------------------------------------" );
        logger.debug( "Goal:          " + mojoExecId );
        logger.debug(
            "Style:         " + ( mojoExecution.getMojoDescriptor().isAggregator() ? "Aggregating" : "Regular" ) );
        logger.debug( "Configuration: " + mojoExecution.getConfiguration() );
    }

    private void debugDependencyRequirements( List<MojoExecution> mojoExecutions )
    {
        Set<String> scopesToCollect = new TreeSet<>();
        Set<String> scopesToResolve = new TreeSet<>();

        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

            String scopeToCollect = mojoDescriptor.getDependencyCollectionRequired();
            if ( StringUtils.isNotEmpty( scopeToCollect ) )
            {
                scopesToCollect.add( scopeToCollect );
            }

            String scopeToResolve = mojoDescriptor.getDependencyResolutionRequired();
            if ( StringUtils.isNotEmpty( scopeToResolve ) )
            {
                scopesToResolve.add( scopeToResolve );
            }
        }

        logger.debug( "Dependencies (collect): " + scopesToCollect );
        logger.debug( "Dependencies (resolve): " + scopesToResolve );
    }

}