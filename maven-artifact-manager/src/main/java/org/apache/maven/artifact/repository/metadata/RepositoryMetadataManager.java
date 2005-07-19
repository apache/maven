package org.apache.maven.artifact.repository.metadata;

import org.apache.maven.artifact.repository.ArtifactRepository;

public interface RepositoryMetadataManager
{
    
    void resolveLocally( RepositoryMetadata repositoryMetadata, ArtifactRepository local )
        throws RepositoryMetadataManagementException;

    void resolve( RepositoryMetadata repositoryMetadata, ArtifactRepository remote, ArtifactRepository local )
        throws RepositoryMetadataManagementException;
    
    void deploy( RepositoryMetadata repositoryMetadata, ArtifactRepository remote )
        throws RepositoryMetadataManagementException;
    
    void install( RepositoryMetadata repositoryMetadata, ArtifactRepository local, String remoteRepositoryId )
        throws RepositoryMetadataManagementException;
    
    void purgeLocalCopy( RepositoryMetadata repositoryMetadata, ArtifactRepository local )
        throws RepositoryMetadataManagementException;

}
