package org.apache.maven.integrationtests;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0058Test
    extends TestCase /*extends AbstractMavenIntegrationTest*/
{

    /**
     * Verify that profiles from settings.xml do not pollute module lists
     * across projects in a reactorized build.
     */
    public void testit0058()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0058" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "--settings ./settings.xml" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "subproject/target/subproject-1.0.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        System.out.println( "it0058 PASS" );
    }
}

