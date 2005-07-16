package org.apache.maven.artifact.metadata;

import java.util.List;
import java.util.Set;

public class ResolutionGroup
{
    
    private final Set artifacts;
    private final List resolutionRepositories;

    public ResolutionGroup( Set artifacts, List resolutionRepositories )
    {
        this.artifacts = artifacts;
        this.resolutionRepositories = resolutionRepositories;
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
