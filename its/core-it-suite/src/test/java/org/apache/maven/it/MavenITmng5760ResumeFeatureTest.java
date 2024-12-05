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
package org.apache.maven.it;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * This is a collection of test cases for <a href="https://issues.apache.org/jira/browse/MNG-5760">MNG-5760</a>,
 * <code>--resume</code> / <code>-r</code> in case of build failures.
 *
 * The test uses a multi-module project with three modules:
 * <ul>
 *     <li>module-a</li>
 *     <li>module-b</li>
 *     <li>module-c (depends on module-b)</li>
 * </ul>
 *
 * @author Maarten Mulders
 * @author Martin Kanters
 */
public class MavenITmng5760ResumeFeatureTest extends AbstractMavenIntegrationTestCase {
    private final File parentDependentTestDir;
    private final File parentIndependentTestDir;
    private final File noProjectTestDir;
    private final File fourModulesTestDir;

    public MavenITmng5760ResumeFeatureTest() throws IOException {
        super("[4.0.0-alpha-1,)");
        this.parentDependentTestDir = extractResources("/mng-5760-resume-feature/parent-dependent");
        this.parentIndependentTestDir = extractResources("/mng-5760-resume-feature/parent-independent");
        this.noProjectTestDir = extractResources("/mng-5760-resume-feature/no-project");
        this.fourModulesTestDir = extractResources("/mng-5760-resume-feature/four-modules");
    }

    /**
     * Tests that the hint at the end of a failed build mentions <code>--resume</code> instead of <code>--resume-from</code>.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testShouldSuggestToResumeWithoutArgs() throws Exception {
        Verifier verifier = newVerifier(parentDependentTestDir.getAbsolutePath());
        verifier.addCliArgument("-Dmodule-b.fail=true");

        try {
            verifier.addCliArgument("test");
            verifier.execute();
            fail("Expected this invocation to fail");
        } catch (final VerificationException ve) {
            verifier.verifyTextInLog("mvn [args] -r");
            verifier.verifyTextNotInLog("mvn [args] -rf :module-b");
        }

        // New build with -r should resume the build from module-b, skipping module-a since it has succeeded already.
        verifier = newVerifier(parentDependentTestDir.getAbsolutePath());
        verifier.addCliArgument("-r");
        verifier.addCliArgument("test");
        verifier.execute();
        verifier.verifyTextNotInLog("Building module-a 1.0");
        verifier.verifyTextInLog("Building module-b 1.0");
        verifier.verifyTextInLog("Building module-c 1.0");
    }

    @Test
    public void testShouldSkipSuccessfulProjects() throws Exception {
        Verifier verifier = newVerifier(parentDependentTestDir.getAbsolutePath());
        verifier.addCliArgument("-Dmodule-a.fail=true");
        verifier.addCliArgument("--fail-at-end");

        try {
            verifier.addCliArgument("test");
            verifier.execute();
            fail("Expected this invocation to fail");
        } catch (final VerificationException ve) {
            // Expected to fail.
        }

        // Let module-b and module-c fail, if they would have been built...
        verifier = newVerifier(parentDependentTestDir.getAbsolutePath());
        verifier.addCliArgument("-Dmodule-b.fail=true");
        verifier.addCliArgument("-Dmodule-c.fail=true");
        // ... but adding -r should exclude those two from the build because the previous Maven invocation
        // marked them as successfully built.
        verifier.addCliArgument("-r");
        verifier.addCliArgument("test");
        verifier.execute();
    }

    @Test
    public void testShouldSkipSuccessfulModulesWhenTheFirstModuleFailed() throws Exception {
        // In this multi-module project, the submodules are not dependent on the parent.
        // This results in the parent to be built last, and module-a to be built first.
        // This enables us to let the first module in the reactor (module-a) fail.
        Verifier verifier = newVerifier(parentIndependentTestDir.getAbsolutePath());
        verifier.addCliArgument("-Dmodule-a.fail=true");
        verifier.addCliArgument("--fail-at-end");

        try {
            verifier.addCliArgument("test");
            verifier.execute();
            fail("Expected this invocation to fail");
        } catch (final VerificationException ve) {
            verifier.verifyTextInLog("mvn [args] -r");
        }

        verifier = newVerifier(parentIndependentTestDir.getAbsolutePath());
        verifier.addCliArgument("-r");
        verifier.addCliArgument("test");
        verifier.execute();
        verifier.verifyTextInLog("Building module-a 1.0");
        verifier.verifyTextNotInLog("Building module-b 1.0");
    }

    @Test
    public void testShouldNotCrashWithoutProject() throws Exception {
        // There is no Maven project available in the test directory.
        // As reported in JIRA this would previously break with a NullPointerException.
        // (see
        // https://issues.apache.org/jira/browse/MNG-5760?focusedCommentId=17143795&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-17143795)
        final Verifier verifier = newVerifier(noProjectTestDir.getAbsolutePath());
        try {
            verifier.addCliArgument("org.apache.maven.plugins:maven-resources-plugin:resources");
            verifier.execute();
        } catch (final VerificationException ve) {
            verifier.verifyTextInLog("Goal requires a project to execute but there is no POM in this directory");
        }
    }

    @Test
    public void testFailureWithParallelBuild() throws Exception {
        // four modules: a, b, c, d
        // c depends on b, d depends on a

        // Let's do a first pass with a and c failing.  The build is parallel,
        // so we have a first thread with a and d, and the second one with b and c
        // The result should be:
        //   a : failure (slow, so b and c will be built in the meantime)
        //   b : success
        //   c : failure
        //   d : skipped
        Verifier verifier = newVerifier(fourModulesTestDir.getAbsolutePath());
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("-Dmodule-a.delay=1000");
        verifier.addCliArgument("-Dmodule-a.fail=true");
        verifier.addCliArgument("-Dmodule-c.fail=true");
        try {
            verifier.addCliArgument("verify");
            verifier.execute();
            fail("Expected this invocation to fail");
        } catch (final VerificationException ve) {
            // Expected to fail.
        }

        // Let module-b fail, if it would have been built...
        verifier = newVerifier(fourModulesTestDir.getAbsolutePath());
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("-Dmodule-b.fail=true");
        // ... but adding -r should exclude it from the build because the previous Maven invocation
        // marked it as successfully built.
        verifier.addCliArgument("-r");
        // The result should be:
        //   a : success
        //   c : success
        //   d : success

        verifier.addCliArgument("verify");
        verifier.execute();
    }

    @Test
    public void testFailureAfterSkipWithParallelBuild() throws Exception {
        // four modules: a, b, c, d
        // c depends on b, d depends on a

        // Let's do a first pass with a and c failing.  The build is parallel,
        // so we have a first thread with a and d, and the second one with b and c
        // The result should be:
        //   a : success
        //   b : success, slow
        //   c : skipped
        //   d : failure
        Verifier verifier = newVerifier(fourModulesTestDir.getAbsolutePath());
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("-Dmodule-b.delay=2000");
        verifier.addCliArgument("-Dmodule-d.fail=true");
        try {
            verifier.addCliArgument("verify");
            verifier.execute();
            fail("Expected this invocation to fail");
        } catch (final VerificationException ve) {
            // Expected to fail.
        }

        // Let module-a and module-b fail, if they would have been built...
        verifier = newVerifier(fourModulesTestDir.getAbsolutePath());
        verifier.addCliArgument("-T2");
        verifier.addCliArgument("-Dmodule-a.fail=true");
        verifier.addCliArgument("-Dmodule-b.fail=true");
        // ... but adding -r should exclude those two from the build because the previous Maven invocation
        // marked them as successfully built.
        verifier.addCliArgument("-r");

        // The result should be:
        //   c : success
        //   d : success
        verifier.addCliArgument("verify");
        verifier.execute();
    }
}
