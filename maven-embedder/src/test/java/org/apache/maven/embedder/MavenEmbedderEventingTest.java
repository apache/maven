package org.apache.maven.embedder;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.extension.ExtensionScanningException;
import org.apache.maven.monitor.event.AbstractWorkspaceMonitor;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.workspace.MavenWorkspaceStore;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import junit.framework.TestCase;

public class MavenEmbedderEventingTest
    extends TestCase
{

    protected String basedir;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir" );

        if ( basedir == null )
        {
            basedir = new File( "." ).getCanonicalPath();
        }
    }

    public void testEmbedderInitializeAndStopEventsFired()
        throws MavenEmbedderException
    {
        TestWorkspaceMonitor testWSMonitor = new TestWorkspaceMonitor();

        Configuration config = new DefaultConfiguration();
        config.setWorkspaceMonitor( testWSMonitor );

        assertEquals( 0, testWSMonitor.initializeCaught );
        assertEquals( 0, testWSMonitor.setManagerCaught );

        MavenEmbedder embedder = new MavenEmbedder( config );

        assertEquals( 1, testWSMonitor.initializeCaught );
        assertEquals( 1, testWSMonitor.setManagerCaught );

        assertEquals( 0, testWSMonitor.stopCaught );
        assertEquals( 0, testWSMonitor.clearCaught );

        assertSame( embedder.getWorkspaceStore(), testWSMonitor.workspaceManager );

        embedder.stop();

        assertEquals( 1, testWSMonitor.stopCaught );
        assertEquals( 1, testWSMonitor.clearCaught );

        assertEquals( 0, testWSMonitor.startMethodCaught );
        assertEquals( 0, testWSMonitor.endMethodCaught );
    }

    public void testStartAndEndMethodEventsFiredOnSimpleReadProject()
        throws IOException, MavenEmbedderException, ProjectBuildingException,
        ExtensionScanningException, MavenExecutionException
    {
        EmbedderAndMonitor em = newEmbedder();

        assertEquals( 0, em.monitor.startMethodCaught );
        assertEquals( 0, em.monitor.endMethodCaught );

        File dir = getFile( "simple-read-project" );
        File pomFile = new File( dir, "pom.xml" );

        em.embedder.readProject( pomFile );

        assertEquals( 1, em.monitor.startMethodCaught );
        assertEquals( 1, em.monitor.endMethodCaught );
        assertEquals( 1, em.monitor.clearCaught );

        assertSame( em.embedder.getWorkspaceStore(), em.monitor.workspaceManager );

        em.embedder.stop();
    }

    public void testStartAndEndMethodEventsFiredOnReadWithDeps()
        throws IOException, MavenEmbedderException, ProjectBuildingException,
        ExtensionScanningException, MavenExecutionException
    {
        File dir = getFile( "read-with-deps" );
        File pomFile = new File( dir, "pom.xml" );
        File localRepoDir = new File( dir, "repo" );

        EmbedderAndMonitor em = newEmbedder( localRepoDir );

        assertEquals( 0, em.monitor.startMethodCaught );
        assertEquals( 0, em.monitor.endMethodCaught );

        em.embedder.readProject( pomFile );

        assertEquals( 1, em.monitor.startMethodCaught );
        assertEquals( 1, em.monitor.endMethodCaught );
        assertEquals( 1, em.monitor.clearCaught );

        assertSame( em.embedder.getWorkspaceStore(), em.monitor.workspaceManager );

        em.embedder.stop();
    }

    public void testStartAndEndMethodEventsFiredOnExecute()
        throws IOException, MavenEmbedderException, ProjectBuildingException,
        ExtensionScanningException, MavenExecutionException
    {
        EmbedderAndMonitor em = newEmbedder();

        assertEquals( 0, em.monitor.startMethodCaught );
        assertEquals( 0, em.monitor.endMethodCaught );

        File dir = getFile( "simple-read-project" );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setGoals( Collections.singletonList( "clean" ) );
        request.setBaseDirectory( dir );

        em.embedder.execute( request );

        assertEquals( 1, em.monitor.startMethodCaught );
        assertEquals( 1, em.monitor.endMethodCaught );
        assertEquals( 1, em.monitor.clearCaught );

        assertSame( em.embedder.getWorkspaceStore(), em.monitor.workspaceManager );

        em.embedder.stop();
    }

    private EmbedderAndMonitor newEmbedder()
        throws MavenEmbedderException
    {
        return newEmbedder( null );
    }

    private EmbedderAndMonitor newEmbedder( File localRepoDir )
        throws MavenEmbedderException
    {
        TestWorkspaceMonitor testWSMonitor = new TestWorkspaceMonitor();

        Configuration config = new DefaultConfiguration();
        config.setWorkspaceMonitor( testWSMonitor );
        config.setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() );

        if ( localRepoDir != null )
        {
            config.setLocalRepository( localRepoDir );
        }

        return new EmbedderAndMonitor( new MavenEmbedder( config ), testWSMonitor );
    }

    private static final class EmbedderAndMonitor
    {
        private MavenEmbedder embedder;

        private TestWorkspaceMonitor monitor;

        private EmbedderAndMonitor( MavenEmbedder embedder,
                                    TestWorkspaceMonitor monitor )
        {
            this.embedder = embedder;
            this.monitor = monitor;
        }
    }

    private File getFile( String path )
        throws IOException
    {
        File testDirectory = new File( basedir, "src/test/eventing-projects/" + path );

        System.out.println( "Test source dir: " + testDirectory );

        File targetDirectory = new File( basedir, "target/eventing-projects/" + path );

        System.out.println( "Test temp dir: " + targetDirectory );

        targetDirectory.getParentFile().mkdirs();

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        return targetDirectory;
    }

    private static final class TestWorkspaceMonitor
        extends AbstractWorkspaceMonitor
    {

        private int initializeCaught = 0;

        private int startMethodCaught = 0;

        private int endMethodCaught = 0;

        private int stopCaught = 0;

        private int setManagerCaught = 0;

        private int clearCaught = 0;

        private MavenWorkspaceStore workspaceManager;

        private boolean clearOnEndMethod = true;

        private boolean clearOnStop = true;

        public void embedderInitialized( long timestamp )
        {
            initializeCaught++;
        }

        public void embedderMethodEnded( String method,
                                         long timestamp )
        {
            endMethodCaught++;
            if ( clearOnEndMethod )
            {
                clearCache();
            }
        }

        public void embedderMethodStarted( String method,
                                           long timestamp )
        {
            startMethodCaught++;
        }

        public void embedderStopped( long timestamp )
        {
            stopCaught++;
            if ( clearOnStop )
            {
                clearCache();
            }
        }

        public void setWorkspaceStore( MavenWorkspaceStore workspaceManager )
        {
            setManagerCaught++;
            this.workspaceManager = workspaceManager;
            super.setWorkspaceStore( workspaceManager );
        }

        public void clearCache()
        {
            clearCaught++;
            super.clearCache();
        }

    }
}
