package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0050Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test surefire inclusion/exclusions
     */
    public void testit0050()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0050" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/testTouchFile.txt" );
        verifier.assertFilePresent( "target/defaultTestTouchFile.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

