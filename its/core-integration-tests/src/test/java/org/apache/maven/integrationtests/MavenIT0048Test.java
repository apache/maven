package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0048Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Verify that default values for mojo parameters are working (indirectly,
     * by verifying that the Surefire mojo is functioning correctly).
     */
    public void testit0048()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0048" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "test" );
        verifier.assertFilePresent( "target/testFileOutput.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

