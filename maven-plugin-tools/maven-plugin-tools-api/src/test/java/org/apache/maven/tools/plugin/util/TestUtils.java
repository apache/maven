package org.apache.maven.tools.plugin.util;

import java.net.URL;

import junit.framework.TestCase;

/**
 * @author jdcasey
 */
public class TestUtils
    extends TestCase
{

    public void testDirnameFunction_METATEST()
    {
        String classname = getClass().getName().replace( '.', '/' ) + ".class";
        String basedir = TestUtils.dirname( classname );

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL resource = cl.getResource( classname );

        assertEquals( resource.getPath(), basedir + classname );
    }

    public static String dirname( String file )
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL fileResource = cl.getResource( file );

        String fullPath = fileResource.getPath();

        return fullPath.substring( 0, fullPath.length() - file.length() );
    }

}