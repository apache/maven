package org.apache.maven.it;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;

import org.junit.jupiter.api.Test;

public class MavenITmng5805PkgTypeMojoConfiguration2
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5805PkgTypeMojoConfiguration2()
    {
        super( "(3.3.3,)" );
    }

    @Test
    public void testPkgTypeMojoConfiguration()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5805-pkg-type-mojo-configuration2" );

        Verifier verifier;


        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog( "CLASS_NAME=org.apache.maven.its.mng5805.TestClass1" );
        verifier.resetStreams();
    }
}
