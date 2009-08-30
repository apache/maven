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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.MultipleArtifactsNotFoundException;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role=ProjectDependenciesResolver.class)
public class DefaultProjectDependenciesResolver
    implements ProjectDependenciesResolver
{

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    public Set<Artifact> resolve( MavenProject project, Collection<String> scopes, MavenSession session )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolve( Collections.singleton( project ), scopes, session );
    }

    public Set<Artifact> resolve( Collection<? extends MavenProject> projects, Collection<String> scopes,
                                  MavenSession session )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        Set<Artifact> resolved = new LinkedHashSet<Artifact>();

        if ( projects == null || projects.isEmpty() )
        {
            return resolved;
        }

        /*
        
        Logic for transitve global exclusions
         
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
            filter = new AndArtifactFilter( Arrays.asList( new ArtifactFilter[]{ new ExcludesArtifactFilter( exclusions ), scopeFilter } ) );
        }
        else
        {
            filter = scopeFilter;
        }        
        */

        ArtifactFilter scopeFilter = new CumulativeScopeArtifactFilter( scopes );

        ArtifactFilter filter = scopeFilter; 

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setResolveRoot( false )
            .setResolveTransitively( true )
            .setFilter( filter )
            .setLocalRepository( session.getLocalRepository() )
            .setOffline( session.isOffline() )
            .setCache( session.getRepositoryCache() );
        // FIXME setTransferListener

        Set<String> projectIds = null;

        for ( MavenProject project : projects )
        {
            request.setArtifact( new ProjectArtifact( project ) );
            request.setManagedVersionMap( project.getManagedVersionMap() );
            request.setRemoteRepositories( project.getRemoteArtifactRepositories() );

            ArtifactResolutionResult result = repositorySystem.resolve( request );

            try
            {
                resolutionErrorHandler.throwErrors( request, result );
            }
            catch ( MultipleArtifactsNotFoundException e )
            {
                if ( projectIds == null )
                {
                    projectIds = new HashSet<String>( projects.size() * 2 );

                    for ( MavenProject p : projects )
                    {
                        String key = ArtifactUtils.key( p.getGroupId(), p.getArtifactId(), p.getVersion() );
                        projectIds.add( key );
                    }
                }

                Collection<Artifact> missing = new HashSet<Artifact>( e.getMissingArtifacts() );

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

}
