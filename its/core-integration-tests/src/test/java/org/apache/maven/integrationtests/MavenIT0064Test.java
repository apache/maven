package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0064Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test the use of a mojo that uses setters instead of private fields
     * for the population of configuration values.
     */
    public void testit0064()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0064" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.plugins", "maven-it-plugin-setter", "1.0", "maven-plugin" );
        verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-setter:setter-touch" );
        verifier.assertFilePresent( "target/fooValue" );
        verifier.assertFilePresent( "target/barValue.baz" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

