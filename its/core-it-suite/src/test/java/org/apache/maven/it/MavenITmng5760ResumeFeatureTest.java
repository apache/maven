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
    private final File parentDependentTestDir;
    private final File parentIndependentTestDir;
    private final File noProjectTestDir;

    public MavenITmng5760ResumeFeatureTest() throws IOException {
        super( "[4.0.0-alpha-1,)" );
        this.parentDependentTestDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/mng-5760-resume-feature/parent-dependent" );
        this.parentIndependentTestDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/mng-5760-resume-feature/parent-independent" );
        this.noProjectTestDir = ResourceExtractor.simpleExtractResources( getClass(),
                "/mng-5760-resume-feature/no-project" );
    }

    /**
     * Tests that the hint at the end of a failed build mentions <code>--resume</code> instead of <code>--resume-from</code>.
     */
    public void testShouldSuggestToResumeWithoutArgs() throws Exception
    {
        Verifier verifier = newVerifier( parentDependentTestDir.getAbsolutePath() );
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

        // New build with -r should resume the build from module-b, skipping module-a since it has succeeded already.
        verifier = newVerifier( parentDependentTestDir.getAbsolutePath() );
        verifier.addCliOption( "-r" );
        verifier.executeGoal( "test" );
        verifyTextNotInLog( verifier, "Building module-a 1.0" );
        verifier.verifyTextInLog( "Building module-b 1.0" );
        verifier.verifyTextInLog( "Building module-c 1.0" );
        verifier.resetStreams();
    }

    public void testShouldSkipSuccessfulProjects() throws Exception
    {
        Verifier verifier = newVerifier( parentDependentTestDir.getAbsolutePath() );
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

        // Let module-b and module-c fail, if they would have been built...
        verifier = newVerifier( parentDependentTestDir.getAbsolutePath() );
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

    public void testShouldSkipSuccessfulModulesWhenTheFirstModuleFailed() throws Exception
    {
        // In this multi-module project, the submodules are not dependent on the parent.
        // This results in the parent to be built last, and module-a to be built first.
        // This enables us to let the first module in the reactor (module-a) fail.
        Verifier verifier = newVerifier( parentIndependentTestDir.getAbsolutePath() );
        verifier.addCliOption( "-Dmodule-a.fail=true" );
        verifier.addCliOption( "--fail-at-end");

        try
        {
            verifier.executeGoal( "test" );
            fail( "Expected this invocation to fail" );
        }
        catch ( final VerificationException ve )
        {
            verifier.verifyTextInLog( "mvn <args> -r" );
        }
        finally
        {
            verifier.resetStreams();
        }

        verifier = newVerifier( parentIndependentTestDir.getAbsolutePath() );
        verifier.addCliOption( "-r" );
        verifier.executeGoal( "test" );
        verifier.verifyTextInLog( "Building module-a 1.0" );
        verifyTextNotInLog( verifier, "Building module-b 1.0" );
        verifier.resetStreams();
    }

    public void testShouldNotCrashWithoutProject() throws Exception
    {
        // There is no Maven project available in the test directory.
        // As reported in JIRA this would previously break with a NullPointerException.
        // (see https://issues.apache.org/jira/browse/MNG-5760?focusedCommentId=17143795&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-17143795)
        final Verifier verifier = newVerifier( noProjectTestDir.getAbsolutePath() );
        try
        {
            verifier.executeGoal( "resources:resources" );
        }
        catch ( final VerificationException ve )
        {
            verifier.verifyTextInLog( "Goal requires a project to execute but there is no POM in this directory" );
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
