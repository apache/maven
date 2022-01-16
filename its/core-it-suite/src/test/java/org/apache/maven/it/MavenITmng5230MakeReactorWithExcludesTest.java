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
        super( "[3.2,)" );
    }

     private void clean( Verifier verifier )
        throws Exception
    {
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "mod-a/target" );
        verifier.deleteDirectory( "mod-b/target" );
        verifier.deleteDirectory( "mod-c/target" );
        verifier.deleteDirectory( "mod-d/target" );
    }

    /**
     * Verify that project list exclusion by itself is not built
     *
     * @throws Exception in case of failure
     */
    public void testitMakeWithExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), true );
        verifier.setMavenDebug( true );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "!mod-b" );
        verifier.setLogFileName( "log-only.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFilePresent( "mod-a/target/touch.txt" );
        verifier.assertFileNotPresent( "mod-b/target/touch.txt" );
        verifier.assertFilePresent( "mod-c/target/touch.txt" );
        verifier.assertFilePresent( "mod-d/target/touch.txt" );
    }

    /**
     * Verify that that exclusion happens on upstream projects.
     *
     * @throws Exception in case of failure
     */
    public void testitMakeUpstreamExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "mod-b,!mod-a" );
        verifier.addCliOption( "-am" );
        verifier.setLogFileName( "log-upstream.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFileNotPresent( "mod-a/target/touch.txt" );
        verifier.assertFilePresent( "mod-b/target/touch.txt" );
        verifier.assertFileNotPresent( "mod-c/target/touch.txt" );
        verifier.assertFileNotPresent( "mod-d/target/touch.txt" );
    }

    /**
     * Verify that project list and all their downstream projects are built.
     *
     * @throws Exception in case of failure
     */
    public void testitMakeDownstreamExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "mod-b,!mod-c" );
        verifier.addCliOption( "-amd" );
        verifier.setLogFileName( "log-downstream.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFileNotPresent( "target/touch.txt" );
        verifier.assertFileNotPresent( "mod-a/target/touch.txt" );
        verifier.assertFilePresent( "mod-b/target/touch.txt" );
        verifier.assertFileNotPresent( "mod-c/target/touch.txt" );
        verifier.assertFileNotPresent( "mod-d/target/touch.txt" );
    }

    /**
     * Verify  project exclusion when also building upstream and downstream projects are built.
     *
     * @throws Exception in case of failure
     */
    public void testitMakeBothExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "mod-b,!mod-a" );
        verifier.addCliOption( "-am" );
        verifier.addCliOption( "-amd" );
        verifier.setLogFileName( "log-both.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFileNotPresent( "mod-a/target/touch.txt" );
        verifier.assertFilePresent( "mod-b/target/touch.txt" );
        verifier.assertFilePresent( "mod-c/target/touch.txt" );
        verifier.assertFileNotPresent( "mod-d/target/touch.txt" );
    }

    /**
     * Verify that using the basedir for exclusion with an exclemation in the project list matches projects with non-default POM files.
     *
     * @throws Exception in case of failure
     */
    public void testitMatchesByBasedirExclamationExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.assertFileNotPresent( "mod-d/pom.xml" );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "!mod-d" );
        verifier.setLogFileName( "log-basedir-exclamation.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFilePresent( "mod-a/target/touch.txt" );
        verifier.assertFilePresent( "mod-b/target/touch.txt" );
        verifier.assertFilePresent( "mod-c/target/touch.txt" );
        verifier.assertFileNotPresent( "mod-d/target/touch.txt" );
    }

    /**
     * Verify that using the basedir for exclusion with a minus in the project list matches projects with non-default POM files.
     *
     * @throws Exception in case of failure
     */
    public void testitMatchesByBasedirMinusExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.assertFileNotPresent( "mod-d/pom.xml" );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "-mod-d" );
        verifier.setLogFileName( "log-basedir-minus.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFilePresent( "mod-a/target/touch.txt" );
        verifier.assertFilePresent( "mod-b/target/touch.txt" );
        verifier.assertFilePresent( "mod-c/target/touch.txt" );
        verifier.assertFileNotPresent( "mod-d/target/touch.txt" );
    }


    /**
     * Verify that the project list can also specify project ids for exclusion
     *
     * @throws Exception in case of failure
     */
    public void testitMatchesByIdExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "!org.apache.maven.its.mng5230:mod-b" );
        verifier.setLogFileName( "log-id.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFilePresent( "mod-a/target/touch.txt" );
        verifier.assertFileNotPresent( "mod-b/target/touch.txt" );
        verifier.assertFilePresent( "mod-c/target/touch.txt" );
        verifier.assertFilePresent( "mod-d/target/touch.txt" );
    }

    /**
     * Verify that the project list exclude can also specify artifact ids.
     *
     * @throws Exception in case of failure
     */
    public void testitMatchesByArtifactIdExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "!:mod-b" );
        verifier.setLogFileName( "log-artifact-id.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFilePresent( "mod-a/target/touch.txt" );
        verifier.assertFileNotPresent( "mod-b/target/touch.txt" );
        verifier.assertFilePresent( "mod-c/target/touch.txt" );
        verifier.assertFilePresent( "mod-d/target/touch.txt" );
    }

     /**
     * Verify that reactor is resumed from specified project with exclude
     *
     * @throws Exception in case of failure
     */
    public void testitResumeFromExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliOption( "-rf" );
        verifier.addCliOption( "mod-b" );
        verifier.addCliOption( "-pl" );
        verifier.addCliOption( "!mod-c" );
        verifier.setLogFileName( "log-resume.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFileNotPresent( "target/touch.txt" );
        verifier.assertFileNotPresent( "mod-a/target/touch.txt" );
        verifier.assertFilePresent( "mod-b/target/touch.txt" );
        verifier.assertFileNotPresent( "mod-c/target/touch.txt" );
        verifier.assertFilePresent( "mod-d/target/touch.txt" );
    }
}
