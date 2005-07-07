package org.apache.maven.artifact.repository.metadata;

import org.apache.maven.artifact.repository.ArtifactRepository;

public interface RepositoryMetadataManager
{

    void get( RepositoryMetadata repositoryMetadata, ArtifactRepository remote, ArtifactRepository local )
        throws RepositoryMetadataManagementException;
    
    void put( RepositoryMetadata repositoryMetadata, ArtifactRepository remote )
        throws RepositoryMetadataManagementException;

}
