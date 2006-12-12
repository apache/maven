package org.apache.maven.integrationtests;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenIT0081Test
    extends AbstractMavenIntegrationTestCase
{

    /**
     * Test per-plugin dependencies.
     */
    public void testit0081()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0081" );
        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.assertFilePresent( "test-component-c/target/org.apache.maven.wagon.providers.ftp.FtpWagon" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

    }
}

