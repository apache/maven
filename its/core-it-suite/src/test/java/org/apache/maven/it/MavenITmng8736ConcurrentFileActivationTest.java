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
     * This test creates a parent with 32 child modules, where:
     * - 16 modules have the activation file (should activate the profile)
     * - 16 modules don't have the activation file (should not activate the profile)
     *
     * The test runs with multiple threads to verify thread safety.
     */
    @Test
    void testConcurrentFileActivation() throws Exception {
        File testDir = extractResources("/mng-8736");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("-T");
        verifier.addCliArgument("4");
        verifier.addCliArgument("-Dmaven.modelBuilder.parallelism=4"); // Use 4 threads for concurrent execution
        // verifier.addCliArgument("-X"); // Enable debug logging to see detailed traces
        verifier.addCliArgument("help:active-profiles");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Print detailed analysis before assertions
        analyzeProfileActivationResults(verifier);

        // Collect all failures before throwing any assertions
        StringBuilder failures = new StringBuilder();

        // Check all 32 modules
        for (int i = 1; i <= 32; i++) {
            String module = "child" + i;
            boolean shouldBeActivated = (i % 2 == 1); // Odd-numbered modules have activation files
            checkProfileActivation(verifier, module, shouldBeActivated, failures);
        }

        // If there are any failures, throw them all at once
        if (failures.length() > 0) {
            throw new AssertionError("Profile activation failures detected:\n" + failures.toString());
        }
    }

    private void checkProfileActivation(
            Verifier verifier, String module, boolean shouldBeActivated, StringBuilder failures) {
        try {
            String logContent = verifier.loadLogContent();
            String moduleSection = extractModuleSection(logContent, module);
            boolean isActivated = moduleSection.contains("file-activated");

            if (shouldBeActivated && !isActivated) {
                failures.append("- ")
                        .append(module)
                        .append(": Profile should be activated but was NOT (has activation file)\n");
            } else if (!shouldBeActivated && isActivated) {
                failures.append("- ")
                        .append(module)
                        .append(": Profile should NOT be activated but WAS (no activation file)\n");
            }
        } catch (Exception e) {
            failures.append("- ")
                    .append(module)
                    .append(": Exception during check: ")
                    .append(e.getMessage())
                    .append("\n");
        }
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

    private void analyzeProfileActivationResults(Verifier verifier) throws Exception {
        String logContent = verifier.loadLogContent();

        System.out.println("\n=== PROFILE ACTIVATION ANALYSIS ===");

        // Check file existence for all modules
        File testDir = new File(verifier.getBasedir());

        System.out.println("\nFile existence verification:");
        for (int i = 1; i <= 32; i++) {
            String module = "child" + i;
            File activationFile = new File(testDir, module + "/activate.marker");
            boolean shouldExist = (i % 2 == 1); // Odd-numbered modules should have activation files
            boolean exists = activationFile.exists();

            if (shouldExist) {
                System.out.println("  " + module + "/activate.marker: " + (exists ? "EXISTS" : "MISSING"));
            } else {
                System.out.println("  " + module + "/activate.marker: "
                        + (exists ? "EXISTS (unexpected!)" : "MISSING (expected)"));
            }
        }

        // Analyze profile activation results
        System.out.println("\nProfile activation results:");
        for (int i = 1; i <= 32; i++) {
            String module = "child" + i;
            String moduleSection = extractModuleSection(logContent, module);
            boolean activated = moduleSection.contains("file-activated");
            boolean shouldBeActivated = (i % 2 == 1); // Odd-numbered modules should have profile activated

            if (shouldBeActivated) {
                System.out.println(
                        "  " + module + ": " + (activated ? "ACTIVATED" : "NOT ACTIVATED") + (activated ? " ✓" : " ✗"));
            } else {
                System.out.println("  " + module + ": " + (activated ? "ACTIVATED (unexpected!)" : "NOT ACTIVATED")
                        + (activated ? " ✗" : " ✓"));
            }
        }

        // Look for thread-related information in debug logs
        System.out.println("\nThread information from debug logs:");
        String[] threadPatterns = {"Thread-", "pool-", "ForkJoinPool", "MultiThreadedBuilder"};

        for (String pattern : threadPatterns) {
            long count =
                    logContent.lines().filter(line -> line.contains(pattern)).count();
            if (count > 0) {
                System.out.println("  Lines containing '" + pattern + "': " + count);
            }
        }

        // Look for profile activation debug messages
        System.out.println("\nProfile activation debug traces:");
        logContent
                .lines()
                .filter(line -> line.contains("file-activated") && line.contains("DEBUG"))
                .limit(10)
                .forEach(line -> System.out.println("  " + line.trim()));

        System.out.println("=== END ANALYSIS ===\n");
    }
}
