package org.apache.maven.it;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;

/**
 * Unit test for simple App.
 */
public class AppTest
    extends TestCase
{

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {

        String targetDir = System.getProperty( "target.dir" );

        System.out.println( "Got 'target.dir' of: '" + targetDir + "'" );

        assertNotNull( "System property 'target.dir' is not present.", targetDir );
        assertTrue( "System property 'target.dir' was not resolved correctly.", targetDir.indexOf( "${" ) < 0 );
    }
}
