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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test for MNG-5668:
 * Verifies that after:xxx phases are executed even when the build fails
 */
class MavenITmng5668AfterPhaseExecutionTest extends AbstractMavenIntegrationTestCase {

    MavenITmng5668AfterPhaseExecutionTest() {
        super("[4.0.0-rc-4,)"); // test is only relevant for Maven 4.0+
    }

    @Test
    void testAfterPhaseExecutionOnFailure() throws Exception {
        File testDir = extractResources("/mng-5668-after-phase-execution");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");

        try {
            verifier.addCliArgument("-b");
            verifier.addCliArgument("concurrent");
            verifier.addCliArgument("verify");
            verifier.execute();
            fail("Build should have failed");
        } catch (VerificationException e) {
            // expected
        }

        // Verify that marker files were created in the expected order
        verifier.verifyFilePresent("target/before-verify.txt");
        verifier.verifyFilePresent("target/verify-failed.txt");
        verifier.verifyFilePresent("target/after-verify.txt");

        // Verify the execution order through timestamps
        long beforeTime = new File(testDir, "target/before-verify.txt").lastModified();
        long failTime = new File(testDir, "target/verify-failed.txt").lastModified();
        long afterTime = new File(testDir, "target/after-verify.txt").lastModified();

        assertTrue(beforeTime <= failTime);
        assertTrue(failTime <= afterTime);
    }
}
