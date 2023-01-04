package org.apache.maven.it;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.VerificationException;

import java.io.File;

import org.junit.jupiter.api.Test;

public class MavenITmng5958LifecyclePhaseBinaryCompat
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5958LifecyclePhaseBinaryCompat()
    {
        super( "(3.3.9,)" );
    }

    @Test
    public void testGood()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5958-lifecycle-phases/good" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog( "CLASS_NAME=java.lang.String" );
    }

    @Test
    public void testBad()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5958-lifecycle-phases/bad" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        try
        {
            verifier.addCliArgument( "validate" );
            verifier.execute();
        }
        catch ( VerificationException e )
        {
            // There is no good way to test for Java 9+ in Verifier in order to add 'java.base/' to the string.
            // Java 11: Internal error: java.lang.ClassCastException: class org.apache.maven..
            verifier.verifyTextInLog( "[ERROR] Internal error: java.lang.ClassCastException: " );
            verifier.verifyTextInLog( "org.apache.maven.lifecycle.mapping.LifecyclePhase cannot be cast to " );
        }
    }
}
