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
 * @since 4.0.0-rc-3
 *
 */
class MavenITgh11140RepoInterpolationTest extends AbstractMavenIntegrationTestCase {

    @Test
    void testInterpolationFromEnvAndProps() throws Exception {
        File testDir = extractResources("/gh-11140-repo-interpolation");
        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        // Provide env vars consumed by POM via ${env.*}
        Path base = testDir.toPath().toAbsolutePath();
        String baseUri = getBaseUri(base);
        verifier.setEnvironmentVariable("IT_REPO_BASE", baseUri);
        verifier.setEnvironmentVariable("IT_DM_BASE", baseUri);

        // Use a cheap goal that prints effective POM
        verifier.addCliArgument("help:effective-pom");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> lines = verifier.loadLogLines();
        // Expect resolved file:// URLs, not placeholders
        assertTrue(lines.stream().anyMatch(s -> s.contains("<id>envRepo</id>")), "envRepo present");
        assertTrue(lines.stream().anyMatch(s -> s.contains("<url>" + baseUri + "/repo</url>")), "envRepo url resolved");
        assertTrue(lines.stream().anyMatch(s -> s.contains("<id>propRepo</id>")), "propRepo present");
        assertTrue(
                lines.stream().anyMatch(s -> s.contains("<url>" + baseUri + "/custom</url>")),
                "propRepo url resolved via property");
        assertTrue(lines.stream().anyMatch(s -> s.contains("<id>distRepo</id>")), "distRepo present");
        assertTrue(
                lines.stream().anyMatch(s -> s.contains("<url>" + baseUri + "/dist</url>")), "distRepo url resolved");
    }

    private static String getBaseUri(Path base) {
        String baseUri = base.toUri().toString();
        if (baseUri.endsWith("/")) {
            baseUri = baseUri.substring(0, baseUri.length() - 1);
        }
        return baseUri;
    }

    @Test
    void testUnresolvedPlaceholderFailsResolution() throws Exception {
        File testDir = extractResources("/gh-11140-repo-interpolation");
        Verifier verifier = newVerifier(testDir.getAbsolutePath());

        // Do NOT set env vars, so placeholders stay
        verifier.addCliArgument("validate");
        try {
            verifier.execute();
        } catch (VerificationException expected) {
            // Expected to fail due to unresolved placeholders during model validation
        }
        // We expect error mentioning uninterpolated expression
        verifier.verifyTextInLog("contains an uninterpolated expression");
    }
}
