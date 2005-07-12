package org.apache.maven.artifact.transform;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.AbstractVersionArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.LatestArtifactMetadata;
import org.apache.maven.artifact.metadata.VersionArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.io.IOException;
import java.util.List;

public class LatestArtifactTransformation
    extends AbstractVersionTransformation
{
    public static final String LATEST_VERSION = "LATEST";

    public void transformForResolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        if ( LATEST_VERSION.equals( artifact.getVersion() ) )
        {
            String version = resolveVersion( artifact, localRepository, remoteRepositories );
            if ( !version.equals( artifact.getVersion() ) )
            {
                artifact.setBaseVersion( version );
                artifact.updateVersion( version, localRepository );
            }
        }
    }

    public void transformForInstall( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        // metadata is added at install time
    }

    public void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        // metadata is added at deploy time
    }

    protected VersionArtifactMetadata retrieveFromRemoteRepository( Artifact artifact,
                                                                    ArtifactRepository remoteRepository,
                                                                    VersionArtifactMetadata localMetadata )
        throws ArtifactMetadataRetrievalException
    {
        AbstractVersionArtifactMetadata metadata = new LatestArtifactMetadata( artifact );
        try
        {
            metadata.retrieveFromRemoteRepository( remoteRepository, wagonManager );
        }
        catch ( ResourceDoesNotExistException e )
        {
            if ( localMetadata.constructVersion() == null )
            {
                throw new ArtifactMetadataRetrievalException( "Unable to find latest version for plugin artifact " + artifact, e );
            }
            // otherwise, ignore - use the local one
        }
        return metadata;
    }

    protected VersionArtifactMetadata readFromLocalRepository( Artifact artifact, ArtifactRepository localRepository )
        throws IOException
    {
        AbstractVersionArtifactMetadata metadata = new LatestArtifactMetadata( artifact );
        metadata.readFromLocalRepository( localRepository );
        return metadata;
    }
}
