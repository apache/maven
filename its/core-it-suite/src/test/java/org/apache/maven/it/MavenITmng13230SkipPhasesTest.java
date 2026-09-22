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

/**
 * Integration tests for the {@code --skip-phases} and {@code --skip-tests} CLI options
 * introduced by <a href="https://github.com/apache/maven/issues/13230">GH-13230</a>.
 *
 * @since 4.1.0
 */
class MavenITmng13230SkipPhasesTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that {@code --skip-phases=test} suppresses mojo executions bound to
     * the {@code test} phase (e.g. maven-surefire-plugin) while still running
     * the phases leading up to and after it.
     */
    @Test
    void skipPhasesSupressesMojosForSkippedPhase() throws Exception {
        Path basedir = extractResources("mng-13230");

        Verifier verifier = newVerifier(basedir);
        verifier.setLogFileName("log-skip-test.txt");
        verifier.addCliArguments("verify", "--skip-phases", "test");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // The test phase was skipped — surefire must not appear in the log
        verifier.verifyTextNotInLog("maven-surefire-plugin");
    }

    /**
     * Verify that {@code --skip-tests} (short: {@code -st}) expands to skipping
     * both the {@code test} and {@code integration-test} phases, so surefire
     * does not run.
     */
    @Test
    void skipTestsOptionSuppressesTestPhase() throws Exception {
        Path basedir = extractResources("mng-13230");

        Verifier verifier = newVerifier(basedir);
        verifier.setLogFileName("log-skip-tests.txt");
        verifier.addCliArguments("verify", "--skip-tests");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Both test and integration-test phases are skipped via --skip-tests
        verifier.verifyTextNotInLog("maven-surefire-plugin");
    }

    /**
     * Verify that without any skip option the {@code test} phase executes normally
     * and surefire appears in the build log.
     */
    @Test
    void noSkipOptionRunsTestPhase() throws Exception {
        Path basedir = extractResources("mng-13230");

        Verifier verifier = newVerifier(basedir);
        verifier.setLogFileName("log-no-skip.txt");
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Surefire is bound to the test phase and must execute
        verifier.verifyTextInLog("maven-surefire-plugin");
    }

    /**
     * Verify that {@code --skip-phases=test} combined with existing phases still
     * works when {@code --skip-phases} already contains {@code test} — idempotency
     * of the phase list must not cause issues.
     */
    @Test
    void skipTestsIsIdempotentWhenTestAlreadyInSkipPhases() throws Exception {
        Path basedir = extractResources("mng-13230");

        Verifier verifier = newVerifier(basedir);
        verifier.setLogFileName("log-idempotent.txt");
        // --skip-phases already lists test; --skip-tests must not duplicate it
        verifier.addCliArguments("verify", "--skip-phases", "test", "--skip-tests");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextNotInLog("maven-surefire-plugin");
    }
}
