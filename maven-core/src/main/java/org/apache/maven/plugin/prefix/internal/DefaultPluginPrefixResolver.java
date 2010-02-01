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

import org.apache.maven.artifact.repository.ArtifactRepository;
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
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.ArtifactDoesNotExistException;
import org.apache.maven.repository.ArtifactTransferFailedException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Resolves a plugin prefix.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = PluginPrefixResolver.class )
public class DefaultPluginPrefixResolver
    implements PluginPrefixResolver
{

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
                                                           request.getLocalRepository(),
                                                           request.getRemoteRepositories() );
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
                PluginDescriptor pluginDescriptor = pluginManager.loadPlugin( plugin, request );

                if ( request.getPrefix().equals( pluginDescriptor.getGoalPrefix() ) )
                {
                    return new DefaultPluginPrefixResult( plugin );
                }
            }
            catch ( Exception e )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.warn( "Failed to retrieve plugin descriptor for " + plugin + ": " + e.getMessage(), e );
                }
                else
                {
                    logger.warn( "Failed to retrieve plugin descriptor for " + plugin + ": " + e.getMessage() );
                }
            }
        }

        return null;
    }

    private PluginPrefixResult resolveFromRepository( PluginPrefixRequest request )
    {
        ArtifactRepository localRepository = request.getLocalRepository();

        // Process all plugin groups in the local repository first to see if we get a hit. A developer may have been
        // developing a plugin locally and installing.
        //
        for ( String pluginGroup : request.getPluginGroups() )
        {
            String localPath = getLocalMetadataPath( pluginGroup, localRepository );

            File groupMetadataFile = new File( localRepository.getBasedir(), localPath );

            PluginPrefixResult result =
                resolveFromRepository( request, pluginGroup, groupMetadataFile, localRepository );

            if ( result != null )
            {
                return result;
            }
        }

        List<ArtifactRepository> recheck = new ArrayList<ArtifactRepository>();

        // Process all the remote repositories.
        //
        for ( String pluginGroup : request.getPluginGroups() )
        {
            for ( ArtifactRepository repository : request.getRemoteRepositories() )
            {
                String localPath = getLocalMetadataPath( pluginGroup, repository );

                File groupMetadataFile = new File( localRepository.getBasedir(), localPath );

                if ( !request.isOffline() && ( !groupMetadataFile.exists() || request.isForceUpdate() ) )
                {
                    String remotePath = getRemoteMetadataPath( pluginGroup, repository );

                    try
                    {
                        repositorySystem.retrieve( repository, groupMetadataFile, remotePath,
                                                   request.getTransferListener() );
                    }
                    catch ( ArtifactTransferFailedException e )
                    {
                        if ( logger.isDebugEnabled() )
                        {
                            logger.warn( "Failed to retrieve " + remotePath + ": " + e.getMessage(), e );
                        }
                        else
                        {
                            logger.warn( "Failed to retrieve " + remotePath + ": " + e.getMessage() );
                        }
                    }
                    catch ( ArtifactDoesNotExistException e )
                    {
                        continue;
                    }
                }
                else if ( !request.isOffline() && !request.isForceUpdate() )
                {
                    recheck.add( repository );
                }

                PluginPrefixResult result = resolveFromRepository( request, pluginGroup, groupMetadataFile, repository );

                if ( result != null )
                {
                    return result;
                }
            }
        }

        // Retry the remote repositories for which we previously only consulted the possibly outdated local cache.
        //
        for ( String pluginGroup : request.getPluginGroups() )
        {
            for ( ArtifactRepository repository : recheck )
            {
                String localPath = getLocalMetadataPath( pluginGroup, repository );

                File groupMetadataFile = new File( localRepository.getBasedir(), localPath );

                String remotePath = getRemoteMetadataPath( pluginGroup, repository );

                try
                {
                    repositorySystem.retrieve( repository, groupMetadataFile, remotePath, request.getTransferListener() );
                }
                catch ( ArtifactTransferFailedException e )
                {
                    if ( logger.isDebugEnabled() )
                    {
                        logger.warn( "Failed to retrieve " + remotePath + ": " + e.getMessage(), e );
                    }
                    else
                    {
                        logger.warn( "Failed to retrieve " + remotePath + ": " + e.getMessage() );
                    }
                }
                catch ( ArtifactDoesNotExistException e )
                {
                    continue;
                }

                PluginPrefixResult result = resolveFromRepository( request, pluginGroup, groupMetadataFile, repository );

                if ( result != null )
                {
                    return result;
                }
            }
        }

        return null;
    }

    private PluginPrefixResult resolveFromRepository( PluginPrefixRequest request, String pluginGroup,
                                                      File metadataFile, ArtifactRepository repository )
    {
        if ( metadataFile.isFile() )
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

    private String getLocalMetadataPath( String groupId, ArtifactRepository repository )
    {
        return groupId.replace( '.', '/' ) + "/" + "maven-metadata-" + repository.getId() + ".xml";
    }

    private String getRemoteMetadataPath( String groupId, ArtifactRepository repository )
    {
        return groupId.replace( '.', '/' ) + "/" + "maven-metadata.xml";
    }

}
