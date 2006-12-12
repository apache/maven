package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0053Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that attached artifacts have the same buildnumber and timestamp
     * as the main artifact. This will not correctly verify until we have
     * some way to pattern-match the buildnumber/timestamp...
     */
    public void testit0053()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0053" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "--no-plugin-registry" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/maven-it-it0053-1.0-SNAPSHOT.jar" );
        verifier.assertFileNotPresent( "target/maven-it-it0053-1.0-SNAPSHOT-sources.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

