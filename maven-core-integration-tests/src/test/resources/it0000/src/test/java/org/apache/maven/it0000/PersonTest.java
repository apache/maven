package org.apache.maven.it0000;

import junit.framework.TestCase;

public class PersonTest
    extends TestCase
{
    public void testPerson()
    {
        IT0000Person person = new IT0000Person();
        
        person.setName( "foo" );
        
        assertEquals( "foo", person.getName() );
    }
}
