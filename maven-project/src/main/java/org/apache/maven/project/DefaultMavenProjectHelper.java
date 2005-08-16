package org.apache.maven.project;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Resource;

import java.io.File;
import java.util.List;

public class DefaultMavenProjectHelper
    implements MavenProjectHelper
{

    // requirement.
    private ArtifactFactory artifactFactory;

    public void attachArtifact( MavenProject project, String artifactType, String artifactClassifier, File artifactFile )
    {
        Artifact artifact = artifactFactory.createArtifactWithClassifier( project.getGroupId(),
                                                                          project.getArtifactId(),
                                                                          project.getVersion(), 
                                                                          null, 
                                                                          "artifactType",
                                                                          "artifactClassifier" );
        
        artifact.setFile( artifactFile );
        artifact.setResolved( true );
        
        project.addAttachedArtifact( artifact );
    }

    public void addResource( MavenProject project, String resourceDirectory, List includes, List excludes )
    {
        Resource resource = new Resource();
        resource.setDirectory( resourceDirectory );
        resource.setIncludes( includes );
        resource.setExcludes( excludes );

        project.addResource( resource );
    }

    public void addTestResource( MavenProject project, String resourceDirectory, List includes, List excludes )
    {
        Resource resource = new Resource();
        resource.setDirectory( resourceDirectory );
        resource.setIncludes( includes );
        resource.setExcludes( excludes );

        project.addTestResource( resource );
    }

}
