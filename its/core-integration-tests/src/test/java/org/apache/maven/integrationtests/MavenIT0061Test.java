package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0061Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Verify that deployment of artifacts to a legacy-layout repository
     * results in a groupId directory of 'the.full.group.id' instead of
     * 'the/full/group/id'.
     */
    public void testit0061()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0061" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "deploy" );
        verifier.assertFilePresent( "target/test-repo/org.apache.maven.its.it0061/jars/maven-it-it0061-1.0.jar" );
        verifier.assertFilePresent( "target/test-repo/org.apache.maven.its.it0061/poms/maven-it-it0061-1.0.pom" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

