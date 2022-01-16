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
 *
 * @author Benjamin Bentmann
 */
public class MavenIT0145ReactorWithIncludesExcludesTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenIT0145ReactorWithIncludesExcludesTest()
    {
        // superseded by make-like reactor mode in 3.x (see also MNG-4260)
        super( "[2.0,3.0-alpha-1)" );
    }

    /**
     * Test the old-style reactor mode with includes/excludes to locate projects.
     *
     * @throws Exception in case of failure
     */
    public void testitDefaultIncludesExcludes()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0145" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "mod-a/target" );
        verifier.deleteDirectory( "mod-b/target" );
        verifier.addCliOption( "-r" );
        verifier.setLogFileName( "log-defaults.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFileNotPresent( "target/touch.txt" );
        verifier.assertFilePresent( "mod-a/target/touch-a.txt" );
        verifier.assertFilePresent( "mod-b/target/touch-b.txt" );
    }

    /**
     * Test the old-style reactor mode with includes/excludes to locate projects.
     *
     * @throws Exception in case of failure
     */
    public void testitCustomIncludes()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0145" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "mod-a/target" );
        verifier.deleteDirectory( "mod-b/target" );
        verifier.addCliOption( "-r" );
        verifier.addCliOption( "-Dmaven.reactor.includes=mod-a/pom.xml" );
        verifier.setLogFileName( "log-includes.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFileNotPresent( "target/touch.txt" );
        verifier.assertFilePresent( "mod-a/target/touch-a.txt" );
        verifier.assertFileNotPresent( "mod-b/target/touch-b.txt" );
    }

    /**
     * Test the old-style reactor mode with includes/excludes to locate projects.
     *
     * @throws Exception in case of failure
     */
    public void testitCustomExcludes()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/it0145" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteDirectory( "mod-a/target" );
        verifier.deleteDirectory( "mod-b/target" );
        verifier.addCliOption( "-r" );
        verifier.addCliOption( "-Dmaven.reactor.excludes=mod-a/pom.xml" );
        verifier.setLogFileName( "log-excludes.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "target/touch.txt" );
        verifier.assertFileNotPresent( "mod-a/target/touch-a.txt" );
        verifier.assertFilePresent( "mod-b/target/touch-b.txt" );
    }

}
