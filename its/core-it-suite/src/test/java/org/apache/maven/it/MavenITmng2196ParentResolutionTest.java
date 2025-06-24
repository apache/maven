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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2196">MNG-2196</a>.
 */
public class MavenITmng2196ParentResolutionTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng2196ParentResolutionTest() {
        super("[2.0,4.0.0-beta-5)");
    }

    /**
     * Verify that multi-module builds where one project references another as
     * a parent can build, even if that parent is not correctly referenced by
     * &lt;relativePath/&gt; and is not in the local repository. [MNG-2196]
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG2196() throws Exception {
        File testDir = extractResources("/mng-2196");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng2196");

        if (matchesVersionRange("(,3.0-alpha-1)")) {
            verifier.addCliArgument("validate");
            verifier.execute();
            verifier.verifyErrorFreeLog();
        } else {
            try {
                verifier.addCliArgument("validate");
                verifier.execute();
                verifier.verifyErrorFreeLog();
                fail("Build should have failed due to bad relativePath");
            } catch (VerificationException e) {
                // expected
            }
        }
    }
}
