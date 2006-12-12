package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0038Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test building project from outside the project directory using '-f'
     * option
     */
    public void testit0038()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0038" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "-f project/pom2.xml" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "project/target/maven-it-it0038-1.0-build2.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

