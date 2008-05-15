package org.apache.maven.integrationtests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test that profiles with multiple activators are activated 
 * when any of the activators are on.
 * 
 */
public class MavenITmng3106ProfileMultipleActivators
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3106ProfileMultipleActivators()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.9,)" );
    }

    /**
     * Test build with two profiles, each with more than one activator.
     * The profiles should be activated even though only one of the activators 
     * returns true.
     * 
     */
    public void testProfilesWithMultipleActivators()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3106-ProfileMultipleActivators" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();
        cliOptions.add( "-Dprofile1.on=true" );

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();
        verifier.assertFilePresent( "target/profile1/touch.txt" );
        verifier.assertFilePresent( "target/profile2/touch.txt" );
        verifier.resetStreams();

    }

}
