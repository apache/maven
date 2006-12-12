package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0034Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test version range junit [3.7,) resolves to 3.8.1
     */
    public void testit0034()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0034" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it-support", "1.4", "jar" );
        verifier.deleteArtifact( "junit", "junit", "3.8", "jar" );
        verifier.executeGoal( "package" );
        verifier.assertArtifactPresent( "junit", "junit", "3.8", "jar" );
        verifier.assertArtifactPresent( "org.apache.maven", "maven-core-it-support", "1.4", "jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

