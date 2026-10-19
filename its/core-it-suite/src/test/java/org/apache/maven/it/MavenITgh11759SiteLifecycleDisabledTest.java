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

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration test for <a href="https://github.com/apache/maven/issues/11759">gh-11759</a>:
 * verifies that setting {@code -Dmaven.site.lifecycle.disabled=true} via {@code MAVEN_OPTS}
 * removes the {@code site} lifecycle so that invoking the {@code site} phase fails with
 * "Unknown lifecycle phase".
 */
class MavenITgh11759SiteLifecycleDisabledTest extends AbstractMavenIntegrationTestCase {

    MavenITgh11759SiteLifecycleDisabledTest() {
        super("[4.0.0-rc-3,)");
    }

    /**
     * Verifies that with {@code maven.site.lifecycle.disabled=true} the site lifecycle
     * is absent and invoking the {@code site} phase causes a build failure.
     */
    @Test
    void testSiteLifecycleDisabledViaSystemProperty() throws Exception {
        File testDir = extractResources("/gh-11759-site-lifecycle-disabled");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.setLogFileName("log-disabled.txt");
        // Pass the flag as a JVM system property to the Maven process.
        // MAVEN_OPTS is the portable way to set JVM args for a forked Maven invocation.
        verifier.setEnvironmentVariable("MAVEN_OPTS", "-Dmaven.site.lifecycle.disabled=true");
        verifier.addCliArgument("site");

        assertThrows(
                VerificationException.class,
                verifier::execute,
                "Expected BUILD FAILURE when site lifecycle is disabled");

        verifier.verifyTextInLog("Unknown lifecycle phase");
    }
}
