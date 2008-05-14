package org.apache.maven.integrationtests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test activation and deactivation of profiles.
 * 
 */
public class MavenITmng3545ProfileDeactivation
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3545ProfileDeactivation()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.9,)" );
    }

    /**
     * Test build with two active by default profiles
     * 
     */
    public void testBasicBuildWithDefaultProfiles()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3545-ProfileDeactivation" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();
        // profile 1 and 2 are active by default
        verifier.assertFilePresent( "target/profile1/touch.txt" );
        verifier.assertFilePresent( "target/profile2/touch.txt" );
        verifier.assertFileNotPresent( "target/profile3/touch.txt" );
        verifier.assertFileNotPresent( "target/profile4/touch.txt" );
        verifier.assertFileNotPresent( "target/profile5/touch.txt" );
        verifier.resetStreams();

    }

    /**
     * Test command line deactivation of active by default profiles.
     * 
     */
    public void testDeactivateDefaultProfiles()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3545-ProfileDeactivation" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();

        // Deactivate active by default profiles
        cliOptions.add( "-P-profile1" );
        cliOptions.add( "-P -profile2" );

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();
        verifier.assertFileNotPresent( "target/profile1/touch.txt" );
        verifier.assertFileNotPresent( "target/profile2/touch.txt" );
        verifier.resetStreams();

    }

    /**
     * Test command line deactivation of a profile that was activated
     * by a property
     * 
     */
    public void testDeactivateActivatedByProp()
        throws Exception
    {

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3545-ProfileDeactivation" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();

        // Activate with a prop, then deactivate
        cliOptions.add( "-Dprofile3-active-by-property=true" );
        cliOptions.add( "-P-profile3" );

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();
        verifier.assertFilePresent( "target/profile1/touch.txt" );
        verifier.assertFilePresent( "target/profile2/touch.txt" );
        verifier.assertFileNotPresent( "target/profile3/touch.txt" );
        verifier.assertFileNotPresent( "target/profile4/touch.txt" );
        verifier.assertFileNotPresent( "target/profile5/touch.txt" );
        verifier.resetStreams();
    }

    /**
     * Test that deactivating from the command line takes priority over
     * activating from the command line.
     * 
     */
    public void testActivateThenDeactivate()
        throws Exception
    {

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3545-ProfileDeactivation" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();

        // Activate then deactivate
        cliOptions.add( "-Pprofile4" );
        cliOptions.add( "-P-profile4" );

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();
        verifier.assertFilePresent( "target/profile1/touch.txt" );
        verifier.assertFilePresent( "target/profile2/touch.txt" );
        verifier.assertFileNotPresent( "target/profile3/touch.txt" );
        verifier.assertFileNotPresent( "target/profile4/touch.txt" );
        verifier.assertFileNotPresent( "target/profile5/touch.txt" );
        verifier.resetStreams();
    }

    /**
     * Test that default profiles are deactivated when another profile is
     * activated.
     * 
     */
    public void testDefaultProfileAutoDeactivation()
        throws Exception
    {

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3545-ProfileDeactivation" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        List cliOptions = new ArrayList();

        // Activate
        cliOptions.add( "-Pprofile4" );

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "package" );

        verifier.verifyErrorFreeLog();
        verifier.assertFileNotPresent( "target/profile1/touch.txt" );
        verifier.assertFileNotPresent( "target/profile2/touch.txt" );
        verifier.assertFileNotPresent( "target/profile3/touch.txt" );
        verifier.assertFilePresent( "target/profile4/touch.txt" );
        verifier.assertFileNotPresent( "target/profile5/touch.txt" );
        verifier.resetStreams();
    }
    
    /**
     * remove the target dir after each test run
     */
    public void tearDown()
        throws IOException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3545-ProfileDeactivation" );

        File targetDir = new File( testDir, "target" );
        if ( targetDir.exists() )
        {
            targetDir.delete();
        }
    }

}
