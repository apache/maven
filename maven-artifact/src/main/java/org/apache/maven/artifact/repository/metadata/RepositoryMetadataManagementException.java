package org.apache.maven.artifact.repository.metadata;

public class RepositoryMetadataManagementException
    extends Exception
{

    private final RepositoryMetadata metadata;

    public RepositoryMetadataManagementException( RepositoryMetadata metadata )
    {
        super( "Failed to resolve repository metadata: " + metadata + ".");
        
        this.metadata = metadata;
    }

    public RepositoryMetadataManagementException( RepositoryMetadata metadata, String message, Throwable cause )
    {
        super( "Failed to resolve repository metadata: " + metadata + ". Error was: " + cause.getMessage(), cause );
        
        this.metadata = metadata;
    }

    public RepositoryMetadataManagementException( RepositoryMetadata metadata, String message )
    {
        super( "Failed to resolve repository metadata: " + metadata + ".");
        
        this.metadata = metadata;
    }
    
    public RepositoryMetadata getMetadata()
    {
        return metadata;
    }

}
