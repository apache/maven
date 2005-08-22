package org.apache.maven.artifact.transform;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.Iterator;
import java.util.List;

public class DefaultArtifactTransformationManager
    implements ArtifactTransformationManager
{

    private List artifactTransformations;
    
    private SnapshotTransformation snapshotTransformation;

    public void transformForResolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        for ( Iterator i = artifactTransformations.iterator(); i.hasNext(); )
        {
            ArtifactTransformation transform = (ArtifactTransformation) i.next();
            transform.transformForResolve( artifact, remoteRepositories, localRepository );
        }
    }

    public void transformForInstall( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        for ( Iterator i = artifactTransformations.iterator(); i.hasNext(); )
        {
            ArtifactTransformation transform = (ArtifactTransformation) i.next();
            transform.transformForInstall( artifact, localRepository );
        }
    }

    public void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        for ( Iterator i = artifactTransformations.iterator(); i.hasNext(); )
        {
            ArtifactTransformation transform = (ArtifactTransformation) i.next();
            transform.transformForDeployment( artifact, remoteRepository );
        }
    }

    public String getSnapshotDeploymentTimestamp()
    {
        return snapshotTransformation.getDeploymentTimestamp();
    }

    public int getSnapshotDeploymentBuildNumber( Artifact artifact )
    {
        return snapshotTransformation.getDeploymentBuildNumber( artifact);
    }

    public String getSnapshotDeploymentVersion( Artifact snapshotArtifact )
    {
        return snapshotTransformation.getDeploymentVersion( snapshotArtifact );
    }

}
