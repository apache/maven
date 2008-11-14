package org.apache.maven.it.it0098;

import junit.framework.TestCase;

public class QuotedCLIPropertyTest
    extends TestCase
{
    
    public void testPropertyValue()
    {
        assertEquals( "Test Property", System.getProperty( "test.property" ) );
    }

}
