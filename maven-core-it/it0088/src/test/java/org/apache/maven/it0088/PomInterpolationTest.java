package org.apache.maven.it0088;

import junit.framework.TestCase;

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringInputStream;
import org.codehaus.plexus.util.StringUtils;

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

        String content = FileUtils.fileRead( testPropertiesFile );

        // Properties files need \\ escaped
        content = StringUtils.replace( content, "\\", "\\\\" );

        testProperties.load( new StringInputStream( content ) );

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

        File projectBuildDirectory = new File( basedir, "target" );

        assertEquals( testProperties.getProperty( "project.build.directory" ), projectBuildDirectory.getAbsolutePath() );
    }
}
