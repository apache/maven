package org.apache.maven.artifact.resolver.filter;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Exclusion;

public class ExclusionArtifactFilter implements ArtifactFilter
{
    private final List<Exclusion> exclusions;

    public ExclusionArtifactFilter( List<Exclusion> exclusions )
    {
        this.exclusions = exclusions;
    }

    @Override
    public boolean include( Artifact artifact )
    {
        for ( Exclusion exclusion : exclusions )
        {
            if ( exclusion.getGroupId().equals( artifact.getGroupId() )
                    && exclusion.getArtifactId().equals( artifact.getArtifactId() ) )
            {
                return false;
            }
        }
        return true;
    }
}
