package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenIT0066Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test that nonstandard POM files will be installed correctly.
     */
    public void testit0066()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0066" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        List cliOptions = new ArrayList();
        cliOptions.add( "-f other-pom.xml" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "install" );
        verifier.assertFilePresent( "" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

