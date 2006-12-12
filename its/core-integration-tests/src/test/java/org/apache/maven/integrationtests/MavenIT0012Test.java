package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0012Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test simple POM interpolation
     */
    public void testit0012()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0012" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-touch:touch" );
        verifier.assertFilePresent( "target/touch-3.8.1.txt" );
        verifier.assertFilePresent( "child-project/target/child-touch-3.0.3.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

