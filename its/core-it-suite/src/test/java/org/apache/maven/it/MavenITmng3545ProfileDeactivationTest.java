package org.apache.maven.it;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Test activation and deactivation of profiles.
 * 
 */
public class MavenITmng3545ProfileDeactivationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3545ProfileDeactivationTest()
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
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3545" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setLogFileName( "log1.txt" );

        List cliOptions = new ArrayList();

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "validate" );

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
    public void testDeactivateDefaultProfilesDash()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3545" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setLogFileName( "log2.txt" );

        List cliOptions = new ArrayList();

        // Deactivate active by default profiles
        cliOptions.add( "-P-profile1" );
        cliOptions.add( "-P -profile2" );

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();
        verifier.assertFileNotPresent( "target/profile1/touch.txt" );
        verifier.assertFileNotPresent( "target/profile2/touch.txt" );
        verifier.resetStreams();
    }

    public void testDeactivateDefaultProfilesExclamation()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3545" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setLogFileName( "log3.txt" );

        List cliOptions = new ArrayList();

        // Deactivate active by default profiles
        cliOptions.add( "-P!profile1" );
        cliOptions.add( "-P !profile2" );

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "validate" );

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
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3545" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setLogFileName( "log4.txt" );

        List cliOptions = new ArrayList();

        // Activate with a prop, then deactivate
        cliOptions.add( "-Dprofile3-active-by-property=true" );
        cliOptions.add( "-P-profile3" );

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "validate" );

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
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3545" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setLogFileName( "log5.txt" );

        List cliOptions = new ArrayList();

        // Activate then deactivate
        cliOptions.add( "-Pprofile4" );
        cliOptions.add( "-P-profile4" );

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "validate" );

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
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3545" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setLogFileName( "log6.txt" );

        List cliOptions = new ArrayList();

        // Activate
        cliOptions.add( "-Pprofile4" );

        verifier.setCliOptions( cliOptions );
        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();
        verifier.assertFileNotPresent( "target/profile1/touch.txt" );
        verifier.assertFileNotPresent( "target/profile2/touch.txt" );
        verifier.assertFileNotPresent( "target/profile3/touch.txt" );
        verifier.assertFilePresent( "target/profile4/touch.txt" );
        verifier.assertFileNotPresent( "target/profile5/touch.txt" );
        verifier.resetStreams();
    }

}
