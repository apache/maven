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

package org.apache.maven.lifecycle.internal;

import org.apache.maven.ProjectDependenciesResolver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.util.*;

/**
 * Resolves dependencies for the artifacts in context of the lifecycle build
 *
 * @author Benjamin Bentmann
 * @author Jason van Zyl
 * @author Kristian Rosenvold (extracted class)
 *         <p/>
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
@Component(role = LifecycleDependencyResolver.class)
public class LifecycleDependencyResolver
{
    @Requirement
    private ProjectDependenciesResolver projectDependenciesResolver;

    @Requirement
    private Logger logger;

    @SuppressWarnings({"UnusedDeclaration"})
    public LifecycleDependencyResolver()
    {
    }

    public LifecycleDependencyResolver( ProjectDependenciesResolver projectDependenciesResolver, Logger logger )
    {
        this.projectDependenciesResolver = projectDependenciesResolver;
        this.logger = logger;
    }

    public void resolveDependencies( boolean aggregating, MavenProject currentProject,
                                     MavenSession sessionForThisModule, MavenExecutionPlan executionPlan )
        throws LifecycleExecutionException
    {
        List<MavenProject> projectsToResolve = getProjects( currentProject, sessionForThisModule, aggregating );
        resolveDependencies( aggregating, sessionForThisModule, executionPlan, projectsToResolve );
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
                                        dependenctContext.isAggregating() );
        }

        dependenctContext.setLastProject( session.getCurrentProject() );
        dependenctContext.setLastDependencyArtifacts( session.getCurrentProject().getDependencyArtifacts() );
    }

    private void resolveDependencies( boolean aggregating, MavenSession session, MavenExecutionPlan executionPlan,
                                      List<MavenProject> projectsToResolve )
        throws LifecycleExecutionException
    {
        for ( MavenProject project : projectsToResolve )
        {
            resolveDependencies( project, aggregating, session, executionPlan );
        }
    }

    private void resolveDependencies( MavenProject project, boolean aggregating, MavenSession session,
                                      MavenExecutionPlan executionPlan )
        throws LifecycleExecutionException
    {
        resolveProjectDependencies( project, executionPlan.getRequiredCollectionScopes(),
                                    executionPlan.getRequiredResolutionScopes(), session, aggregating );
    }

    private void resolveProjectDependencies( MavenProject project, Collection<String> scopesToCollect,
                                             Collection<String> scopesToResolve, MavenSession session,
                                             boolean aggregating )
        throws LifecycleExecutionException
    {
        Set<Artifact> artifacts =
            getProjectDependencies( project, scopesToCollect, scopesToResolve, session, aggregating );
        updateProjectArtifacts( project, artifacts );
    }

    private void updateProjectArtifacts( MavenProject project, Set<Artifact> artifacts )
    {
        project.setResolvedArtifacts( artifacts );

        if ( project.getDependencyArtifacts() == null )
        {
            project.setDependencyArtifacts( getDependencyArtifacts( project, artifacts ) );
        }
    }

    private Set<Artifact> getProjectDependencies( MavenProject project, Collection<String> scopesToCollect,
                                                  Collection<String> scopesToResolve, MavenSession session,
                                                  boolean aggregating )
        throws LifecycleExecutionException
    {
        Set<Artifact> artifacts;
        try
        {
            try
            {
                artifacts = projectDependenciesResolver.resolve( project, scopesToCollect, scopesToResolve, session );
            }
            catch ( MultipleArtifactsNotFoundException e )
            {
                /*
                * MNG-2277, the check below compensates for our bad plugin support where we ended up with aggregator
                * plugins that require dependency resolution although they usually run in phases of the build where project
                * artifacts haven't been assembled yet. The prime example of this is "mvn release:prepare".
                */
                if ( aggregating && areAllArtifactsInReactor( session.getProjects(), e.getMissingArtifacts() ) )
                {
                    logger.warn( "The following artifacts could not be resolved at this point of the build" +
                        " but seem to be part of the reactor:" );

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


    private Set<Artifact> getDependencyArtifacts( MavenProject project, Set<Artifact> artifacts )
    {
        Set<String> directDependencies = new HashSet<String>( project.getDependencies().size() * 2 );
        for ( Dependency dependency : project.getDependencies() )
        {
            directDependencies.add( dependency.getManagementKey() );
        }

        Set<Artifact> dependencyArtifacts = new LinkedHashSet<Artifact>( project.getDependencies().size() * 2 );
        for ( Artifact artifact : artifacts )
        {
            if ( directDependencies.contains( artifact.getDependencyConflictId() ) )
            {
                dependencyArtifacts.add( artifact );
            }
        }
        return dependencyArtifacts;
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
