package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MavenIT0027Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test @execute with a custom lifecycle, including configuration
     */
    public void testit0027()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0027" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List goals = Arrays.asList( new String[]{"org.apache.maven.its.plugins:maven-it-plugin-fork:fork",
            "org.apache.maven.its.plugins:maven-it-plugin-fork:fork-goal"} );
        verifier.executeGoals( goals );
        verifier.assertFilePresent( "target/forked/touch.txt" );
        verifier.assertFilePresent( "target/forked2/touch.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

