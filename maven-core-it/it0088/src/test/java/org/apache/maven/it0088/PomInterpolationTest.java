package org.apache.maven.it0088;

import junit.framework.TestCase;

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;

public class PomInterpolationTest
    extends TestCase
{
    private String basedir;

    protected void setUp()
        throws Exception
    {
        basedir = System.getProperty( "basedir" );
    }

    public void testProjectBuildDirectoryAfterResourceFiltering()
        throws Exception
    {
        Properties testProperties = new Properties();

        File testPropertiesFile = new File( basedir, "target/classes/test.properties" );

        assertTrue( testPropertiesFile.exists() );

        testProperties.load( new FileInputStream( testPropertiesFile ) );

        File projectBuildDirectory = new File( basedir, "target" );

        assertEquals( projectBuildDirectory.getAbsolutePath(), testProperties.getProperty( "project.build.directory" ) );
    }

    public void testProjectBuildDirectoryForMojoExecution()
        throws Exception
    {
        Properties testProperties = new Properties();

        File testPropertiesFile = new File( basedir, "target/mojo-generated.properties" );

        assertTrue( testPropertiesFile.exists() );

        testProperties.load( new FileInputStream( testPropertiesFile ) );

        File projectBuildDirectory = new File( basedir, "target" );

        assertEquals( projectBuildDirectory.getAbsolutePath(), testProperties.getProperty( "project.build.directory" ) );
        assertEquals( projectBuildDirectory.getAbsolutePath(), testProperties.getProperty( "targetDirectoryFile" ) );
        assertEquals( "target", testProperties.getProperty( "targetDirectoryString" ) );
    }
}
