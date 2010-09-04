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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.repository.ArtifactRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;
import org.sonatype.aether.transfer.MetadataNotFoundException;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.metadata.DefaultMetadata;

/**
 * Resolves a plugin prefix.
 * 
 * @since 3.0
 * @author Benjamin Bentmann
 */
@Component( role = PluginPrefixResolver.class )
public class DefaultPluginPrefixResolver
    implements PluginPrefixResolver
{

    private static final String REPOSITORY_CONTEXT = "plugin";

    @Requirement
    private Logger logger;

    @Requirement
    private BuildPluginManager pluginManager;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
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
        List<MetadataRequest> requests = new ArrayList<MetadataRequest>();

        for ( String pluginGroup : request.getPluginGroups() )
        {
            org.sonatype.aether.metadata.Metadata metadata =
                new DefaultMetadata( pluginGroup, "maven-metadata.xml", DefaultMetadata.Nature.RELEASE_OR_SNAPSHOT );

            requests.add( new MetadataRequest( metadata, null, REPOSITORY_CONTEXT ) );

            for ( RemoteRepository repository : request.getRepositories() )
            {
                requests.add( new MetadataRequest( metadata, repository, REPOSITORY_CONTEXT ) );
            }
        }

        // initial try, use locally cached metadata

        List<MetadataResult> results = repositorySystem.resolveMetadata( request.getRepositorySession(), requests );
        requests.clear();

        PluginPrefixResult result = processResults( request, results, requests );

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

            return processResults( request, results, null );
        }

        return null;
    }

    private PluginPrefixResult processResults( PluginPrefixRequest request, List<MetadataResult> results,
                                               List<MetadataRequest> requests )
    {
        for ( MetadataResult res : results )
        {
            if ( res.getException() != null )
            {
                if ( res.getException() instanceof MetadataNotFoundException )
                {
                    logger.debug( "Could not find " + res.getRequest().getMetadata() + " in "
                        + res.getRequest().getRepository() );
                }
                else if ( logger.isDebugEnabled() )
                {
                    logger.warn( "Could not retrieve " + res.getRequest().getMetadata() + " from "
                        + res.getRequest().getRepository() + ": " + res.getException().getMessage(), res.getException() );
                }
                else
                {
                    logger.warn( "Could not retrieve " + res.getRequest().getMetadata() + " from "
                        + res.getRequest().getRepository() + ": " + res.getException().getMessage() );
                }
            }

            org.sonatype.aether.metadata.Metadata metadata = res.getMetadata();

            if ( metadata != null )
            {
                ArtifactRepository repository = res.getRequest().getRepository();
                if ( repository == null )
                {
                    repository = request.getRepositorySession().getLocalRepository();
                }

                PluginPrefixResult result =
                    resolveFromRepository( request, metadata.getGroupId(), metadata.getFile(), repository );

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

    private PluginPrefixResult resolveFromRepository( PluginPrefixRequest request, String pluginGroup,
                                                      File metadataFile, ArtifactRepository repository )
    {
        if ( metadataFile != null && metadataFile.isFile() )
        {
            try
            {
                Map<String, ?> options = Collections.singletonMap( MetadataReader.IS_STRICT, Boolean.FALSE );

                Metadata pluginGroupMetadata = metadataReader.read( metadataFile, options );

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
                if ( logger.isDebugEnabled() )
                {
                    logger.warn( "Error reading plugin group metadata: " + e.getMessage(), e );
                }
                else
                {
                    logger.warn( "Error reading plugin group metadata: " + e.getMessage() );
                }
            }
        }

        return null;
    }

}
