package org.apache.maven;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.Repository;

import java.util.List;

/**
 * @author Jason van Zyl
 */
public interface MavenTools
{
    String ROLE = MavenTools.class.getName();

    // ----------------------------------------------------------------------------
    // Methods taken from ProjectUtils
    // ----------------------------------------------------------------------------

    List buildArtifactRepositories( List repositories )
        throws InvalidRepositoryException;

    ArtifactRepository buildDeploymentArtifactRepository( DeploymentRepository repo )
        throws InvalidRepositoryException;

    ArtifactRepository buildArtifactRepository( Repository repo )
        throws InvalidRepositoryException;
}
