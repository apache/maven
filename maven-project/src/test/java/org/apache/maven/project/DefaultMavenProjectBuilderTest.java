package org.apache.maven.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

public class DefaultMavenProjectBuilderTest
    extends PlexusTestCase
{

    private List filesToDelete = new ArrayList();

    private File localRepoDir;

    private DefaultMavenProjectBuilder projectBuilder;

    public void setUp()
        throws Exception
    {
        super.setUp();

        projectBuilder = (DefaultMavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );
        
        localRepoDir = new File( System.getProperty( "java.io.tmpdir" ), "local-repo." + System.currentTimeMillis() );
        localRepoDir.mkdirs();

        filesToDelete.add( localRepoDir );
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();

        if ( !filesToDelete.isEmpty() )
        {
            for ( Iterator it = filesToDelete.iterator(); it.hasNext(); )
            {
                File file = (File) it.next();

                if ( file.exists() )
                {
                    if ( file.isDirectory() )
                    {
                        FileUtils.deleteDirectory( file );
                    }
                    else
                    {
                        file.delete();
                    }
                }
            }
        }
    }

    public void testShouldInjectOneProfileToStandaloneSuperPom()
        throws Exception
    {
        ProfileManager pm = new DefaultProfileManager( getContainer(), new Properties() );

        String profileId = "test-profile";
        String key = "test";
        String value = "value";

        Profile profile = new Profile();
        profile.setId( profileId );
        profile.addProperty( key, value );

        pm.addProfile( profile );
        pm.explicitlyActivate( profileId );

        MavenProject project = projectBuilder.buildStandaloneSuperProject( getLocalRepository(), pm );

        assertEquals( value, project.getProperties().getProperty( key ) );
    }

    public void testShouldInjectProfileWithRepositoryToStandaloneSuperPom()
        throws Exception
    {
        ProfileManager pm = new DefaultProfileManager( getContainer(), new Properties() );

        String profileId = "test-profile";
        String repoId = "test-repo";

        Profile profile = new Profile();
        profile.setId( profileId );

        Repository repo = new Repository();
        repo.setId( repoId );
        repo.setUrl( "http://www.google.com" );

        profile.addRepository( repo );

        pm.addProfile( profile );
        pm.explicitlyActivate( profileId );

        MavenProject project = projectBuilder.buildStandaloneSuperProject( getLocalRepository(), pm );

        List repositories = project.getRepositories();

        assertNotNull( repositories );

        Repository result = null;

        for ( Iterator it = repositories.iterator(); it.hasNext(); )
        {
            Repository candidate = (Repository) it.next();

            if ( repoId.equals( candidate.getId() ) )
            {
                result = candidate;
                break;
            }
        }

        assertNotNull( "Profile-injected repository not found in super-POM.", result );

        assertEquals( "Profile-injected repository was not first in repo list within super-POM", repoId,
                      ( (Repository) repositories.get( 0 ) ).getId() );
    }

    protected ArtifactRepository getLocalRepository()
        throws Exception
    {
        ArtifactRepositoryLayout repoLayout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE,
                                                                                 "legacy" );

        ArtifactRepository r = new DefaultArtifactRepository( "local", "file://" + localRepoDir.getAbsolutePath(),
                                                              repoLayout );

        return r;
    }
}
