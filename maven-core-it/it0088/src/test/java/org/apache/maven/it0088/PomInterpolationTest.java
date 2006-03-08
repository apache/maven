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

        assertEquals( testProperties.getProperty( "project.build.directory" ), projectBuildDirectory.getAbsolutePath() );
    }

    public void testProjectBuildDirectoryAfterForMojoExecution()
        throws Exception
    {
        Properties testProperties = new Properties();

        File testPropertiesFile = new File( basedir, "target/mojo-generated.properties" );

        assertTrue( testPropertiesFile.exists() );

        testProperties.load( new FileInputStream( testPropertiesFile ) );

        // [jdcasey] NOTE: This property is not a java.io.File, so it will NOT be adjusted
        // to the basedir! We need to simply check that it's value is "target", rather than
        // new java.io.File( basedir, "target" ).getAbsolutePath();
        assertEquals( testProperties.getProperty( "project.build.directory" ), "target" );
    }
}
