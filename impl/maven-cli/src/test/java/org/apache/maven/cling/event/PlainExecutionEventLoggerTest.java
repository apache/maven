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

import java.util.List;

import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.jline.JLineMessageBuilderFactory;
import org.apache.maven.jline.MessageUtils;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.slf4j.Logger;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PlainExecutionEventLogger} — the compact one-line-per-module renderer.
 */
class PlainExecutionEventLoggerTest {

    private MockitoSession mockitoSession;

    private Logger logger;
    private PlainExecutionEventLogger plainLogger;
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
    void beforeEach() {
        mockitoSession = Mockito.mockitoSession().startMocking();
        logger = mock(Logger.class);
        lenient().when(logger.isInfoEnabled()).thenReturn(true);
        lenient().when(logger.isWarnEnabled()).thenReturn(true);
        plainLogger = new PlainExecutionEventLogger(messageBuilderFactory, logger);
    }

    @AfterEach
    void afterEach() {
        mockitoSession.finishMocking();
    }

    @Test
    void testProjectStartedSuppressed() {
        // In plain mode, projectStarted should produce NO output
        ExecutionEvent event = mock(ExecutionEvent.class);

        plainLogger.projectStarted(event);

        // Verify no logger calls were made
        verify(logger, never()).info(anyString());
    }

    @Test
    void testMojoStartedSuppressed() {
        // In plain mode, mojoStarted should produce NO output
        ExecutionEvent event = mock(ExecutionEvent.class);

        plainLogger.mojoStarted(event);

        verify(logger, never()).info(anyString());
    }

    @Test
    void testSingleProjectSucceeded() {
        MavenProject project = generateMavenProject("Maven Core");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildSuccess(project, 2100));

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project));
        when(session.getAllProjects()).thenReturn(List.of(project));
        when(session.getResult()).thenReturn(executionResult);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);

        ExecutionEvent projectEvent = mock(ExecutionEvent.class);
        when(projectEvent.getProject()).thenReturn(project);
        when(projectEvent.getSession()).thenReturn(session);

        plainLogger.sessionStarted(sessionEvent);
        plainLogger.projectSucceeded(projectEvent);

        // Single project: no progress counter
        verify(logger).info(matches("Maven Core.*SUCCESS.*2\\.1"));
    }

    @Test
    void testMultiModuleProjectOneLinePerModule() {
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
        when(session.getResult()).thenReturn(executionResult);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);
        plainLogger.sessionStarted(sessionEvent);

        // Simulate lifecycle
        ExecutionEvent event1 = mockProjectEvent(project1, session);
        ExecutionEvent event2 = mockProjectEvent(project2, session);
        ExecutionEvent event3 = mockProjectEvent(project3, session);

        plainLogger.projectSucceeded(event1);
        plainLogger.projectSucceeded(event2);
        plainLogger.projectSucceeded(event3);

        // Multi-module: each line has progress counter [n/total]
        InOrder inOrder = inOrder(logger);
        inOrder.verify(logger).info(matches("API.*\\[1/3\\].*SUCCESS.*1\\.0"));
        inOrder.verify(logger).info(matches("Core.*\\[2/3\\].*SUCCESS.*3\\.0"));
        inOrder.verify(logger).info(matches("CLI.*\\[3/3\\].*SUCCESS.*2\\.0"));
    }

    @Test
    void testProjectFailedShowsFailure() {
        MavenProject project = generateMavenProject("Core");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildFailure(project, 5000, new Exception("Compile error")));

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project));
        when(session.getAllProjects()).thenReturn(List.of(project));
        when(session.getResult()).thenReturn(executionResult);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);
        plainLogger.sessionStarted(sessionEvent);

        ExecutionEvent event = mockProjectEvent(project, session);
        plainLogger.projectFailed(event);

        verify(logger).info(matches("Core.*FAILURE.*5\\.0"));
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

        plainLogger.sessionStarted(sessionEvent);
        plainLogger.sessionEnded(sessionEvent);

        // Verify BUILD SUCCESS and stats
        InOrder inOrder = inOrder(logger);
        inOrder.verify(logger).info("");
        inOrder.verify(logger).info("BUILD SUCCESS");
        inOrder.verify(logger).info(matches("2 modules.*2 passed"));
        inOrder.verify(logger).info(eq("Total time:  {}{}"), anyString(), anyString());
        inOrder.verify(logger).info("Full report: target/build-reports/build-report-latest.json");
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

        plainLogger.sessionStarted(sessionEvent);
        plainLogger.sessionEnded(sessionEvent);

        InOrder inOrder = inOrder(logger);
        inOrder.verify(logger).info("");
        inOrder.verify(logger).info("BUILD FAILURE");
        inOrder.verify(logger).info(matches("3 modules.*1 passed.*1 failed.*1 skipped"));
    }

    @Test
    void testMojoSkippedStillWarns() {
        ExecutionEvent event = mock(ExecutionEvent.class);
        var mojoExec = mock(org.apache.maven.plugin.MojoExecution.class);
        when(mojoExec.getGoal()).thenReturn("deploy");
        when(event.getMojoExecution()).thenReturn(mojoExec);

        plainLogger.mojoSkipped(event);

        verify(logger).warn(anyString(), eq("deploy"));
    }

    @Test
    void testResumeFromProgress() {
        // When resuming, allProjects > projects (some already built)
        MavenProject project1 = generateMavenProject("API");
        MavenProject project2 = generateMavenProject("Core");
        MavenProject project3 = generateMavenProject("CLI");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildSuccess(project2, 1000));
        executionResult.addBuildSummary(new BuildSuccess(project3, 2000));

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project2, project3)); // resumed from project2
        when(session.getAllProjects()).thenReturn(List.of(project1, project2, project3));
        when(session.getResult()).thenReturn(executionResult);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);
        plainLogger.sessionStarted(sessionEvent);

        ExecutionEvent event2 = mockProjectEvent(project2, session);
        ExecutionEvent event3 = mockProjectEvent(project3, session);

        plainLogger.projectSucceeded(event2);
        plainLogger.projectSucceeded(event3);

        // Progress should start from 2/3, not 1/3
        InOrder inOrder = inOrder(logger);
        inOrder.verify(logger).info(matches("Core.*\\[2/3\\].*SUCCESS"));
        inOrder.verify(logger).info(matches("CLI.*\\[3/3\\].*SUCCESS"));
    }

    // ---- Helpers ----

    private static MavenProject generateMavenProject(String projectName) {
        MavenProject project = mock(MavenProject.class);
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
