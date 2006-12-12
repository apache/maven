package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MavenIT0073Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Tests context passing between mojos in the same plugin.
     */
    public void testit0073()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0073" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.plugins", "maven-it-plugin-context-passing", "1.0",
                                 "maven-plugin" );
        List goals = Arrays.asList( new String[]{"org.apache.maven.its.plugins:maven-it-plugin-context-passing:throw",
            "org.apache.maven.its.plugins:maven-it-plugin-context-passing:catch"} );
        verifier.executeGoals( goals );
        verifier.assertFilePresent( "target/thrown-value" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

