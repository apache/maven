package org.apache.maven.integrationtests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class IntegrationTestSuite
    extends AbstractMavenIntegrationTestCase
{
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite( MavenIT0111PluginsThatRequireAResourceFromAnExtensionTest.class );
        return suite;
    }
}
