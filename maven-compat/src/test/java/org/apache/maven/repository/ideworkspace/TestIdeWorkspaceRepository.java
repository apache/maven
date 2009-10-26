package org.apache.maven.repository.ideworkspace;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.repository.LocalArtifactRepository;
import org.codehaus.plexus.component.annotations.Component;

@Component( role = LocalArtifactRepository.class, hint = LocalArtifactRepository.IDE_WORKSPACE )
public class TestIdeWorkspaceRepository
    extends LocalArtifactRepository
{

    public static final String GROUP_ID = "test";

    public static final String ARTIFACT_ID = "test";

    public static final String VERSION = "1.0.0-SNAPSHOT";

    public static final File ARTIFACT_FILE = new File( "/a/b/c/d" );

    @Override
    public Artifact find( Artifact artifact )
    {
        if ( GROUP_ID.equals( artifact.getGroupId() ) && ARTIFACT_ID.equals( artifact.getArtifactId() )
            && VERSION.equals( artifact.getVersion() ) )
        {
            artifact.setFile( ARTIFACT_FILE );
            artifact.setResolved( true );
        }
        return artifact;
    }

    @Override
    public boolean hasLocalMetadata()
    {
        return false;
    }

}
