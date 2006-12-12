package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0009Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test plugin configuration and goal configuration that overrides what the
     * mojo has specified.
     */
    public void testit0009()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0009" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.plugins", "maven-it-plugin-touch", "1.0", "maven-plugin" );
        verifier.executeGoal( "generate-resources" );
        verifier.assertFilePresent( "target/pluginItem" );
        verifier.assertFilePresent( "target/goalItem" );
        verifier.assertFileNotPresent( "target/bad-item" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

