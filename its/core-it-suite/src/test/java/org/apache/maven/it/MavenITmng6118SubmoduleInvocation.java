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
import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * This is a collection of test cases for <a href="https://issues.apache.org/jira/browse/MNG-6118">MNG-6118</a>,
 * invoking Maven in a submodule of a multi-module project.
 *
 * The test uses a multi-module project with two modules:
 * <ul>
 *     <li>app (depends on lib)</li>
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

    public MavenITmng6118SubmoduleInvocation() throws IOException
    {
        super( "[4.0.0-alpha-1,)" );
        testDir = ResourceExtractor.simpleExtractResources( getClass(), RESOURCE_PATH );
    }

    /**
     * Performs a {@code cd app && mvn compile} invocation. Verifies that inter-module dependencies are resolved.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testInSubModule() throws Exception
    {
        // Compile the whole project first.
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "compile" );

        final File submoduleDirectory = new File( testDir, "app" );
        verifier = newVerifier( submoduleDirectory.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setLogFileName( "log-insubmodule.txt" );
        verifier.executeGoal( "compile" );
    }

    /**
     * Performs a {@code mvn -f app/pom.xml compile} invocation. Verifies that inter-module dependencies are resolved.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testWithFile() throws Exception
    {
        // Compile the whole project first.
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.executeGoal( "compile" );

        verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.setLogFileName( "log-withfile.txt" );
        verifier.addCliOption( "-f" );
        verifier.addCliOption( "app/pom.xml" );
        verifier.executeGoal( "compile" );
    }

    /**
     * Performs a {@code mvn -f app/pom.xml -am compile} invocation. Verifies that dependent modules are also built.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testWithFileAndAlsoMake() throws Exception
    {
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliOption( "-am" );
        verifier.addCliOption( "-f" );
        verifier.addCliOption( "app/pom.xml" );
        verifier.setLogFileName( "log-withfilealsomake.txt" );
        verifier.executeGoal( "compile" );
        verifier.verifyTextInLog( "Building Maven Integration Test :: MNG-6118 :: Library 1.0" );
    }

    /**
     * Performs a {@code cd app && mvn compile -am} invocation. Verifies that dependent modules are also built.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testInSubModuleWithAlsoMake() throws Exception
    {
        File submoduleDirectory = new File( testDir, "app" );
        Verifier verifier = newVerifier( submoduleDirectory.getAbsolutePath() );
        verifier.addCliOption( "-am" );
        verifier.setLogFileName( "log-insubmodulealsomake.txt" );
        verifier.executeGoal( "compile" );
        verifier.verifyTextInLog( "Building Maven Integration Test :: MNG-6118 :: Library 1.0" );
    }
}
