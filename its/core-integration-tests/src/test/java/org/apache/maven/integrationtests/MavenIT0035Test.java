package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0035Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test artifact relocation.
     */
    public void testit0035()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0035" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it-support", "1.1", "jar" );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it-support", "1.1", "pom" );
        verifier.deleteArtifact( "org.apache.maven", "maven-core-it-support-old-location", "1.1", "pom" );
        verifier.executeGoal( "package" );
        verifier.assertArtifactPresent( "org.apache.maven", "maven-core-it-support", "1.1", "jar" );
        verifier.assertArtifactPresent( "org.apache.maven", "maven-core-it-support", "1.1", "pom" );
        verifier.assertArtifactPresent( "org.apache.maven", "maven-core-it-support-old-location", "1.1", "pom" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

