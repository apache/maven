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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2339">MNG-2339</a>.
 * @since 2.0.8
 *
 */
public class MavenITmng2339BadProjectInterpolationTest extends AbstractMavenIntegrationTestCase {

    @Test
    public void testitMNG2339a() throws Exception {
        Path testDir = extractResourcesAsPath("/mng-2339/a");

        Verifier verifier;

        verifier = newVerifier(testDir.toString());
        verifier.setAutoclean(false);

        verifier.addCliArgument("-Dversion=foo");
        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyErrorFreeLog();
    }

    // test that -Dversion=1.0 is still available for interpolation.
    @Test
    @Disabled("Requires Maven version: (2.0.8,4.0.0-alpha-1)")
    public void testitMNG2339b() throws Exception {
        // requiresMavenVersion("(2.0.8,4.0.0-alpha-1)");
        Path testDir = extractResourcesAsPath("/mng-2339/b");

        Verifier verifier;

        verifier = newVerifier(testDir.toString());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");

        verifier.setLogFileName("log-pom-specified.txt");
        verifier.addCliArgument("initialize");
        verifier.execute();

        assertTrue(
                testDir.resolve("target/touch-1.txt").exists(),
                "Touchfile using ${project.version} for ${version} does not exist.");

        verifier.verifyErrorFreeLog();

        verifier = newVerifier(testDir.toString());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");

        verifier.addCliArgument("-Dversion=2");
        verifier.setLogFileName("log-cli-specified.txt");
        verifier.addCliArgument("initialize");
        verifier.execute();

        verifier.verifyErrorFreeLog();

        assertTrue(
                testDir.resolve("target/touch-2.txt").exists(),
                "Touchfile using CLI-specified ${version} does not exist.");
    }
}
