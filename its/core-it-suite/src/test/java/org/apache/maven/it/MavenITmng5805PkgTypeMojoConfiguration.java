package org.apache.maven.it;

import java.io.File;

import org.apache.maven.it.util.ResourceExtractor;

public class MavenITmng5805PkgTypeMojoConfiguration
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5805PkgTypeMojoConfiguration()
    {
        super( "(3.3.2,)" );
    }

    public void testPkgTypeMojoConfiguration()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5805-pkg-type-mojo-configuration" );
        
        Verifier verifier;
        
        verifier = newVerifier( new File( testDir, "extension" ).getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        verifier = newVerifier( new File( testDir, "plugin" ).getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        verifier = newVerifier( new File( testDir, "plugin-dep" ).getAbsolutePath() );
        verifier.executeGoal( "install" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        
        verifier = newVerifier( new File( testDir, "project" ).getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
