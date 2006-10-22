package it0039.project2.src.test.java.org.apache.maven.it0039;

import junit.framework.TestCase;

public class Person2Test
    extends TestCase
{
    public void testPerson()
    {
        Person person = new Person();

        person.setName( "foo" );

        assertEquals( "foo", person.getName() );
    }
}
