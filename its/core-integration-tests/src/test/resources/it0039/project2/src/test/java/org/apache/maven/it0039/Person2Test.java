package org.apache.maven.it0039;

import junit.framework.TestCase;

public class Person2Test
    extends TestCase
{
    public void testPerson()
    {
        Person2 person = new Person2();

        person.setName( "foo" );

        assertEquals( "foo", person.getName() );
    }
}
