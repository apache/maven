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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.BuildInvocation;
import org.apache.maven.api.ExecutionEvent;
import org.apache.maven.api.ExecutionEventType;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.Plugin;
import org.apache.maven.api.Project;
import org.apache.maven.api.ReactorFailureBehavior;
import org.apache.maven.api.Session;
import org.apache.maven.api.build.report.BuildReport;
import org.apache.maven.api.build.report.BuildStatus;
import org.apache.maven.api.logging.LogEvent;
import org.apache.maven.api.logging.LogLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the new API-based {@link BuildReportCollector}.
 */
class BuildReportCollectorTest {

    private BuildReportCollector collector;

    @BeforeEach
    void setUp() {
        collector = new BuildReportCollector();
    }

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void testBuildReportAssembly() {
        Project project = createProject("org.example", "my-app", "1.0.0");
        Session session = createSession(project);

        collector.sessionStarted(createEvent(ExecutionEventType.SESSION_STARTED, session, project, null));
        collector.projectStarted(createEvent(ExecutionEventType.PROJECT_STARTED, session, project, null));

        MojoExecution mojo = createMojoExecution(
                "org.apache.maven.plugins", "maven-compiler-plugin", "3.15.0", "compile", "default-compile", "compile");
        collector.mojoStarted(createEvent(ExecutionEventType.MOJO_STARTED, session, project, mojo));
        collector.mojoSucceeded(createEvent(ExecutionEventType.MOJO_SUCCEEDED, session, project, mojo));

        collector.projectSucceeded(createEvent(ExecutionEventType.PROJECT_SUCCEEDED, session, project, null));

        BuildReport report = collector.buildReport(session, false);

        assertNotNull(report);
        assertEquals(BuildStatus.SUCCESS, report.status());
        assertFalse(report.multiSubproject());
        assertEquals(1, report.threads());
        assertEquals(1, report.subprojects().size());
        assertEquals("org.example", report.subprojects().get(0).groupId());
        assertEquals("my-app", report.subprojects().get(0).artifactId());
        assertEquals(BuildStatus.SUCCESS, report.subprojects().get(0).status());
        assertEquals(1, report.subprojects().get(0).mojos().size());
        assertEquals("compile", report.subprojects().get(0).mojos().get(0).goal());
        assertEquals(
                BuildStatus.SUCCESS, report.subprojects().get(0).mojos().get(0).status());
        assertNull(report.subprojects().get(0).traceId());
        assertNull(report.subprojects().get(0).spanId());
        assertTrue(report.failures().isEmpty());
        assertTrue(report.problems().isEmpty());

        // invocation assertions
        assertNotNull(report.invocation());
        assertEquals(List.of("clean", "install"), report.invocation().goals());
    }

    @Test
    void testTraceContextIsCapturedFromMdc() {
        // This test verifies trace context from MDC. If the SLF4J backend doesn't support MDC
        // (e.g. NOP), MDC.put() is a no-op and MDC.get() returns null, so we skip assertions
        // that depend on MDC support.
        Project project = createProject("org.example", "my-app", "1.0.0");
        Session session = createSession(project);

        collector.sessionStarted(createEvent(ExecutionEventType.SESSION_STARTED, session, project, null));

        MDC.put(BuildReportCollector.TRACE_ID_MDC_KEY, "module-trace");
        MDC.put(BuildReportCollector.SPAN_ID_MDC_KEY, "module-span");
        boolean mdcSupported = MDC.get(BuildReportCollector.TRACE_ID_MDC_KEY) != null;
        collector.projectStarted(createEvent(ExecutionEventType.PROJECT_STARTED, session, project, null));

        MojoExecution mojo = createMojoExecution(
                "org.apache.maven.plugins", "maven-compiler-plugin", "3.15.0", "compile", "default-compile", "compile");
        MDC.put(BuildReportCollector.TRACE_ID_MDC_KEY, "mojo-trace");
        MDC.put(BuildReportCollector.SPAN_ID_MDC_KEY, "mojo-span");
        collector.mojoStarted(createEvent(ExecutionEventType.MOJO_STARTED, session, project, mojo));
        MDC.clear();
        collector.mojoSucceeded(createEvent(ExecutionEventType.MOJO_SUCCEEDED, session, project, mojo));
        collector.projectSucceeded(createEvent(ExecutionEventType.PROJECT_SUCCEEDED, session, project, null));

        BuildReport report = collector.buildReport(session, false);
        var subproject = report.subprojects().get(0);
        if (mdcSupported) {
            assertEquals("module-trace", subproject.traceId());
            assertEquals("module-span", subproject.spanId());
            assertEquals("mojo-trace", subproject.mojos().get(0).traceId());
            assertEquals("mojo-span", subproject.mojos().get(0).spanId());
        } else {
            // Without MDC support, trace context fields are null
            assertNull(subproject.traceId());
            assertNull(subproject.spanId());
        }
    }

    @Test
    void testBuildReportWithFailure() {
        Project project = createProject("org.example", "my-app", "1.0.0");
        Session session = createSession(project);

        collector.sessionStarted(createEvent(ExecutionEventType.SESSION_STARTED, session, project, null));
        collector.projectStarted(createEvent(ExecutionEventType.PROJECT_STARTED, session, project, null));

        MojoExecution mojo = createMojoExecution(
                "org.apache.maven.plugins", "maven-compiler-plugin", "3.15.0", "compile", "default-compile", "compile");
        collector.mojoStarted(createEvent(ExecutionEventType.MOJO_STARTED, session, project, mojo));
        collector.mojoFailed(createEvent(ExecutionEventType.MOJO_FAILED, session, project, mojo));

        RuntimeException failure = new RuntimeException("Compilation failure: 3 errors");
        collector.projectFailed(
                createEventWithException(ExecutionEventType.PROJECT_FAILED, session, project, null, failure));

        BuildReport report = collector.buildReport(session, true);

        assertEquals(BuildStatus.FAILURE, report.status());
        assertEquals(1, report.failures().size());
        assertEquals("org.example:my-app:1.0.0", report.failures().get(0).subproject());
        assertTrue(report.failures().get(0).message().contains("Compilation failure"));
        assertNotNull(report.failures().get(0).timestamp(), "failure should have a timestamp");
        assertEquals("RuntimeException", report.failures().get(0).exceptionType(), "exceptionType from cause");
    }

    @Test
    void testMojoSkippedIsRecorded() {
        Project project = createProject("org.example", "my-app", "1.0.0");
        Session session = createSession(project);

        collector.sessionStarted(createEvent(ExecutionEventType.SESSION_STARTED, session, project, null));
        collector.projectStarted(createEvent(ExecutionEventType.PROJECT_STARTED, session, project, null));

        MojoExecution mojo = createMojoExecution(
                "org.apache.maven.plugins", "maven-surefire-plugin", "3.0.0", "test", "default-test", "test");
        collector.mojoSkipped(createEvent(ExecutionEventType.MOJO_SKIPPED, session, project, mojo));

        collector.projectSucceeded(createEvent(ExecutionEventType.PROJECT_SUCCEEDED, session, project, null));

        BuildReport report = collector.buildReport(session, false);

        assertEquals(1, report.subprojects().size());
        var subproject = report.subprojects().get(0);
        assertEquals(1, subproject.mojos().size(), "skipped mojo should appear in the report");
        var mojoReport = subproject.mojos().get(0);
        assertEquals("test", mojoReport.goal());
        assertEquals(BuildStatus.SKIPPED, mojoReport.status());
        assertEquals(Duration.ZERO, mojoReport.duration());
        assertTrue(mojoReport.output().isEmpty());
    }

    @Test
    void testLogEventCapIsManagedPerScope() {
        final int defaultCap = 500;
        Project project = createProject("org.example", "my-app", "1.0.0");
        Session session = createSession(project);

        collector.sessionStarted(createEvent(ExecutionEventType.SESSION_STARTED, session, project, null));
        collector.projectStarted(createEvent(ExecutionEventType.PROJECT_STARTED, session, project, null));

        MojoExecution mojo = createMojoExecution(
                "org.apache.maven.plugins", "maven-compiler-plugin", "3.15.0", "compile", "default-compile", "compile");
        collector.mojoStarted(createEvent(ExecutionEventType.MOJO_STARTED, session, project, mojo));

        int overCap = defaultCap + 10;
        LogEvent event = stubLogEvent();
        for (int i = 0; i < overCap; i++) {
            collector.accept(event);
        }

        collector.mojoSucceeded(createEvent(ExecutionEventType.MOJO_SUCCEEDED, session, project, mojo));
        collector.projectSucceeded(createEvent(ExecutionEventType.PROJECT_SUCCEEDED, session, project, null));

        BuildReport report = collector.buildReport(session, false);

        List<?> mojoOutput = report.subprojects().get(0).mojos().get(0).output();
        assertEquals(
                defaultCap, mojoOutput.size(), "mojo log buffer must be capped at the default maxLogEventsPerScope");
        assertEquals(0, report.subprojects().get(0).output().size(), "subproject buffer must be empty");
    }

    @Test
    void testAcceptIsNoOpWhenNoSession() {
        // Should not throw
        collector.accept(stubLogEvent());
    }

    @Test
    void testStackTraceIsTruncated() {
        RuntimeException deep = createDeepException(50);
        String truncated = BuildReportCollector.truncateStackTrace(deep, 30);
        assertTrue(truncated.contains("more lines truncated"), "deep stack traces should be truncated");
    }

    @Test
    void testShortStackTraceIsNotTruncated() {
        RuntimeException shallow = new RuntimeException("short");
        shallow.setStackTrace(
                new StackTraceElement[] {new StackTraceElement("com.example.Foo", "bar", "Foo.java", 42)});
        String result = BuildReportCollector.truncateStackTrace(shallow, 30);
        assertFalse(result.contains("more lines truncated"), "short stack traces should not be truncated");
    }

    @Test
    void testMultiModuleBuild() {
        Project parent = createProject("org.example", "parent", "1.0.0");
        Project child1 = createProject("org.example", "child-api", "1.0.0");
        Project child2 = createProject("org.example", "child-impl", "1.0.0");

        Session session = createSession(parent, child1, child2);

        collector.sessionStarted(createEvent(ExecutionEventType.SESSION_STARTED, session, parent, null));

        for (Project p : List.of(parent, child1, child2)) {
            collector.projectStarted(createEvent(ExecutionEventType.PROJECT_STARTED, session, p, null));
            collector.projectSucceeded(createEvent(ExecutionEventType.PROJECT_SUCCEEDED, session, p, null));
        }

        BuildReport report = collector.buildReport(session, false);

        assertEquals(BuildStatus.SUCCESS, report.status());
        assertTrue(report.multiSubproject());
        assertEquals(3, report.subprojects().size());
        assertEquals("parent", report.subprojects().get(0).artifactId());
        assertEquals("child-api", report.subprojects().get(1).artifactId());
        assertEquals("child-impl", report.subprojects().get(2).artifactId());
    }

    @Test
    void testSessionStateIsClearedAfterSessionEnded() {
        Project project = createProject("org.example", "my-app", "1.0.0");
        Session session = createSession(project);

        collector.sessionStarted(createEvent(ExecutionEventType.SESSION_STARTED, session, project, null));
        collector.projectStarted(createEvent(ExecutionEventType.PROJECT_STARTED, session, project, null));
        collector.projectSucceeded(createEvent(ExecutionEventType.PROJECT_SUCCEEDED, session, project, null));
        collector.sessionEnded(createEvent(ExecutionEventType.SESSION_ENDED, session, project, null));

        // After session ends, accept should be a no-op (session is null)
        collector.accept(stubLogEvent()); // should not throw

        // A second session should work cleanly
        Project project2 = createProject("org.example", "other-app", "2.0.0");
        Session session2 = createSession(project2);

        collector.sessionStarted(createEvent(ExecutionEventType.SESSION_STARTED, session2, project2, null));
        collector.projectStarted(createEvent(ExecutionEventType.PROJECT_STARTED, session2, project2, null));
        collector.projectSucceeded(createEvent(ExecutionEventType.PROJECT_SUCCEEDED, session2, project2, null));

        BuildReport report2 = collector.buildReport(session2, false);

        assertEquals(1, report2.subprojects().size(), "second build must have exactly 1 subproject");
        assertEquals("other-app", report2.subprojects().get(0).artifactId(), "must be the second build's project");
    }

    // ---- Test helpers using Mockito ----

    private static LogEvent stubLogEvent() {
        return new LogEvent() {
            @Override
            public Instant timestamp() {
                return Instant.EPOCH;
            }

            @Override
            public LogLevel level() {
                return LogLevel.INFO;
            }

            @Override
            public String message() {
                return "test";
            }

            @Override
            public String loggerName() {
                return null;
            }
        };
    }

    private static Project createProject(String groupId, String artifactId, String version) {
        Project project = mock(Project.class);
        lenient().when(project.groupId()).thenReturn(groupId);
        lenient().when(project.artifactId()).thenReturn(artifactId);
        lenient().when(project.version()).thenReturn(version);
        return project;
    }

    private static Session createSession(Project... projects) {
        BuildInvocation invocation = new DefaultBuildInvocation(
                List.of("clean", "install"),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                ReactorFailureBehavior.FAIL_FAST,
                false,
                false,
                false,
                true,
                false,
                1);

        Session session = mock(Session.class);
        when(session.getProjects()).thenReturn(List.of(projects));
        when(session.buildInvocation()).thenReturn(invocation);
        when(session.getDegreeOfConcurrency()).thenReturn(1);
        when(session.getUserProperties()).thenReturn(Map.of());
        when(session.getSystemProperties()).thenReturn(Map.of("maven.version", "1.0.0"));
        lenient().when(session.getTopDirectory()).thenReturn(null);
        lenient().when(session.getStartTime()).thenReturn(MonotonicClock.now());
        return session;
    }

    private static MojoExecution createMojoExecution(
            String groupId, String artifactId, String version, String goal, String executionId, String phase) {
        org.apache.maven.api.model.Plugin modelPlugin = org.apache.maven.api.model.Plugin.newBuilder()
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .build();

        Plugin plugin = mock(Plugin.class);
        when(plugin.getModel()).thenReturn(modelPlugin);

        MojoExecution mojo = mock(MojoExecution.class);
        when(mojo.plugin()).thenReturn(plugin);
        when(mojo.goal()).thenReturn(goal);
        when(mojo.executionId()).thenReturn(Optional.ofNullable(executionId));
        when(mojo.lifecyclePhase()).thenReturn(Optional.ofNullable(phase));
        return mojo;
    }

    private static ExecutionEvent createEvent(
            ExecutionEventType type, Session session, Project project, MojoExecution mojo) {
        ExecutionEvent event = mock(ExecutionEvent.class);
        when(event.type()).thenReturn(type);
        when(event.session()).thenReturn(session);
        when(event.project()).thenReturn(Optional.ofNullable(project));
        when(event.mojoExecution()).thenReturn(Optional.ofNullable(mojo));
        when(event.exception()).thenReturn(Optional.empty());
        return event;
    }

    private static ExecutionEvent createEventWithException(
            ExecutionEventType type, Session session, Project project, MojoExecution mojo, Exception exception) {
        ExecutionEvent event = mock(ExecutionEvent.class);
        when(event.type()).thenReturn(type);
        when(event.session()).thenReturn(session);
        when(event.project()).thenReturn(Optional.ofNullable(project));
        when(event.mojoExecution()).thenReturn(Optional.ofNullable(mojo));
        when(event.exception()).thenReturn(Optional.ofNullable(exception));
        return event;
    }

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
