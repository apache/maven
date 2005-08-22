package org.apache.maven.artifact.transform;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.List;

/**
 * Manages multiple ArtifactTransformation instances and applies them in succession.
 */
public interface ArtifactTransformationManager
{
    String ROLE = ArtifactTransformationManager.class.getName();

    /**
     * Take in a artifact and return the transformed artifact for locating in the remote repository. If no
     * transformation has occured the original artifact is returned.
     *
     * @param artifact Artifact to be transformed.
     * @param remoteRepositories the repositories to check
     * @param localRepository the local repository
     */
    void transformForResolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException;

    /**
     * Take in a artifact and return the transformed artifact for locating in the local repository. If no
     * transformation has occured the original artifact is returned.
     *
     * @param artifact Artifact to be transformed.
     * @param localRepository the local repository it will be stored in
     */
    void transformForInstall( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException;

    /**
     * Take in a artifact and return the transformed artifact for distributing toa remote repository. If no
     * transformation has occured the original artifact is returned.
     *
     * @param artifact Artifact to be transformed.
     * @param remoteRepository the repository to deploy to
     */
    void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException;

    /**
     * Return the timestamp which will be used to deploy artifacts from this build.
     */
    String getSnapshotDeploymentTimestamp();

    /**
     * Return the buildnumber which will be used to deploy artifacts from this build.
     */
    int getSnapshotDeploymentBuildNumber( Artifact snapshotArtifact );
    
    /**
     * Return the artifact-version which will be used to deploy artifacts from this build.
     */
    String getSnapshotDeploymentVersion( Artifact snapshotArtifact );

}
