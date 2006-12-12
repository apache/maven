package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0037Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test building with alternate pom file using '-f'
     */
    public void testit0037()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0037" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "-f pom2.xml" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );
        verifier.assertFilePresent( "target/maven-it-it0037-1.0-build2.jar" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

