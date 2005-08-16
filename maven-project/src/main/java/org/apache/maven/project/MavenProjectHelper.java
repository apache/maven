package org.apache.maven.project;

import java.io.File;
import java.util.List;

public interface MavenProjectHelper
{
    
    String ROLE = MavenProjectHelper.class.getName();

    void attachArtifact( MavenProject project, String artifactType, String artifactClassifier, File artifactFile );
    
    void addResource( MavenProject project, String resourceDirectory, List includes, List excludes );
    
    void addTestResource( MavenProject project, String resourceDirectory, List includes, List excludes );
    
}
