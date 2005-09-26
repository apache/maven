package org.apache.maven.embedder;

import junit.framework.TestCase;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.Set;
import java.util.Collections;

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

        EventDispatcher eventDispatcher = new DefaultEventDispatcher();

        maven.execute( pom, Collections.singletonList( "package" ), eventDispatcher, targetDirectory );

        File jar = new File( targetDirectory, "target/embedder-test-project-1.0-SNAPSHOT.jar" );

        assertTrue( jar.exists() );
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

        System.out.println( "artifact.getFile().getAbsolutePath() = " + artifact.getFile().getAbsolutePath() );
    }

    // ----------------------------------------------------------------------
    // Internal Utilities
    // ----------------------------------------------------------------------

    protected File getPomFile()
    {
        return new File( basedir, "src/test/resources/pom.xml" );
    }
}
