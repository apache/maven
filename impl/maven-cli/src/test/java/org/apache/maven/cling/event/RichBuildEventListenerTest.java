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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.build.report.LogEvent;
import org.apache.maven.api.build.report.LogLevel;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.build.DefaultLogEvent;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RichBuildEventListener}.
 */
class RichBuildEventListenerTest {

    private MockitoSession mockitoSession;
    private Terminal terminal;
    private ByteArrayOutputStream terminalOutput;
    private RichBuildEventListener listener;

    @BeforeEach
    void beforeEach() throws Exception {
        mockitoSession = Mockito.mockitoSession().startMocking();
        terminalOutput = new ByteArrayOutputStream();
        // DumbTerminal: supported=false (fallback mode), output goes to terminalOutput
        terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), terminalOutput);
        terminal.setSize(new Size(120, 40));
        listener = new RichBuildEventListener(terminal, msg -> {});
    }

    @AfterEach
    void afterEach() throws Exception {
        terminal.close();
        mockitoSession.finishMocking();
    }

    @Test
    void testProjectStartedAndFinished() {
        MavenSession session =
                createSession(List.of(createProject("api"), createProject("core"), createProject("cli")));

        listener.initReactor(session);

        listener.projectStarted("api");
        listener.projectFinished("api");
    }

    @Test
    void testLogMessagePassthrough() {
        listener.log("Test log message");
        String output = terminalOutput.toString();
        assertTrue(output.contains("Test log message"), "Expected log message in output: " + output);
    }

    @Test
    void testProjectLogMessageFiltersInfo() {
        LogEvent infoEvent = new DefaultLogEvent(
                MonotonicClock.now(),
                LogLevel.INFO,
                "Compiling 42 source files",
                "compiler",
                null,
                "[INFO] Compiling 42 source files");
        listener.projectLogMessage("api", infoEvent);
        String output = terminalOutput.toString();
        assertFalse(
                output.contains("Compiling 42 source files"),
                "INFO messages should be suppressed in rich mode: " + output);
    }

    @Test
    void testProjectLogMessageSuppressesWarningInline() {
        // In rich mode, warnings are suppressed inline and only counted for the
        // end-of-build summary — they don't scroll above the status bar.
        LogEvent warnEvent = new DefaultLogEvent(
                MonotonicClock.now(),
                LogLevel.WARN,
                "Deprecated API usage",
                "compiler",
                null,
                "[WARNING] Deprecated API usage");
        listener.projectLogMessage("api", warnEvent);
        String output = terminalOutput.toString();
        assertFalse(
                output.contains("[WARNING] Deprecated API usage"),
                "WARN messages should be suppressed inline in rich mode: " + output);
        assertEquals(1, listener.getWarningCount(), "Warning count should be tracked");
    }

    @Test
    void testProjectLogMessageShowsError() {
        LogEvent errorEvent = new DefaultLogEvent(
                MonotonicClock.now(),
                LogLevel.ERROR,
                "Compilation failure",
                "compiler",
                null,
                "[ERROR] Compilation failure");
        listener.projectLogMessage("api", errorEvent);
        String output = terminalOutput.toString();
        assertTrue(output.contains("[ERROR] Compilation failure"), "ERROR messages should pass through: " + output);
    }

    @Test
    void testMojoStartedUpdatesState() {
        MavenSession session = createSession(List.of(createProject("api"), createProject("core")));
        listener.initReactor(session);

        listener.projectStarted("api");

        ExecutionEvent mojoEvent = mock(ExecutionEvent.class);
        MavenProject project = createProject("api");
        when(mojoEvent.getProject()).thenReturn(project);
        var mojoExec = mock(org.apache.maven.plugin.MojoExecution.class);
        when(mojoExec.getArtifactId()).thenReturn("maven-compiler-plugin");
        when(mojoExec.getGoal()).thenReturn("compile");
        when(mojoEvent.getMojoExecution()).thenReturn(mojoExec);

        listener.mojoStarted(mojoEvent);

        listener.projectFinished("api");
    }

    @Test
    void testTransferEvents() {
        MavenSession session = createSession(List.of(createProject("api")));
        listener.initReactor(session);

        TransferResource resource = mock(TransferResource.class);
        when(resource.getResourceName()).thenReturn("org/apache/maven/core/4.1.0/core-4.1.0.jar");
        when(resource.getContentLength()).thenReturn(524288L);

        TransferEvent startEvent = mock(TransferEvent.class);
        when(startEvent.getType()).thenReturn(TransferEvent.EventType.STARTED);
        when(startEvent.getResource()).thenReturn(resource);

        listener.transfer("api", startEvent);

        TransferEvent progressEvent = mock(TransferEvent.class);
        when(progressEvent.getType()).thenReturn(TransferEvent.EventType.PROGRESSED);
        when(progressEvent.getResource()).thenReturn(resource);
        when(progressEvent.getTransferredBytes()).thenReturn(262144L);

        listener.transfer("api", progressEvent);

        TransferEvent doneEvent = mock(TransferEvent.class);
        when(doneEvent.getType()).thenReturn(TransferEvent.EventType.SUCCEEDED);
        when(doneEvent.getResource()).thenReturn(resource);

        listener.transfer("api", doneEvent);
    }

    @Test
    void testParallelProjects() {
        MavenSession session =
                createSession(List.of(createProject("api"), createProject("core"), createProject("cli")));
        listener.initReactor(session);

        listener.projectStarted("api");
        listener.projectStarted("core");

        listener.projectFinished("api");

        listener.projectStarted("cli");

        listener.projectFinished("core");
        listener.projectFinished("cli");
    }

    @Test
    void testExecutionFailureUpdatesState() {
        MavenSession session = createSession(List.of(createProject("api")));
        listener.initReactor(session);

        listener.projectStarted("api");
        listener.executionFailure("api", true, "Compilation error");
        listener.projectFinished("api");
    }

    @Test
    void testTearDown() throws Exception {
        MavenSession session = createSession(List.of(createProject("api")));
        listener.initReactor(session);
        listener.projectStarted("api");

        listener.tearDown();
    }

    @Test
    void testFinishCallsTearDown() throws Exception {
        MavenSession session = createSession(List.of(createProject("api")));
        listener.initReactor(session);

        listener.finish(0);
    }

    @Test
    void testFailCallsTearDown() throws Exception {
        MavenSession session = createSession(List.of(createProject("api")));
        listener.initReactor(session);

        listener.fail(new RuntimeException("build error"));
    }

    @Test
    void testTruncateAnsiPlainText() {
        String truncated = RichBuildEventListener.truncateAnsi("hello world", 5);
        // When truncated, a RESET escape is appended to close any open styling
        assertTrue(truncated.startsWith("hello"), "Should start with 'hello': " + truncated);
        // Should not contain characters beyond "hello" (except ANSI reset)
        String stripped = truncated.replaceAll("\033\\[[^a-zA-Z]*[a-zA-Z]", "");
        assertEquals("hello", stripped);
    }

    @Test
    void testTruncateAnsiPreservesEscapeSequences() {
        // ANSI color codes should not count toward visible length
        String colored = "\033[1mhello\033[0m world";
        String truncated = RichBuildEventListener.truncateAnsi(colored, 5);
        // Should keep "hello" (5 visible chars) with the bold prefix
        assertTrue(truncated.contains("hello"), "Truncated should contain 'hello': " + truncated);
        assertTrue(truncated.contains("\033[1m"), "Truncated should preserve ANSI prefix");
    }

    @Test
    void testTruncateAnsiNoTruncationNeeded() {
        String s = "short";
        assertEquals(s, RichBuildEventListener.truncateAnsi(s, 100));
    }

    // ---- Helpers ----

    private static MavenProject createProject(String artifactId) {
        MavenProject project = mock(MavenProject.class);
        lenient().when(project.getArtifactId()).thenReturn(artifactId);
        lenient().when(project.getName()).thenReturn(artifactId);
        lenient().when(project.getPackaging()).thenReturn("jar");
        lenient().when(project.getVersion()).thenReturn("4.1.0-SNAPSHOT");
        return project;
    }

    private static MavenSession createSession(List<MavenProject> projects) {
        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(projects);
        when(session.getAllProjects()).thenReturn(projects);
        MavenExecutionRequest request = mock(MavenExecutionRequest.class);
        lenient().when(request.getDegreeOfConcurrency()).thenReturn(1);
        lenient().when(session.getRequest()).thenReturn(request);
        return session;
    }
}
