package org.apache.maven.project;

import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactResolver;

public class TestProjectBuilder extends DefaultMavenProjectBuilder
{

    public void setArtifactResolver( ArtifactResolver resolver )
    {
        artifactResolver = resolver;
    }
    
    public void setArtifactMetadataSource( ArtifactMetadataSource metadataSource )
    {
        artifactMetadataSource = metadataSource;
    }
}
