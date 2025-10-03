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
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ITs for repository/distributionManagement URL interpolation.
 */
class MavenITRepoInterpolationTest extends AbstractMavenIntegrationTestCase {

    MavenITRepoInterpolationTest() {
        super("(4.0.0-rc-3,)");
    }

    @Test
    void testInterpolationFromEnvAndProps() throws Exception {
        File testDir = extractResources("/mng-repo-interpolation");
        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        // Provide env vars consumed by POM via ${env.*}
        Path base = testDir.toPath().toAbsolutePath();
        verifier.setEnvironmentVariable("IT_REPO_BASE", base.toUri().toString());
        verifier.setEnvironmentVariable("IT_DM_BASE", base.toUri().toString());

        // Use a cheap goal that prints effective POM
        verifier.addCliArgument("help:effective-pom");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> lines = verifier.loadLogLines();
        // Expect resolved file:// URLs, not placeholders
        assertTrue(lines.stream().anyMatch(s -> s.contains("<id>envRepo</id>")), "envRepo present");
        assertTrue(
                lines.stream().anyMatch(s -> s.contains("<url>" + base.toUri() + "repo</url>")),
                "envRepo url resolved");
        assertTrue(lines.stream().anyMatch(s -> s.contains("<id>propRepo</id>")), "propRepo present");
        assertTrue(
                lines.stream().anyMatch(s -> s.contains("<url>" + base.toUri() + "custom</url>")),
                "propRepo url resolved via property");
        assertTrue(lines.stream().anyMatch(s -> s.contains("<id>distRepo</id>")), "distRepo present");
        assertTrue(
                lines.stream().anyMatch(s -> s.contains("<url>" + base.toUri() + "dist</url>")),
                "distRepo url resolved");
    }

    @Test
    void testUnresolvedPlaceholderFailsResolution() throws Exception {
        File testDir = extractResources("/mng-repo-interpolation");
        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        // Do NOT set env vars, so placeholders stay
        verifier.addCliArgument("validate");
        try {
            verifier.execute();
        } catch (VerificationException expected) {
            // Expected to fail due to unresolved placeholders reaching repository construction
        }
        // We expect some error mentioning 'Not fully interpolated remote repository' or similar
        verifier.verifyTextInLog("Not fully interpolated remote repository");
    }
}
