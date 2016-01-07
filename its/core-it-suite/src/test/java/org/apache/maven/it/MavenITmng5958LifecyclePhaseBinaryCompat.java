package org.apache.maven.it;

import java.io.File;

import org.apache.maven.it.util.ResourceExtractor;

public class MavenITmng5958LifecyclePhaseBinaryCompat
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5958LifecyclePhaseBinaryCompat()
    {
        super( "(3.3.9,)" );
    }

    public void testGood()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5958-lifecycle-phases/good" );
        
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog( "CLASS_NAME=java.lang.String" );
        verifier.resetStreams();
    }
    
    public void testBad()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5958-lifecycle-phases/bad" );
        
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        try
        {
            verifier.executeGoal( "validate" );
        }
        catch ( VerificationException e )
        {
            verifier.verifyTextInLog( "[ERROR] Internal error: java.lang.ClassCastException: "
                + "org.apache.maven.lifecycle.mapping.LifecyclePhase cannot be cast to java.lang.String -> [Help 1]" );
        }
        verifier.resetStreams();
    }
}
