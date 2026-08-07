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
package org.apache.maven.internal.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.build.report.BuildReport;
import org.apache.maven.api.build.report.BuildStatus;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test that exercises the full build report pipeline:
 * BuildReportCollector → DefaultDiagnosticCollector → BuildReportJsonWriter → file.
 * <p>
 * This test simulates a complete multi-module build lifecycle with mojo
 * executions, problems, and failures, then verifies the resulting
 * JSON report file contains all expected data.
 */
class BuildReportIntegrationTest {

    @TempDir
    Path tempDir;

    private DefaultDiagnosticCollector diagnosticCollector;
    private BuildReportCollector collector;

    @BeforeEach
    void setUp() {
        diagnosticCollector = new DefaultDiagnosticCollector();
        collector = new BuildReportCollector(diagnosticCollector);
    }

    private static BuilderProblem warning(String key, String message, String source) {
        return BuilderProblem.builder()
                .source(source)
                .message(message)
                .severity(BuilderProblem.Severity.WARNING)
                .key(key)
                .build();
    }

    private static BuilderProblem warning(String key, String message, String source, String suggestion, String docUrl) {
        return BuilderProblem.builder()
                .source(source)
                .message(message)
                .severity(BuilderProblem.Severity.WARNING)
                .key(key)
                .suggestion(suggestion)
                .documentationUrl(docUrl)
                .build();
    }

    private static BuilderProblem error(String key, String message, String source) {
        return BuilderProblem.builder()
                .source(source)
                .message(message)
                .severity(BuilderProblem.Severity.ERROR)
                .key(key)
                .build();
    }

    /**
     * Full lifecycle: multi-module build with mojos, problems, and JSON persistence.
     */
    @Test
    void testFullMultiModuleBuildWithProblems() throws IOException {
        MavenProject parent = createProject("com.example", "parent", "2.0.0");
        MavenProject api = createProject("com.example", "api", "2.0.0");
        MavenProject impl = createProject("com.example", "impl", "2.0.0");

        MavenSession session = createSession(parent, api, impl);
        MavenExecutionResult result = session.getResult();

        // Session starts
        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, parent, null));

        // --- Module: parent ---
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, parent, null));
        result.addBuildSummary(new BuildSuccess(parent, 500));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, parent, null));

        // --- Module: api ---
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, api, null));

        MojoExecution compileApi = createMojoExecution(
                "org.apache.maven.plugins", "maven-compiler-plugin", "3.15.0", "compile", "default-compile", "compile");
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoStarted, session, api, compileApi));

        // Report a deprecation warning during compile
        diagnosticCollector.report(warning(
                "deprecated-source-target",
                "source/target value 8 is deprecated",
                "maven-compiler-plugin:3.15.0:compile",
                "Update <maven.compiler.source> to 11 or higher",
                null));

        collector.onEvent(createEvent(ExecutionEvent.Type.MojoSucceeded, session, api, compileApi));

        MojoExecution testApi = createMojoExecution(
                "org.apache.maven.plugins", "maven-surefire-plugin", "3.5.0", "test", "default-test", "test");
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoStarted, session, api, testApi));
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoSucceeded, session, api, testApi));

        result.addBuildSummary(new BuildSuccess(api, 3000));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, api, null));

        // --- Module: impl ---
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, impl, null));

        MojoExecution compileImpl = createMojoExecution(
                "org.apache.maven.plugins", "maven-compiler-plugin", "3.15.0", "compile", "default-compile", "compile");
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoStarted, session, impl, compileImpl));

        // Same warning fires again in a different module — should be deduplicated
        diagnosticCollector.report(warning("deprecated-source-target", "source/target value 8 is deprecated", "impl"));

        // Also report a unique error problem
        diagnosticCollector.report(error("build-error", "Missing required dependency", "impl"));

        collector.onEvent(createEvent(ExecutionEvent.Type.MojoSucceeded, session, impl, compileImpl));
        result.addBuildSummary(new BuildSuccess(impl, 2000));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, impl, null));

        // --- Build report ---
        BuildReport report = collector.buildReport(session);

        // Basic assertions
        assertNotNull(report);
        assertEquals(BuildStatus.SUCCESS, report.status());
        assertTrue(report.multiModule());
        assertEquals(3, report.modules().size());

        // Problems in the report
        assertEquals(2, report.problems().size());
        assertEquals("deprecated-source-target", report.problems().get(0).getKey());
        assertEquals(BuilderProblem.Severity.WARNING, report.problems().get(0).getSeverity());
        assertEquals("build-error", report.problems().get(1).getKey());
        assertEquals(BuilderProblem.Severity.ERROR, report.problems().get(1).getSeverity());

        // Summary should show count = 2 for the warning
        assertEquals(2, diagnosticCollector.getSummary().get(0).count());
        assertEquals(1, diagnosticCollector.getSummary().get(1).count());

        // Module reports
        assertEquals("parent", report.modules().get(0).artifactId());
        assertEquals("api", report.modules().get(1).artifactId());
        assertEquals(2, report.modules().get(1).mojos().size());
        assertEquals("compile", report.modules().get(1).mojos().get(0).goal());
        assertEquals("test", report.modules().get(1).mojos().get(1).goal());
        assertEquals("impl", report.modules().get(2).artifactId());

        // Write to JSON and verify
        collector.writeReport(report, session);

        Path reportsDir = tempDir.resolve("target").resolve(BuildReportCollector.REPORT_DIR);
        Path latestFile = reportsDir.resolve(BuildReportCollector.REPORT_LATEST);
        assertTrue(Files.exists(latestFile), "build-report-latest.json should exist");

        String json = Files.readString(latestFile);

        // Verify JSON structure
        assertTrue(json.contains("\"formatVersion\": 1"));
        assertTrue(json.contains("\"status\": \"SUCCESS\""));
        assertTrue(json.contains("\"multiModule\": true"));
        assertTrue(json.contains("\"threads\": 1"));

        // Modules in JSON
        assertTrue(json.contains("\"artifactId\": \"parent\""));
        assertTrue(json.contains("\"artifactId\": \"api\""));
        assertTrue(json.contains("\"artifactId\": \"impl\""));

        // Mojos in JSON
        assertTrue(json.contains("\"goal\": \"compile\""));
        assertTrue(json.contains("\"goal\": \"test\""));

        // Problems in JSON
        assertTrue(json.contains("\"problems\": ["));
        assertTrue(json.contains("\"key\": \"deprecated-source-target\""));
        assertTrue(json.contains("\"severity\": \"WARNING\""));
        assertTrue(json.contains("\"suggestion\": \"Update <maven.compiler.source> to 11 or higher\""));
        assertTrue(json.contains("\"key\": \"build-error\""));
        assertTrue(json.contains("\"severity\": \"ERROR\""));

        // Failures should be empty
        assertTrue(json.contains("\"failures\": []"));

        // Output arrays should be present (even if empty in unit test — no SLF4J sink)
        assertTrue(json.contains("\"output\": ["));
    }

    /**
     * Tests that the problem summary printing doesn't throw when there are no problems.
     */
    @Test
    void testDiagnosticSummaryWithNoProblems() {
        // Should be a no-op, not throw
        collector.printDiagnosticSummary();
    }

    /**
     * Tests that the problem summary printing handles mixed severities.
     */
    @Test
    void testDiagnosticSummaryWithMixedSeverities() {
        diagnosticCollector.report(warning("warn-1", "first warning", null));
        diagnosticCollector.report(warning("warn-1", "first warning (dup)", null));
        diagnosticCollector.report(error("err-1", "first error", null));
        diagnosticCollector.report(warning("warn-2", "second warning", null));

        // Should not throw
        collector.printDiagnosticSummary();

        assertTrue(diagnosticCollector.hasWarnings());
        assertTrue(diagnosticCollector.hasErrors());
    }

    /**
     * Tests navigation methods on the built report.
     */
    @Test
    void testReportNavigationMethods() {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        MavenExecutionResult result = session.getResult();

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));

        MojoExecution mojo = createMojoExecution(
                "org.apache.maven.plugins", "maven-compiler-plugin", "3.15.0", "compile", "default-compile", "compile");
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoStarted, session, project, mojo));
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoSucceeded, session, project, mojo));

        result.addBuildSummary(new BuildSuccess(project, 5000));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, project, null));

        BuildReport report = collector.buildReport(session);

        // findModule by GAV
        var moduleOpt = report.findModule("org.example:my-app:1.0.0");
        assertTrue(moduleOpt.isPresent());
        assertEquals("my-app", moduleOpt.get().artifactId());
        assertEquals("org.example:my-app:1.0.0", moduleOpt.get().id());

        // findMojo by id
        var mojoOpt = moduleOpt.get().findMojo("maven-compiler-plugin:3.15.0:compile");
        assertTrue(mojoOpt.isPresent());
        assertEquals("compile", mojoOpt.get().goal());

        // Not found
        assertFalse(report.findModule("nonexistent:module:1.0").isPresent());
    }

    // ---- Test helpers ----

    private MavenProject createProject(String groupId, String artifactId, String version) {
        MavenProject project = new MavenProject();
        project.setGroupId(groupId);
        project.setArtifactId(artifactId);
        project.setVersion(version);
        return project;
    }

    private MavenSession createSession(MavenProject... projects) {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setStartInstant(MonotonicClock.now());
        request.setGoals(List.of("clean", "install"));
        request.setTopDirectory(tempDir);

        Properties systemProperties = new Properties();
        systemProperties.setProperty("maven.version", "4.1.0-SNAPSHOT");
        request.setSystemProperties(systemProperties);

        MavenExecutionResult result = new DefaultMavenExecutionResult();

        @SuppressWarnings("deprecation")
        MavenSession session = new MavenSession(null, null, request, result);
        session.setProjects(List.of(projects));
        return session;
    }

    private MojoExecution createMojoExecution(
            String groupId, String artifactId, String version, String goal, String executionId, String phase) {
        @SuppressWarnings("deprecation")
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId(groupId);
        pluginDescriptor.setArtifactId(artifactId);
        pluginDescriptor.setVersion(version);

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal(goal);
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);

        MojoExecution execution = new MojoExecution(mojoDescriptor, executionId);
        execution.setLifecyclePhase(phase);

        return execution;
    }

    private ExecutionEvent createEvent(
            ExecutionEvent.Type type, MavenSession session, MavenProject project, MojoExecution mojo) {
        return new ExecutionEvent() {
            @Override
            public Type getType() {
                return type;
            }

            @Override
            public MavenSession getSession() {
                return session;
            }

            @Override
            public MavenProject getProject() {
                return project;
            }

            @Override
            public MojoExecution getMojoExecution() {
                return mojo;
            }

            @Override
            public Exception getException() {
                return null;
            }
        };
    }
}
