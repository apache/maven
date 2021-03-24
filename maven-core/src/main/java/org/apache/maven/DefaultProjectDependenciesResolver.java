package org.apache.maven;

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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.repository.RepositorySystem;

/**
 * @deprecated As of 3.2.2, and there is no direct replacement. This is an internal class which was not marked as such,
 *             but should have been.
 *
 */
@Deprecated
@Named
@Singleton
public class DefaultProjectDependenciesResolver
    implements ProjectDependenciesResolver
{

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private ResolutionErrorHandler resolutionErrorHandler;

    public Set<Artifact> resolve( MavenProject project, Collection<String> scopesToResolve, MavenSession session )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolve( Collections.singleton( project ), scopesToResolve, session );
    }

    public Set<Artifact> resolve( MavenProject project, Collection<String> scopesToCollect,
                                  Collection<String> scopesToResolve, MavenSession session )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        Set<MavenProject> mavenProjects = Collections.singleton( project );
        return resolveImpl( mavenProjects, scopesToCollect, scopesToResolve, session,
                            getIgnorableArtifacts( mavenProjects ) );
    }

    public Set<Artifact> resolve( Collection<? extends MavenProject> projects, Collection<String> scopesToResolve,
                                  MavenSession session )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveImpl( projects, null, scopesToResolve, session, getIgnorableArtifacts( projects ) );
    }

    public Set<Artifact> resolve( MavenProject project, Collection<String> scopesToCollect,
                                  Collection<String> scopesToResolve, MavenSession session,
                                  Set<Artifact> ignoreableArtifacts )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveImpl( Collections.singleton( project ), scopesToCollect, scopesToResolve, session,
                            getIgnorableArtifacts( ignoreableArtifacts ) );
    }


    private Set<Artifact> resolveImpl( Collection<? extends MavenProject> projects, Collection<String> scopesToCollect,
                                       Collection<String> scopesToResolve, MavenSession session,
                                       Set<String> projectIds )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        Set<Artifact> resolved = new LinkedHashSet<>();

        if ( projects == null || projects.isEmpty() )
        {
            return resolved;
        }

        if ( ( scopesToCollect == null || scopesToCollect.isEmpty() )
            && ( scopesToResolve == null || scopesToResolve.isEmpty() ) )
        {
            return resolved;
        }

        /*

        Logic for transitive global exclusions

        List<String> exclusions = new ArrayList<String>();

        for ( Dependency d : project.getDependencies() )
        {
            if ( d.getExclusions() != null )
            {
                for ( Exclusion e : d.getExclusions() )
                {
                    exclusions.add(  e.getGroupId() + ":" + e.getArtifactId() );
                }
            }
        }

        ArtifactFilter scopeFilter = new ScopeArtifactFilter( scope );

        ArtifactFilter filter;

        if ( ! exclusions.isEmpty() )
        {
            filter = new AndArtifactFilter( Arrays.asList( new ArtifactFilter[]{
                new ExcludesArtifactFilter( exclusions ), scopeFilter } ) );
        }
        else
        {
            filter = scopeFilter;
        }
        */

        CumulativeScopeArtifactFilter resolutionScopeFilter = new CumulativeScopeArtifactFilter( scopesToResolve );

        CumulativeScopeArtifactFilter collectionScopeFilter = new CumulativeScopeArtifactFilter( scopesToCollect );
        collectionScopeFilter = new CumulativeScopeArtifactFilter( collectionScopeFilter, resolutionScopeFilter );

        ArtifactResolutionRequest request =
            new ArtifactResolutionRequest().setResolveRoot( false ).setResolveTransitively( true ).setCollectionFilter(
                collectionScopeFilter ).setResolutionFilter( resolutionScopeFilter ).setLocalRepository(
                session.getLocalRepository() ).setOffline( session.isOffline() ).setForceUpdate(
                session.getRequest().isUpdateSnapshots() );
        request.setServers( session.getRequest().getServers() );
        request.setMirrors( session.getRequest().getMirrors() );
        request.setProxies( session.getRequest().getProxies() );

        for ( MavenProject project : projects )
        {
            request.setArtifact( new ProjectArtifact( project ) );
            request.setArtifactDependencies( project.getDependencyArtifacts() );
            request.setManagedVersionMap( project.getManagedVersionMap() );
            request.setRemoteRepositories( project.getRemoteArtifactRepositories() );

            ArtifactResolutionResult result = repositorySystem.resolve( request );

            try
            {
                resolutionErrorHandler.throwErrors( request, result );
            }
            catch ( MultipleArtifactsNotFoundException e )
            {

                Collection<Artifact> missing = new HashSet<>( e.getMissingArtifacts() );

                for ( Iterator<Artifact> it = missing.iterator(); it.hasNext(); )
                {
                    String key = ArtifactUtils.key( it.next() );
                    if ( projectIds.contains( key ) )
                    {
                        it.remove();
                    }
                }

                if ( !missing.isEmpty() )
                {
                    throw e;
                }
            }

            resolved.addAll( result.getArtifacts() );
        }

        return resolved;
    }


    private Set<String> getIgnorableArtifacts( Collection<? extends MavenProject> projects )
    {
        Set<String> projectIds = new HashSet<>( projects.size() * 2 );

        for ( MavenProject p : projects )
        {
            String key = ArtifactUtils.key( p.getGroupId(), p.getArtifactId(), p.getVersion() );
            projectIds.add( key );
        }
        return projectIds;
    }

    private Set<String> getIgnorableArtifacts( Iterable<Artifact> artifactIterable )
    {
        Set<String> projectIds = new HashSet<>();

        for ( Artifact artifact : artifactIterable )
        {
            String key = ArtifactUtils.key( artifact );
            projectIds.add( key );
        }
        return projectIds;
    }

}
