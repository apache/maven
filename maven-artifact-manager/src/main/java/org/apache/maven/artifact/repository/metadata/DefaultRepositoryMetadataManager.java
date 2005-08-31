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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DefaultRepositoryMetadataManager
    extends AbstractLogEnabled
    implements RepositoryMetadataManager
{
    // component requirement
    private WagonManager wagonManager;

    /**
     * @todo very primitve. Probably we can cache artifacts themselves in a central location, as well as reset the flag over time in a long running process.
     */
    private Set cachedMetadata = new HashSet();

    public void resolve( RepositoryMetadata metadata, List repositories, ArtifactRepository local )
        throws RepositoryMetadataManagementException
    {
        boolean alreadyResolved = alreadyResolved( metadata );
        if ( !alreadyResolved )
        {
            for ( Iterator i = repositories.iterator(); i.hasNext(); )
            {
                ArtifactRepository repository = (ArtifactRepository) i.next();

                // TODO: replace with a more general repository update mechanism like artifact metadata uses
                // (Actually, this should now supersede artifact metadata...)
                File metadataFile = new File( local.getBasedir(), local.pathOfRepositoryMetadata( metadata ) );

                if ( !metadataFile.exists() )
                {
                    try
                    {
                        try
                        {
                            wagonManager.getRepositoryMetadata( metadata, repository, metadataFile );
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
                                String message = "Cannot find " + metadata +
                                    " in remote repository - Using local copy.";

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

                cachedMetadata.add( metadata.getRepositoryPath() );
            }
        }
    }

    public void deploy( File source, RepositoryMetadata metadata, ArtifactRepository remote )
        throws RepositoryMetadataManagementException
    {
        try
        {
            wagonManager.putRepositoryMetadata( source, metadata, remote );
        }
        catch ( TransferFailedException e )
        {
            throw new RepositoryMetadataManagementException( metadata, "Failed to upload repository metadata.", e );
        }

    }

    public void install( File source, RepositoryMetadata metadata, ArtifactRepository local )
        throws RepositoryMetadataManagementException
    {
        File metadataFile = new File( local.getBasedir(), local.pathOfRepositoryMetadata( metadata ) );

        try
        {
            File dir = metadataFile.getParentFile();

            if ( !dir.exists() )
            {
                dir.mkdirs();
            }

            FileUtils.copyFile( source, metadataFile );
        }
        catch ( IOException e )
        {
            throw new RepositoryMetadataManagementException( metadata, "Failed to install repository metadata.", e );
        }

    }

    private boolean alreadyResolved( RepositoryMetadata metadata )
    {
        return cachedMetadata.contains( metadata.getRepositoryPath() );
    }
}
