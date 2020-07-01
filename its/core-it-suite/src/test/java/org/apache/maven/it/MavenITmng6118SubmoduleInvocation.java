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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a collection of test cases for <a href="https://issues.apache.org/jira/browse/MNG-6118">MNG-6118</a>,
 * invoking Maven in a submodule of a multi-module project.
 *
 * The test uses a multi-module project with two modules:
 * <ul>
 *     <li>app</li> (depends on lib)
 *     <li>lib</li>
 * </ul>
 *
 * @author Maarten Mulders
 * @author Martin Kanters
 */
public class MavenITmng6118SubmoduleInvocation extends AbstractMavenIntegrationTestCase
{
    private static final String RESOURCE_PATH = "/mng-6118-submodule-invocation-full-reactor";
    private final File testDir;
    private final Map<String, String> envVars = new HashMap<>();

    public MavenITmng6118SubmoduleInvocation() throws IOException
    {
        super( "[3.7.0,)" );
        testDir = ResourceExtractor.simpleExtractResources( getClass(), RESOURCE_PATH );
        // It seems MAVEN_BASEDIR isn't always properly set, so make sure to have the right value here
        // as it is determined by the mvn script.
        envVars.put( "MAVEN_BASEDIR", testDir.getAbsolutePath() );
    }

    /**
     * Performs a <code>cd app && mvn compile</code> invocation. Verifies that inter-module dependencies are resolved.
     */
    public void testInSubModule() throws IOException, VerificationException
    {
        // Compile the whole project first.
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "compile", envVars );

        final File submoduleDirectory = new File( testDir, "app" );
        verifier = newVerifier( submoduleDirectory.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setLogFileName( "log-insubmodule.txt" );
        verifier.executeGoal( "compile", envVars );
    }

    /**
     * Performs a <code>mvn -f app/pom.xml compile</code> invocation. Verifies that inter-module dependencies are resolved.
     */
    public void testWithFile() throws IOException, VerificationException
    {
        // Compile the whole project first.
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "compile" );

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setLogFileName( "log-withfile.txt" );
        verifier.addCliOption( "-f app/pom.xml" );
        verifier.executeGoal( "compile", envVars );
    }

    /**
     * Performs a <code>mvn -f app/pom.xml -am compile</code> invocation. Verifies that dependent modules are also built.
     */
    public void testWithFileAndAlsoMake() throws IOException, VerificationException
    {
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliOption( "-am" );
        verifier.addCliOption( "-f app/pom.xml" );
        verifier.setLogFileName( "log-withfilealsomake.txt" );
        verifier.executeGoal( "compile", envVars );
        verifier.verifyTextInLog( "Building Maven Integration Test :: MNG-6118 :: Library 1.0" );
    }

    /**
     * Performs a <code>cd app && mvn compile -am</code> invocation. Verifies that dependent modules are also built.
     */
    public void testInSubModuleWithAlsoMake() throws IOException, VerificationException
    {
        File submoduleDirectory = new File( testDir, "app" );
        Verifier verifier = newVerifier( submoduleDirectory.getAbsolutePath() );
        verifier.addCliOption( "-am" );
        verifier.setLogFileName( "log-insubmodulealsomake.txt" );
        verifier.executeGoal( "compile", envVars );
        verifier.verifyTextInLog( "Building Maven Integration Test :: MNG-6118 :: Library 1.0" );
    }
}
