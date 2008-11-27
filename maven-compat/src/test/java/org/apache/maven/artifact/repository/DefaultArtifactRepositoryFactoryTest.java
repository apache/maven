package org.apache.maven.artifact.repository;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.UnknownRepositoryLayoutException;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DefaultArtifactRepositoryFactoryTest
    extends PlexusTestCase
{
    private ArtifactRepositoryFactory repoFactory;

    private Set<File> toDelete = new HashSet<File>();

    public void setUp()
        throws Exception
    {
        super.setUp();

        repoFactory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );
    }

    public void tearDown()
        throws Exception
    {
        for ( File f : toDelete )
        {
            if ( f.exists() )
            {
                FileUtils.forceDelete( f );
            }
        }
    }

    private File createTempDir()
        throws IOException
    {
        File f = File.createTempFile( "DefaultArtifactRepositoryFactoryTest.", ".dir" );
        FileUtils.forceDelete( f );

        f.mkdirs();
        toDelete.add( f );

        return f;
    }

    public void test_createLocalRepository()
        throws IOException, InvalidRepositoryException
    {
        File dir = createTempDir();
        ArtifactRepository localRepo = repoFactory.createLocalRepository( dir );

        assertEquals( dir.getAbsolutePath(), localRepo.getBasedir() );
        assertEquals( ArtifactRepositoryFactory.LOCAL_REPOSITORY_ID, localRepo.getId() );
        assertTrue( localRepo.getLayout() instanceof DefaultRepositoryLayout );
    }

    public void test_getLayout_ReturnDefaultLayout()
        throws UnknownRepositoryLayoutException
    {
        ArtifactRepositoryLayout layout = repoFactory.getLayout( "default" );

        assertTrue( layout instanceof DefaultRepositoryLayout );
    }

    public void testRetrievalOfKnownRepositoryLayouts()
        throws Exception
    {
        ArtifactRepositoryLayout defaultLayout = repoFactory.getLayout( "default" );
        assertNotNull( defaultLayout );

        ArtifactRepositoryLayout legacyLayout = repoFactory.getLayout( "legacy" );
        assertNotNull( legacyLayout );

        ArtifactRepositoryLayout flatLayout = repoFactory.getLayout( "flat" );
        assertNotNull( flatLayout );
    }

    public void testCreationOfDeploymentRepository()
        throws Exception
    {
        ArtifactRepository repository = repoFactory.createDeploymentArtifactRepository( "id", "file:///tmp/repo", "default", false );
        assertNotNull( repository );        
        assertNotNull( repository.getLayout() );
        assertEquals( "id", repository.getId() );        
    }
}
