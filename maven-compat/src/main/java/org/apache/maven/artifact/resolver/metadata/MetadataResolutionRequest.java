package org.apache.maven.artifact.resolver.metadata;

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;


/** @author Oleg Gusakov */
public class MetadataResolutionRequest
{
    protected ArtifactMetadata query;
    protected ArtifactRepository localRepository;
    protected List<ArtifactRepository> remoteRepositories;

    //--------------------------------------------------------------------
    public MetadataResolutionRequest()
    {
    }

    //--------------------------------------------------------------------
    public MetadataResolutionRequest( ArtifactMetadata query,
                                      ArtifactRepository localRepository,
                                      List<ArtifactRepository> remoteRepositories )
    {
        this.query = query;
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
    }

    //--------------------------------------------------------------------
    public ArtifactMetadata getQuery()
    {
        return query;
    }

    public void setQuery( ArtifactMetadata query )
    {
        this.query = query;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return remoteRepositories;
    }

    public void setRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
    }
    //--------------------------------------------------------------------
    //--------------------------------------------------------------------
}
