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
 * IT to assert unresolved placeholders cause failure when used.
 * @since 4.0.0-rc-3
 *
 */
class MavenITgh11140RepoDmUnresolvedTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testFailsOnUnresolvedPlaceholders() throws Exception {
        File testDir = extractResources("/gh-11140-repo-dm-unresolved");
        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        try {
            verifier.addCliArgument("validate");
            verifier.execute();
        } catch (VerificationException expected) {
            // Expected to fail due to unresolved placeholders during model validation
        }
        // We expect error mentioning uninterpolated expression
        verifier.verifyTextInLog("contains an uninterpolated expression");
    }
}
