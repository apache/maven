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

import org.apache.maven.BuildFailureException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.extension.ExtensionScanningException;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;

import java.util.ArrayList;
import java.util.List;

/** @author Jason van Zyl */
public class DefaultMavenExecutionResult
    implements MavenExecutionResult
{
    private MavenProject project;

    private List topologicallySortedProjects;

    private ArtifactResolutionResult artifactResolutionResult;

    private List exceptions;

    private ReactorManager reactorManager;

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
        return exceptions;
    }

    public MavenExecutionResult addExtensionScanningException( ExtensionScanningException e )
    {
        addException( e );

        return this;
    }

    public MavenExecutionResult addProjectBuildingException( ProjectBuildingException e )
    {
        addException( e );

        return this;
    }

    public MavenExecutionResult addMavenExecutionException( MavenExecutionException e )
    {
        addException( e );

        return this;
    }

    public MavenExecutionResult addBuildFailureException( BuildFailureException e )
    {
        addException( e );

        return this;
    }

    public MavenExecutionResult addDuplicateProjectException( DuplicateProjectException e )
    {
        addException( e );

        return this;
    }

    public MavenExecutionResult addLifecycleExecutionException( LifecycleExecutionException e )
    {
        addException( e );

        return this;
    }

    public MavenExecutionResult addUnknownException( Throwable t )
    {
        addException( t );

        return this;
    }

    private void addException( Throwable t )
    {
        if ( exceptions == null )
        {
            exceptions = new ArrayList();
        }

        exceptions.add( t );
    }

    public boolean hasExceptions()
    {
        return (( exceptions != null ) && ( exceptions.size() > 0 ) );
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
}
