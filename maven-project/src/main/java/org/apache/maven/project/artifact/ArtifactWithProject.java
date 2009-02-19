package org.apache.maven.project.artifact;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;

import java.util.Collection;
import java.util.Iterator;

public class ArtifactWithProject
    extends DefaultArtifact
{

    private final MavenProject project;

    public ArtifactWithProject( MavenProject project, String type, String classifier,
                                ArtifactHandler artifactHandler, boolean optional )
    {
        super( project.getGroupId(), project.getArtifactId(), VersionRange.createFromVersion( project.getVersion() ),
               null, type, classifier, artifactHandler, optional );
        
        this.project = project;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public ProjectArtifactMetadata getProjectArtifactMetadata()
    {
        return getProjectArtifactMetadata( this );
    }

    public static ProjectArtifactMetadata getProjectArtifactMetadata( Artifact artifact )
    {
        Collection metadataList = artifact.getMetadataList();
        if ( metadataList != null )
        {
            for ( Iterator it = metadataList.iterator(); it.hasNext(); )
            {
                Object metadata = it.next();
                if ( metadata instanceof ProjectArtifactMetadata )
                {
                    return (ProjectArtifactMetadata) metadata;
                }
            }
        }

        return null;
    }

}
