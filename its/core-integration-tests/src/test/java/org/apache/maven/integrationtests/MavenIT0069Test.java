package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0069Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test offline mode.
     */
    public void testit0069()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0069" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "-o" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "compile" );
        verifier.assertFilePresent( "target/classes/org/apache/maven/it0069/ClassworldBasedThing.class" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

