package org.apache.maven.integrationtests;

import junit.framework.TestCase;

/**
 * @author Jason van Zyl
 */
public abstract class AbstractMavenIntegrationTestCase
    extends TestCase
{
    private boolean printed = false;

    protected void setUp()
    {
        if ( !printed )
        {
            String simpleName = getClass().getSimpleName();
            simpleName = simpleName.startsWith( "MavenIT" ) ? simpleName.substring( "MavenIT".length() ) : simpleName;
            simpleName = simpleName.endsWith( "Test" ) ? simpleName.substring(0, simpleName.length() -4 ) : simpleName;

            System.out.println( simpleName + ".." );
            printed = true;
        }
    }
}
