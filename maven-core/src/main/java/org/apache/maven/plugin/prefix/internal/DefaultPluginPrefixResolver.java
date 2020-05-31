package org.apache.maven.plugin.prefix.internal;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.MetadataReader;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.prefix.PluginPrefixRequest;
import org.apache.maven.plugin.prefix.PluginPrefixResolver;
import org.apache.maven.plugin.prefix.PluginPrefixResult;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;

/**
 * Resolves a plugin prefix.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultPluginPrefixResolver
    implements PluginPrefixResolver
{

    private static final String REPOSITORY_CONTEXT = "plugin";

    @Inject
    private Logger logger;

    @Inject
    private BuildPluginManager pluginManager;

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private MetadataReader metadataReader;

    public PluginPrefixResult resolve( PluginPrefixRequest request )
        throws NoPluginFoundForPrefixException
    {
        logger.debug( "Resolving plugin prefix " + request.getPrefix() + " from " + request.getPluginGroups() );

        PluginPrefixResult result = resolveFromProject( request );

        if ( result == null )
        {
            result = resolveFromRepository( request );

            if ( result == null )
            {
                throw new NoPluginFoundForPrefixException( request.getPrefix(), request.getPluginGroups(),
                                                           request.getRepositorySession().getLocalRepository(),
                                                           request.getRepositories() );
            }
            else if ( logger.isDebugEnabled() )
            {
                logger.debug( "Resolved plugin prefix " + request.getPrefix() + " to " + result.getGroupId() + ":"
                    + result.getArtifactId() + " from repository "
                    + ( result.getRepository() != null ? result.getRepository().getId() : "null" ) );
            }
        }
        else if ( logger.isDebugEnabled() )
        {
            logger.debug( "Resolved plugin prefix " + request.getPrefix() + " to " + result.getGroupId() + ":"
                + result.getArtifactId() + " from POM " + request.getPom() );
        }

        return result;
    }

    private PluginPrefixResult resolveFromProject( PluginPrefixRequest request )
    {
        PluginPrefixResult result = null;

        if ( request.getPom() != null && request.getPom().getBuild() != null )
        {
            Build build = request.getPom().getBuild();

            result = resolveFromProject( request, build.getPlugins() );

            if ( result == null && build.getPluginManagement() != null )
            {
                result = resolveFromProject( request, build.getPluginManagement().getPlugins() );
            }
        }

        return result;
    }

    private PluginPrefixResult resolveFromProject( PluginPrefixRequest request, List<Plugin> plugins )
    {
        for ( Plugin plugin : plugins )
        {
            try
            {
                PluginDescriptor pluginDescriptor =
                    pluginManager.loadPlugin( plugin, request.getRepositories(), request.getRepositorySession() );

                if ( request.getPrefix().equals( pluginDescriptor.getGoalPrefix() ) )
                {
                    return new DefaultPluginPrefixResult( plugin );
                }
            }
            catch ( Exception e )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.warn( "Failed to retrieve plugin descriptor for " + plugin.getId() + ": " + e.getMessage(),
                                 e );
                }
                else
                {
                    logger.warn( "Failed to retrieve plugin descriptor for " + plugin.getId() + ": " + e.getMessage() );
                }
            }
        }

        return null;
    }

    private PluginPrefixResult resolveFromRepository( PluginPrefixRequest request )
    {
        RequestTrace trace = RequestTrace.newChild( null, request );

        List<MetadataRequest> requests = new ArrayList<>();

        for ( String pluginGroup : request.getPluginGroups() )
        {
            org.eclipse.aether.metadata.Metadata metadata =
                new DefaultMetadata( pluginGroup, "maven-metadata.xml", DefaultMetadata.Nature.RELEASE_OR_SNAPSHOT );

            requests.add( new MetadataRequest( metadata, null, REPOSITORY_CONTEXT ).setTrace( trace ) );

            for ( RemoteRepository repository : request.getRepositories() )
            {
                requests.add( new MetadataRequest( metadata, repository, REPOSITORY_CONTEXT ).setTrace( trace ) );
            }
        }

        // initial try, use locally cached metadata

        List<MetadataResult> results = repositorySystem.resolveMetadata( request.getRepositorySession(), requests );
        requests.clear();

        PluginPrefixResult result = processResults( request, trace, results, requests );

        if ( result != null )
        {
            return result;
        }

        // second try, refetch all (possibly outdated) metadata that wasn't updated in the first attempt

        if ( !request.getRepositorySession().isOffline() && !requests.isEmpty() )
        {
            DefaultRepositorySystemSession session =
                new DefaultRepositorySystemSession( request.getRepositorySession() );
            session.setUpdatePolicy( RepositoryPolicy.UPDATE_POLICY_ALWAYS );

            results = repositorySystem.resolveMetadata( session, requests );

            return processResults( request, trace, results, null );
        }

        return null;
    }

    private PluginPrefixResult processResults( PluginPrefixRequest request, RequestTrace trace,
                                               List<MetadataResult> results, List<MetadataRequest> requests )
    {
        for ( MetadataResult res : results )
        {
            org.eclipse.aether.metadata.Metadata metadata = res.getMetadata();

            if ( metadata != null )
            {
                ArtifactRepository repository = res.getRequest().getRepository();
                if ( repository == null )
                {
                    repository = request.getRepositorySession().getLocalRepository();
                }

                PluginPrefixResult result =
                    resolveFromRepository( request, trace, metadata.getGroupId(), metadata, repository );

                if ( result != null )
                {
                    return result;
                }
            }

            if ( requests != null && !res.isUpdated() )
            {
                requests.add( res.getRequest() );
            }
        }

        return null;
    }

    private PluginPrefixResult resolveFromRepository( PluginPrefixRequest request, RequestTrace trace,
                                                      String pluginGroup,
                                                      org.eclipse.aether.metadata.Metadata metadata,
                                                      ArtifactRepository repository )
    {
        if ( metadata != null && metadata.getFile() != null && metadata.getFile().isFile() )
        {
            try
            {
                Map<String, ?> options = Collections.singletonMap( MetadataReader.IS_STRICT, Boolean.FALSE );

                Metadata pluginGroupMetadata = metadataReader.read( metadata.getFile(), options );

                List<org.apache.maven.artifact.repository.metadata.Plugin> plugins = pluginGroupMetadata.getPlugins();

                if ( plugins != null )
                {
                    for ( org.apache.maven.artifact.repository.metadata.Plugin plugin : plugins )
                    {
                        if ( request.getPrefix().equals( plugin.getPrefix() ) )
                        {
                            return new DefaultPluginPrefixResult( pluginGroup, plugin.getArtifactId(), repository );
                        }
                    }
                }
            }
            catch ( IOException e )
            {
                invalidMetadata( request.getRepositorySession(), trace, metadata, repository, e );
            }
        }

        return null;
    }

    private void invalidMetadata( RepositorySystemSession session, RequestTrace trace,
                                  org.eclipse.aether.metadata.Metadata metadata, ArtifactRepository repository,
                                  Exception exception )
    {
        RepositoryListener listener = session.getRepositoryListener();
        if ( listener != null )
        {
            RepositoryEvent.Builder event = new RepositoryEvent.Builder( session, EventType.METADATA_INVALID );
            event.setTrace( trace );
            event.setMetadata( metadata );
            event.setException( exception );
            event.setRepository( repository );
            listener.metadataInvalid( event.build() );
        }
    }

}
