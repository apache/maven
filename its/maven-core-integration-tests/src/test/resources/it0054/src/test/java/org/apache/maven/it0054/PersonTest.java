package org.apache.maven.it0054;

import java.util.Properties;
import junit.framework.TestCase;

public class PersonTest
    extends TestCase
{
    public void testPerson()
        throws Exception
    {
        Person person = new Person();
        
        person.setName( "foo" );
        
        assertEquals( "foo", person.getName() );

        Properties p = new Properties();
        p.load( getClass().getResourceAsStream( "/it0054.properties" ) );
        assertEquals( "check name", "jason", p.getProperty( "name" ) );
        assertEquals( "check surname", "van zyl", p.getProperty( "surname" ) );
        assertEquals( "check country", "Canada", p.getProperty( "country" ) );
    }
}
