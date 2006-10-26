package org.apache.maven.it0010;

public class PersonFinder
{
    public void findPerson()
        throws Exception
    {
        // look it up at runtime, but do not require it at compile time
        Class.forName( "org.codehaus.classworlds.ClassRealm" );
    }
}
