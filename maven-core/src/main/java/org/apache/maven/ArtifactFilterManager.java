package org.apache.maven;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

public interface ArtifactFilterManager
{
    /**
     * Returns a filter for core + extension artifacts.
     */
    ArtifactFilter getArtifactFilter();

    /**
     * Returns a filter for only the core artifacts.
     */
    ArtifactFilter getCoreArtifactFilter();

    /**
     * Exclude an extension artifact (doesn't affect getArtifactFilter's result,
     * only getExtensionArtifactFilter).
     * @param artifactId
     */
    void excludeArtifact( String artifactId );
}