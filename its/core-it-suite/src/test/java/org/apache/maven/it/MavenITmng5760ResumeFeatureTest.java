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
import java.util.List;

/**
 * This is a collection of test cases for <a href="https://issues.apache.org/jira/browse/MNG-5760">MNG-5760</a>,
 * <code>--resume</code> / <code>-r</code> in case of build failures.
 *
 * The test uses a multi-module project with three modules:
 * <ul>
 *     <li>module-a</li>
 *     <li>module-b</li>
 *     <li>module-c</li> (depends on module-b)
 * </ul>
 *
 * @author Maarten Mulders
 * @author Martin Kanters
 */
public class MavenITmng5760ResumeFeatureTest extends AbstractMavenIntegrationTestCase {
    private final File testDir;

    public MavenITmng5760ResumeFeatureTest() throws IOException {
        super( "[3.7.0,)" );
        this.testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-5760-resume-feature" );
    }

    /**
     * Tests that the hint at the end of a failed build mentions <code>--resume</code> instead of <code>--resume-from</code>.
     */
    public void testShouldSuggestToResumeWithoutArgs() throws Exception
    {
        final Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliOption( "-Dmodule-b.fail=true" );

        try
        {
            verifier.executeGoal( "test" );
            fail( "Expected this invocation to fail" );
        }
        catch ( final VerificationException ve )
        {
            verifier.verifyTextInLog( "mvn <args> -r" );
            verifyTextNotInLog( verifier, "mvn <args> -rf :module-b" );
        }
        finally
        {
            verifier.resetStreams();
        }
    }

    public void testShouldSkipSuccessfulProjects() throws Exception
    {
        final Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.addCliOption( "-Dmodule-a.fail=true" );
        verifier.addCliOption( "--fail-at-end");

        try
        {
            verifier.executeGoal( "test" );
            fail( "Expected this invocation to fail" );
        }
        catch ( final VerificationException ve )
        {
            // Expected to fail.
        }
        finally
        {
            verifier.resetStreams();
        }

        verifier.getCliOptions().clear();

        // Let module-b and module-c fail, if they would have been built...
        verifier.addCliOption( "-Dmodule-b.fail=true" );
        verifier.addCliOption( "-Dmodule-c.fail=true" );
        // ... but adding -r should exclude those two from the build because the previous Maven invocation
        // marked them as successfully built.
        verifier.addCliOption( "-r" );
        try
        {
            verifier.executeGoal( "test" );
        }
        finally
        {
            verifier.resetStreams();
        }
    }

    /**
     * Throws an exception if the text <strong>is</strong> present in the log.
     *
     * @param verifier the verifier to use
     * @param text the text to assert present
     * @throws VerificationException if text is not found in log
     */
    private void verifyTextNotInLog( Verifier verifier, String text )
            throws VerificationException
    {
        List<String> lines = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );

        for ( String line : lines )
        {
            if ( Verifier.stripAnsi( line ).contains( text ) )
            {
                throw new VerificationException( "Text found in log: " + text );
            }
        }
    }
}
