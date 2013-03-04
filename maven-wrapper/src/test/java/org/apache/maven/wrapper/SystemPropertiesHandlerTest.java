package org.apache.maven.wrapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class SystemPropertiesHandlerTest
{

    private File tmpDir = new File( "target/test-files/SystemPropertiesHandlerTest" );

    @Before
    public void setupTempDir()
    {
        tmpDir.mkdirs();
    }

    @Test
    public void testParsePropertiesFile()
        throws Exception
    {
        File propFile = new File( tmpDir, "props" );
        Properties props = new Properties();
        props.put( "a", "b" );
        props.put( "systemProp.c", "d" );
        props.put( "systemProp.", "e" );

        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream( propFile );
            props.store( fos, "" );
        }
        finally
        {
            IOUtils.closeQuietly( fos );
        }

        Map<String, String> expected = new HashMap<String, String>();
        expected.put( "c", "d" );

        assertThat( SystemPropertiesHandler.getSystemProperties( propFile ), equalTo( expected ) );
    }

    @Test
    public void ifNoPropertyFileExistShouldReturnEmptyMap()
    {
        Map<String, String> expected = new HashMap<String, String>();
        assertThat( SystemPropertiesHandler.getSystemProperties( new File( tmpDir, "unknown" ) ), equalTo( expected ) );
    }
}
