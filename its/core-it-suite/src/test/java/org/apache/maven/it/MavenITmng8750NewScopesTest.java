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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.shared.verifier.VerificationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * This is a test set for Maven 4 new dependency scopes: compile-only, test-only, and test-runtime.
 *
 * Verifies that:
 * - compile-only dependencies appear in compile classpath but not runtime classpath
 * - test-only dependencies appear in test compile classpath but not test runtime classpath
 * - test-runtime dependencies appear in test runtime classpath but not test compile classpath
 * - Consumer POMs exclude these new scopes for Maven 3 compatibility
 */
public class MavenITmng8750NewScopesTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng8750NewScopesTest() {
        super("[4.0.0,)");
    }

    /**
     * Test that compile-only dependencies work correctly.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testCompileOnlyScope() throws Exception {
        File testDir = extractResources("/mng-8750-new-scopes");
        File projectDir = new File(testDir, "compile-only-test");

        Verifier verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("clean");
        verifier.addCliArgument("test");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify that the test passes, which means compile-only scope works correctly
        // The test itself verifies that compile-only dependencies are not available at runtime
        verifier.verifyTextInLog("Runtime classpath verification: PASSED");

        // Verify classpath files were generated
        File compileClasspath = new File(projectDir, "target/compile-classpath.txt");
        File runtimeClasspath = new File(projectDir, "target/runtime-classpath.txt");

        assertTrue(compileClasspath.exists(), "Compile classpath file should exist");
        assertTrue(runtimeClasspath.exists(), "Runtime classpath file should exist");
    }

    /**
     * Test that test-only dependencies work correctly.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testTestOnlyScope() throws Exception {
        File testDir = extractResources("/mng-8750-new-scopes");
        File projectDir = new File(testDir, "test-only-test");

        Verifier verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("clean");
        verifier.addCliArgument("test");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify that the test passes, which means test-only scope works correctly
        // The test itself verifies that test-only dependencies are not available at test runtime
        verifier.verifyTextInLog("Test runtime classpath verification: PASSED");

        // Verify classpath files were generated
        File testCompileClasspath = new File(projectDir, "target/test-compile-classpath.txt");
        File testRuntimeClasspath = new File(projectDir, "target/test-runtime-classpath.txt");

        assertTrue(testCompileClasspath.exists(), "Test compile classpath file should exist");
        assertTrue(testRuntimeClasspath.exists(), "Test runtime classpath file should exist");
    }

    /**
     * Test that test-runtime dependencies work correctly.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testTestRuntimeScope() throws Exception {
        File testDir = extractResources("/mng-8750-new-scopes");
        File projectDir = new File(testDir, "test-runtime-test");

        Verifier verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("clean");
        verifier.addCliArgument("test");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify that the test passes, which means test-runtime scope works correctly
        // The test itself verifies that test-runtime dependencies are available at test runtime
        verifier.verifyTextInLog("Test runtime classpath verification: PASSED");

        // Verify classpath files were generated
        File testCompileClasspath = new File(projectDir, "target/test-compile-classpath.txt");
        File testRuntimeClasspath = new File(projectDir, "target/test-runtime-classpath.txt");

        assertTrue(testCompileClasspath.exists(), "Test compile classpath file should exist");
        assertTrue(testRuntimeClasspath.exists(), "Test runtime classpath file should exist");
    }

    /**
     * Test that consumer POMs exclude new scopes for Maven 3 compatibility.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testConsumerPomExcludesNewScopes() throws Exception {
        File testDir = extractResources("/mng-8750-new-scopes");
        File projectDir = new File(testDir, "consumer-pom-test");

        Verifier verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("clean");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Check that consumer POM was generated
        Path consumerPom = projectDir
                .toPath()
                .resolve(Paths.get(
                        "target",
                        "project-local-repo",
                        "org.apache.maven.its.mng8750",
                        "consumer-pom-test",
                        "1.0",
                        "consumer-pom-test-1.0-consumer.pom"));

        assertTrue(Files.exists(consumerPom), "Consumer POM should exist");

        // Verify consumer POM content excludes new scopes
        String consumerPomContent = Files.readString(consumerPom);
        assertFalse(consumerPomContent.contains("compile-only"), "Consumer POM should not contain compile-only scope");
        assertFalse(consumerPomContent.contains("test-only"), "Consumer POM should not contain test-only scope");
        assertFalse(consumerPomContent.contains("test-runtime"), "Consumer POM should not contain test-runtime scope");

        // Verify that dependencies with new scopes are either excluded or transformed
        assertTrue(
                consumerPomContent.contains("compile") || consumerPomContent.contains("provided"),
                "Consumer POM should contain only Maven 3 compatible scopes");
    }

    /**
     * Test all new scopes together in a comprehensive scenario.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testAllNewScopesTogether() throws Exception {
        File testDir = extractResources("/mng-8750-new-scopes");
        File projectDir = new File(testDir, "comprehensive-test");

        Verifier verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("clean");
        verifier.addCliArgument("test");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify all scope behaviors work correctly together
        verifier.verifyTextInLog("Compile-only scope verification: PASSED");
        verifier.verifyTextInLog("Test-only scope verification: PASSED");
        verifier.verifyTextInLog("Test-runtime scope verification: PASSED");
        verifier.verifyTextInLog("All scope verifications: PASSED");

        // Verify all classpath files were generated
        File compileClasspath = new File(projectDir, "target/compile-classpath.txt");
        File runtimeClasspath = new File(projectDir, "target/runtime-classpath.txt");
        File testCompileClasspath = new File(projectDir, "target/test-compile-classpath.txt");
        File testRuntimeClasspath = new File(projectDir, "target/test-runtime-classpath.txt");

        assertTrue(compileClasspath.exists(), "Compile classpath file should exist");
        assertTrue(runtimeClasspath.exists(), "Runtime classpath file should exist");
        assertTrue(testCompileClasspath.exists(), "Test compile classpath file should exist");
        assertTrue(testRuntimeClasspath.exists(), "Test runtime classpath file should exist");
    }

    /**
     * Test that new scopes fail validation when using modelVersion 4.0.0.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testValidationFailureWithModelVersion40() throws Exception {
        File testDir = extractResources("/mng-8750-new-scopes");
        File projectDir = new File(testDir, "validation-failure-test");

        Verifier verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("clean");
        verifier.addCliArgument("validate");

        try {
            verifier.execute();
            fail("Expected validation to fail when using new scopes with modelVersion 4.0.0");
        } catch (VerificationException e) {
            // Expected - validation should fail
            verifier.verifyTextInLog("is not supported");

            // Verify that the error mentions the unsupported scopes
            String log = verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false);
            assertTrue(
                    log.contains("compile-only") || log.contains("scope"), "Error should mention compile-only scope");
            assertTrue(log.contains("modelVersion") || log.contains("4.0.0"), "Error should mention model version");
        }
    }

    /**
     * Test that new scopes work correctly when using modelVersion 4.1.0.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testValidationSuccessWithModelVersion41() throws Exception {
        File testDir = extractResources("/mng-8750-new-scopes");
        File projectDir = new File(testDir, "validation-success-test");

        Verifier verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("clean");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify that validation succeeded - no errors about unsupported scopes
        String log = verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false);
        assertFalse(log.contains("is not supported"), "Validation should succeed with modelVersion 4.1.0");
        assertFalse(log.contains("Unknown scope"), "No unknown scope errors should occur with modelVersion 4.1.0");
    }
}
