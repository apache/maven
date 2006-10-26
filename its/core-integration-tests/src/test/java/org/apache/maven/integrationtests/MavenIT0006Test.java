package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0006Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Integration test for the verifier plugin.
     */
    public void testit0006()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0006" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "integration-test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "it0006 PASS" );
    }
}

