package org.apache.maven.embedder;

import junit.framework.TestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

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

        maven = new MavenEmbedder( classLoader, new MavenEmbedderConsoleLogger() );
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

    public void testSimplePhaseExecutionUsingABaseDirectory()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/embedder-test-project" );

        File targetDirectory = new File( basedir, "target/embedder-test-project0" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setBasedir( targetDirectory )
            .setGoals( Arrays.asList( new String[]{ "package" } ) );

        MavenExecutionResult result = maven.execute( request );

        MavenProject project = result.getMavenProject();

        assertEquals( "embedder-test-project", project.getArtifactId() );

        File jar = new File( targetDirectory, "target/embedder-test-project-1.0-SNAPSHOT.jar" );

        assertTrue( jar.exists() );
    }

    public void testSimplePhaseExecutionUsingAPomFile()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/embedder-test-project" );

        File targetDirectory = new File( basedir, "target/embedder-test-project1" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setPomFile( new File( targetDirectory, "pom.xml" ).getAbsolutePath() )
            .setGoals( Arrays.asList( new String[]{ "package" } ) );

        MavenExecutionResult result = maven.execute( request );

        MavenProject project = result.getMavenProject();

        assertEquals( "embedder-test-project", project.getArtifactId() );

        File jar = new File( targetDirectory, "target/embedder-test-project-1.0-SNAPSHOT.jar" );

        assertTrue( jar.exists() );
    }

    // ----------------------------------------------------------------------
    // Test mock plugin metadata
    // ----------------------------------------------------------------------

    public void xtestMockPluginMetadata()
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

    public void testNothing()
    {
    }
}
