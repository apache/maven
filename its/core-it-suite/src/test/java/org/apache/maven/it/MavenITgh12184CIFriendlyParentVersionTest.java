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
 * Integration test for <a href="https://github.com/apache/maven/issues/12184">#12184</a>.
 * Verifies that CI-friendly {@code ${revision}} in parent version works in Maven 4
 * native mode (without maven3Personality) for model version 4.0.0 projects.
 */
class MavenITgh12184CIFriendlyParentVersionTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that a multi-module project with {@code ${revision}} in parent version
     * builds successfully when the property is defined in the parent POM properties.
     */
    @Test
    void testCiFriendlyParentVersionFromProperties() throws Exception {
        Path testDir = extractResources("/gh-12184-ci-friendly-parent-version");

        Verifier verifier = newVerifier(testDir);
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    /**
     * Verify that a multi-module project with {@code ${revision}} in parent version
     * builds successfully when the property is overridden via command line.
     */
    @Test
    void testCiFriendlyParentVersionFromCli() throws Exception {
        Path testDir = extractResources("/gh-12184-ci-friendly-parent-version");

        Verifier verifier = newVerifier(testDir);
        verifier.addCliArgument("-Drevision=2.0.0");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
