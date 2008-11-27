package org.apache.maven.artifact.resolver;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.List;

/**
 * A simple recording of the Artifacts that could not be resolved for a given resolution request, along with
 * the remote repositories where attempts were made to resolve the artifacts.
 *
 * @author Jason van Zyl
 */
public class UnresolvedArtifacts
{
    private Artifact originatingArtifact;

    private List<Artifact> artifacts;

    private List<ArtifactRepository> remoteRepositories;

    public UnresolvedArtifacts( Artifact originatingArtifact,
                                List<Artifact> artifacts,
                                List<ArtifactRepository> remoteRepositories )
    {
        this.originatingArtifact = originatingArtifact;

        this.artifacts = artifacts;

        this.remoteRepositories = remoteRepositories;
    }

    public Artifact getOriginatingArtifact()
    {
        return originatingArtifact;
    }

    public List<Artifact> getArtifacts()
    {
        return artifacts;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return remoteRepositories;
    }
}
