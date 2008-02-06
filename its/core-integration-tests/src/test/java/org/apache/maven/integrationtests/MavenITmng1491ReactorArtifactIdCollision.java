package org.apache.maven.integrationtests;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenITmng1491ReactorArtifactIdCollision
    extends AbstractMavenIntegrationTestCase
{
    public void testitMNG1491 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1491-reactorArtifactIdCollision" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        try
        {
            verifier.executeGoal( "initialize" );

            verifier.verifyErrorFreeLog();

            fail( "Build should fail due to duplicate artifactId's in the reactor." );
        }
        catch( VerificationException e )
        {
            // expected.
        }
    }
}
