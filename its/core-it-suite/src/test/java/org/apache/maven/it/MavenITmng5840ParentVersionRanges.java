package org.apache.maven.it;

import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

public class MavenITmng5840ParentVersionRanges
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng5840ParentVersionRanges()
    {
        super( "[3.3,)" );
    }

    public void testParentRangeRelativePathPointsToWrongVersion()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5840-relative-path-range-negative" );

        Verifier verifier = newVerifier( new File( testDir, "parent-1" ).getAbsolutePath(), "remote" );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = newVerifier( new File( testDir, "child" ).getAbsolutePath(), "remote" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

    public void testParentRangeRelativePathPointsToCorrectVersion()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5840-relative-path-range-positive" );

        Verifier verifier = newVerifier( new File( testDir, "parent-1" ).getAbsolutePath(), "remote" );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier = newVerifier( new File( testDir, "child" ).getAbsolutePath(), "remote" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }

}
