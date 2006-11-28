package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0051Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test source attachment when -DperformRelease=true is specified.
     */
    public void testit0051()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0051" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "--no-plugin-registry -DperformRelease=true" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/maven-it-it0051-1.0.jar" );
        verifier.assertFilePresent( "target/maven-it-it0051-1.0-sources.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "it0051 PASS" );
    }
}

