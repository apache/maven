package org.apache.maven.it0090;

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

    public void testProjectBuildDirectoryAfterForMojoExecution()
        throws Exception
    {
        Properties testProperties = new Properties();

        File testPropertiesFile = new File( basedir, "target/mojo-generated.properties" );

        assertTrue( testPropertiesFile.exists() );

        testProperties.load( new FileInputStream( testPropertiesFile ) );

        File projectBuildDirectory = new File( basedir, "target" );

        assertEquals( testProperties.getProperty( "maven.test.envar" ), "MAVEN_TEST_ENVAR_VALUE" );
    }
}
