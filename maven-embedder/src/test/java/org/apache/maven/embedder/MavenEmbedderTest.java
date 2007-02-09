package org.apache.maven.embedder;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginManagerException;

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

    private void assertNoExceptions( MavenExecutionResult result )
    {
        List exceptions = result.getExceptions();
        if ( ( exceptions == null ) || exceptions.isEmpty() )
        {
            // everything is a-ok.
            return;
        }

        System.err.println( "Encountered " + exceptions.size() + " exception(s)." );
        Iterator it = exceptions.iterator();
        while ( it.hasNext() )
        {
            Exception exception = (Exception) it.next();
            exception.printStackTrace( System.err );
        }

        fail( "Encountered Exceptions in MavenExecutionResult during " + getName() );
    }

    // ----------------------------------------------------------------------
    // Goal/Phase execution tests
    // ----------------------------------------------------------------------

    public void testExecutionUsingABaseDirectory()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/embedder-test-project" );

        File targetDirectory = new File( basedir, "target/embedder-test-project0" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setBaseDirectory( targetDirectory )
            .setShowErrors( true ).setGoals( Arrays.asList( new String[] { "package" } ) );

        MavenExecutionResult result = maven.execute( request );

        assertNoExceptions( result );

        MavenProject project = result.getMavenProject();

        assertEquals( "embedder-test-project", project.getArtifactId() );

        File jar = new File( targetDirectory, "target/embedder-test-project-1.0-SNAPSHOT.jar" );

        assertTrue( jar.exists() );
    }

    public void testExecutionUsingAPomFile()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/embedder-test-project" );

        File targetDirectory = new File( basedir, "target/embedder-test-project1" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setPomFile(
                                                                                       new File( targetDirectory,
                                                                                                 "pom.xml" )
                                                                                           .getAbsolutePath() )
            .setShowErrors( true ).setGoals( Arrays.asList( new String[] { "package" } ) );

        MavenExecutionResult result = maven.execute( request );

        assertNoExceptions( result );
        
        MavenProject project = result.getMavenProject();

        assertEquals( "embedder-test-project", project.getArtifactId() );

        File jar = new File( targetDirectory, "target/embedder-test-project-1.0-SNAPSHOT.jar" );

        assertTrue( jar.exists() );
    }

    public void testExecutionUsingAProfileWhichSetsAProperty()
        throws Exception
    {
        File testDirectory = new File( basedir, "src/test/embedder-test-project" );

        File targetDirectory = new File( basedir, "target/embedder-test-project2" );

        FileUtils.copyDirectoryStructure( testDirectory, targetDirectory );

        // Check with profile not active

        MavenExecutionRequest requestWithoutProfile = new DefaultMavenExecutionRequest()
            .setPomFile( new File( targetDirectory, "pom.xml" ).getAbsolutePath() ).setShowErrors( true )
            .setGoals( Arrays.asList( new String[] { "validate" } ) );

        MavenExecutionResult r0 = maven.execute( requestWithoutProfile );
        
        assertNoExceptions( r0 );

        MavenProject p0 = r0.getMavenProject();

        assertNull( p0.getProperties().getProperty( "embedderProfile" ) );

        assertNull( p0.getProperties().getProperty( "name" ) );

        assertNull( p0.getProperties().getProperty( "occupation" ) );

        // Check with profile activated

        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setPomFile(
                                                                                       new File( targetDirectory,
                                                                                                 "pom.xml" )
                                                                                           .getAbsolutePath() )
            .setShowErrors( true ).setGoals( Arrays.asList( new String[] { "validate" } ) )
            .addActiveProfile( "embedderProfile" );

        MavenExecutionResult r1 = maven.execute( request );

        MavenProject p1 = r1.getMavenProject();

        assertEquals( "true", p1.getProperties().getProperty( "embedderProfile" ) );

        assertEquals( "jason", p1.getProperties().getProperty( "name" ) );

        assertEquals( "somnambulance", p1.getProperties().getProperty( "occupation" ) );
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
    // Model Reading
    // ----------------------------------------------------------------------

    public void testModelReading()
        throws Exception
    {
        // ----------------------------------------------------------------------
        // Test model reading
        // ----------------------------------------------------------------------

        Model model = maven.readModel( getPomFile() );

        assertEquals( "org.apache.maven", model.getGroupId() );
    }

    public void testProjectReading()
        throws Exception
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setShowErrors( true )
            .setPomFile( getPomFile().getAbsolutePath() );

        MavenExecutionResult result = maven.readProjectWithDependencies( request );
        
        assertNoExceptions( result );

        assertEquals( "org.apache.maven", result.getMavenProject().getGroupId() );

        Set artifacts = result.getMavenProject().getArtifacts();

        assertEquals( 1, artifacts.size() );

        Artifact artifact = (Artifact) artifacts.iterator().next();

        System.out.println( "artifact = " + artifact );
    }

    public void testProjectWithExtensionsReading()
        throws Exception
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest().setShowErrors( true )
            .setPomFile( new File( basedir, "src/test/resources/pom2.xml" ).getAbsolutePath() );

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        MavenExecutionResult result = new ExtendableMavenEmbedder( classLoader ).readProjectWithDependencies( request );
        
        assertNoExceptions( result );

        //        Iterator it = result.getMavenProject().getTestClasspathElements().iterator();
        //        while(it.hasNext()) {
        //            Object object = (Object) it.next();
        //            System.out.println(" element=" + object);
        //        }

        // sources, test sources, and the junit jar..
        assertEquals( 3, result.getMavenProject().getTestClasspathElements().size() );

    }

    // ----------------------------------------------------------------------------
    // Model Writing
    // ----------------------------------------------------------------------------

    public void testModelWriting()
        throws Exception
    {
        Model model = maven.readModel( getPomFile() );

        model.setGroupId( "org.apache.maven.new" );

        File file = new File( basedir, "target/model.xml" );

        Writer writer = new FileWriter( file );

        maven.writeModel( writer, model );

        writer.close();

        model = maven.readModel( file );

        assertEquals( "org.apache.maven.new", model.getGroupId() );
    }

    // ----------------------------------------------------------------------
    // Internal Utilities
    // ----------------------------------------------------------------------

    protected File getPomFile()
    {
        return new File( basedir, "src/test/resources/pom.xml" );
    }

    private class ExtendableMavenEmbedder
        extends MavenEmbedder
    {

        public ExtendableMavenEmbedder( ClassLoader classLoader )
            throws MavenEmbedderException
        {
            super( classLoader, new MavenEmbedderConsoleLogger() );
        }

        protected Map getPluginExtensionComponents( Plugin plugin )
            throws PluginManagerException
        {
            Map toReturn = new HashMap();
            MyArtifactHandler handler = new MyArtifactHandler();
            toReturn.put( "mkleint", handler );
            return toReturn;
        }

        protected void verifyPlugin( Plugin plugin, MavenProject project )
        {
            //ignore don't want to actually verify in test
        }
    }

    private class MyArtifactHandler
        implements ArtifactHandler
    {

        public String getExtension()
        {
            return "jar";
        }

        public String getDirectory()
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public String getClassifier()
        {
            return null;
        }

        public String getPackaging()
        {
            return "mkleint";
        }

        public boolean isIncludesDependencies()
        {
            return false;
        }

        public String getLanguage()
        {
            return "java";
        }

        public boolean isAddedToClasspath()
        {
            return true;
        }
    }
}
