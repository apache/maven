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
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for Maven 4 new dependency scopes: compile-only, test-only, and test-runtime.
 *
 * Verifies that:
 * - compile-only dependencies appear in compile classpath but not runtime classpath
 * - test-only dependencies appear in test compile classpath but not test runtime classpath
 * - test-runtime dependencies appear in test runtime classpath but not test compile classpath
 *
 * Each test method runs a small test project (under src/test/resources/mng-8750-new-scopes)
 * that writes out classpath files and asserts expected inclusion/exclusion behavior.
 *
 * This IT manually manages {@code .mvn} directories, so instructs Verifier to NOT create any.
 */
public class MavenITmng8750NewScopesTest extends AbstractMavenIntegrationTestCase {

    @BeforeEach
    void installDependencies() throws VerificationException, IOException {
        File testDir = extractResources("/mng-8750-new-scopes");

        File depsDir = new File(testDir, "deps");
        Verifier deps = newVerifier(depsDir.getAbsolutePath(), false);
        deps.addCliArgument("install");
        deps.addCliArgument("-Dmaven.consumer.pom.flatten=true");
        deps.execute();
        deps.verifyErrorFreeLog();
    }

    /**
     * compile-only: available during compilation only.
     * Behavior validated by the test project:
     * - Present on compile classpath
     * - Not present on runtime classpath
     * - Not transitive
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testCompileOnlyScope() throws Exception {
        File testDir = extractResources("/mng-8750-new-scopes");
        File projectDir = new File(testDir, "compile-only-test");

        Verifier verifier = newVerifier(projectDir.getAbsolutePath(), false);
        verifier.addCliArgument("clean");
        verifier.addCliArgument("test");
        verifier.addCliArgument("-Dmaven.consumer.pom.flatten=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify execution completed; detailed checks are within the test project
        verifier.verifyErrorFreeLog();

        // Verify classpath files were generated
        File compileClasspath = new File(projectDir, "target/compile-classpath.txt");
        File runtimeClasspath = new File(projectDir, "target/runtime-classpath.txt");

        assertTrue(compileClasspath.exists(), "Compile classpath file should exist");
        assertTrue(runtimeClasspath.exists(), "Runtime classpath file should exist");
    }

    /**
     * test-only: available during test compilation only.
     * Behavior validated by the test project:
     * - Present on test compile classpath
     * - Not present on test runtime classpath
     * - Not transitive
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testTestOnlyScope() throws Exception {
        File testDir = extractResources("/mng-8750-new-scopes");
        File projectDir = new File(testDir, "test-only-test");

        Verifier verifier = newVerifier(projectDir.getAbsolutePath(), false);
        verifier.addCliArgument("clean");
        verifier.addCliArgument("test");
        verifier.addCliArgument("-Dmaven.consumer.pom.flatten=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify execution completed; detailed checks are within the test project
        verifier.verifyErrorFreeLog();

        // Verify classpath files were generated
        File testCompileClasspath = new File(projectDir, "target/test-compile-classpath.txt");
        File testRuntimeClasspath = new File(projectDir, "target/test-runtime-classpath.txt");

        assertTrue(testCompileClasspath.exists(), "Test compile classpath file should exist");
        assertTrue(testRuntimeClasspath.exists(), "Test runtime classpath file should exist");
    }

    /**
     * test-runtime: available during test runtime only.
     * Behavior validated by the test project:
     * - Not present on test compile classpath
     * - Present on test runtime classpath
     * - Transitive
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testTestRuntimeScope() throws Exception {
        File testDir = extractResources("/mng-8750-new-scopes");
        File projectDir = new File(testDir, "test-runtime-test");

        Verifier verifier = newVerifier(projectDir.getAbsolutePath(), false);
        verifier.addCliArgument("clean");
        verifier.addCliArgument("test");
        verifier.addCliArgument("-Dmaven.consumer.pom.flatten=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify execution completed; detailed checks are within the test project
        verifier.verifyErrorFreeLog();

        // Verify classpath files were generated
        File testCompileClasspath = new File(projectDir, "target/test-compile-classpath.txt");
        File testRuntimeClasspath = new File(projectDir, "target/test-runtime-classpath.txt");

        assertTrue(testCompileClasspath.exists(), "Test compile classpath file should exist");
        assertTrue(testRuntimeClasspath.exists(), "Test runtime classpath file should exist");
    }

    /**
     * Comprehensive scenario exercising all new scopes together.
     * The test project asserts the expected inclusion/exclusion on all classpaths.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testAllNewScopesTogether() throws Exception {
        File testDir = extractResources("/mng-8750-new-scopes");
        File projectDir = new File(testDir, "comprehensive-test");

        Verifier verifier = newVerifier(projectDir.getAbsolutePath(), false);
        verifier.addCliArgument("clean");
        verifier.addCliArgument("test");
        verifier.addCliArgument("-Dmaven.consumer.pom.flatten=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify execution completed; detailed checks are within the test project
        verifier.verifyErrorFreeLog();

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
     * Validation rule: modelVersion 4.0.0 must reject new scopes (compile-only, test-only, test-runtime).
     * This test uses a POM with modelVersion 4.0.0 and new scopes and expects validation to fail.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testValidationFailureWithModelVersion40() throws Exception {
        File testDir = extractResources("/mng-8750-new-scopes");
        File projectDir = new File(testDir, "validation-failure-test");

        Verifier verifier = newVerifier(projectDir.getAbsolutePath(), false);
        verifier.addCliArgument("clean");
        verifier.addCliArgument("validate");
        verifier.addCliArgument("-Dmaven.consumer.pom.flatten=true");

        assertThrows(
                VerificationException.class,
                verifier::execute,
                "Expected validation to fail when using new scopes with modelVersion 4.0.0");
        String log = verifier.loadLogContent();
        assertTrue(
                log.contains("is not supported") || log.contains("Unknown scope"),
                "Error should indicate unsupported/unknown scope");
    }

    /**
     * Validation rule: modelVersion 4.1.0 (and later) accepts new scopes.
     * This test uses a POM with modelVersion 4.1.0 and new scopes and expects validation to succeed.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testValidationSuccessWithModelVersion41() throws Exception {
        File testDir = extractResources("/mng-8750-new-scopes");
        File projectDir = new File(testDir, "validation-success-test");

        Verifier verifier = newVerifier(projectDir.getAbsolutePath(), false);
        verifier.addCliArgument("clean");
        verifier.addCliArgument("validate");
        verifier.addCliArgument("-Dmaven.consumer.pom.flatten=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify that validation succeeded - no errors about unsupported scopes
        String log = verifier.loadLogContent();
        assertFalse(log.contains("is not supported"), "Validation should succeed with modelVersion 4.1.0");
        assertFalse(log.contains("Unknown scope"), "No unknown scope errors should occur with modelVersion 4.1.0");
    }
}
