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

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenITgh13306PackagingProviderTest extends AbstractMavenIntegrationTestCase {

    @Test
    void providerFromProjectExtensionIsVisibleOnlyToItsProject() throws Exception {
        Path testDir = extractResources("gh-13306-packaging-provider");

        Verifier verifier = newVerifier(testDir.resolve("extension"));
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.gh13306");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(testDir.resolve("client"));
        verifier.setAutoclean(false);
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(testDir.resolve("reactor"));
        verifier.setAutoclean(false);
        verifier.addCliArgument("validate");
        try {
            verifier.execute();
            fail("The packaging provider leaked into a project without the extension");
        } catch (VerificationException e) {
            assertTrue(e.getMessage().contains("Unknown packaging: app-client"), e.getMessage());
            assertTrue(e.getMessage().contains("without-extension"), e.getMessage());
        }
    }
}
