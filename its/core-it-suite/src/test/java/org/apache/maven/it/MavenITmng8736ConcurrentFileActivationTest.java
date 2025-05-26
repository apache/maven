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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8736">MNG-8736</a>.
 *
 * Tests concurrent file-based profile activation in a multi-module build to ensure
 * that profiles are correctly activated/deactivated based on file existence in each module.
 */
class MavenITmng8736ConcurrentFileActivationTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8736ConcurrentFileActivationTest() {
        super("[4.0.0-alpha-1,)");
    }

    /**
     * Verify that file-based profile activation works correctly in concurrent builds.
     *
     * This test creates a parent with 8 child modules, where:
     * - 4 modules have the activation file (should activate the profile)
     * - 4 modules don't have the activation file (should not activate the profile)
     *
     * The test runs with multiple threads to verify thread safety.
     */
    @Test
    void testConcurrentFileActivation() throws Exception {
        File testDir = extractResources("/mng-8736");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("-T");
        verifier.addCliArgument("4"); // Use 4 threads for concurrent execution
        verifier.addCliArgument("help:active-profiles");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify that modules with activation file have the profile activated
        verifyProfileActivatedInLog(verifier, "child1");
        verifyProfileActivatedInLog(verifier, "child3");
        verifyProfileActivatedInLog(verifier, "child5");
        verifyProfileActivatedInLog(verifier, "child7");

        // Verify that modules without activation file don't have the profile activated
        verifyProfileNotActivatedInLog(verifier, "child2");
        verifyProfileNotActivatedInLog(verifier, "child4");
        verifyProfileNotActivatedInLog(verifier, "child6");
        verifyProfileNotActivatedInLog(verifier, "child8");
    }

    private void verifyProfileActivatedInLog(Verifier verifier, String module) throws Exception {
        // Check that the log contains evidence of profile activation for this module
        String logContent = verifier.loadLogContent();
        String moduleSection = extractModuleSection(logContent, module);
        if (!moduleSection.contains("file-activated")) {
            throw new AssertionError("Profile 'file-activated' was not activated in module " + module
                    + ". This indicates a concurrency issue with file-based profile activation."
                    + "\nModule section: " + moduleSection);
        }
    }

    private void verifyProfileNotActivatedInLog(Verifier verifier, String module) throws Exception {
        // Check that the log does not contain evidence of profile activation for this module
        String logContent = verifier.loadLogContent();
        String moduleSection = extractModuleSection(logContent, module);
        if (moduleSection.contains("file-activated")) {
            throw new AssertionError("Profile 'file-activated' was incorrectly activated in module " + module
                    + ". Module section: " + moduleSection);
        }
    }

    private String extractModuleSection(String logContent, String module) {
        // Extract the section of the log that corresponds to this module using the active profiles output
        String projectMarker =
                "Active Profiles for Project 'org.apache.maven.its.mng8736:" + module + ":jar:1.0-SNAPSHOT':";
        String nextProjectMarker = "Active Profiles for Project 'org.apache.maven.its.mng8736:";

        int startIndex = logContent.indexOf(projectMarker);
        if (startIndex == -1) {
            return "Module section not found for " + module;
        }

        int endIndex = logContent.indexOf(nextProjectMarker, startIndex + projectMarker.length());
        if (endIndex == -1) {
            // Look for other end markers
            endIndex = logContent.indexOf("[INFO] Copying org.apache.maven.its.mng8736", startIndex);
            if (endIndex == -1) {
                endIndex = logContent.length();
            }
        }

        return logContent.substring(startIndex, endIndex);
    }
}
