package org.apache.maven.artifact.repository.metadata;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class DefaultRepositoryMetadataManager
    extends AbstractLogEnabled
    implements RepositoryMetadataManager
{

    // component requirement
    private WagonManager wagonManager;

    // only resolve repository metadata once per session...
    private Map cachedMetadata = new HashMap();

    public void resolveLocally( RepositoryMetadata metadata, ArtifactRepository local )
        throws RepositoryMetadataManagementException
    {
        resolve( metadata, null, local );
    }

    public void resolve( RepositoryMetadata metadata, ArtifactRepository remote, ArtifactRepository local )
        throws RepositoryMetadataManagementException
    {
        File metadataFile = (File) cachedMetadata.get( metadata.getRepositoryPath() );

        if ( metadataFile == null )
        {
            metadataFile = constructLocalRepositoryFile( metadata, local );

            if ( !metadataFile.exists() && remote != null )
            {
                try
                {
                    try
                    {
                        wagonManager.getRepositoryMetadata( metadata, remote, metadataFile );
                    }
                    catch ( ResourceDoesNotExistException e )
                    {
                        if ( !metadataFile.exists() )
                        {
                            throw new RepositoryMetadataManagementException( metadata,
                                                                             "Remote repository metadata not found.",
                                                                             e );
                        }
                        else
                        {
                            String message = "Cannot find " + metadata + " in remote repository - Using local copy.";

                            getLogger().info( message );

                            getLogger().debug( message, e );
                        }
                    }
                }
                catch ( TransferFailedException e )
                {
                    throw new RepositoryMetadataManagementException( metadata,
                                                                     "Failed to download repository metadata.", e );
                }
            }
            else
            {
                getLogger().info( "Using local copy of " + metadata + " from: " + metadataFile );
            }

            if ( metadataFile.exists() )
            {
                if ( !verifyFileNotEmpty( metadataFile ) )
                {
                    throw new InvalidRepositoryMetadataException( metadata, "Metadata located in file: " +
                        metadataFile + " appears to be corrupt (file is empty). DOWNLOAD FAILED." );
                }

                cachedMetadata.put( metadata.getRepositoryPath(), metadataFile );
            }
        }

        metadata.setFile( metadataFile );
    }

    private boolean verifyFileNotEmpty( File metadataFile )
    {
        InputStream verifyInputStream = null;

        try
        {
            verifyInputStream = new FileInputStream( metadataFile );

            return verifyInputStream.available() > 0;
        }
        catch ( IOException e )
        {
            return false;
        }
        finally
        {
            IOUtil.close( verifyInputStream );
        }
    }

    public void deploy( RepositoryMetadata metadata, ArtifactRepository remote )
        throws RepositoryMetadataManagementException
    {
        File metadataFile = metadata.getFile();

        try
        {
            wagonManager.putRepositoryMetadata( metadataFile, metadata, remote );

            metadata.setFile( metadataFile );
        }
        catch ( TransferFailedException e )
        {
            throw new RepositoryMetadataManagementException( metadata, "Failed to upload repository metadata.", e );
        }

    }

    public void install( RepositoryMetadata metadata, ArtifactRepository local )
        throws RepositoryMetadataManagementException
    {
        File metadataFile = constructLocalRepositoryFile( metadata, local );

        try
        {
            File dir = metadataFile.getParentFile();

            if ( !dir.exists() )
            {
                dir.mkdirs();
            }

            FileUtils.copyFile( metadata.getFile(), metadataFile );
        }
        catch ( IOException e )
        {
            throw new RepositoryMetadataManagementException( metadata, "Failed to install repository metadata.", e );
        }

    }

    public void purgeLocalCopy( RepositoryMetadata metadata, ArtifactRepository local )
        throws RepositoryMetadataManagementException
    {
        File metadataFile = constructLocalRepositoryFile( metadata, local );

        if ( metadataFile.exists() )
        {
            if ( !metadataFile.delete() )
            {
                throw new RepositoryMetadataManagementException( metadata,
                                                                 "Failed to purge local copy from: " + metadataFile );
            }
        }
    }

    private File constructLocalRepositoryFile( RepositoryMetadata metadata, ArtifactRepository local )
    {
        String metadataPath = local.formatAsFile( metadata.getRepositoryPath() );

        metadataPath = metadataPath.replace( File.separatorChar, '/' );

        return new File( local.getBasedir(), metadataPath );
    }

}
