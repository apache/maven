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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the {@code mvnval} POM validation tool.
 * <p>
 * Verifies that {@code mvn --val} reports a clean POM as such and reports the problems of a
 * broken one, and that the verdict does not depend on a build being possible: validation stops
 * after the raw model, so no parent is resolved and nothing is downloaded.
 * <p>
 * Uses {@code mvn --val} rather than the {@code mvnval} script directly. The {@code --val} flag
 * is handled by the {@code mvn} launcher, which points {@code MAVEN_MAIN_CLASS} at
 * {@code MavenValCling}.
 *
 * @since 4.1.0
 */
class MavenITgh10442MvnvalValidatePomTest extends AbstractMavenIntegrationTestCase {

    /**
     * A POM with nothing wrong with it should be reported as having no problems, and the tool
     * should exit successfully.
     */
    @Test
    void testReportsCleanPom() throws Exception {
        Path testDir = extractResources("mvnval-validate-pom");

        Verifier verifier = newVerifier(testDir.resolve("clean").toString());
        verifier.setForkJvm(true);
        verifier.setLogFileName("clean.txt");
        verifier.addCliArgument("--val");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("no problems");
    }

    /**
     * A POM with a duplicate dependency should make the tool exit with a non-zero code and name
     * the problem together with the line it sits on.
     */
    @Test
    void testReportsDuplicateDependency() throws Exception {
        Path testDir = extractResources("mvnval-validate-pom");

        Verifier verifier = newVerifier(testDir.resolve("duplicate-dependency").toString());
        verifier.setForkJvm(true);
        verifier.setLogFileName("duplicate.txt");
        verifier.addCliArgument("--val");

        assertThrows(
                VerificationException.class, verifier::execute, "Validation should have exited with a non-zero code");

        verifier.verifyTextInLog("must be unique");
        // Which line is not the point, and hard-coding it would tie the test to the length of the
        // license header above the dependency.
        assertTrue(
                verifier.loadLogLines().stream().anyMatch(l -> l.matches(".*must be unique.*line \\d+.*")),
                "the problem should be reported with the line it sits on");
    }

    /**
     * The JSON format should produce a single machine-readable document rather than log lines.
     */
    @Test
    void testReportsJson() throws Exception {
        Path testDir = extractResources("mvnval-validate-pom");

        Verifier verifier = newVerifier(testDir.resolve("clean").toString());
        verifier.setForkJvm(true);
        verifier.setLogFileName("json.txt");
        verifier.addCliArgument("--val");
        verifier.addCliArgument("--format");
        verifier.addCliArgument("json");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("\"problems\":[]");
    }
}
