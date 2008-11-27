package org.apache.maven.artifact.resolver.metadata;

import java.util.Collection;

import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * 
 * @author Jason van Zyl
 *  
 */
public class MetadataResolution
{
    /** resolved MD  */
    private ArtifactMetadata artifactMetadata;

    /** repositories, added by this POM  */
    private Collection<ArtifactRepository> metadataRepositories;
    //-------------------------------------------------------------------
    public MetadataResolution( ArtifactMetadata artifactMetadata )
    {
        this.artifactMetadata = artifactMetadata;
    }
    //-------------------------------------------------------------------
    public MetadataResolution( ArtifactMetadata artifactMetadata,
    		Collection<ArtifactRepository> metadataRepositories )
    {
    	this( artifactMetadata );
        this.metadataRepositories = metadataRepositories;
    }
    //-------------------------------------------------------------------
	public Collection<ArtifactRepository> getMetadataRepositories()
	{
		return metadataRepositories;
	}

	public void setMetadataRepositories(
			Collection<ArtifactRepository> metadataRepositories)
	{
		this.metadataRepositories = metadataRepositories;
	}
    //-------------------------------------------------------------------
	public ArtifactMetadata getArtifactMetadata()
	{
		return artifactMetadata;
	}

	public void setArtifactMetadata(ArtifactMetadata artifactMetadata)
	{
		this.artifactMetadata = artifactMetadata;
	}
    //-------------------------------------------------------------------
    //-------------------------------------------------------------------
}
