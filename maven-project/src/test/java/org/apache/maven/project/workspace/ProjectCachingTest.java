package org.apache.maven.project.workspace;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.model.Model;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.DefaultProfileActivationContext;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.build.model.ModelLineageBuilder;
import org.apache.maven.workspace.MavenWorkspaceStore;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

// TODO: Add conversion/tests for modelAndFileCache -> projectWorkspace stuff in simple cases.
// TODO: Add tests for project parents from cache (using model-and-file stuff, maybe?)
public class ProjectCachingTest
    extends PlexusTestCase
{

    private static final String MY_PATH = ProjectCachingTest.class.getName().replace( '.', '/' )
                                          + ".class";

    private ProjectWorkspace projectWorkspace;

    private MavenWorkspaceStore workspaceStore;

    private MavenProjectBuilder projectBuilder;

    private ModelLineageBuilder lineageBuilder;

    private ArtifactRepositoryFactory repoFactory;

    private ProfileManager profileManager;

    private ArtifactRepository localRepo;

    private ArtifactFactory artifactFactory;

    private List dirsToDelete = new ArrayList();

    public void setUp()
        throws Exception
    {
        super.setUp();
        getContainer().getLoggerManager().setThresholds( Logger.LEVEL_DEBUG );

        projectWorkspace = (ProjectWorkspace) lookup( ProjectWorkspace.class );
        workspaceStore = (MavenWorkspaceStore) lookup( MavenWorkspaceStore.class );
        projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.class );
        lineageBuilder = (ModelLineageBuilder) lookup( ModelLineageBuilder.class );
        repoFactory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.class );
        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.class );

        File localRepoDir = File.createTempFile( "local-repo.", ".tmp" );
        localRepoDir.delete();
        localRepoDir.mkdirs();

        dirsToDelete.add( localRepoDir );

        localRepo = repoFactory.createLocalRepository( localRepoDir );
        profileManager = new DefaultProfileManager(
                                                    getContainer(),
                                                    new DefaultProfileActivationContext(
                                                                                         System.getProperties(),
                                                                                         true ) );
    }

    public void tearDown()
        throws Exception
    {
        workspaceStore.clear();

        if ( !dirsToDelete.isEmpty() )
        {
            for ( Iterator it = dirsToDelete.iterator(); it.hasNext(); )
            {
                File dir = (File) it.next();
                try
                {
                    FileUtils.deleteDirectory( dir );
                }
                catch ( IOException e )
                {
                    // ignore.
                }
            }
        }

        super.tearDown();
    }

    public void testBuildFromRepository_PreferCachedProject()
        throws ProjectBuildingException
    {
        String gid = "org.apache.maven.tests";
        String aid = "buildFromRepo-checkCacheFirst";
        String ver = "1";

        MavenProject project = newProject( gid, aid, ver );
        projectWorkspace.storeProjectByCoordinate( project );

        Artifact artifact = artifactFactory.createProjectArtifact( gid, aid, ver );

        MavenProject result = projectBuilder.buildFromRepository( artifact,
                                                                  Collections.EMPTY_LIST,
                                                                  localRepo );

        assertSame( project, result );
    }

    public void testBuildFromRepository_StoreProjectByCoordOnlyIfUncached()
        throws ProjectBuildingException, InvalidRepositoryException
    {
        File lrDir = getFile( "buildFromRepo" );
        File pomFile = new File( lrDir, "tests/project-caching/1/project-caching-1.pom" );

        String gid = "tests";
        String aid = "project-caching";
        String ver = "1";

        Artifact artifact = artifactFactory.createProjectArtifact( gid, aid, ver );

        ArtifactRepository localRepo = repoFactory.createLocalRepository( lrDir );

        MavenProject project = projectBuilder.buildFromRepository( artifact,
                                                                   Collections.EMPTY_LIST,
                                                                   localRepo );

        MavenProject r1 = projectWorkspace.getProject( pomFile );

        MavenProject r2 = projectWorkspace.getProject( gid, aid, ver );

        assertNull( r1 );

        assertSame( project, r2 );
    }

    public void testBuildFromRepository_DontCheckCacheForRELEASEMetaVersion()
        throws ProjectBuildingException, InvalidRepositoryException
    {
        File lrDir = getFile( "buildFromRepo" );
        File pomFile = new File( lrDir, "tests/project-caching/1/project-caching-1.pom" );

        String gid = "tests";
        String aid = "project-caching";
        String ver = "1";

        MavenProject seed = newProject( gid, aid, ver );

        Artifact artifact = artifactFactory.createProjectArtifact( gid,
                                                                   aid,
                                                                   Artifact.RELEASE_VERSION );

        ArtifactRepository localRepo = repoFactory.createLocalRepository( lrDir );

        MavenProject project = projectBuilder.buildFromRepository( artifact,
                                                                   Collections.EMPTY_LIST,
                                                                   localRepo );

        assertNotSame( seed, project );

        MavenProject r1 = projectWorkspace.getProject( pomFile );

        MavenProject r2 = projectWorkspace.getProject( gid, aid, ver );

        assertNull( r1 );

        assertSame( project, r2 );
    }

    public void testBuildFromRepository_DontCheckCacheForLATESTMetaVersion()
        throws ProjectBuildingException, InvalidRepositoryException
    {
        File lrDir = getFile( "buildFromRepo" );
        File pomFile = new File( lrDir, "tests/project-caching/1/project-caching-1.pom" );

        String gid = "tests";
        String aid = "project-caching";
        String ver = "1";

        MavenProject seed = newProject( gid, aid, ver );
        projectWorkspace.storeProjectByCoordinate( seed );

        Artifact artifact = artifactFactory.createProjectArtifact( gid,
                                                                   aid,
                                                                   Artifact.RELEASE_VERSION );

        ArtifactRepository localRepo = repoFactory.createLocalRepository( lrDir );

        MavenProject project = projectBuilder.buildFromRepository( artifact,
                                                                   Collections.EMPTY_LIST,
                                                                   localRepo );

        assertNotSame( seed, project );

        MavenProject r1 = projectWorkspace.getProject( pomFile );

        MavenProject r2 = projectWorkspace.getProject( gid, aid, ver );

        assertNull( r1 );

        assertSame( project, r2 );
    }

    public void testBuildFromFile_PreferProjectCachedByFile()
        throws ProjectBuildingException, InvalidRepositoryException
    {
        File pomFile = getFile( "buildFromFile/pom.xml" );

        String gid = "org.apache.maven.tests";
        String aid = "build-from-file";
        String ver = "1";

        MavenProject seed = newProject( gid, aid, ver );
        seed.setFile( pomFile );

        projectWorkspace.storeProjectByFile( seed );

        MavenProject project = projectBuilder.build( pomFile, localRepo, profileManager );

        assertSame( seed, project );

        assertNull( projectWorkspace.getProject( gid, aid, ver ) );
    }

    public void testBuildFromFile_StoreByCoordAndFileIfUncached()
        throws ProjectBuildingException, InvalidRepositoryException
    {
        File pomFile = getFile( "buildFromFile/pom.xml" );

        String gid = "org.apache.maven.tests";
        String aid = "build-from-file";
        String ver = "1";

        assertNull( projectWorkspace.getProject( pomFile ) );
        assertNull( projectWorkspace.getProject( gid, aid, ver ) );

        MavenProject project = projectBuilder.build( pomFile, localRepo, profileManager );

        MavenProject byFile = projectWorkspace.getProject( pomFile );
        MavenProject byCoord = projectWorkspace.getProject( gid, aid, ver );

        assertSame( project, byFile );
        assertSame( project, byCoord );
    }

    private MavenProject newProject( String gid,
                                     String aid,
                                     String ver )
    {
        Model model = new Model();
        model.setGroupId( gid );
        model.setArtifactId( aid );
        model.setVersion( ver );

        MavenProject project = new MavenProject( model );

        return project;
    }

    private File getFile( String path )
    {
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        URL myRes = cloader.getResource( MY_PATH );

        File myFile = new File( myRes.getPath() );

        return new File( myFile.getParentFile(), path );
    }

}
