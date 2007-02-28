package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MavenIT0077Test
    extends AbstractMavenIntegrationTestCase
{
    /**
     * Test test jar attachment.
     */
    public void testit0077()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0077" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteArtifact( "org.apache.maven.its.it0077", "sub1", "1.0", "test-jar" );
        verifier.executeGoal( "install" );
        verifier.assertArtifactPresent( "org.apache.maven.its.it0077", "sub1", "1.0", "test-jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}

