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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-4193">MNG-4193</a>.
 * 
 * @author Benjamin Bentmann
 */
public class MavenITmng2576MakeLikeReactorTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2576MakeLikeReactorTest()
    {
        super( "[2.1.0,)" ); 
    }

    /**
     * Verify that project list by itself only builds specified projects.
     */
    public void testitOnlyList()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2576" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "sub-a/target" );
        verifier.deleteDirectory( "sub-b/target" );
        verifier.deleteDirectory( "sub-c/target" );
        verifier.deleteDirectory( "sub-d/target" );
        verifier.getCliOptions().add( "-pl" );
        verifier.getCliOptions().add( "sub-b" );
        verifier.setLogFileName( "log-only.txt" );
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
     * Verify that project list and all their upstream projects are built.
     */
    public void testitMakeUpstream()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2576" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "sub-a/target" );
        verifier.deleteDirectory( "sub-b/target" );
        verifier.deleteDirectory( "sub-c/target" );
        verifier.deleteDirectory( "sub-d/target" );
        verifier.getCliOptions().add( "-pl" );
        verifier.getCliOptions().add( "sub-b" );
        verifier.getCliOptions().add( "-am" );
        verifier.setLogFileName( "log-upstream.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFilePresent( "sub-a/target/touch.txt" );
        verifier.assertFilePresent( "sub-b/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-c/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-d/target/touch.txt" );
    }

    /**
     * Verify that project list and all their downstream projects are built.
     */
    public void testitMakeDownstream()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2576" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "sub-a/target" );
        verifier.deleteDirectory( "sub-b/target" );
        verifier.deleteDirectory( "sub-c/target" );
        verifier.deleteDirectory( "sub-d/target" );
        verifier.getCliOptions().add( "-pl" );
        verifier.getCliOptions().add( "sub-b" );
        verifier.getCliOptions().add( "-amd" );
        verifier.setLogFileName( "log-downstream.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFileNotPresent( "target/touch.txt" );
        verifier.assertFileNotPresent( "sub-a/target/touch.txt" );
        verifier.assertFilePresent( "sub-b/target/touch.txt" );
        verifier.assertFilePresent( "sub-c/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-d/target/touch.txt" );
    }

    /**
     * Verify that project list and all their upstream and downstream projects are built.
     */
    public void testitMakeBoth()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2576" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "sub-a/target" );
        verifier.deleteDirectory( "sub-b/target" );
        verifier.deleteDirectory( "sub-c/target" );
        verifier.deleteDirectory( "sub-d/target" );
        verifier.getCliOptions().add( "-pl" );
        verifier.getCliOptions().add( "sub-b" );
        verifier.getCliOptions().add( "-am" );
        verifier.getCliOptions().add( "-amd" );
        verifier.setLogFileName( "log-both.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFilePresent( "sub-a/target/touch.txt" );
        verifier.assertFilePresent( "sub-b/target/touch.txt" );
        verifier.assertFilePresent( "sub-c/target/touch.txt" );
        verifier.assertFileNotPresent( "sub-d/target/touch.txt" );
    }

}
