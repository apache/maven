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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;

import org.junit.jupiter.api.Test;

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
    @Test
    public void testitMakeWithExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath(), true );
        verifier.setMavenDebug( true );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliArgument( "-pl" );
        verifier.addCliArgument( "!mod-b" );
        verifier.setLogFileName( "log-only.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFilePresent( "mod-a/target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-b/target/touch.txt" );
        verifier.verifyFilePresent( "mod-c/target/touch.txt" );
        verifier.verifyFilePresent( "mod-d/target/touch.txt" );
    }

    /**
     * Verify that that exclusion happens on upstream projects.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMakeUpstreamExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliArgument( "-pl" );
        verifier.addCliArgument( "mod-b,!mod-a" );
        verifier.addCliArgument( "-am" );
        verifier.setLogFileName( "log-upstream.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-a/target/touch.txt" );
        verifier.verifyFilePresent( "mod-b/target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-c/target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-d/target/touch.txt" );
    }

    /**
     * Verify that project list and all their downstream projects are built.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMakeDownstreamExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliArgument( "-pl" );
        verifier.addCliArgument( "mod-b,!mod-c" );
        verifier.addCliArgument( "-amd" );
        verifier.setLogFileName( "log-downstream.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFileNotPresent( "target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-a/target/touch.txt" );
        verifier.verifyFilePresent( "mod-b/target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-c/target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-d/target/touch.txt" );
    }

    /**
     * Verify  project exclusion when also building upstream and downstream projects are built.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMakeBothExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliArgument( "-pl" );
        verifier.addCliArgument( "mod-b,!mod-a" );
        verifier.addCliArgument( "-am" );
        verifier.addCliArgument( "-amd" );
        verifier.setLogFileName( "log-both.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-a/target/touch.txt" );
        verifier.verifyFilePresent( "mod-b/target/touch.txt" );
        verifier.verifyFilePresent( "mod-c/target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-d/target/touch.txt" );
    }

    /**
     * Verify that using the basedir for exclusion with an exclamation in the project list matches projects with non-default POM files.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMatchesByBasedirExclamationExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.verifyFileNotPresent( "mod-d/pom.xml" );
        verifier.addCliArgument( "-pl" );
        verifier.addCliArgument( "!mod-d" );
        verifier.setLogFileName( "log-basedir-exclamation.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFilePresent( "mod-a/target/touch.txt" );
        verifier.verifyFilePresent( "mod-b/target/touch.txt" );
        verifier.verifyFilePresent( "mod-c/target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-d/target/touch.txt" );
    }

    /**
     * Verify that using the basedir for exclusion with a minus in the project list matches projects with non-default POM files.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMatchesByBasedirMinusExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.verifyFileNotPresent( "mod-d/pom.xml" );
        verifier.addCliArgument( "-pl" );
        verifier.addCliArgument( "-mod-d" );
        verifier.setLogFileName( "log-basedir-minus.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFilePresent( "mod-a/target/touch.txt" );
        verifier.verifyFilePresent( "mod-b/target/touch.txt" );
        verifier.verifyFilePresent( "mod-c/target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-d/target/touch.txt" );
    }


    /**
     * Verify that the project list can also specify project ids for exclusion
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMatchesByIdExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliArgument( "-pl" );
        verifier.addCliArgument( "!org.apache.maven.its.mng5230:mod-b" );
        verifier.setLogFileName( "log-id.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFilePresent( "mod-a/target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-b/target/touch.txt" );
        verifier.verifyFilePresent( "mod-c/target/touch.txt" );
        verifier.verifyFilePresent( "mod-d/target/touch.txt" );
    }

    /**
     * Verify that the project list exclude can also specify artifact ids.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMatchesByArtifactIdExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliArgument( "-pl" );
        verifier.addCliArgument( "!:mod-b" );
        verifier.setLogFileName( "log-artifact-id.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent( "target/touch.txt" );
        verifier.verifyFilePresent( "mod-a/target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-b/target/touch.txt" );
        verifier.verifyFilePresent( "mod-c/target/touch.txt" );
        verifier.verifyFilePresent( "mod-d/target/touch.txt" );
    }

     /**
     * Verify that reactor is resumed from specified project with exclude
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitResumeFromExclude()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5230-make-reactor-with-excludes" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        clean( verifier );
        verifier.addCliArgument( "-rf" );
        verifier.addCliArgument( "mod-b" );
        verifier.addCliArgument( "-pl" );
        verifier.addCliArgument( "!mod-c" );
        verifier.setLogFileName( "log-resume.txt" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFileNotPresent( "target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-a/target/touch.txt" );
        verifier.verifyFilePresent( "mod-b/target/touch.txt" );
        verifier.verifyFileNotPresent( "mod-c/target/touch.txt" );
        verifier.verifyFilePresent( "mod-d/target/touch.txt" );
    }
}
