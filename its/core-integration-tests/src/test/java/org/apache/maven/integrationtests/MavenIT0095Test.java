package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0095Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test URL calculation when modules are in sibling dirs of parent. (MNG-2006)
     */
    public void testit0095()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0095" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "it0095 PASS" );
    }
}

