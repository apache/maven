package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0040Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test the use of a packaging from a plugin
     */
    public void testit0040()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0040" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/maven-it-it0040-1.0-it.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

