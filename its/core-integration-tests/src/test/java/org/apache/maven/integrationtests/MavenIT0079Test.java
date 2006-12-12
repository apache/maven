package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0079Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that source attachments have the same build number as the main
     * artifact when deployed.
     */
    public void testit0079()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0079" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "deploy" );
        verifier.assertFilePresent(
            "target/test-repo/org/apache/maven/its/it0079/maven-it-it0079/SNAPSHOT/maven-it-it0079-*-1.jar" );
        verifier.assertFilePresent(
            "target/test-repo/org/apache/maven/its/it0079/maven-it-it0079/SNAPSHOT/maven-it-it0079-*-1-sources.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

