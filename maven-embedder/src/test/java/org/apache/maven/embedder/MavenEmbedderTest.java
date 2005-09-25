package org.apache.maven.embedder;

import junit.framework.TestCase;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import java.io.File;

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
    }

    public void testMavenEmbedder()
        throws Exception
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        maven = new MavenEmbedder();

        maven.setClassLoader( classLoader );

        maven.start();

        modelReadingTest();

        projectReadingTest();

        maven.stop();
    }

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
        MavenProject project = maven.readProject( getPomFile() );

        assertEquals( "org.apache.maven", project.getGroupId() );
    }

    // ----------------------------------------------------------------------
    // Internal Utilities
    // ----------------------------------------------------------------------

    protected File getPomFile()
    {
        return new File( basedir, "src/test/resources/pom.xml" );
    }
}
