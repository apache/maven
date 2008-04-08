package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenITmng3485OverrideWagonExtensionTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3485OverrideWagonExtensionTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8, 2.1-SNAPSHOT)" ); // only test in 2.0.9+
    }

    public void testitMNG3485 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3485-overrideWagonExtension" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "deploy" );

        verifier.assertFilePresent( "target/wagon-data" );
        verifier.verifyErrorFreeLog();

        verifier.resetStreams();
    }
}
