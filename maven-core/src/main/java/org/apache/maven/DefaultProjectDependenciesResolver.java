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
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.OrArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
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
    
    public Set<Artifact> resolve( MavenProject project, Collection<String> scopes, RepositoryRequest repositoryRequest )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {        
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

        OrArtifactFilter scopeFilter = new OrArtifactFilter();

        for ( String scope : scopes )
        {
            scopeFilter.add( new ScopeArtifactFilter( scope ) );
        }

        ArtifactFilter filter = scopeFilter; 

        ArtifactResolutionRequest request = new ArtifactResolutionRequest( repositoryRequest )
            .setArtifact( new ProjectArtifact( project ) )
            .setResolveRoot( false )
            .setResolveTransitively( true )
            .setManagedVersionMap( project.getManagedVersionMap() )
            .setFilter( filter );
        // FIXME setTransferListener
        
        ArtifactResolutionResult result = repositorySystem.resolve( request );                
        resolutionErrorHandler.throwErrors( request, result );
        project.setArtifacts( result.getArtifacts() );
        return result.getArtifacts();        
    }  
}
