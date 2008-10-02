package org.apache.maven.execution;

import org.apache.maven.artifact.versioning.ArtifactVersion;

public class ApplicationInformation
{
    private ArtifactVersion version;
    private String builtOn;
    
    public ApplicationInformation( ArtifactVersion version, String builtOn )
    {
        this.version = version;
        this.builtOn = builtOn;
    }

    public ArtifactVersion getVersion()
    {
        return version;
    }

    public String getBuiltOn()
    {
        return builtOn;
    }        
}
