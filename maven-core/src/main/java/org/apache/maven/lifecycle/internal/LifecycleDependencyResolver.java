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

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.util.*;

/**
 * Resolves dependencies for the artifacts in context of the lifecycle build
 * 
 * @since 3.0-beta-1
 * @author Benjamin Bentmann
 * @author Jason van Zyl
 * @author Kristian Rosenvold (extracted class)
 *         <p/>
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
@Component( role = LifecycleDependencyResolver.class )
public class LifecycleDependencyResolver
{
    @Requirement
    private ProjectDependenciesResolver projectDependenciesResolver;

    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactFactory artifactFactory;

    @SuppressWarnings( { "UnusedDeclaration" } )
    public LifecycleDependencyResolver()
    {
    }

    public LifecycleDependencyResolver( ProjectDependenciesResolver projectDependenciesResolver, Logger logger )
    {
        this.projectDependenciesResolver = projectDependenciesResolver;
        this.logger = logger;
    }

    public void resolveDependencies( boolean aggregating, MavenProject currentProject,
                                     MavenSession sessionForThisModule, MavenExecutionPlan executionPlan,
                                     Set<Artifact> projectArtifacts )
        throws LifecycleExecutionException
    {
        List<MavenProject> projectsToResolve = getProjects( currentProject, sessionForThisModule, aggregating );
        resolveDependencies( aggregating, sessionForThisModule, executionPlan, projectsToResolve, projectArtifacts );
    }

    public static List<MavenProject> getProjects( MavenProject project, MavenSession session, boolean aggregator )
    {
        if ( aggregator )
        {
            return session.getProjects();
        }
        else
        {
            return Collections.singletonList( project );
        }
    }

    public void checkForUpdate( MavenSession session, DependencyContext dependenctContext )
        throws LifecycleExecutionException
    {

        if ( dependenctContext.isSameButUpdatedProject( session ) )
        {
            resolveProjectDependencies( dependenctContext.getLastProject(), dependenctContext.getScopesToCollect(),
                                         dependenctContext.getScopesToResolve(), session,
                                         dependenctContext.isAggregating(), new HashSet<Artifact>() );
        }

        dependenctContext.setLastProject( session.getCurrentProject() );
        dependenctContext.setLastDependencyArtifacts( session.getCurrentProject().getDependencyArtifacts() );
    }

    private void resolveDependencies( boolean aggregating, MavenSession session, MavenExecutionPlan executionPlan,
                                       List<MavenProject> projectsToResolve, Set<Artifact> projectArtifacts )
        throws LifecycleExecutionException
    {
        for ( MavenProject project : projectsToResolve )
        {
            resolveProjectDependencies( project, executionPlan.getRequiredCollectionScopes(),
                                         executionPlan.getRequiredResolutionScopes(), session, aggregating,
                                         projectArtifacts );
        }
    }

    private void resolveProjectDependencies( MavenProject project, Collection<String> scopesToCollect,
                                              Collection<String> scopesToResolve, MavenSession session,
                                              boolean aggregating, Set<Artifact> projectArtifacts )
        throws LifecycleExecutionException
    {
        if ( project.getDependencyArtifacts() == null )
        {
            try
            {
                project.setDependencyArtifacts( project.createArtifacts( artifactFactory, null, null ) );
            }
            catch ( InvalidDependencyVersionException e )
            {
                throw new LifecycleExecutionException( e );
            }
        }

        Set<Artifact> artifacts =
            getProjectDependencies( project, scopesToCollect, scopesToResolve, session, aggregating,
                                    projectArtifacts );

        project.setResolvedArtifacts( artifacts );
    }

    private Set<Artifact> getProjectDependencies( MavenProject project, Collection<String> scopesToCollect,
                                                  Collection<String> scopesToResolve, MavenSession session,
                                                  boolean aggregating, Set<Artifact> projectArtifacts )
        throws LifecycleExecutionException
    {
        Set<Artifact> artifacts;
        try
        {
            try
            {
                artifacts = projectDependenciesResolver.resolve( project , scopesToCollect,
                                                                 scopesToResolve, session, projectArtifacts );
            }
            catch ( MultipleArtifactsNotFoundException e )
            {
                /*
                * MNG-2277, the check below compensates for our bad plugin support where we ended up with aggregator
                * plugins that require dependency resolution although they usually run in phases of the build where
                * project artifacts haven't been assembled yet. The prime example of this is "mvn release:prepare".
                */
                artifacts = handleException( session, aggregating, e );
            }

            return artifacts;
        }
        catch ( ArtifactResolutionException e )
        {
            throw new LifecycleExecutionException( null, project, e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new LifecycleExecutionException( null, project, e );
        }

    }


    private Set<Artifact> handleException( MavenSession session, boolean aggregating,
                                           MultipleArtifactsNotFoundException e )
        throws MultipleArtifactsNotFoundException
    {
        Set<Artifact> artifacts;
        /*
        * MNG-2277, the check below compensates for our bad plugin support where we ended up with aggregator
        * plugins that require dependency resolution although they usually run in phases of the build where project
        * artifacts haven't been assembled yet. The prime example of this is "mvn release:prepare".
        */
        if ( aggregating && areAllArtifactsInReactor( session.getProjects(), e.getMissingArtifacts() ) )
        {
            logger.warn( "The following artifacts could not be resolved at this point of the build"
                + " but seem to be part of the reactor:" );

            for ( Artifact artifact : e.getMissingArtifacts() )
            {
                logger.warn( "o " + artifact.getId() );
            }

            logger.warn( "Try running the build up to the lifecycle phase \"package\"" );

            artifacts = new LinkedHashSet<Artifact>( e.getResolvedArtifacts() );
        }
        else
        {
            throw e;
        }
        return artifacts;
    }

    private boolean areAllArtifactsInReactor( Collection<MavenProject> projects, Collection<Artifact> artifacts )
    {
        Set<String> projectKeys = getReactorProjectKeys( projects );

        for ( Artifact artifact : artifacts )
        {
            String key = ArtifactUtils.key( artifact );
            if ( !projectKeys.contains( key ) )
            {
                return false;
            }
        }

        return true;
    }

    private Set<String> getReactorProjectKeys( Collection<MavenProject> projects )
    {
        Set<String> projectKeys = new HashSet<String>( projects.size() * 2 );
        for ( MavenProject project : projects )
        {
            String key = ArtifactUtils.key( project.getGroupId(), project.getArtifactId(), project.getVersion() );
            projectKeys.add( key );
        }
        return projectKeys;
    }

}
