package org.apache.maven.its.itmng1412;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.lang.StringUtils;

import junit.framework.TestCase;

/**
 * Test that dependencies order in classpath matches pom.xml.
 * 
 * @author <a href="mailto:hboutemy@apache.org">Herve Boutemy</a>
 * 
 */
public class ITmng1412DependenciesOrderTest
extends TestCase
{
    private final static String[] EXTENSIONS = {
        // same order as in pom.xml
        "commons-net",
        "commons-collections",
        "commons-lang",
        "commons-io",
    };
    
    public void testOrder() throws IOException
    {
        String expected = StringUtils.join( EXTENSIONS, ',' );
        StringBuffer found = new StringBuffer();

        Enumeration resources = this.getClass().getClassLoader().getResources( "META-INF/MANIFEST.MF" );
        while ( resources.hasMoreElements() )
        {
            URL url = (URL) resources.nextElement();

            Manifest manifest = new Manifest( url.openStream() );
            Attributes attributes = manifest.getMainAttributes();
            String extensionName = attributes.getValue( "Extension-Name" );

            if ( ( extensionName != null ) && extensionName.startsWith( "commons" ) )
            {
                if ( found.length() > 0 )
                {
                    found.append( ',' );
                }
                found.append( extensionName );
            }
        }

        System.out.println( "Expected:\n\n" + expected + "\n\nFound:\n\n" + found );

        assertEquals( "dependencies order in classpath should match pom.xml", expected, found.toString() );
    }
}
