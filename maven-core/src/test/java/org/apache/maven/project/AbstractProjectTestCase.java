package org.apache.maven.project;

import org.apache.maven.MavenTestCase;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;

public class AbstractProjectTestCase
    extends MavenTestCase
{
    protected MavenProjectBuilder projectBuilder;

    private ArtifactRepository localRepository;

    public void setUp()
        throws Exception
    {
        super.setUp();

        projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );

        assertNotNull( "Test projectBuilder can't be null!", projectBuilder );

        localRepository = new ArtifactRepository();

        localRepository.setUrl( "file://" + getTestFile( "src/test/resources/local-repo" ) );
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    protected MavenProject buildProject( File f, boolean followTransitiveDeps )
       throws Exception
    {
        MavenProject project;

        project = projectBuilder.build( f, followTransitiveDeps );

        assertNotNull( "Project is null", project );

        return project;
    }

    protected MavenProject buildProject( File f )
        throws Exception
    {
        return buildProject( f, false );
    }
}
