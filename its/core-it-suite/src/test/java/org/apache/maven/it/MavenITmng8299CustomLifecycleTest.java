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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8299">MNG-8299</a>.
 */
class MavenITmng8299CustomLifecycleTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8299CustomLifecycleTest() {
        super("[2.0,4.0.0-alpha-13],[4.0.0-beta-5,)");
    }

    /**
     *  Verify that invoking the third phase will invoke the first two
     */
    @Test
    void testPhaseOrdering() throws Exception {
        File testDir = extractResources("/mng-8299-custom-lifecycle");

        Verifier verifier = newVerifier(new File(testDir, "CustomLifecyclePlugin").getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(new File(testDir, "CustomLifecycleProject").getAbsolutePath());
        verifier.addCliArgument("phase3");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[INFO] Doing Phase 1 stuff. Oh yeah baby.");
    }
}
