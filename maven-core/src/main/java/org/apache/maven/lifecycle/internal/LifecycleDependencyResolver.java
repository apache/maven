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

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.filter.AndDependencyFilter;
import org.sonatype.aether.util.filter.ScopeDependencyFilter;

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
@Component(role = LifecycleDependencyResolver.class)
public class LifecycleDependencyResolver
{

    @Requirement
    private ProjectDependenciesResolver dependenciesResolver;

    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactFactory artifactFactory;

    @SuppressWarnings({"UnusedDeclaration"})
    public LifecycleDependencyResolver()
    {
    }

    public LifecycleDependencyResolver( ProjectDependenciesResolver projectDependenciesResolver, Logger logger )
    {
        this.dependenciesResolver = projectDependenciesResolver;
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
            getDependencies( project, scopesToCollect, scopesToResolve, session, aggregating, projectArtifacts );

        project.setResolvedArtifacts( artifacts );

        Map<String, Artifact> map = new HashMap<String, Artifact>();
        for ( Artifact artifact : artifacts )
        {
            map.put( artifact.getId(), artifact );
        }
        for ( Artifact artifact : project.getDependencyArtifacts() )
        {
            if ( artifact.getFile() == null )
            {
                Artifact resolved = map.get( artifact.getId() );
                if ( resolved != null )
                {
                    artifact.setFile( resolved.getFile() );
                    artifact.setDependencyTrail( resolved.getDependencyTrail() );
                    artifact.setResolvedVersion( resolved.getVersion() );
                    artifact.setResolved( true );
                }
            }
        }
    }

    private Set<Artifact> getDependencies( MavenProject project, Collection<String> scopesToCollect,
                                           Collection<String> scopesToResolve, MavenSession session,
                                           boolean aggregating, Set<Artifact> projectArtifacts )
        throws LifecycleExecutionException
    {
        if ( scopesToCollect == null )
        {
            scopesToCollect = Collections.emptySet();
        }
        if ( scopesToResolve == null )
        {
            scopesToResolve = Collections.emptySet();
        }

        if ( scopesToCollect.isEmpty() && scopesToResolve.isEmpty() )
        {
            return new LinkedHashSet<Artifact>();
        }

        scopesToCollect = new HashSet<String>( scopesToCollect );
        scopesToCollect.addAll( scopesToResolve );

        DependencyFilter collectionFilter = new ScopeDependencyFilter( null, negate( scopesToCollect ) );
        DependencyFilter resolutionFilter = new ScopeDependencyFilter( null, negate( scopesToResolve ) );
        resolutionFilter = AndDependencyFilter.newInstance( collectionFilter, resolutionFilter );
        resolutionFilter =
            AndDependencyFilter.newInstance( resolutionFilter, new ReactorDependencyFilter( projectArtifacts ) );

        DependencyResolutionResult result;
        try
        {
            DefaultDependencyResolutionRequest request =
                new DefaultDependencyResolutionRequest( project, session.getRepositorySession() );
            request.setResolutionFilter( resolutionFilter );

            result = dependenciesResolver.resolve( request );
        }
        catch ( DependencyResolutionException e )
        {
            result = e.getResult();

            /*
             * MNG-2277, the check below compensates for our bad plugin support where we ended up with aggregator
             * plugins that require dependency resolution although they usually run in phases of the build where project
             * artifacts haven't been assembled yet. The prime example of this is "mvn release:prepare".
             */
            if ( aggregating && areAllDependenciesInReactor( session.getProjects(), result.getUnresolvedDependencies() ) )
            {
                logger.warn( "The following dependencies could not be resolved at this point of the build"
                    + " but seem to be part of the reactor:" );

                for ( Dependency dependency : result.getUnresolvedDependencies() )
                {
                    logger.warn( "o " + dependency );
                }

                logger.warn( "Try running the build up to the lifecycle phase \"package\"" );
            }
            else
            {
                throw new LifecycleExecutionException( null, project, e );
            }
        }

        Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
        if ( result.getDependencyGraph() != null && !result.getDependencyGraph().getChildren().isEmpty() )
        {
            RepositoryUtils.toArtifacts( artifacts, result.getDependencyGraph().getChildren(),
                                         Collections.singletonList( project.getArtifact().getId() ), collectionFilter );
        }
        return artifacts;
    }

    private boolean areAllDependenciesInReactor( Collection<MavenProject> projects, Collection<Dependency> dependencies )
    {
        Set<String> projectKeys = getReactorProjectKeys( projects );

        for ( Dependency dependency : dependencies )
        {
            org.sonatype.aether.artifact.Artifact a = dependency.getArtifact();
            String key = ArtifactUtils.key( a.getGroupId(), a.getArtifactId(), a.getVersion() );
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

    private Collection<String> negate( Collection<String> scopes )
    {
        Collection<String> result = new HashSet<String>();
        Collections.addAll( result, "system", "compile", "provided", "runtime", "test" );

        for ( String scope : scopes )
        {
            if ( "compile".equals( scope ) )
            {
                result.remove( "compile" );
                result.remove( "system" );
                result.remove( "provided" );
            }
            else if ( "runtime".equals( scope ) )
            {
                result.remove( "compile" );
                result.remove( "runtime" );
            }
            else if ( "compile+runtime".equals( scope ) )
            {
                result.remove( "compile" );
                result.remove( "system" );
                result.remove( "provided" );
                result.remove( "runtime" );
            }
            else if ( "runtime+system".equals( scope ) )
            {
                result.remove( "compile" );
                result.remove( "system" );
                result.remove( "runtime" );
            }
            else if ( "test".equals( scope ) )
            {
                result.clear();
            }
        }

        return result;
    }

    private static class ReactorDependencyFilter
        implements DependencyFilter
    {

        private Set<String> keys = new HashSet<String>();

        public ReactorDependencyFilter( Collection<Artifact> artifacts )
        {
            for ( Artifact artifact : artifacts )
            {
                String key = ArtifactUtils.key( artifact );
                keys.add( key );
            }
        }

        public boolean accept( DependencyNode node, List<DependencyNode> parents )
        {
            Dependency dependency = node.getDependency();
            if ( dependency != null )
            {
                org.sonatype.aether.artifact.Artifact a = dependency.getArtifact();
                String key = ArtifactUtils.key( a.getGroupId(), a.getArtifactId(), a.getVersion() );
                return !keys.contains( key );
            }
            return false;
        }

    }

}
