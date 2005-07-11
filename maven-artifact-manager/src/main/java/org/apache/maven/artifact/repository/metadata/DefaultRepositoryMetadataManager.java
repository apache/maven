package org.apache.maven.artifact.repository.metadata;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

public class DefaultRepositoryMetadataManager
    extends AbstractLogEnabled
    implements RepositoryMetadataManager
{

    // component requirement
    private WagonManager wagonManager;

    public void resolve( RepositoryMetadata metadata, ArtifactRepository remote, ArtifactRepository local, String remoteId )
        throws RepositoryMetadataManagementException
    {
        String metadataPath = local.formatAsFile( metadata.getRepositoryPath() );

        String realignedPath = metadataPath.replace( File.separatorChar, '/' );

        if ( !realignedPath.startsWith( "/" ) )
        {
            realignedPath = "/" + realignedPath;
        }

        realignedPath = "/REPOSITORY-INF/" + remoteId + realignedPath;

        File metadataFile = new File( local.getBasedir(), realignedPath );

        if ( remote == null )
        {
            if ( metadataFile.exists() )
            {
                getLogger().warn( "Cannot retrieve repository metadata for: " + metadataPath + ". Using locally cached version instead." );
                
                getLogger().debug( "Error retrieving repository metadata: " + metadataPath + ". Reason: repository is null." );
                
                metadata.setFile( metadataFile );
            }
            else
            {
                throw new RepositoryMetadataManagementException( metadata, "Cannot retrieve repository metadata from null repository." );
            }
        }
        else
        {
            try
            {
                wagonManager.getRepositoryMetadata( metadata, remote, metadataFile );

                metadata.setFile( metadataFile );
            }
            catch ( TransferFailedException e )
            {
                throw new RepositoryMetadataManagementException( metadata, "Failed to download repository metadata.", e );
            }
            catch ( ResourceDoesNotExistException e )
            {
                if ( metadataFile.exists() )
                {
                    getLogger().warn( "Cannot find repository metadata for: " + metadataPath + ". Using locally cached version instead." );
                    getLogger().debug( "Error retrieving repository metadata: " + metadataPath, e );
                    
                    metadata.setFile( metadataFile );
                }
                else
                {
                    throw new RepositoryMetadataManagementException( metadata, "Remote repository metadata not found.", e );
                }
            }
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

    public void install( RepositoryMetadata metadata, ArtifactRepository local, String remoteRepositoryId )
    throws RepositoryMetadataManagementException
    {
        String realignedPath = local.formatAsFile( metadata.getRepositoryPath() );

        realignedPath = realignedPath.replace( File.separatorChar, '/' );

        if ( !realignedPath.startsWith( "/" ) )
        {
            realignedPath = "/" + realignedPath;
        }

        realignedPath = "/REPOSITORY-INF/" + remoteRepositoryId + realignedPath;

        File metadataFile = new File( local.getBasedir(), realignedPath ).getAbsoluteFile();

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

}
