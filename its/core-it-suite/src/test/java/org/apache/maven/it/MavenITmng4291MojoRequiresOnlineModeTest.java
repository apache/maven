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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4291">MNG-4291</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4291MojoRequiresOnlineModeTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4291MojoRequiresOnlineModeTest() {
        super(ALL_MAVEN_VERSIONS);
    }

    /**
     * Test that the mojo annotation @requiresOnline is recognized. For a direct mojo invocation, this means to fail
     * when Maven is in offline mode but the mojo requires online model.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitDirectInvocation() throws Exception {
        File testDir = extractResources("/mng-4291");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.setLogFileName("log-direct.txt");
        verifier.addCliArgument("--offline");
        try {
            verifier.addCliArgument("org.apache.maven.its.plugins:maven-it-plugin-online:2.1-SNAPSHOT:touch");
            verifier.execute();
            verifier.verifyErrorFreeLog();
            fail("Request to execute online mojo during offline mode did not fail the build.");
        } catch (VerificationException e) {
            // expected
        }
    }

    /**
     * Test that the mojo annotation @requiresOnline is recognized. For a mojo invocation bound to a lifecycle phase,
     * this means to skip the mojo when Maven is in offline mode but the mojo requires online mode.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitLifecycleInvocation() throws Exception {
        File testDir = extractResources("/mng-4291");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.setLogFileName("log-lifecycle.txt");
        verifier.addCliArgument("--offline");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFileNotPresent("target/touch.txt");
    }
}
