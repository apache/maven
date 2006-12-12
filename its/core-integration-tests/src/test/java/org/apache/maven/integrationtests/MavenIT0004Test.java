package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0004Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * The simplest of pom installation. We have a pom and we install it in
     * local repository.
     */
    public void testit0004()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0004" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven", "maven-it-it0004", "1.0", "pom" );
        verifier.executeGoal( "install:install" );
        verifier.assertArtifactPresent( "org.apache.maven.its.it0004", "maven-it-it0004", "1.0", "pom" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

