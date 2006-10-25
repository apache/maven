package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0025Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Test multiple goal executions with different execution-level configs.
     */
    public void testit0025()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0025" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "process-sources" );
        verifier.assertFilePresent( "target/test.txt" );
        verifier.assertFilePresent( "target/test2.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "it0025 PASS" );
    }
}

