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

import org.junit.jupiter.api.Test;

/**
 * This is a test case for <a href="https://issues.apache.org/jira/browse/MNG-4660">MNG-4660</a>.
 *
 * @author Maarten Mulders
 * @author Martin Kanters
 */
public class MavenITmng4660ResumeFromTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng4660ResumeFromTest() {
        super("[4.0.0-alpha-1,)");
    }

    /**
     * Test that the --resume-from flag resolves dependencies inside the same Maven project
     * without having them installed first.
     * This test case uses the target/classes directory of module-a, for the situation where
     * module-a has not been packaged.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testShouldResolveOutputDirectoryFromEarlierBuild() throws Exception {
        final File testDir = extractResources("/mng-4660-resume-from");

        final Verifier verifier1 = newVerifier(testDir.getAbsolutePath());
        verifier1.deleteDirectory("target");
        verifier1.deleteArtifacts("org.apache.maven.its.mng4660");

        try {
            verifier1.addCliArgument("test"); // The test goal will not create a packaged artifact
            verifier1.execute();
            fail("Expected this invocation to fail"); // See TestCase.java
        } catch (final VerificationException ve) {
            verifier1.verifyTextInLog("Deliberately fail test case");
        }

        final Verifier verifier2 = newVerifier(testDir.getAbsolutePath());
        verifier2.setAutoclean(false);
        verifier2.addCliArgument("--resume-from");
        verifier2.addCliArgument(":module-b");
        verifier2.addCliArgument("compile");
        verifier2.execute(); // to prevent the unit test from failing (again)

        verifier2.verifyErrorFreeLog();
    }

    /**
     * Test that the --resume-from flag resolves dependencies inside the same Maven project
     * without having them installed first.
     * This test case uses the packaged artifact of module-a.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testShouldResolvePackagedArtifactFromEarlierBuild() throws Exception {
        final File testDir = extractResources("/mng-4660-resume-from");

        final Verifier verifier1 = newVerifier(testDir.getAbsolutePath());
        verifier1.deleteDirectory("target");
        verifier1.deleteArtifacts("org.apache.maven.its.mng4660");

        try {
            verifier1.addCliArgument("verify"); // The verify goal will create a packaged artifact
            verifier1.execute();
            fail("Expected this invocation to fail"); // See TestCase.java
        } catch (final VerificationException ve) {
            verifier1.verifyTextInLog("Deliberately fail test case");
        }

        final Verifier verifier2 = newVerifier(testDir.getAbsolutePath());
        verifier2.setAutoclean(false);
        verifier2.addCliArgument("--resume-from");
        verifier2.addCliArgument(":module-b");
        verifier2.addCliArgument("compile"); // to prevent the unit test from failing (again)
        verifier2.execute();

        verifier2.verifyErrorFreeLog();
    }
}
