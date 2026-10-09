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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
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
 * Tests for {@link MachineExecutionEventLogger}.
 */
class MachineExecutionEventLoggerTest {

    private MockitoSession mockitoSession;
    private List<String> capturedOutput;
    private MachineBuildEventListener machineBel;
    private MachineExecutionEventLogger logger;

    @BeforeEach
    void beforeEach() {
        mockitoSession = Mockito.mockitoSession().startMocking();
        capturedOutput = new ArrayList<>();
        machineBel = new MachineBuildEventListener(capturedOutput::add);
        logger = new MachineExecutionEventLogger(machineBel);
    }

    @AfterEach
    void afterEach() {
        mockitoSession.finishMocking();
    }

    @Test
    void testSessionStartedEmitsBuildStarted() {
        MavenProject project1 = createProject("API", "api");
        MavenProject project2 = createProject("Core", "core");

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setGoals(List.of("clean", "install"));

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project1, project2));
        when(session.getAllProjects()).thenReturn(List.of(project1, project2));
        when(session.getRequest()).thenReturn(request);

        ExecutionEvent event = mock(ExecutionEvent.class);
        when(event.getSession()).thenReturn(session);

        logger.sessionStarted(event);

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.contains("\"event\":\"build.started\""));
        assertTrue(json.contains("\"projectCount\":2"));
        assertTrue(json.contains("\"goals\":\"clean install\""));
    }

    @Test
    void testSessionEndedEmitsBuildFinished() {
        MavenProject project1 = createProject("API", "api");
        MavenProject project2 = createProject("Core", "core");

        MavenExecutionResult result = new DefaultMavenExecutionResult();
        result.addBuildSummary(new BuildSuccess(project1, 1000));
        result.addBuildSummary(new BuildSuccess(project2, 2000));

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setGoals(List.of("install"));

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project1, project2));
        when(session.getAllProjects()).thenReturn(List.of(project1, project2));
        when(session.getResult()).thenReturn(result);
        when(session.getRequest()).thenReturn(request);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);

        logger.sessionStarted(sessionEvent);
        capturedOutput.clear();

        logger.sessionEnded(sessionEvent);

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.contains("\"event\":\"build.finished\""));
        assertTrue(json.contains("\"status\":\"SUCCESS\""));
        assertTrue(json.contains("\"total\":2"));
        assertTrue(json.contains("\"passed\":2"));
        assertTrue(json.contains("\"failed\":0"));
        assertTrue(json.contains("\"skipped\":0"));
        assertTrue(json.contains("\"duration\":"));
    }

    @Test
    void testSessionEndedWithFailures() {
        MavenProject project1 = createProject("API", "api");
        MavenProject project2 = createProject("Core", "core");
        MavenProject project3 = createProject("CLI", "cli");

        MavenExecutionResult result = new DefaultMavenExecutionResult();
        result.addBuildSummary(new BuildSuccess(project1, 1000));
        result.addBuildSummary(new BuildFailure(project2, 2000, new Exception("Compile error")));
        result.addException(new Exception("Compile error"));

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setGoals(List.of("install"));

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project1, project2, project3));
        when(session.getAllProjects()).thenReturn(List.of(project1, project2, project3));
        when(session.getResult()).thenReturn(result);
        when(session.getRequest()).thenReturn(request);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);

        logger.sessionStarted(sessionEvent);
        capturedOutput.clear();

        logger.sessionEnded(sessionEvent);

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.contains("\"status\":\"FAILURE\""));
        assertTrue(json.contains("\"passed\":1"));
        assertTrue(json.contains("\"failed\":1"));
        assertTrue(json.contains("\"skipped\":1"));
    }

    @Test
    void testProjectStartedEmitsModuleStarted() {
        MavenProject project = createProject("Maven Core", "maven-core");
        setupSessionForProjectEvents(project);

        ExecutionEvent event = mock(ExecutionEvent.class);
        when(event.getProject()).thenReturn(project);

        logger.projectStarted(event);

        // build.started + module.started
        assertEquals(2, capturedOutput.size());
        String json = capturedOutput.get(1);
        assertTrue(json.contains("\"event\":\"module.started\""));
        assertTrue(json.contains("\"module\":\"Maven Core\""));
        assertTrue(json.contains("\"groupId\":\"org.apache.maven\""));
        assertTrue(json.contains("\"artifactId\":\"maven-core\""));
        assertTrue(json.contains("\"version\":\"4.1.0-SNAPSHOT\""));
        assertTrue(json.contains("\"index\":1"));
        assertTrue(json.contains("\"total\":1"));
    }

    @Test
    void testProjectSucceededEmitsModuleSucceeded() {
        MavenProject project = createProject("Core", "core");
        MavenSession session = setupSessionForProjectEvents(project);

        MavenExecutionResult result = new DefaultMavenExecutionResult();
        result.addBuildSummary(new BuildSuccess(project, 2100));
        when(session.getResult()).thenReturn(result);

        ExecutionEvent event = mock(ExecutionEvent.class);
        when(event.getProject()).thenReturn(project);
        when(event.getSession()).thenReturn(session);

        logger.projectSucceeded(event);

        // build.started + module.succeeded
        assertEquals(2, capturedOutput.size());
        String json = capturedOutput.get(1);
        assertTrue(json.contains("\"event\":\"module.succeeded\""));
        assertTrue(json.contains("\"module\":\"Core\""));
        assertTrue(json.contains("\"duration\":2.1"));
    }

    @Test
    void testProjectFailedEmitsModuleFailed() {
        MavenProject project = createProject("Core", "core");
        MavenSession session = setupSessionForProjectEvents(project);

        MavenExecutionResult result = new DefaultMavenExecutionResult();
        result.addBuildSummary(new BuildFailure(project, 5000, new Exception("Compile error")));
        when(session.getResult()).thenReturn(result);

        ExecutionEvent event = mock(ExecutionEvent.class);
        when(event.getProject()).thenReturn(project);
        when(event.getSession()).thenReturn(session);
        when(event.getException()).thenReturn(new Exception("Compile error"));

        logger.projectFailed(event);

        assertEquals(2, capturedOutput.size());
        String json = capturedOutput.get(1);
        assertTrue(json.contains("\"event\":\"module.failed\""));
        assertTrue(json.contains("\"duration\":5.0"));
        assertTrue(json.contains("\"error\":\"Compile error\""));
    }

    @Test
    void testMojoStartedEmitsJsonLine() {
        MavenProject project = createProject("Core", "core");
        MojoExecution mojo = createMojoExecution("maven-compiler-plugin", "compile", "compile", "default-compile");

        ExecutionEvent event = mock(ExecutionEvent.class);
        when(event.getProject()).thenReturn(project);
        when(event.getMojoExecution()).thenReturn(mojo);

        logger.mojoStarted(event);

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.contains("\"event\":\"mojo.started\""));
        assertTrue(json.contains("\"module\":\"Core\""));
        assertTrue(json.contains("\"plugin\":\"maven-compiler-plugin\""));
        assertTrue(json.contains("\"goal\":\"compile\""));
        assertTrue(json.contains("\"phase\":\"compile\""));
        assertTrue(json.contains("\"executionId\":\"default-compile\""));
    }

    @Test
    void testMojoSucceededIncludesDuration() {
        MavenProject project = createProject("Core", "core");
        MojoExecution mojo = createMojoExecution("maven-compiler-plugin", "compile", "compile", "default-compile");

        ExecutionEvent startEvent = mock(ExecutionEvent.class);
        when(startEvent.getProject()).thenReturn(project);
        when(startEvent.getMojoExecution()).thenReturn(mojo);

        ExecutionEvent endEvent = mock(ExecutionEvent.class);
        when(endEvent.getProject()).thenReturn(project);
        when(endEvent.getMojoExecution()).thenReturn(mojo);

        logger.mojoStarted(startEvent);
        capturedOutput.clear();

        logger.mojoSucceeded(endEvent);

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.contains("\"event\":\"mojo.succeeded\""));
        assertTrue(json.contains("\"duration\":"));
    }

    @Test
    void testMojoFailedIncludesError() {
        MavenProject project = createProject("Core", "core");
        MojoExecution mojo = createMojoExecution("maven-compiler-plugin", "compile", "compile", "default-compile");

        ExecutionEvent startEvent = mock(ExecutionEvent.class);
        when(startEvent.getProject()).thenReturn(project);
        when(startEvent.getMojoExecution()).thenReturn(mojo);

        ExecutionEvent endEvent = mock(ExecutionEvent.class);
        when(endEvent.getProject()).thenReturn(project);
        when(endEvent.getMojoExecution()).thenReturn(mojo);
        when(endEvent.getException()).thenReturn(new Exception("Cannot find symbol: class Foo"));

        logger.mojoStarted(startEvent);
        capturedOutput.clear();

        logger.mojoFailed(endEvent);

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.contains("\"event\":\"mojo.failed\""));
        assertTrue(json.contains("\"error\":\"Cannot find symbol: class Foo\""));
    }

    @Test
    void testMojoSkippedEmitsJsonLine() {
        MavenProject project = createProject("Core", "core");
        MojoExecution mojo = createMojoExecution("maven-deploy-plugin", "deploy", "deploy", "default-deploy");

        ExecutionEvent event = mock(ExecutionEvent.class);
        when(event.getProject()).thenReturn(project);
        when(event.getMojoExecution()).thenReturn(mojo);

        logger.mojoSkipped(event);

        assertEquals(1, capturedOutput.size());
        String json = capturedOutput.get(0);
        assertTrue(json.contains("\"event\":\"mojo.skipped\""));
        assertTrue(json.contains("\"plugin\":\"maven-deploy-plugin\""));
        assertTrue(json.contains("\"goal\":\"deploy\""));
    }

    @Test
    void testMultiModuleLifecycle() {
        MavenProject project1 = createProject("API", "api");
        MavenProject project2 = createProject("Core", "core");
        MavenProject project3 = createProject("CLI", "cli");

        MavenExecutionResult result = new DefaultMavenExecutionResult();
        result.addBuildSummary(new BuildSuccess(project1, 1000));
        result.addBuildSummary(new BuildSuccess(project2, 3000));
        result.addBuildSummary(new BuildSuccess(project3, 2000));

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setGoals(List.of("install"));

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project1, project2, project3));
        when(session.getAllProjects()).thenReturn(List.of(project1, project2, project3));
        when(session.getResult()).thenReturn(result);
        when(session.getRequest()).thenReturn(request);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);

        // Full lifecycle
        logger.sessionStarted(sessionEvent);

        for (MavenProject p : List.of(project1, project2, project3)) {
            ExecutionEvent pe = mock(ExecutionEvent.class);
            when(pe.getProject()).thenReturn(p);
            when(pe.getSession()).thenReturn(session);
            logger.projectStarted(pe);
            logger.projectSucceeded(pe);
        }

        logger.sessionEnded(sessionEvent);

        // build.started + 3*(module.started + module.succeeded) + build.finished = 8
        assertEquals(8, capturedOutput.size());

        // First event is build.started
        assertTrue(capturedOutput.get(0).contains("\"event\":\"build.started\""));
        // Check module indices
        assertTrue(capturedOutput.get(1).contains("\"index\":1"));
        assertTrue(capturedOutput.get(3).contains("\"index\":2"));
        assertTrue(capturedOutput.get(5).contains("\"index\":3"));
        // Last event is build.finished
        assertTrue(capturedOutput.get(7).contains("\"event\":\"build.finished\""));
    }

    @Test
    void testAllOutputIsValidJsonLines() {
        MavenProject project = createProject("Core", "core");
        MavenSession session = setupSessionForProjectEvents(project);

        MavenExecutionResult result = new DefaultMavenExecutionResult();
        result.addBuildSummary(new BuildSuccess(project, 1000));
        when(session.getResult()).thenReturn(result);

        MojoExecution mojo = createMojoExecution("maven-compiler-plugin", "compile", "compile", "default-compile");

        ExecutionEvent projectEvent = mock(ExecutionEvent.class);
        when(projectEvent.getProject()).thenReturn(project);
        when(projectEvent.getSession()).thenReturn(session);
        when(projectEvent.getMojoExecution()).thenReturn(mojo);

        // Generate various events
        logger.projectStarted(projectEvent);
        logger.mojoStarted(projectEvent);
        machineBel.log("Some log message");
        logger.mojoSucceeded(projectEvent);
        logger.projectSucceeded(projectEvent);

        // All lines should be valid JSON (start with { and end with })
        for (String line : capturedOutput) {
            assertTrue(line.startsWith("{"), "Should start with {: " + line);
            assertTrue(line.endsWith("}"), "Should end with }: " + line);
            // Should not contain raw newlines
            assertFalse(line.contains("\n"), "Should not contain raw newlines: " + line);
            assertFalse(line.contains("\r"), "Should not contain raw carriage returns: " + line);
        }
    }

    // ---- Helpers ----

    private static MavenProject createProject(String name, String artifactId) {
        MavenProject project = mock(MavenProject.class);
        lenient().when(project.getName()).thenReturn(name);
        lenient().when(project.getArtifactId()).thenReturn(artifactId);
        lenient().when(project.getGroupId()).thenReturn("org.apache.maven");
        lenient().when(project.getVersion()).thenReturn("4.1.0-SNAPSHOT");
        lenient().when(project.getPackaging()).thenReturn("jar");
        return project;
    }

    private static MojoExecution createMojoExecution(String artifactId, String goal, String phase, String executionId) {
        MojoExecution mojo = mock(MojoExecution.class);
        lenient().when(mojo.getArtifactId()).thenReturn(artifactId);
        lenient().when(mojo.getGoal()).thenReturn(goal);
        lenient().when(mojo.getLifecyclePhase()).thenReturn(phase);
        lenient().when(mojo.getExecutionId()).thenReturn(executionId);
        return mojo;
    }

    private MavenSession setupSessionForProjectEvents(MavenProject project) {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setGoals(List.of("install"));

        MavenExecutionResult result = new DefaultMavenExecutionResult();

        MavenSession session = mock(MavenSession.class);
        when(session.getProjects()).thenReturn(List.of(project));
        when(session.getAllProjects()).thenReturn(List.of(project));
        lenient().when(session.getResult()).thenReturn(result);
        when(session.getRequest()).thenReturn(request);

        ExecutionEvent sessionEvent = mock(ExecutionEvent.class);
        when(sessionEvent.getSession()).thenReturn(session);
        logger.sessionStarted(sessionEvent);

        return session;
    }
}
