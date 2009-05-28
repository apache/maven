package org.apache.maven.project.artifact;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.project.MavenProject;

public class ProjectArtifact
    extends DefaultArtifact
{
    private MavenProject project;

    public ProjectArtifact( MavenProject project )
    {
        super( project.getGroupId(), project.getArtifactId(), project.getVersion(), null, "pom", null, new PomArtifactHandler() );
        this.project = project;
        setFile( project.getFile() );
        setResolved( true );
    }

    public MavenProject getProject()
    {
        return project;
    }
    
    static class PomArtifactHandler
        implements ArtifactHandler
    {
        public String getClassifier()
        {
            return "pom";
        }

        public String getDirectory()
        {
            return null;
        }

        public String getExtension()
        {
            return "pom";
        }

        public String getLanguage()
        {
            return "none";
        }

        public String getPackaging()
        {
            return "pom";
        }

        public boolean isAddedToClasspath()
        {
            return false;
        }

        public boolean isIncludesDependencies()
        {
            return false;
        }
    }
}
