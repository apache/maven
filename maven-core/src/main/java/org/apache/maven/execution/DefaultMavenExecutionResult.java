package org.apache.maven.execution;

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

import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.lifecycle.plan.BuildPlan;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** @author Jason van Zyl */
public class DefaultMavenExecutionResult
    implements MavenExecutionResult
{
    private MavenProject project;

    private List topologicallySortedProjects;

    private ArtifactResolutionResult artifactResolutionResult;

    private List exceptions;

    private ReactorManager reactorManager;

    private Map buildPlans;

    public MavenExecutionResult setProject( MavenProject project )
    {
        this.project = project;

        return this;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public MavenExecutionResult setTopologicallySortedProjects( List topologicallySortedProjects )
    {
        this.topologicallySortedProjects = topologicallySortedProjects;

        return this;
    }

    public List getTopologicallySortedProjects()
    {
        return topologicallySortedProjects;
    }

    public ArtifactResolutionResult getArtifactResolutionResult()
    {
        return artifactResolutionResult;
    }

    public MavenExecutionResult setArtifactResolutionResult( ArtifactResolutionResult artifactResolutionResult )
    {
        this.artifactResolutionResult = artifactResolutionResult;

        return this;
    }

    public List getExceptions()
    {
        return exceptions == null ? Collections.EMPTY_LIST : exceptions;
    }

    public MavenExecutionResult addException( Throwable t )
    {
        if ( exceptions == null )
        {
            exceptions = new ArrayList();
        }

        exceptions.add( t );

        return this;
    }

    public boolean hasExceptions()
    {
        return !getExceptions().isEmpty();
    }

    public ReactorManager getReactorManager()
    {
        return reactorManager;
    }

    public MavenExecutionResult setReactorManager( ReactorManager reactorManager )
    {
        this.reactorManager = reactorManager;

        return this;
    }

    public BuildPlan getBuildPlan( String projectId )
    {
        return (BuildPlan) buildPlans.get( projectId );
    }

    public BuildPlan getBuildPlan( MavenProject project )
    {
        return (BuildPlan) buildPlans.get( project.getId() );
    }

    public void setBuildPlans( Map buildPlans )
    {
        this.buildPlans = buildPlans;
    }
}
