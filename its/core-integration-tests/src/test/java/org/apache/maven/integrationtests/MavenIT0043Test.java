package org.apache.maven.integrationtests;

import junit.framework.TestCase;
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
        verifier.assertFilePresent( "target/maven-it-it0043-1.0-SNAPSHOT.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

