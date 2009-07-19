package org.apache.maven.artifact.repository.layout;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.component.annotations.Component;

/**
 * The code in this class is taken from DefaultRepositorylayout, located at:
 * http://svn.apache.org/viewvc/maven/components/trunk/maven-artifact/src/main/java/org/apache/maven/artifact/repository/layout/DefaultRepositoryLayout.java
 *
 * @version $Id$
 */
@Component(role=ArtifactRepositoryLayout.class, hint="flat")                                                                         
public class FlatRepositoryLayout
    implements ArtifactRepositoryLayout
{
    private static final char ARTIFACT_SEPARATOR = '-';

    private static final char GROUP_SEPARATOR = '.';

    public String getId()
    {
        return "flat";
    }
    
    public String pathOf( Artifact artifact )
    {
        ArtifactHandler artifactHandler = artifact.getArtifactHandler();

        StringBuilder path = new StringBuilder( 128 );

        path.append( artifact.getArtifactId() ).append( ARTIFACT_SEPARATOR ).append( artifact.getVersion() );

        if ( artifact.hasClassifier() )
        {
            path.append( ARTIFACT_SEPARATOR ).append( artifact.getClassifier() );
        }

        if ( artifactHandler.getExtension() != null && artifactHandler.getExtension().length() > 0 )
        {
            path.append( GROUP_SEPARATOR ).append( artifactHandler.getExtension() );
        }

        return path.toString();
    }

    public String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata,
                                                 ArtifactRepository repository )
    {
        return pathOfRepositoryMetadata( metadata.getLocalFilename( repository ) );
    }

    private String pathOfRepositoryMetadata( String filename )
    {
        StringBuilder path = new StringBuilder( 128 );

        path.append( filename );

        return path.toString();
    }

    public String pathOfRemoteRepositoryMetadata( ArtifactMetadata metadata )
    {
        return pathOfRepositoryMetadata( metadata.getRemoteFilename() );
    }
}
