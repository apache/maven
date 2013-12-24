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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;

/**
 * Test case adapted from MNG-2576
 * @author Luuk van den Broek
 */
public class MavenITmng5230MakeReactorWithExcludesTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng5230MakeReactorWithExcludesTest()
    {
        super( "[2.1.0,)" ); 
    }

     private void clean( Verifier verifier )
        throws Exception
    {
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "sub-a/target" );
        verifier.deleteDirectory( "sub-b/target" );
        verifier.deleteDirectory( "sub-c/target" );
        verifier.deleteDirectory( "sub-d/target" );
    }

    /**
     * Verify that project list exclusion by itself is not built
     */
    public void testitMakeWithExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "!sub-b" );
        verifier.setLogFileName( "log-only.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFilePresent( "sub-a/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-b/target/touch.txt" );
        verifier.assertFilePresent( "sub-c/target/touch.txt" );
        verifier.assertFilePresent( "sub-d/target/touch.txt" );
    }

    /**
     * Verify that that exclusion happens on upstream projects.
     */
    public void testitMakeUpstreamExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "sub-b,!sub-a" );
        verifier.addCliOption( "-am" );
        verifier.setLogFileName( "log-upstream.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFileNotPresent( "sub-a/target/touch.txt" );
        verifier.assertFilePresent( "sub-b/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-c/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-d/target/touch.txt" );
    }

    /**
     * Verify that project list and all their downstream projects are built.
     */
    public void testitMakeDownstreamExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "sub-b,!sub-c" );
        verifier.addCliOption( "-amd" );
        verifier.setLogFileName( "log-downstream.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFileNotPresent( "target/touch.txt" );
        verifier.assertFileNotPresent( "sub-a/target/touch.txt" );
        verifier.assertFilePresent( "sub-b/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-c/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-d/target/touch.txt" );
    }

    /**
     * Verify  project exclusion when also building upstream and downstream projects are built.
     */
    public void testitMakeBothExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "sub-b,!sub-a" );
        verifier.addCliOption( "-am" );
        verifier.addCliOption( "-amd" );
        verifier.setLogFileName( "log-both.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFileNotPresent( "sub-a/target/touch.txt" );
        verifier.assertFilePresent( "sub-b/target/touch.txt" );
        verifier.assertFilePresent( "sub-c/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-d/target/touch.txt" );
    }

    /**
     * Verify that using the basedir for exclusion in the project list  matches projects with non-default POM files.
     */
    public void testitMatchesByBasedirExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.assertFileNotPresent( "sub-d/pom.xml" );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "!sub-d" );
        verifier.setLogFileName( "log-basedir.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFilePresent( "sub-a/target/touch.txt" );
        verifier.assertFilePresent( "sub-b/target/touch.txt" );
        verifier.assertFilePresent( "sub-c/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-d/target/touch.txt" );
    }

    /**
     * Verify that the project list can also specify project ids for exclusion
     */
    public void testitMatchesByIdExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "!org.apache.maven.its.mng5230:sub-b" );
        verifier.setLogFileName( "log-id.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFilePresent( "sub-a/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-b/target/touch.txt" );
        verifier.assertFilePresent( "sub-c/target/touch.txt" );
        verifier.assertFilePresent( "sub-d/target/touch.txt" );
    }

    /**
     * Verify that the project list exclude can also specify aritfact ids.
     */
    public void testitMatchesByArtifactIdExclude()
        throws Exception
    {
        // as per MNG-4244
        requiresMavenVersion( "[3.0-alpha-3,)" );

        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "!:sub-b" );
        verifier.setLogFileName( "log-artifact-id.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFilePresent( "sub-a/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-b/target/touch.txt" );
        verifier.assertFilePresent( "sub-c/target/touch.txt" );
        verifier.assertFilePresent( "sub-d/target/touch.txt" );
    }
    
     /**
     * Verify that reactor is resumed from specified project with exclude
     */
    public void testitResumeFromExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-rf" );
        verifier.addCliOption( "sub-b" );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "!sub-c" );
        verifier.setLogFileName( "log-resume.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFileNotPresent( "target/touch.txt" );
        verifier.assertFileNotPresent( "sub-a/target/touch.txt" );
        verifier.assertFilePresent( "sub-b/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-c/target/touch.txt" );
        verifier.assertFilePresent( "sub-d/target/touch.txt" );
    }
}