package org.apache.maven.artifact.repository.metadata;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;

import java.io.File;

public class DefaultRepositoryMetadataManager
    implements RepositoryMetadataManager
{

    // component requirement
    private WagonManager wagonManager;

    public void get( RepositoryMetadata metadata, ArtifactRepository remote, ArtifactRepository local )
        throws RepositoryMetadataManagementException
    {
        String realignedPath = local.formatDirectory( metadata.getRepositoryPath() );

        realignedPath = realignedPath.replace( File.separatorChar, '/' );

        if ( !realignedPath.startsWith( "/" ) )
        {
            realignedPath = "/" + realignedPath;
        }

        realignedPath = "/REPOSITORY-INF/" + remote.getId() + realignedPath;

        File metadataFile = new File( local.getBasedir(), realignedPath );

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
            throw new RepositoryMetadataManagementException( metadata, "Remote repository metadata not found.", e );
        }
    }

    public void put( RepositoryMetadata metadata, ArtifactRepository remote )
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

}
