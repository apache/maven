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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test for a Maven plugin that compiles against Maven 3 API
 * but detects and leverages the Maven 4 API at runtime via reflection.
 * <p>
 * Verifies:
 * <ol>
 *   <li>The plugin compiles successfully against Maven 3.9.x API when built by Maven 4</li>
 *   <li>The plugin detects Maven 4 at runtime via
 *       {@code Class.forName("org.apache.maven.api.Session")}</li>
 *   <li>The plugin extracts enhanced info from the Maven 4 {@code Session} via
 *       {@code MavenSession.getSession()} reflection bridge</li>
 *   <li>Maven 3 baseline output (GAV, dependencies) is always present</li>
 *   <li>Maven 4 enhanced output (maven version, root/top directory, start time,
 *       concurrency, reactor count) is present when running on Maven 4</li>
 * </ol>
 */
public class MavenITDualApiPluginTest extends AbstractMavenIntegrationTestCase {

    private Path testDir;

    @BeforeEach
    public void setUp() throws Exception {
        testDir = extractResources("mng-dual-api-plugin");
    }

    @Test
    public void testDualApiPluginDetectsMaven4() throws Exception {
        //
        // Step 1: Build and install the dual-API plugin.
        // It compiles against Maven 3.9.x API (provided scope),
        // which Maven 4's compat module satisfies at runtime.
        //
        Verifier v0 = newVerifier(testDir);
        v0.setAutoclean(false);
        v0.deleteDirectory("target");
        v0.deleteArtifacts("org.apache.maven.its.dualapi");
        v0.addCliArgument("install");
        v0.execute();
        v0.verifyErrorFreeLog();

        //
        // Step 2: Execute the plugin's project-info goal against the same project.
        // Since we're running on Maven 4, the plugin should:
        //   - detect Maven 4 runtime
        //   - emit [MVN3] baseline info
        //   - emit [MVN4] enhanced info from the new API
        //
        Verifier v1 = newVerifier(testDir);
        v1.setAutoclean(false);
        v1.addCliArgument(
                "org.apache.maven.its.dualapi:dual-api-maven-plugin:0.0.1-SNAPSHOT:project-info");
        v1.execute();
        v1.verifyErrorFreeLog();

        // ── Verify runtime detection ────────────────────────────────
        v1.verifyTextInLog("[RUNTIME] Maven 4");

        // ── Verify Maven 3 baseline output is present ───────────────
        v1.verifyTextInLog("[MVN3] groupId = org.apache.maven.its.dualapi");
        v1.verifyTextInLog("[MVN3] artifactId = dual-api-maven-plugin");
        v1.verifyTextInLog("[MVN3] version = 0.0.1-SNAPSHOT");
        v1.verifyTextInLog("[MVN3] packaging = maven-plugin");

        // ── Verify Maven 4 enhanced output is present ───────────────
        // These are extracted via reflection on org.apache.maven.api.Session
        v1.verifyTextInLog("[MVN4] maven.version = ");
        v1.verifyTextInLog("[MVN4] root.directory = ");
        v1.verifyTextInLog("[MVN4] top.directory = ");
        v1.verifyTextInLog("[MVN4] start.time = ");
        v1.verifyTextInLog("[MVN4] degree.of.concurrency = ");
        v1.verifyTextInLog("[MVN4] reactor.project.count = ");
    }
}
