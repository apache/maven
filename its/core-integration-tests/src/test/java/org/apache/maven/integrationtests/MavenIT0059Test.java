package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MavenIT0059Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Verify that maven-1 POMs will be ignored but not stop the resolution
     * process.
     */
    public void testit0059()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0059" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "--settings settings.xml" );
        verifier.setCliOptions( cliOptions );
        Properties verifierProperties = new Properties();
        verifierProperties.put( "failOnErrorOutput", "false" );
        verifier.setVerifierProperties( verifierProperties );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/maven-it-it0059-1.0.jar" );
        // don't verify error free log
        verifier.resetStreams();

    }
}

