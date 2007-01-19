package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0043Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test for repository inheritence - ensure using the same id overrides the defaults
     */
    public void testit0043()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0043" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "child1/target/maven-it-it0043-child1-1.0-SNAPSHOT.jar" );
        verifier.assertFilePresent( "child2/target/maven-it-it0043-child2-1.0-SNAPSHOT.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

