package org.apache.maven.it0010;

import junit.framework.TestCase;

public class PersonFinderTest
    extends TestCase
{
    public void testFindPerson()
        throws Exception
    {
        // should be no exceptions
        new PersonFinder().findPerson();
    }
}
