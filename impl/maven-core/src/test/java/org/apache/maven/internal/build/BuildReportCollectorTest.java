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

class BuildReportCollectorTest {

    @TempDir
    Path tempDir;

    private BuildReportCollector collector;

    @BeforeEach
    void setUp() {
        collector = new BuildReportCollector();
    }

    @Test
    void testBuildReportAssembly() {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        MavenExecutionResult result = session.getResult();

        // Simulate: session started -> project started -> mojo started -> mojo succeeded -> project succeeded ->
        // session
        // ended
        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));

        MojoExecution mojo = createMojoExecution(
                "org.apache.maven.plugins", "maven-compiler-plugin", "3.15.0", "compile", "default-compile", "compile");
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoStarted, session, project, mojo));
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoSucceeded, session, project, mojo));

        // Record build success in the result
        result.addBuildSummary(new BuildSuccess(project, 5000));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, project, null));

        // Build the report
        BuildReport report = collector.buildReport(session);

        assertNotNull(report);
        assertEquals(BuildStatus.SUCCESS, report.status());
        assertEquals(1, report.formatVersion());
        assertEquals("1.0.0", report.mavenVersion());
        assertFalse(report.multiModule());
        assertEquals(1, report.threads());
        assertEquals(1, report.modules().size());
        assertEquals("org.example", report.modules().get(0).groupId());
        assertEquals("my-app", report.modules().get(0).artifactId());
        assertEquals(BuildStatus.SUCCESS, report.modules().get(0).status());
        assertEquals(1, report.modules().get(0).mojos().size());
        assertEquals("compile", report.modules().get(0).mojos().get(0).goal());
        assertEquals(BuildStatus.SUCCESS, report.modules().get(0).mojos().get(0).status());
        assertTrue(report.failures().isEmpty());
        assertTrue(report.problems().isEmpty());
    }

    @Test
    void testBuildReportWithFailure() {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        MavenExecutionResult result = session.getResult();

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));

        MojoExecution mojo = createMojoExecution(
                "org.apache.maven.plugins", "maven-compiler-plugin", "3.15.0", "compile", "default-compile", "compile");
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoStarted, session, project, mojo));
        collector.onEvent(createEvent(ExecutionEvent.Type.MojoFailed, session, project, mojo));

        RuntimeException failure = new RuntimeException("Compilation failure: 3 errors");
        result.addBuildSummary(new org.apache.maven.execution.BuildFailure(project, 3000, failure));
        result.addException(failure);
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectFailed, session, project, null));

        BuildReport report = collector.buildReport(session);

        assertEquals(BuildStatus.FAILURE, report.status());
        assertEquals(1, report.failures().size());
        assertEquals("org.example:my-app:1.0.0", report.failures().get(0).module());
        assertTrue(report.failures().get(0).message().contains("Compilation failure"));
        assertNotNull(report.failures().get(0).timestamp(), "failure should have a timestamp");
        assertEquals("RuntimeException", report.failures().get(0).exceptionType(), "exceptionType from cause");

        // Navigate from failure -> module -> mojo using lookup methods
        var failureReport = report.failures().get(0);
        var moduleOpt = report.findModule(failureReport);
        assertTrue(moduleOpt.isPresent(), "findModule(FailureReport) should find the module");
        assertEquals("my-app", moduleOpt.get().artifactId());
        assertEquals("org.example:my-app:1.0.0", moduleOpt.get().id());

        assertNotNull(failureReport.mojo(), "failure should reference a mojo");
        var mojoOpt = moduleOpt.get().findMojo(failureReport.mojo());
        assertTrue(mojoOpt.isPresent(), "findMojo should find the failed mojo");
        assertEquals("compile", mojoOpt.get().goal());
        assertEquals(BuildStatus.FAILURE, mojoOpt.get().status());
        assertEquals("maven-compiler-plugin:3.15.0:compile", mojoOpt.get().id());
    }

    @Test
    void testWriteReportToFile() throws IOException {
        MavenProject project = createProject("org.example", "my-app", "1.0.0");
        MavenSession session = createSession(project);
        session.getResult().addBuildSummary(new BuildSuccess(project, 1000));

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, project, null));
        collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, project, null));

        BuildReport report = collector.buildReport(session);
        collector.writeReport(report, session);

        Path reportsDir = tempDir.resolve("target").resolve(BuildReportCollector.REPORT_DIR);
        Path latestFile = reportsDir.resolve(BuildReportCollector.REPORT_LATEST);
        assertTrue(Files.exists(latestFile), "build-report-latest.json should exist");

        String content = Files.readString(latestFile);
        assertTrue(content.contains("\"formatVersion\": 1"));
        assertTrue(content.contains("\"status\": \"SUCCESS\""));
        assertTrue(content.contains("\"artifactId\": \"my-app\""));
    }

    @Test
    void testMultiModuleBuild() {
        MavenProject parent = createProject("org.example", "parent", "1.0.0");
        MavenProject child1 = createProject("org.example", "child-api", "1.0.0");
        MavenProject child2 = createProject("org.example", "child-impl", "1.0.0");

        MavenSession session = createSession(parent, child1, child2);
        MavenExecutionResult result = session.getResult();

        collector.onEvent(createEvent(ExecutionEvent.Type.SessionStarted, session, parent, null));

        // Build each module
        for (MavenProject p : List.of(parent, child1, child2)) {
            collector.onEvent(createEvent(ExecutionEvent.Type.ProjectStarted, session, p, null));
            result.addBuildSummary(new BuildSuccess(p, 1000));
            collector.onEvent(createEvent(ExecutionEvent.Type.ProjectSucceeded, session, p, null));
        }

        BuildReport report = collector.buildReport(session);

        assertEquals(BuildStatus.SUCCESS, report.status());
        assertTrue(report.multiModule());
        assertEquals(3, report.modules().size());
        assertEquals("parent", report.modules().get(0).artifactId());
        assertEquals("child-api", report.modules().get(1).artifactId());
        assertEquals("child-impl", report.modules().get(2).artifactId());
    }

    @Test
    void testStackTraceIsTruncated() {
        // Build a throwable with a deep stack trace
        RuntimeException deep = createDeepException(50);
        String truncated = BuildReportCollector.truncateStackTrace(deep);

        // Should contain the truncation notice
        assertTrue(truncated.contains("more lines truncated"), "deep stack traces should be truncated");
    }

    @Test
    void testShortStackTraceIsNotTruncated() {
        RuntimeException shallow = new RuntimeException("short");
        // Trim the stack to a known-small size so it's guaranteed under the limit
        shallow.setStackTrace(
                new StackTraceElement[] {new StackTraceElement("com.example.Foo", "bar", "Foo.java", 42)});
        String result = BuildReportCollector.truncateStackTrace(shallow);

        // Short stack traces should NOT contain the truncation notice
        assertFalse(result.contains("more lines truncated"), "short stack traces should not be truncated");
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
        systemProperties.setProperty("maven.version", "1.0.0");
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

    /**
     * Creates an exception with a stack trace of at least {@code depth} lines.
     */
    private static RuntimeException createDeepException(int depth) {
        try {
            throwDeep(depth);
        } catch (RuntimeException e) {
            return e;
        }
        throw new AssertionError("unreachable");
    }

    private static void throwDeep(int remaining) {
        if (remaining <= 0) {
            throw new RuntimeException("deep exception");
        }
        throwDeep(remaining - 1);
    }
}
