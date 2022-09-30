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
import org.apache.maven.shared.verifier.VerificationException;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MavenITmng6566ExecuteAnnotationShouldNotReExecuteGoalsTest
    extends AbstractMavenIntegrationTestCase
{
    private static final String RESOURCE_PATH = "/mng-6566-execute-annotation-should-not-re-execute-goals";
    private static final String PLUGIN_KEY = "org.apache.maven.its.mng6566:plugin:1.0-SNAPSHOT";

    private File testDir;

    public MavenITmng6566ExecuteAnnotationShouldNotReExecuteGoalsTest()
    {
        super( "[4.0.0-alpha-1,)" );
    }

    @BeforeEach
    public void setUp()
            throws Exception
    {
        testDir = ResourceExtractor.simpleExtractResources( getClass(), RESOURCE_PATH );

        File pluginDir = new File( testDir, "plugin" );
        Verifier verifier = newVerifier( pluginDir.getAbsolutePath(), "remote" );
        verifier.executeGoal( "install" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testRunsCompileGoalOnceWithDirectPluginInvocation()
            throws Exception
    {
        File consumerDir = new File( testDir, "consumer" );

        Verifier verifier = newVerifier( consumerDir.getAbsolutePath() );
        verifier.setLogFileName( "log-direct-plugin-invocation.txt" );
        verifier.executeGoal( PLUGIN_KEY + ":require-compile-phase" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();

        assertCompiledOnce( verifier );
        verifier.verifyTextInLog( "MNG-6566 plugin require-compile-phase goal executed" );
    }

    /**
     * This test uses the <pre>require-compile-phase</pre> goal of the test plugin.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testRunsCompileGoalOnceWithPhaseExecution()
            throws Exception
    {
        File consumerDir = new File( testDir, "consumer" );

        Verifier verifier = newVerifier( consumerDir.getAbsolutePath() );
        verifier.setLogFileName( "log-phase-execution.txt" );
        verifier.executeGoal( "compile" );
        verifier.resetStreams();
        verifier.verifyErrorFreeLog();

        assertCompiledOnce( verifier );
        verifier.verifyTextInLog( "MNG-6566 plugin require-compile-phase goal executed" );
    }

    private void assertCompiledOnce( Verifier verifier )
            throws VerificationException
    {
        List<String> lines = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );
        int counter = 0;
        for ( String line : lines )
        {
            if ( line.contains( "maven-compiler-plugin:0.1-stub-SNAPSHOT:compile") )
            {
                counter++;
            }
        }
        assertEquals( "Compile goal was expected to run once", counter, 1 );
    }
}
