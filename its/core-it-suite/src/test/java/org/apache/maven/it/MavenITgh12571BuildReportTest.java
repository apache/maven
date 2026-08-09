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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Build Report Foundation feature.
 * <p>
 * Covers: build report JSON generation, console modes (plain, machine, verbose),
 * warning mode, version info on failure, and the {@code mvnlog} viewer tool.
 *
 * @see <a href="https://github.com/apache/maven/issues/12571">gh-12571</a>
 * @since 4.1.0
 */
class MavenITgh12571BuildReportTest extends AbstractMavenIntegrationTestCase {

    // -------------------------------------------------------------------------
    // Build report JSON generation
    // -------------------------------------------------------------------------

    /**
     * Verify that a successful single-module build produces a JSON report file
     * containing the expected top-level fields.
     */
    @Test
    void testBuildReportJsonGenerated() throws Exception {
        Path basedir = extractResources("gh-12571-build-report");

        Verifier verifier = newVerifier(basedir);
        verifier.setLogFileName("report-gen.txt");
        verifier.addCliArgument("--console=verbose");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Build report file must exist
        Path reportFile = basedir.resolve("target/build-reports/build-report-latest.json");
        verifier.verifyFilePresent(reportFile);

        // Verify JSON structure
        String json = Files.readString(reportFile);
        assertTrue(json.contains("\"formatVersion\""), "Should contain formatVersion");
        assertTrue(json.contains("\"status\""), "Should contain status");
        assertTrue(json.contains("\"SUCCESS\""), "Status should be SUCCESS");
        assertTrue(json.contains("\"duration\""), "Should contain duration");
        assertTrue(json.contains("\"mavenVersion\""), "Should contain mavenVersion");
        assertTrue(json.contains("\"javaVersion\""), "Should contain javaVersion");
        assertTrue(json.contains("\"modules\""), "Should contain modules array");
        assertTrue(json.contains("\"problems\""), "Should contain problems array");
        assertTrue(json.contains("\"failures\""), "Should contain failures array");
        assertTrue(json.contains("\"build-report-test\""), "Should contain artifactId");
    }

    /**
     * Verify that a multi-module build produces a JSON report with all modules listed.
     */
    @Test
    void testBuildReportMultiModule() throws Exception {
        Path basedir = extractResources("gh-12571-multi-module");

        Verifier verifier = newVerifier(basedir);
        verifier.setLogFileName("multi-module.txt");
        verifier.addCliArgument("--console=verbose");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path reportFile = basedir.resolve("target/build-reports/build-report-latest.json");
        verifier.verifyFilePresent(reportFile);

        String json = Files.readString(reportFile);
        assertTrue(json.contains("\"module-a\""), "Should contain module-a");
        assertTrue(json.contains("\"module-b\""), "Should contain module-b");
        assertTrue(json.contains("\"multi-module-parent\""), "Should contain parent");
        assertTrue(json.contains("\"multiModule\""), "Should contain multiModule flag");
    }

    // -------------------------------------------------------------------------
    // Console modes
    // -------------------------------------------------------------------------

    /**
     * Verify that {@code --console=plain} produces compact output without
     * the full mojo-level detail that verbose mode shows.
     */
    @Test
    void testConsolePlainMode() throws Exception {
        Path basedir = extractResources("gh-12571-multi-module");

        Verifier verifier = newVerifier(basedir);
        verifier.setLogFileName("plain.txt");
        verifier.addCliArgument("--console=plain");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Plain mode should still show BUILD SUCCESS and Total time
        verifier.verifyTextInLog("BUILD SUCCESS");
        verifier.verifyTextInLog("Total time:");

        // Plain mode should NOT show the verbose "--- plugin:goal" lines
        List<String> lines = verifier.loadLines("plain.txt");
        boolean hasPluginLine = lines.stream().anyMatch(l -> l.matches(".*---.*:.*---.*"));
        assertFalse(hasPluginLine, "Plain mode should not contain verbose mojo execution lines");
    }

    /**
     * Verify that {@code --console=machine} produces JSON lines output
     * with typed events.
     */
    @Test
    void testConsoleMachineMode() throws Exception {
        Path basedir = extractResources("gh-12571-build-report");

        Verifier verifier = newVerifier(basedir);
        verifier.setLogFileName("machine.txt");
        verifier.addCliArgument("--console=machine");
        verifier.addCliArgument("validate");
        verifier.execute();

        List<String> lines = verifier.loadLines("machine.txt");

        // Machine mode should produce JSON lines with "event" fields
        boolean hasBuildStarted = lines.stream().anyMatch(l -> l.contains("\"event\":\"build.started\""));
        boolean hasBuildFinished = lines.stream().anyMatch(l -> l.contains("\"event\":\"build.finished\""));
        boolean hasModuleStarted = lines.stream().anyMatch(l -> l.contains("\"event\":\"module.started\""));
        boolean hasTimestamp = lines.stream().anyMatch(l -> l.contains("\"timestamp\""));

        assertTrue(hasBuildStarted, "Should contain build.started event");
        assertTrue(hasBuildFinished, "Should contain build.finished event");
        assertTrue(hasModuleStarted, "Should contain module.started event");
        assertTrue(hasTimestamp, "Events should contain timestamps");

        // build.finished should report SUCCESS
        boolean hasSuccess =
                lines.stream().anyMatch(l -> l.contains("\"event\":\"build.finished\"") && l.contains("\"SUCCESS\""));
        assertTrue(hasSuccess, "build.finished should report SUCCESS");
    }

    /**
     * Verify that {@code --console=verbose} (the classic Maven output) includes
     * the standard banner lines and mojo execution details.
     */
    @Test
    void testConsoleVerboseMode() throws Exception {
        Path basedir = extractResources("gh-12571-build-report");

        Verifier verifier = newVerifier(basedir);
        verifier.setLogFileName("verbose.txt");
        verifier.addCliArgument("--console=verbose");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("BUILD SUCCESS");
        verifier.verifyTextInLog("Total time:");
        // Verbose mode shows the horizontal rule separator
        verifier.verifyTextInLog("------------------------------------------------------------------------");
    }

    /**
     * Verify that when the {@code CI} environment variable is set,
     * {@code --console=auto} resolves to plain mode (no verbose mojo lines).
     */
    @Test
    void testConsoleAutoDetectsCi() throws Exception {
        Path basedir = extractResources("gh-12571-multi-module");

        Verifier verifier = newVerifier(basedir);
        verifier.setLogFileName("auto-ci.txt");
        verifier.setEnvironmentVariable("CI", "true");
        verifier.addCliArgument("--console=auto");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("BUILD SUCCESS");

        // In CI mode (plain), should NOT show verbose mojo execution lines
        List<String> lines = verifier.loadLines("auto-ci.txt");
        boolean hasPluginLine = lines.stream().anyMatch(l -> l.matches(".*---.*:.*---.*"));
        assertFalse(hasPluginLine, "Auto mode with CI=true should not produce verbose mojo lines");
    }

    // -------------------------------------------------------------------------
    // Warning mode
    // -------------------------------------------------------------------------

    /**
     * Verify that {@code --warning-mode=none} suppresses the diagnostic summary
     * at the end of the build.
     */
    @Test
    void testWarningModeNone() throws Exception {
        Path basedir = extractResources("gh-12571-build-report");

        Verifier verifier = newVerifier(basedir);
        verifier.setLogFileName("warn-none.txt");
        verifier.addCliArgument("--console=verbose");
        verifier.addCliArgument("--warning-mode=none");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // With --warning-mode=none, the diagnostic summary line should not appear
        verifier.verifyTextNotInLog("Diagnostics:");
    }

    // -------------------------------------------------------------------------
    // Version info on failure (MNG-7372)
    // -------------------------------------------------------------------------

    /**
     * Verify that on BUILD FAILURE the Maven version and Java version
     * are printed in the summary output.
     */
    @Test
    void testVersionInfoOnFailure() throws Exception {
        Path basedir = extractResources("gh-12571-build-report");

        Verifier verifier = newVerifier(basedir);
        verifier.setLogFileName("fail-version.txt");
        verifier.addCliArgument("--console=verbose");
        // Invoke a non-existent goal to trigger a failure
        verifier.addCliArgument("org.apache.maven.plugins:non-existent-plugin:1.0:goal");

        boolean failed = false;
        try {
            verifier.execute();
        } catch (VerificationException e) {
            failed = true;
        }

        assertTrue(failed, "Build should have failed");
        verifier.verifyTextInLog("BUILD FAILURE");
        // The version info line should be present (e.g. "Maven 4.1.0-SNAPSHOT  |  Java 21.0.x")
        verifier.verifyTextInLog("Maven");
        verifier.verifyTextInLog("Java");
    }

    // -------------------------------------------------------------------------
    // mvnlog viewer
    // -------------------------------------------------------------------------

    /**
     * Verify that {@code mvnlog} displays a summary of the last build report
     * after a successful build.
     */
    @Test
    void testMvnlogShowsSummary() throws Exception {
        Path basedir = extractResources("gh-12571-build-report");

        // Step 1: run a build to generate the report
        Verifier buildVerifier = newVerifier(basedir);
        buildVerifier.setLogFileName("build-for-mvnlog.txt");
        buildVerifier.addCliArgument("--console=verbose");
        buildVerifier.addCliArgument("validate");
        buildVerifier.execute();
        buildVerifier.verifyErrorFreeLog();

        // Verify the report was generated
        Path reportFile = basedir.resolve("target/build-reports/build-report-latest.json");
        buildVerifier.verifyFilePresent(reportFile);

        // Step 2: run mvnlog to view the report (forked: embedded executor does not know mvnlog)
        Verifier logVerifier = newVerifier(basedir);
        logVerifier.setLogFileName("mvnlog-summary.txt");
        logVerifier.setForkJvm(true);
        logVerifier.setExecutable("mvnlog");
        logVerifier.execute();

        // mvnlog should display report content
        logVerifier.verifyTextInLog("Build Report");
        logVerifier.verifyTextInLog("SUCCESS");
    }

    /**
     * Verify that {@code mvnlog --full} shows per-module detail.
     */
    @Test
    void testMvnlogFullView() throws Exception {
        Path basedir = extractResources("gh-12571-multi-module");

        // Step 1: run a build to generate the report
        Verifier buildVerifier = newVerifier(basedir);
        buildVerifier.setLogFileName("build-for-full.txt");
        buildVerifier.addCliArgument("--console=verbose");
        buildVerifier.addCliArgument("validate");
        buildVerifier.execute();
        buildVerifier.verifyErrorFreeLog();

        // Step 2: run mvnlog --full (forked: embedded executor does not know mvnlog)
        Verifier logVerifier = newVerifier(basedir);
        logVerifier.setLogFileName("mvnlog-full.txt");
        logVerifier.setForkJvm(true);
        logVerifier.setExecutable("mvnlog");
        logVerifier.addCliArgument("--full");
        logVerifier.execute();

        // Full view should show individual module details
        logVerifier.verifyTextInLog("module-a");
        logVerifier.verifyTextInLog("module-b");
    }

    /**
     * Verify that {@code mvnlog} reports an error when no build report exists.
     */
    @Test
    void testMvnlogNoReport() throws Exception {
        Path basedir = extractResources("gh-12571-build-report");

        // Clean any prior reports
        Path reportsDir = basedir.resolve("target/build-reports");
        if (Files.isDirectory(reportsDir)) {
            Files.walk(reportsDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception e) {
                            // ignore
                        }
                    });
        }

        Verifier logVerifier = newVerifier(basedir);
        logVerifier.setLogFileName("mvnlog-no-report.txt");
        logVerifier.setForkJvm(true);
        logVerifier.setExecutable("mvnlog");

        boolean failed = false;
        try {
            logVerifier.execute();
        } catch (VerificationException e) {
            failed = true;
        }

        assertTrue(failed, "mvnlog should fail when no report exists");
        logVerifier.verifyTextInLog("Build report not found");
    }

    /**
     * Verify that {@code mvnlog --list} lists available reports.
     */
    @Test
    void testMvnlogListReports() throws Exception {
        Path basedir = extractResources("gh-12571-build-report");

        // Step 1: run a build to generate at least one report
        Verifier buildVerifier = newVerifier(basedir);
        buildVerifier.setLogFileName("build-for-list.txt");
        buildVerifier.addCliArgument("--console=verbose");
        buildVerifier.addCliArgument("validate");
        buildVerifier.execute();
        buildVerifier.verifyErrorFreeLog();

        // Step 2: run mvnlog --list (forked: embedded executor does not know mvnlog)
        Verifier logVerifier = newVerifier(basedir);
        logVerifier.setLogFileName("mvnlog-list.txt");
        logVerifier.setForkJvm(true);
        logVerifier.setExecutable("mvnlog");
        logVerifier.addCliArgument("--list");
        logVerifier.execute();

        // Should list the report file(s)
        logVerifier.verifyTextInLog("build-report");
    }

    /**
     * Verify that {@code mvnlog --json} outputs the raw JSON build report,
     * suitable for piping to tools like {@code jq}.
     */
    @Test
    void testMvnlogJsonOutput() throws Exception {
        Path basedir = extractResources("gh-12571-build-report");

        // Step 1: run a build to generate the report
        Verifier buildVerifier = newVerifier(basedir);
        buildVerifier.setLogFileName("build-for-json.txt");
        buildVerifier.addCliArgument("--console=verbose");
        buildVerifier.addCliArgument("validate");
        buildVerifier.execute();
        buildVerifier.verifyErrorFreeLog();

        // Step 2: run mvnlog --json (forked: embedded executor does not know mvnlog)
        Verifier logVerifier = newVerifier(basedir);
        logVerifier.setLogFileName("mvnlog-json.txt");
        logVerifier.setForkJvm(true);
        logVerifier.setExecutable("mvnlog");
        logVerifier.addCliArgument("--json");
        logVerifier.execute();

        // Output should be valid JSON with expected fields
        logVerifier.verifyTextInLog("\"formatVersion\"");
        logVerifier.verifyTextInLog("\"status\"");
        logVerifier.verifyTextInLog("\"modules\"");
        logVerifier.verifyTextInLog("\"mavenVersion\"");
    }
}
