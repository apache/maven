package org.apache.maven;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

public interface ArtifactFilterManager
{

    ArtifactFilter getArtifactFilter();

    void excludeArtifact( String artifactId );

}