package org.apache.maven.it;

import java.io.File;
import java.io.IOException;

import org.apache.maven.it.util.ResourceExtractor;
import org.junit.Test;

public class MavenITmng7045DropUselessAndOutdatedCdiApiTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng7045DropUselessAndOutdatedCdiApiTest()
    {
        super( "[4.0.0-alpha-1,)" );
    }

    @Test
    public void testShouldNotLeakCdiApi()
        throws IOException, VerificationException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-7045" );
        Verifier verifier = newVerifier( testDir.getAbsolutePath()) ;

        verifier.executeGoal( "process-classes" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();
    }

}
