package org.apache.maven.artifact.metadata;

import org.apache.maven.artifact.Artifact;

import java.util.List;
import java.util.Set;

public class ResolutionGroup
{
    
    private final Set artifacts;
    private final List resolutionRepositories;
    private final Artifact pomArtifact;

    public ResolutionGroup( Artifact pomArtifact, Set artifacts, List resolutionRepositories )
    {
        this.pomArtifact = pomArtifact;
        this.artifacts = artifacts;
        this.resolutionRepositories = resolutionRepositories;
    }
    
    public Artifact getPomArtifact()
    {
        return pomArtifact;
    }
    
    public Set getArtifacts()
    {
        return artifacts;
    }
    
    public List getResolutionRepositories()
    {
        return resolutionRepositories;
    }

}
