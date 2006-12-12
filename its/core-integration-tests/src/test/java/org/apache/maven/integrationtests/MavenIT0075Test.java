package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MavenIT0075Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Verify that direct invocation of a mojo from the command line still
     * results in the processing of modules included via profiles.
     */
    public void testit0075()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0075" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "-Dactivate=anything" );
        verifier.setCliOptions( cliOptions );
        List goals = Arrays.asList( new String[]{"help:active-profiles", "package", "eclipse:eclipse", "clean:clean"} );
        verifier.executeGoals( goals );
        verifier.assertFileNotPresent( "sub1/target/maven-it-it0075-sub1-1.0.jar" );
        verifier.assertFileNotPresent( "sub2/target/maven-it-it0075-sub2-1.0.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

