package org.apache.maven.project.workspace;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.model.Model;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.DefaultProfileActivationContext;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.build.model.ModelAndFile;
import org.apache.maven.project.build.model.ModelLineage;
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

public class ModelAndFileCachingTest
    extends PlexusTestCase
{

    private static final String MY_PKG = ModelAndFileCachingTest.class.getPackage().getName().replace( '.', '/' );

    private static final String MY_PATH = ModelAndFileCachingTest.class.getName()
                                                                       .replace( '.', '/' )
                                          + ".class";

    private ProjectWorkspace projectWorkspace;

    private MavenWorkspaceStore workspaceStore;

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

    public void testResolveParentPom_PreferCachedInstance()
        throws IOException, ProjectBuildingException
    {
        File childPomFile = getFile( "resolveParentPom/pom.xml" );

        String gid = "tests";
        String aid = "resolve-parent-pom-parent";
        String ver = "1";

        ModelAndFile maf = newModelAndFile( gid, aid, ver );
        projectWorkspace.storeModelAndFile( maf );

        ModelLineage lineage = lineageBuilder.buildModelLineage( childPomFile,
                                                                 new DefaultProjectBuilderConfiguration().setLocalRepository( localRepo ).setGlobalProfileManager( profileManager ),
                                                                 Collections.EMPTY_LIST,
                                                                 false,
                                                                 false );

        assertSame( maf.getModel(), lineage.getDeepestAncestorModel() );
    }

    public void testResolveParentPom_StoreByFileAndGAVIfUncached()
        throws IOException, ProjectBuildingException
    {
        File childPomFile = getFile( "resolveParentPom/childAndParent/child/pom.xml" );
        File parentPomFile = new File( childPomFile.getParentFile().getParentFile(), "pom.xml" );

        String gid = "tests";
        String aid = "childAndParent-parent";
        String ver = "1";

        ModelLineage lineage = lineageBuilder.buildModelLineage( childPomFile,
                                                                 new DefaultProjectBuilderConfiguration().setLocalRepository( localRepo ).setGlobalProfileManager( profileManager ),
                                                                 Collections.EMPTY_LIST,
                                                                 false,
                                                                 true );

        assertEquals( parentPomFile.getCanonicalPath(), lineage.getDeepestAncestorFile()
                                                               .getCanonicalPath() );

        ModelAndFile maf1 = projectWorkspace.getModelAndFile( gid, aid, ver );
        assertNotNull( maf1 );
        assertSame( maf1.getModel(), lineage.getDeepestAncestorModel() );

        ModelAndFile maf2 = projectWorkspace.getModelAndFile( parentPomFile );
        assertNotNull( maf2 );
        assertSame( maf2.getModel(), lineage.getDeepestAncestorModel() );
    }

    public void testReadModel_PreferModelInstanceCachedByFile()
        throws IOException, ProjectBuildingException
    {
        File pomFile = new File( "test/pom.xml" );

        String gid = "tests";
        String aid = "read-model";
        String ver = "1";

        ModelAndFile maf = newModelAndFile( gid, aid, ver, pomFile );
        projectWorkspace.storeModelAndFile( maf );

        ModelLineage lineage = lineageBuilder.buildModelLineage( pomFile,
                                                                 new DefaultProjectBuilderConfiguration().setLocalRepository( localRepo ).setGlobalProfileManager( profileManager ),
                                                                 Collections.EMPTY_LIST,
                                                                 false,
                                                                 false );

        assertSame( maf.getModel(), lineage.getOriginatingModel() );
    }

    public void testBuildModelLineage_StoreByFileAndGAVIfUncached()
        throws IOException, ProjectBuildingException
    {
        File pomFile = getFile( "buildModelLineage/pom.xml" );

        String gid = "tests";
        String aid = "build-model-lineage";
        String ver = "1";

        ModelLineage lineage = lineageBuilder.buildModelLineage( pomFile,
                                                                 new DefaultProjectBuilderConfiguration().setLocalRepository( localRepo ).setGlobalProfileManager( profileManager ),
                                                                 Collections.EMPTY_LIST,
                                                                 false,
                                                                 false );

        assertEquals( pomFile.getCanonicalPath(), lineage.getOriginatingPOMFile()
                                                               .getCanonicalPath() );

        ModelAndFile maf1 = projectWorkspace.getModelAndFile( gid, aid, ver );
        assertNotNull( maf1 );
        assertSame( maf1.getModel(), lineage.getOriginatingModel() );

        ModelAndFile maf2 = projectWorkspace.getModelAndFile( pomFile );
        assertNotNull( maf2 );
        assertSame( maf2.getModel(), lineage.getOriginatingModel() );
    }

    private ModelAndFile newModelAndFile( String gid,
                                          String aid,
                                          String ver )
        throws IOException
    {
        return newModelAndFile( gid, aid, ver, File.createTempFile( "model-and-file.", ".tmp" ) );
    }

    private ModelAndFile newModelAndFile( String gid,
                                          String aid,
                                          String ver,
                                          File file )
    {
        Model model = new Model();
        model.setGroupId( gid );
        model.setArtifactId( aid );
        model.setVersion( ver );

        ModelAndFile maf = new ModelAndFile( model, file, false );

        return maf;
    }

    private File getFile( String path )
    {
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        URL res = cloader.getResource( MY_PATH );

        File myFile = new File( res.getPath() );

        File result = new File( myFile.getParentFile(), path );

        if ( !result.exists() )
        {
            result = new File( "src/test/resources", MY_PKG + "/" + path );
        }

        return result;
    }

}
