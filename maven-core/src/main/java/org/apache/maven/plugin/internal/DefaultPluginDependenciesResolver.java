package org.apache.maven.plugin.internal;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.ArtifactFilterManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Assists in resolving the dependencies of a plugin. <strong>Warning:</strong> This is an internal utility class that
 * is only public for technical reasons, it is not part of the public API. In particular, this class can be changed or
 * deleted without prior notice.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = PluginDependenciesResolver.class )
public class DefaultPluginDependenciesResolver
    implements PluginDependenciesResolver
{

    @Requirement
    protected RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    @Requirement
    private ArtifactFilterManager artifactFilterManager;

    public Artifact resolve( Plugin plugin, ArtifactResolutionRequest request )
        throws PluginResolutionException
    {
        Artifact pluginArtifact = repositorySystem.createPluginArtifact( plugin );

        request.setArtifact( pluginArtifact );
        request.setResolveRoot( true );
        request.setResolveTransitively( false );

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginResolutionException( plugin, e );
        }

        return pluginArtifact;
    }

    public List<Artifact> resolve( Plugin plugin, Artifact pluginArtifact, ArtifactResolutionRequest request,
                                   ArtifactFilter dependencyFilter )
        throws PluginResolutionException
    {
        if ( pluginArtifact == null )
        {
            pluginArtifact = repositorySystem.createPluginArtifact( plugin );
        }

        Set<Artifact> overrideArtifacts = new LinkedHashSet<Artifact>();
        for ( Dependency dependency : plugin.getDependencies() )
        {
	          if ( !Artifact.SCOPE_SYSTEM.equals( dependency.getScope() ) )
	          {
	              dependency.setScope( Artifact.SCOPE_RUNTIME );
	          }
            overrideArtifacts.add( repositorySystem.createDependencyArtifact( dependency ) );
        }

        ArtifactFilter collectionFilter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME_PLUS_SYSTEM );

        ArtifactFilter resolutionFilter = artifactFilterManager.getCoreArtifactFilter();

        PluginDependencyResolutionListener listener = new PluginDependencyResolutionListener( resolutionFilter );

        if ( dependencyFilter != null )
        {
            resolutionFilter = new AndArtifactFilter( Arrays.asList( resolutionFilter, dependencyFilter ) );
        }

        request.setArtifact( pluginArtifact );
        request.setArtifactDependencies( overrideArtifacts );
        request.setCollectionFilter( collectionFilter );
        request.setResolutionFilter( resolutionFilter );
        request.setResolveRoot( true );
        request.setResolveTransitively( true );
        request.addListener( listener );

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginResolutionException( plugin, e );
        }

        List<Artifact> pluginArtifacts = new ArrayList<Artifact>( result.getArtifacts() );

        listener.removeBannedDependencies( pluginArtifacts );

        addPlexusUtils( pluginArtifacts, plugin, request );

        return pluginArtifacts;
    }

    // backward-compatibility with Maven 2.x
    private void addPlexusUtils( List<Artifact> pluginArtifacts, Plugin plugin, RepositoryRequest repositoryRequest )
        throws PluginResolutionException
    {
        for ( Artifact artifact : pluginArtifacts )
        {
            if ( "org.codehaus.plexus:plexus-utils:jar".equals( artifact.getDependencyConflictId() ) )
            {
                return;
            }
        }

        Artifact plexusUtils =
            repositorySystem.createArtifact( "org.codehaus.plexus", "plexus-utils", "1.1", Artifact.SCOPE_RUNTIME,
                                             "jar" );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest( repositoryRequest );
        request.setArtifact( plexusUtils );
        request.setResolveRoot( true );
        request.setResolveTransitively( false );

        ArtifactResolutionResult result = repositorySystem.resolve( request );
        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new PluginResolutionException( plugin, e );
        }

        pluginArtifacts.add( plexusUtils );
    }

}
