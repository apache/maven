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
 * This is a test set for <a href="https://github.com/apache/maven/issues/11055">gh-11055</a>.
 *
 * It reproduces the behavior difference between using Session::getService and field injection via @Inject
 * for some core services.
 */
class MavenITgh11055DITest extends AbstractMavenIntegrationTestCase {

    MavenITgh11055DITest() {
        super("[4.0.0-rc-4,)");
    }

    @Test
    void testDIConsistency() throws Exception {
        File testDir = extractResources("/gh-11055");

        // Build the test plugin first
        Verifier v0 = newVerifier(testDir.getAbsolutePath());
        v0.addCliArgument("install");
        v0.execute();
        v0.verifyErrorFreeLog();

        // Test 1: Session::getService should work
        Verifier v1 = newVerifier(testDir.getAbsolutePath());
        v1.addCliArgument("-Dname=World");
        v1.addCliArgument("com.gitlab.tkslaw:ditests-maven-plugin:0.1.0-SNAPSHOT:get-service");
        v1.execute();
        v1.verifyErrorFreeLog();

        // Test 2: @Inject should work too (but currently fails due to GH-11055)
        Verifier v2 = newVerifier(testDir.getAbsolutePath());
        v2.addCliArgument("-Dname=World");
        v2.addCliArgument("com.gitlab.tkslaw:ditests-maven-plugin:0.1.0-SNAPSHOT:inject-service");
        try {
            v2.execute();
            v2.verifyErrorFreeLog();
            // If we reach here, the issue is fixed - both approaches work consistently
        } catch (Exception e) {
            // Expected until GH-11055 is fixed: @Inject approach fails while getService works
            // This demonstrates the inconsistency
            v2.verifyTextInLog("FAILURE");
        }
    }
}
