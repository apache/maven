package org.apache.maven.project;

import org.apache.maven.MavenTestCase;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.util.FileUtils;

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

    public void initLocalRepository()
    {
        File tempLocalRepository = new File( getBasedir(), "target/temp-repo-local" );
        
        // delete 
        try
        {
            FileUtils.deleteDirectory( tempLocalRepository );
        }
        catch ( Exception e )
        {
            System.out.println( "Could not delete the remaining from previous tests!! Test will continue anyway " );

        }

        tempLocalRepository.mkdirs();

        System.out.println( "Created temporary local repository: " + tempLocalRepository );

        System.setProperty( "maven.repo.local", tempLocalRepository.getPath() );
    }

    protected MavenProject buildProject( File f, ArtifactRepository localRepository, boolean followTransitiveDeps  )
       throws Exception
    {
        MavenProject project;

        project = projectBuilder.build( f, localRepository, followTransitiveDeps );

        assertNotNull( "Project is null", project );

        return project;
    }

    protected MavenProject buildProject( File f, ArtifactRepository localRepository )
        throws Exception
    {
        return buildProject( f, localRepository, false );
    }
}
