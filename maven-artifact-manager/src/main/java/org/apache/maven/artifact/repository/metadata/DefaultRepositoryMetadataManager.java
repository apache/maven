package org.apache.maven.artifact.repository.metadata;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
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
                    File tempMetadataFile = File.createTempFile( "plugins.xml", null );

                    try
                    {
                        wagonManager.getRepositoryMetadata( metadata, remote, tempMetadataFile );

                        if ( !metadataFile.exists()
                            || ( metadataFile.lastModified() <= tempMetadataFile.lastModified() ) )
                        {
                            if ( !tempMetadataFile.renameTo( metadataFile ) )
                            {
                                FileUtils.copyFile( tempMetadataFile, metadataFile );

                                tempMetadataFile.delete();
                            }
                        }
                    }
                    catch ( ResourceDoesNotExistException e )
                    {
                        if ( !metadataFile.exists() )
                        {
                            throw new RepositoryMetadataManagementException( metadata,
                                                                             "Remote repository metadata not found.", e );
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
                catch ( IOException e )
                {
                    throw new RepositoryMetadataManagementException(
                                                                     metadata,
                                                                     "Error constructing temporary metadata download file.",
                                                                     e );
                }
            }
            else
            {
                getLogger().info( "Using local copy of " + metadata + " from: " + metadataFile );
            }

            metadata.setFile( metadataFile );
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
                throw new RepositoryMetadataManagementException( metadata, "Failed to purge local copy from: " + metadataFile );
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
