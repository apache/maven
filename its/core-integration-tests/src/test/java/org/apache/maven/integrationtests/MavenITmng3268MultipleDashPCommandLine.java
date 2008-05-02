package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenITmng3268MultipleDashPCommandLine
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3268MultipleDashPCommandLine()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.9,)" );
    }

    public void testitMNG2234 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3268-MultipleDashPCommandLine" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();
        cliOptions.add( "-Pprofile1,profile2" );
        cliOptions.add( "-Pprofile3" );
        cliOptions.add( "-P profile4" );
        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();
        verifier.assertFilePresent( "target/profile1/touch.txt" );
        verifier.assertFilePresent( "target/profile2/touch.txt" );
        verifier.assertFilePresent( "target/profile3/touch.txt" );
        verifier.assertFilePresent( "target/profile4/touch.txt" );
        verifier.resetStreams();
    }
}
