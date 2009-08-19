package org.apache.maven.plugin.version.internal;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataReadException;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Resolves a version for a plugin.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = PluginVersionResolver.class )
public class DefaultPluginVersionResolver
    implements PluginVersionResolver
{

    @Requirement
    private Logger logger;

    @Requirement
    private RepositorySystem repositorySystem;

    public PluginVersionResult resolve( PluginVersionRequest request )
        throws PluginVersionResolutionException
    {
        DefaultPluginVersionResult result = new DefaultPluginVersionResult();

        Throwable error = null;

        ArtifactRepository localRepository = request.getLocalRepository();

        File artifactMetadataFile = null;

        String localPath;

        // Search in remote repositories for a (released) version.
        //
        // maven-metadata-{central|nexus|...}.xml
        //
        // TODO: we should cycle through the repositories but take the repository which actually
        // satisfied the prefix.
        for ( ArtifactRepository repository : request.getRemoteRepositories() )
        {
            localPath =
                request.getGroupId().replace( '.', '/' ) + "/" + request.getArtifactId() + "/maven-metadata-"
                    + repository.getId() + ".xml";

            artifactMetadataFile = new File( localRepository.getBasedir(), localPath );

            if ( !artifactMetadataFile.exists() /* || user requests snapshot updates */)
            {
                String remotePath =
                    request.getGroupId().replace( '.', '/' ) + "/" + request.getArtifactId() + "/maven-metadata.xml";

                try
                {
                    repositorySystem.retrieve( repository, artifactMetadataFile, remotePath, null );
                }
                catch ( TransferFailedException e )
                {
                    error = e;

                    logger.debug( "Failed to retrieve " + remotePath, e );

                    continue;
                }
                catch ( ResourceDoesNotExistException e )
                {
                    continue;
                }
            }

            result.setRepository( repository );

            break;
        }

        // Search in the local repositiory for a (development) version
        //
        // maven-metadata-local.xml
        //
        if ( artifactMetadataFile == null || !artifactMetadataFile.exists() )
        {
            localPath =
                request.getGroupId().replace( '.', '/' ) + "/" + request.getArtifactId() + "/maven-metadata-"
                    + localRepository.getId() + ".xml";

            artifactMetadataFile = new File( localRepository.getBasedir(), localPath );

            result.setRepository( localRepository );
        }

        if ( artifactMetadataFile.exists() )
        {
            logger.debug( "Extracting version for plugin " + request.getGroupId() + ':' + request.getArtifactId()
                + " from " + artifactMetadataFile );

            try
            {
                Metadata pluginMetadata = readMetadata( artifactMetadataFile );

                if ( pluginMetadata.getVersioning() != null )
                {
                    String release = pluginMetadata.getVersioning().getRelease();

                    if ( StringUtils.isNotEmpty( release ) )
                    {
                        result.setVersion( release );
                    }
                    else
                    {
                        String latest = pluginMetadata.getVersioning().getLatest();

                        if ( StringUtils.isNotEmpty( latest ) )
                        {
                            result.setVersion( latest );
                        }
                    }
                }
            }
            catch ( RepositoryMetadataReadException e )
            {
                throw new PluginVersionResolutionException( request.getGroupId(), request.getArtifactId(),
                                                            e.getMessage(), e );
            }
        }
        else if ( error != null )
        {
            throw new PluginVersionResolutionException( request.getGroupId(), request.getArtifactId(),
                                                        error.getMessage(), error );
        }

        if ( StringUtils.isEmpty( result.getVersion() ) )
        {
            throw new PluginVersionResolutionException( request.getGroupId(), request.getArtifactId(),
                                                        "Plugin not found in any repository" );
        }

        return result;
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
