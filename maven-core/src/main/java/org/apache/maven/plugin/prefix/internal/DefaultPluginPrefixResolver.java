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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataReadException;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.prefix.PluginPrefixRequest;
import org.apache.maven.plugin.prefix.PluginPrefixResolver;
import org.apache.maven.plugin.prefix.PluginPrefixResult;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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

    public PluginPrefixResult resolve( PluginPrefixRequest request )
        throws NoPluginFoundForPrefixException
    {
        PluginPrefixResult result = resolveFromProject( request );

        if ( result == null )
        {
            result = resolveFromRepository( request );

            if ( result == null )
            {
                throw new NoPluginFoundForPrefixException( request.getPrefix(), request.getLocalRepository(),
                                                           request.getRemoteRepositories() );
            }
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
            String localPath =
                pluginGroup.replace( '.', '/' ) + "/" + "maven-metadata-" + localRepository.getId() + ".xml";

            File destination = new File( localRepository.getBasedir(), localPath );

            PluginPrefixResult result = resolveFromRepository( request, pluginGroup, destination, localRepository );

            if ( result != null )
            {
                return result;
            }
        }

        // Process all the remote repositories.
        //
        for ( String pluginGroup : request.getPluginGroups() )
        {
            for ( ArtifactRepository repository : request.getRemoteRepositories() )
            {
                String localPath =
                    pluginGroup.replace( '.', '/' ) + "/" + "maven-metadata-" + repository.getId() + ".xml";

                File destination = new File( localRepository.getBasedir(), localPath );

                if ( !request.isOffline() )
                {
                    String remotePath = pluginGroup.replace( '.', '/' ) + "/" + "maven-metadata.xml";

                    try
                    {
                        repositorySystem.retrieve( repository, destination, remotePath, null );
                    }
                    catch ( TransferFailedException e )
                    {
                        if ( logger.isDebugEnabled() )
                        {
                            logger.warn( "Failed to retrieve " + remotePath + ": " + e.getMessage(), e );
                        }
                        else
                        {
                            logger.warn( "Failed to retrieve " + remotePath + ": " + e.getMessage() );
                        }

                        continue;
                    }
                    catch ( ResourceDoesNotExistException e )
                    {
                        continue;
                    }
                }

                PluginPrefixResult result = resolveFromRepository( request, pluginGroup, destination, repository );

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
                Metadata pluginGroupMetadata = readMetadata( metadataFile );

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
            catch ( RepositoryMetadataReadException e )
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

    private Metadata readMetadata( File mappingFile )
        throws RepositoryMetadataReadException
    {
        Metadata result;

        Reader reader = null;
        try
        {
            reader = ReaderFactory.newXmlReader( mappingFile );

            MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();

            result = mappingReader.read( reader, false );
        }
        catch ( FileNotFoundException e )
        {
            throw new RepositoryMetadataReadException( "Cannot read metadata from '" + mappingFile + "'", e );
        }
        catch ( IOException e )
        {
            throw new RepositoryMetadataReadException( "Cannot read metadata from '" + mappingFile + "': "
                + e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new RepositoryMetadataReadException( "Cannot read metadata from '" + mappingFile + "': "
                + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        return result;
    }

}
