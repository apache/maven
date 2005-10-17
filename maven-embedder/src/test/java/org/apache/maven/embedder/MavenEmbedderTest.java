package org.apache.maven.embedder;

import junit.framework.TestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.model.Model;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class MavenEmbedderTest
    extends TestCase
{
    private String basedir;

    private MavenEmbedder maven;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        basedir = System.getProperty( "basedir" );

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        maven = new MavenEmbedder();

        maven.setClassLoader( classLoader );

        maven.setLogger( new MavenEmbedderConsoleLogger() );

        maven.start();
    }

    protected void tearDown()
        throws Exception
    {
        maven.stop();
    }

    public void testMavenEmbedder()
        throws Exception
    {
        modelReadingTest();

        projectReadingTest();
    }

    // ----------------------------------------------------------------------
    // Goal/Phase execution tests
    // ----------------------------------------------------------------------

    public void testPhaseExecution()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/embedder-test-project" );

        File targetDirectory = new File( basedir, "target/embedder-test-project" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        File pomFile = new File( targetDirectory, "pom.xml" );

        MavenProject pom = maven.readProjectWithDependencies( pomFile );

        EventMonitor eventMonitor = new DefaultEventMonitor( new PlexusLoggerAdapter( new MavenEmbedderConsoleLogger() ) );

        maven.execute( pom,
                       Collections.singletonList( "package" ),
                       eventMonitor,
                       new ConsoleDownloadMonitor(),
                       new Properties(),
                       targetDirectory );

        File jar = new File( targetDirectory, "target/embedder-test-project-1.0-SNAPSHOT.jar" );

        assertTrue( jar.exists() );
    }

    // ----------------------------------------------------------------------
    // Test mock plugin metadata
    // ----------------------------------------------------------------------

    public void testMockPluginMetadata()
        throws Exception
    {
        List plugins = maven.getAvailablePlugins();

        SummaryPluginDescriptor spd = (SummaryPluginDescriptor) plugins.get( 0 );

        assertNotNull( spd );

        PluginDescriptor pd = maven.getPluginDescriptor( spd );

        assertNotNull( pd );

        assertEquals( "org.apache.maven.plugins", pd.getGroupId() );
    }

    // ----------------------------------------------------------------------
    // Lifecycle phases
    // ----------------------------------------------------------------------

    public void testRetrievingLifecyclePhases()
        throws Exception
    {
        List phases = maven.getLifecyclePhases();       

        assertEquals( "validate", (String) phases.get( 0 ) );

        assertEquals( "initialize", (String) phases.get( 1 ) );

        assertEquals( "generate-sources", (String) phases.get( 2 ) );
    }

    // ----------------------------------------------------------------------
    // Repository
    // ----------------------------------------------------------------------

    public void testLocalRepositoryRetrieval()
        throws Exception
    {
        assertNotNull( maven.getLocalRepository().getBasedir() );
    }


    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void modelReadingTest()
        throws Exception
    {
        // ----------------------------------------------------------------------
        // Test model reading
        // ----------------------------------------------------------------------

        Model model = maven.readModel( getPomFile() );

        assertEquals( "org.apache.maven", model.getGroupId() );
    }

    protected void projectReadingTest()
        throws Exception
    {
        MavenProject project = maven.readProjectWithDependencies( getPomFile() );

        assertEquals( "org.apache.maven", project.getGroupId() );

        Set artifacts = project.getArtifacts();

        assertEquals( 1, artifacts.size() );

        Artifact artifact = (Artifact) artifacts.iterator().next();
    }

    // ----------------------------------------------------------------------
    // Internal Utilities
    // ----------------------------------------------------------------------

    protected File getPomFile()
    {
        return new File( basedir, "src/test/resources/pom.xml" );
    }
}
