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
package org.apache.maven.cling.event;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;

import org.apache.maven.api.build.report.LogLevel;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.build.DefaultLogEvent;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.project.MavenProject;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RichExecutionEventLogger}.
 * <p>
 * In rich mode, all output goes directly to the terminal writer via
 * {@link RichBuildEventListener#log(String)}, bypassing SLF4J entirely.
 * Tests capture the terminal output to verify content.
 */
class RichExecutionEventLoggerTest {

    private MockitoSession mockitoSession;
    private Logger logger;
    private Terminal terminal;
    private ByteArrayOutputStream terminalOutput;
    private RichBuildEventListener buildEventListener;
    private RichExecutionEventLogger richLogger;
    private final JLineMessageBuilderFactory messageBuilderFactory = new JLineMessageBuilderFactory();

    @BeforeAll
    static void setUp() {
        MessageUtils.setColorEnabled(false);
    }

    @AfterAll
    static void tearDown() {
        MessageUtils.setColorEnabled(true);
    }

    @BeforeEach
    void beforeEach() throws Exception {
        mockitoSession = Mockito.mockitoSession().startMocking();
        logger = mock(Logger.class);
        lenient().when(logger.isInfoEnabled()).thenReturn(true);
        lenient().when(logger.isWarnEnabled()).thenReturn(true);
        terminalOutput = new ByteArrayOutputStream();
        terminal = new DumbTerminal(System.in, terminalOutput);
        terminal.setSize(new Size(120, 40));
        buildEventListener = new RichBuildEventListener(terminal, msg -> {});
        richLogger = new RichExecutionEventLogger(messageBuilderFactory, buildEventListener, logger);
    }

    @AfterEach
    void afterEach() throws Exception {
        terminal.close();
        mockitoSession.finishMocking();
    }

    @Test
    void testProjectStartedSuppressed() {
        // In rich mode, projectStarted should produce NO output (status bar handles it)
        ExecutionEvent event = mock(ExecutionEvent.class);

        richLogger.projectStarted(event);

        verify(logger, never()).info(anyString());
        assertTrue(terminalOutput.toString().isEmpty(), "No terminal output expected");
    }

    @Test
    void testMojoStartedSuppressed() {
        // In rich mode, mojoStarted should produce NO output (status bar handles it)
        ExecutionEvent event = mock(ExecutionEvent.class);

        richLogger.mojoStarted(event);

        verify(logger, never()).info(anyString());
        assertTrue(terminalOutput.toString().isEmpty(), "No terminal output expected");
    }

    @Test
    void testProjectSucceededSuppressed() {
        // In rich mode, projectSucceeded produces NO scrolling output —
        // the status bar checkmarks already indicate completion.
        MavenProject project = generateMavenProject("Maven Core");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildSuccess(project, 2100));

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project));
        when(session.getAllProjects()).thenReturn(List.of(project));
        lenient().when(session.getResult()).thenReturn(executionResult);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);

        richLogger.sessionStarted(sessionEvent);

        // Clear terminal output accumulated from sessionStarted (status bar init)
        terminalOutput.reset();

        ExecutionEvent projectEvent = mock(ExecutionEvent.class);
        lenient().when(projectEvent.getProject()).thenReturn(project);
        lenient().when(projectEvent.getSession()).thenReturn(session);

        richLogger.projectSucceeded(projectEvent);

        // No output — SUCCESS lines are suppressed in rich mode
        String output = terminalOutput.toString();
        assertFalse(output.contains("SUCCESS"), "SUCCESS line should be suppressed in rich mode");
    }

    @Test
    void testProjectFailedShowsCross() {
        MavenProject project = generateMavenProject("Core");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildFailure(project, 5000, new Exception("Compile error")));

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project));
        when(session.getAllProjects()).thenReturn(List.of(project));
        when(session.getResult()).thenReturn(executionResult);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);
        richLogger.sessionStarted(sessionEvent);

        ExecutionEvent event = mockProjectEvent(project, session);
        richLogger.projectFailed(event);

        String output = terminalOutput.toString();
        assertTrue(output.contains("Core"), "Should contain project name");
        assertTrue(output.contains("FAILURE"), "Should contain FAILURE status");
    }

    @Test
    void testMultiModuleSuccessSuppressed() {
        // In rich mode, per-module SUCCESS lines are suppressed — the status bar
        // already shows ✓/●/○ indicators and the [n/total] counter.
        MavenProject project1 = generateMavenProject("API");
        MavenProject project2 = generateMavenProject("Core");
        MavenProject project3 = generateMavenProject("CLI");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildSuccess(project1, 1000));
        executionResult.addBuildSummary(new BuildSuccess(project2, 3000));
        executionResult.addBuildSummary(new BuildSuccess(project3, 2000));

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project1, project2, project3));
        when(session.getAllProjects()).thenReturn(List.of(project1, project2, project3));
        lenient().when(session.getResult()).thenReturn(executionResult);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);
        richLogger.sessionStarted(sessionEvent);

        // Clear terminal output from sessionStarted
        terminalOutput.reset();

        // mockProjectEvent stubs are unused since projectSucceeded is a no-op;
        // call with a plain mock to avoid UnnecessaryStubbing errors.
        richLogger.projectSucceeded(mock(ExecutionEvent.class));
        richLogger.projectSucceeded(mock(ExecutionEvent.class));
        richLogger.projectSucceeded(mock(ExecutionEvent.class));

        String output = terminalOutput.toString();
        // No per-module SUCCESS lines should appear
        assertFalse(output.contains("SUCCESS"), "SUCCESS lines should be suppressed in rich mode");
        assertFalse(output.contains("[1/3]"), "Progress counters should not appear");
    }

    @Test
    void testSessionEndedShowsSummary() {
        MavenProject project1 = generateMavenProject("API");
        MavenProject project2 = generateMavenProject("Core");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildSuccess(project1, 1000));
        executionResult.addBuildSummary(new BuildSuccess(project2, 2000));

        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();

        MavenSession session = mock(MavenSession.class);
        when(session.getResult()).thenReturn(executionResult);
        when(session.getRequest()).thenReturn(executionRequest);
        when(session.getProjects()).thenReturn(List.of(project1, project2));
        when(session.getAllProjects()).thenReturn(List.of(project1, project2));

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);

        richLogger.sessionStarted(sessionEvent);
        richLogger.sessionEnded(sessionEvent);

        // Summary goes to terminal writer, not logger
        String output = terminalOutput.toString();
        assertTrue(output.contains("BUILD SUCCESS"), "Should contain BUILD SUCCESS");
        assertTrue(output.contains("2 modules"), "Should contain module count");
        assertTrue(output.contains("2 passed"), "Should contain passed count");
        assertTrue(output.contains("Total time:"), "Should contain total time");
        assertTrue(
                output.contains("Full report: target/build-reports/build-report-latest.json"),
                "Should contain report path");
        // MNG-7372: version info only on failure, not success
        assertFalse(output.contains("Maven:"), "Should NOT contain Maven version on success");
    }

    @Test
    void testSessionEndedWithFailures() {
        MavenProject project1 = generateMavenProject("API");
        MavenProject project2 = generateMavenProject("Core");
        MavenProject project3 = generateMavenProject("CLI");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildSuccess(project1, 1000));
        executionResult.addBuildSummary(new BuildFailure(project2, 2000, new Exception("Error")));
        executionResult.addException(new Exception("Error"));

        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();

        MavenSession session = mock(MavenSession.class);
        when(session.getResult()).thenReturn(executionResult);
        when(session.getRequest()).thenReturn(executionRequest);
        when(session.getProjects()).thenReturn(List.of(project1, project2, project3));
        when(session.getAllProjects()).thenReturn(List.of(project1, project2, project3));

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);

        richLogger.sessionStarted(sessionEvent);
        richLogger.sessionEnded(sessionEvent);

        String output = terminalOutput.toString();
        assertTrue(output.contains("BUILD FAILURE"), "Should contain BUILD FAILURE");
        assertTrue(output.contains("3 modules"), "Should contain module count");
        assertTrue(output.contains("1 passed"), "Should contain passed count");
        assertTrue(output.contains("1 failed"), "Should contain failed count");
        assertTrue(output.contains("1 skipped"), "Should contain skipped count");
        // MNG-7372: version info shown on failure
        assertTrue(output.contains("Maven:"), "Should contain Maven version on failure");
        assertTrue(output.contains("Java:"), "Should contain Java version on failure");
    }

    @Test
    void testMojoSkippedStillWarns() {
        // mojoSkipped still uses logger.warn (goes through SLF4J for WARN level)
        ExecutionEvent event = mock(ExecutionEvent.class);
        var mojoExec = mock(org.apache.maven.plugin.MojoExecution.class);
        when(mojoExec.getGoal()).thenReturn("deploy");
        when(event.getMojoExecution()).thenReturn(mojoExec);

        richLogger.mojoSkipped(event);

        verify(logger).warn(anyString(), eq("deploy"));
    }

    @Test
    void testOutputBypassesSLF4J() {
        // Verify that summary output does NOT go through logger.info
        MavenProject project = generateMavenProject("Core");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildSuccess(project, 1000));

        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project));
        when(session.getAllProjects()).thenReturn(List.of(project));
        when(session.getResult()).thenReturn(executionResult);
        when(session.getRequest()).thenReturn(executionRequest);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);

        richLogger.sessionStarted(sessionEvent);
        richLogger.sessionEnded(sessionEvent);

        // No logger.info calls — all output goes through terminal writer
        verify(logger, never()).info(anyString());

        // But the output IS in the terminal
        String output = terminalOutput.toString();
        assertFalse(output.isEmpty(), "Terminal should have output");
        assertTrue(output.contains("BUILD SUCCESS"), "Terminal should contain BUILD SUCCESS");
    }

    @Test
    void testWarningSummaryShown() {
        MavenProject project = generateMavenProject("Core");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildSuccess(project, 1000));

        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project));
        when(session.getAllProjects()).thenReturn(List.of(project));
        when(session.getResult()).thenReturn(executionResult);
        when(session.getRequest()).thenReturn(executionRequest);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);

        richLogger.sessionStarted(sessionEvent);

        // Simulate 3 warnings arriving during the build
        Instant now = Instant.now();
        buildEventListener.projectLogMessage(
                "core",
                new DefaultLogEvent(now, LogLevel.WARN, "unchecked cast", "javac", null, "[WARNING] unchecked cast"));
        buildEventListener.projectLogMessage(
                "core",
                new DefaultLogEvent(now, LogLevel.WARN, "deprecated API", "javac", null, "[WARNING] deprecated API"));
        buildEventListener.projectLogMessage(
                "core",
                new DefaultLogEvent(now, LogLevel.WARN, "unused import", "javac", null, "[WARNING] unused import"));

        richLogger.sessionEnded(sessionEvent);

        String output = terminalOutput.toString();
        assertTrue(output.contains("BUILD SUCCESS"), "Should contain BUILD SUCCESS");
        assertTrue(output.contains("3 warning"), "Should contain warning count");
        assertTrue(output.contains("Diagnostics:"), "Should contain Diagnostics label");
        assertTrue(output.contains("mvnlog"), "Should hint how to see warning details");
    }

    @Test
    void testNoWarningSummaryWhenClean() {
        MavenProject project = generateMavenProject("Core");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildSuccess(project, 1000));

        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project));
        when(session.getAllProjects()).thenReturn(List.of(project));
        when(session.getResult()).thenReturn(executionResult);
        when(session.getRequest()).thenReturn(executionRequest);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);

        richLogger.sessionStarted(sessionEvent);
        richLogger.sessionEnded(sessionEvent);

        String output = terminalOutput.toString();
        assertTrue(output.contains("BUILD SUCCESS"), "Should contain BUILD SUCCESS");
        assertFalse(output.contains("Diagnostics:"), "Should NOT contain Diagnostics when no warnings");
    }

    // ---- Helpers ----

    private static MavenProject generateMavenProject(String projectName) {
        MavenProject project = mock(MavenProject.class);
        lenient()
                .when(project.getArtifactId())
                .thenReturn(projectName.toLowerCase().replace(" ", "-"));
        lenient().when(project.getPackaging()).thenReturn("jar");
        lenient().when(project.getVersion()).thenReturn("4.1.0-SNAPSHOT");
        lenient().when(project.getName()).thenReturn(projectName);
        return project;
    }

    private static ExecutionEvent mockProjectEvent(MavenProject project, MavenSession session) {
        ExecutionEvent event = mock(ExecutionEvent.class);
        when(event.getProject()).thenReturn(project);
        when(event.getSession()).thenReturn(session);
        return event;
    }
}
