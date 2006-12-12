package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0049Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test parameter alias usage.
     */
    public void testit0049()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0049" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        //todo: i don't think we need to delete this plugin
        //verifier.deleteArtifact( "org.apache.maven.its.plugins", "maven-it-plugin-touch", "1.0", "maven-plugin" );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-touch:touch" );
        verifier.assertFilePresent( "target/touchFile.txt" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

