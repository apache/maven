package org.apache.maven.it0105;

import junit.framework.TestCase;

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FilterTest
    extends TestCase
{
    private String basedir;

    private Properties properties;

    protected void setUp()
        throws Exception
    {
        basedir = System.getProperty( "basedir" );

        properties = new Properties();

        File testPropertiesFile = new File( basedir, "target/classes/test.properties" );

        assertTrue( testPropertiesFile.exists() );

        properties.load( new FileInputStream( testPropertiesFile ) );
    }
    
    public void testSystemPropertyInterpolation()
        throws IOException
    {
        assertEquals( "System property", System.getProperty( "java.version" ), properties.getProperty( "systemProperty" ) );
    }    

    public void testParameterInterpolation()
        throws IOException
    {
        assertEquals( "Parameter", System.getProperty( "parameter" ), properties.getProperty( "parameter" ) );
    }    

    public void testPomPropertyInterpolation()
        throws IOException
    {
        assertEquals( "Pom Property", "foo", properties.getProperty( "pom.property" ) );
    }    

}
