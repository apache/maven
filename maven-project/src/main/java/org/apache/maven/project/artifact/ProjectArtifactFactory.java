package org.apache.maven.project.artifact;

import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.project.MavenProject;

public class ProjectArtifactFactory
    extends DefaultArtifactFactory
{
    
    public ArtifactWithProject create( MavenProject project )
    {
        ArtifactHandler handler = getArtifactHandlerManager().getArtifactHandler( project.getPackaging() );

        return new ArtifactWithProject( project, project.getPackaging(), null, handler, false );
    }

    public ArtifactWithProject create( MavenProject project, String type, String classifier, boolean optional )
    {
        ArtifactHandler handler = getArtifactHandlerManager().getArtifactHandler( type );

        return new ArtifactWithProject( project, type, classifier, handler, optional );
    }

}
