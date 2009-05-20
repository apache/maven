package org.apache.maven.project.interpolation;

import org.apache.maven.project.MavenProject;

import java.io.IOException;

public interface CoordinateInterpolator
{
    
    String ROLE = CoordinateInterpolator.class.getName();
    
    String COORDINATE_INTERPOLATED_POMFILE = ".pom-transformed.xml";

    void interpolateArtifactCoordinates( MavenProject project )
    throws IOException, ModelInterpolationException;

}
