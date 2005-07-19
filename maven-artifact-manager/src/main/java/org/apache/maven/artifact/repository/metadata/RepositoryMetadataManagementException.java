package org.apache.maven.artifact.repository.metadata;

public class RepositoryMetadataManagementException
    extends Exception
{

    private final RepositoryMetadata metadata;

    public RepositoryMetadataManagementException( RepositoryMetadata metadata, String message, Throwable cause )
    {
        super( "Failed to resolve repository metadata: " + metadata + ".\n\nOriginal message: " + message + "\n\nError was: " + cause.getMessage(), cause );
        
        this.metadata = metadata;
    }

    public RepositoryMetadataManagementException( RepositoryMetadata metadata, String message )
    {
        super( "Failed to resolve repository metadata: " + metadata + ".\n\nOriginal message: " + message );
        
        this.metadata = metadata;
    }

    public RepositoryMetadata getMetadata()
    {
        return metadata;
    }

}
