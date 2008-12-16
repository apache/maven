package org.apache.maven.project.builder;

import java.io.IOException;

import org.apache.maven.artifact.Artifact;

public interface PomArtifactResolver
{
    public void resolve( Artifact artifact )
        throws IOException;
}
