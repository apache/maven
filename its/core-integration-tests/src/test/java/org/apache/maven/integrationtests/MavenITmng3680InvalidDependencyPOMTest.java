package org.apache.maven.integrationtests;

import java.io.File;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Verify that dependencies with invalid POMs can still be used without failing
 * the build.
 * 
 * @author jdcasey
 */
public class MavenITmng3680InvalidDependencyPOMTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3680InvalidDependencyPOMTest()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.9,)" );
    }
    
    public void testitMNG3680 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3680-invalidDependencyPOM" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.executeGoal( "compile" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
    }
}
