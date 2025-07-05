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
 * This is a test set for <a href="https://github.com/apache/maven/issues/2576">GH-2576</a>.
 *
 * Verifies that the -itr (ignore transitive repositories) option works correctly.
 */
public class MavenITgh2576IgnoreTransitiveRepositoriesTest extends AbstractMavenIntegrationTestCase {

    public MavenITgh2576IgnoreTransitiveRepositoriesTest() {
        super("[4.0.0-alpha-1,)");
    }

    /**
     * Verify that the -itr option ignores repositories introduced by transitive dependencies.
     * Without -itr, the build should succeed because the transitive dependency can be found
     * in the repository declared by the direct dependency.
     * With -itr, the build should fail because the transitive repository is ignored.
     *
     * @throws Exception in case of failure
     */
    @Test
    void testIgnoreTransitiveRepositories() throws Exception {
        File testDir = extractResources("/gh-2576-ignore-transitive-repositories");

        // First test without -itr - should succeed
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.gh2576");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Now test with -itr - should fail because transitive repo is ignored
        Verifier verifierWithItr = newVerifier(testDir.getAbsolutePath());
        verifierWithItr.setAutoclean(false);
        verifierWithItr.deleteDirectory("target");
        verifierWithItr.deleteArtifacts("org.apache.maven.its.gh2576");
        verifierWithItr.filterFile("settings-template.xml", "settings.xml");
        verifierWithItr.addCliArgument("--settings");
        verifierWithItr.addCliArgument("settings.xml");
        verifierWithItr.addCliArgument("-itr");
        verifierWithItr.addCliArgument("validate");
        try {
            verifierWithItr.execute();
            // If we get here, the test failed because it should have thrown an exception
            throw new AssertionError("Build should have failed with -itr option");
        } catch (VerificationException e) {
            // Expected - the build should fail when transitive repositories are ignored
            verifierWithItr.verifyTextInLog("Could not resolve dependencies");
        }
    }
}
