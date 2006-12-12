package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0099Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that parent-POMs cached during a build are available as parents
     * to other POMs in the multimodule build. [MNG-2130]
     */
    public void testit0099()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0099" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.it0099", "maven-it-it0099-parent", "1", "pom" );
        verifier.executeGoal( "package" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

