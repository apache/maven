package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0018Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Ensure that managed dependencies for dependency POMs are calculated
     * correctly when resolved. Removes commons-logging-1.0.3 and checks it is
     * redownloaded.
     */
    public void testit0018()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0018" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "commons-logging", "commons-logging", "1.0.3", "jar" );
        verifier.executeGoal( "package" );
// TODO: I would like to build some small core-it artifacts for this purpose instead
        verifier.assertArtifactPresent( "commons-logging", "commons-logging", "1.0.3", "jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

