package org.apache.maven.project;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Model;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

public class DefaultMavenProjectHelperTest
    extends PlexusTestCase
{

    private MavenProjectHelper mavenProjectHelper;

    private ArtifactFactory artifactFactory;

    public void setUp()
        throws Exception
    {
        super.setUp();

        mavenProjectHelper = (MavenProjectHelper) lookup( MavenProjectHelper.ROLE );
        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
    }

    public void testShouldThrowExceptionWhenDuplicateAttachmentIsAdded()
    {
        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "1" );

        MavenProject project = new MavenProject();

        Artifact projectArtifact = artifactFactory.createBuildArtifact( model.getGroupId(), model.getArtifactId(), model.getVersion(), "jar" );
        project.setArtifact( projectArtifact );

        File artifactFile = new File( "nothing" );
        File artifactFile2 = new File( "nothing-else" );

        mavenProjectHelper.attachArtifact( project, "jar", "c", artifactFile );

        try
        {
            mavenProjectHelper.attachArtifact( project, "jar", "c", artifactFile2 );

            fail( "Should throw DuplicateArtifactAttachmentException" );
        }
        catch( DuplicateArtifactAttachmentException e )
        {
            assertEquals( artifactFile2, e.getArtifact().getFile() );
            assertSame( project, e.getProject() );
        }
    }

}
