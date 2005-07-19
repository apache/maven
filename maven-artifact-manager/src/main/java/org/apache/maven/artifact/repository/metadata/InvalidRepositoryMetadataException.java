package org.apache.maven.artifact.repository.metadata;

public class InvalidRepositoryMetadataException
    extends RepositoryMetadataManagementException
{

    public InvalidRepositoryMetadataException( RepositoryMetadata metadata, String message, Throwable cause )
    {
        super( metadata, message, cause );
    }

    public InvalidRepositoryMetadataException( RepositoryMetadata metadata, String message )
    {
        super( metadata, message );
    }

}
