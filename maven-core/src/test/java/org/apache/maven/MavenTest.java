package org.apache.maven;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenTest
    extends MavenTestCase
{
    public void testMaven()
        throws Exception
    {
        lookup( Maven.ROLE );
    }
}
