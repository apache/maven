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
package org.apache.maven.cli.event;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExecutionEventLoggerTest {
    private MockitoSession mockitoSession;

    private ExecutionEventLogger executionEventLogger;

    private Logger logger;

    @BeforeAll
    public static void setUpBeforeAll() {
        MessageUtils.setColorEnabled(false);
    }

    @AfterAll
    public static void tearDownAfterAll() {
        MessageUtils.setColorEnabled(true);
    }

    @BeforeEach
    public void setup() {
        mockitoSession = Mockito.mockitoSession().startMocking();
        logger = mock(Logger.class);
        when(logger.isInfoEnabled()).thenReturn(true);
        executionEventLogger = new ExecutionEventLogger(logger);
    }

    @AfterEach
    public void afterEach() {
        mockitoSession.finishMocking();
    }

    @Test
    public void testProjectStarted() {
        // prepare
        File basedir = new File("").getAbsoluteFile();
        ExecutionEvent event = mock(ExecutionEvent.class);
        MavenProject project = mock(MavenProject.class);
        when(project.getGroupId()).thenReturn("org.apache.maven");
        when(project.getArtifactId()).thenReturn("maven-embedder");
        when(project.getPackaging()).thenReturn("jar");
        when(project.getName()).thenReturn("Apache Maven Embedder");
        when(project.getVersion()).thenReturn("3.5.4-SNAPSHOT");
        when(project.getFile()).thenReturn(new File(basedir, "maven-embedder/pom.xml"));
        when(event.getProject()).thenReturn(project);

        MavenProject rootProject = mock(MavenProject.class);
        when(rootProject.getBasedir()).thenReturn(basedir);
        MavenSession session = mock(MavenSession.class);
        when(session.getTopLevelProject()).thenReturn(rootProject);
        when(event.getSession()).thenReturn(session);

        // execute
        executionEventLogger.projectStarted(event);

        // verify
        InOrder inOrder = inOrder(logger);
        inOrder.verify(logger).info("");
        inOrder.verify(logger).info("------------------< org.apache.maven:maven-embedder >-------------------");
        inOrder.verify(logger).info("Building Apache Maven Embedder 3.5.4-SNAPSHOT");
        inOrder.verify(logger).info(adaptDirSeparator("  from maven-embedder/pom.xml"));
        inOrder.verify(logger).info("--------------------------------[ jar ]---------------------------------");
    }

    @Test
    public void testProjectStartedOverflow() {
        // prepare
        File basedir = new File("").getAbsoluteFile();
        ExecutionEvent event = mock(ExecutionEvent.class);
        MavenProject project = mock(MavenProject.class);
        when(project.getGroupId()).thenReturn("org.apache.maven.plugins.overflow");
        when(project.getArtifactId()).thenReturn("maven-project-info-reports-plugin");
        when(project.getPackaging()).thenReturn("maven-plugin");
        when(project.getName()).thenReturn("Apache Maven Project Info Reports Plugin");
        when(project.getVersion()).thenReturn("3.0.0-SNAPSHOT");
        when(event.getProject()).thenReturn(project);
        when(project.getFile()).thenReturn(new File(basedir, "pom.xml"));
        when(project.getBasedir()).thenReturn(basedir);

        MavenSession session = mock(MavenSession.class);
        when(session.getTopLevelProject()).thenReturn(project);
        when(event.getSession()).thenReturn(session);

        // execute
        executionEventLogger.projectStarted(event);

        // verify
        InOrder inOrder = inOrder(logger);
        inOrder.verify(logger).info("");
        inOrder.verify(logger).info("--< org.apache.maven.plugins.overflow:maven-project-info-reports-plugin >--");
        inOrder.verify(logger).info("Building Apache Maven Project Info Reports Plugin 3.0.0-SNAPSHOT");
        inOrder.verify(logger).info(adaptDirSeparator("  from pom.xml"));
        inOrder.verify(logger).info("----------------------------[ maven-plugin ]----------------------------");
    }

    @Test
    public void testProjectStartedNoPom() {
        // prepare
        ExecutionEvent event = mock(ExecutionEvent.class);
        MavenProject project = mock(MavenProject.class);
        when(project.getGroupId()).thenReturn("org.apache.maven");
        when(project.getArtifactId()).thenReturn("standalone-pom");
        when(project.getPackaging()).thenReturn("pom");
        when(project.getName()).thenReturn("Maven Stub Project (No POM)");
        when(project.getVersion()).thenReturn("1");
        when(event.getProject()).thenReturn(project);
        when(project.getFile()).thenReturn(null);

        // execute
        executionEventLogger.projectStarted(event);

        // verify
        InOrder inOrder = inOrder(logger);
        inOrder.verify(logger).info("");
        inOrder.verify(logger).info("------------------< org.apache.maven:standalone-pom >-------------------");
        inOrder.verify(logger).info("Building Maven Stub Project (No POM) 1");
        inOrder.verify(logger).info("--------------------------------[ pom ]---------------------------------");
    }

    @Test
    public void testSessionEndedSingleProject() {
        // prepare
        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();

        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        executionRequest.setStartTime(new Date());

        MavenSession mavenSession = mock(MavenSession.class);
        when(mavenSession.getResult()).thenReturn(executionResult);
        when(mavenSession.getRequest()).thenReturn(executionRequest);

        ExecutionEvent event = mock(ExecutionEvent.class);
        when(event.getSession()).thenReturn(mavenSession);

        // execute
        executionEventLogger.sessionEnded(event);

        // verify
        InOrder inOrder = inOrder(logger);
        inOrder.verify(logger).info("------------------------------------------------------------------------");
        inOrder.verify(logger).info("BUILD SUCCESS");
        inOrder.verify(logger).info("------------------------------------------------------------------------");
        inOrder.verify(logger).info(eq("Total time:  {}{}"), anyString(), anyString());
        inOrder.verify(logger).info(eq("Finished at: {}"), anyString());
        inOrder.verify(logger).info("------------------------------------------------------------------------");
    }

    @Test
    public void testSessionEndedSuccessMultimodule() {
        // prepare
        MavenProject project1 = aProject("artifact1");
        MavenProject project2 = aProject("artifact2");
        MavenProject project3 = aProject("artifact3");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildSuccess(project1, 1000));
        executionResult.addBuildSummary(new BuildSuccess(project2, 2000));
        executionResult.addBuildSummary(new BuildSuccess(project3, 3000));

        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        executionRequest.setStartTime(new Date());

        ProjectDependencyGraph projectDependencyGraph = mock(ProjectDependencyGraph.class);
        when(projectDependencyGraph.getSortedProjects()).thenReturn(Arrays.asList(project1, project2, project3));

        MavenSession mavenSession = mock(MavenSession.class);
        when(mavenSession.getResult()).thenReturn(executionResult);
        when(mavenSession.getRequest()).thenReturn(executionRequest);
        when(mavenSession.getProjects()).thenReturn(Arrays.asList(project1, project2, project3));
        when(mavenSession.getTopLevelProject()).thenReturn(project1);
        when(mavenSession.getProjectDependencyGraph()).thenReturn(projectDependencyGraph);

        ExecutionEvent event = mock(ExecutionEvent.class);
        when(event.getSession()).thenReturn(mavenSession);

        // execute
        executionEventLogger.sessionEnded(event);

        // verify
        InOrder inOrder = inOrder(logger);
        inOrder.verify(logger).info("------------------------------------------------------------------------");
        inOrder.verify(logger).info("Reactor Summary for Maven Project artifact1 1.0.0-SNAPSHOT:");
        inOrder.verify(logger).info("");
        inOrder.verify(logger).info("Maven Project artifact1 ............................ SUCCESS [  1.000 s]");
        inOrder.verify(logger).info("Maven Project artifact2 ............................ SUCCESS [  2.000 s]");
        inOrder.verify(logger).info("Maven Project artifact3 ............................ SUCCESS [  3.000 s]");
        inOrder.verify(logger).info("------------------------------------------------------------------------");
        inOrder.verify(logger).info("BUILD SUCCESS");
        inOrder.verify(logger).info("------------------------------------------------------------------------");
        inOrder.verify(logger).info(eq("Total time:  {}{}"), anyString(), anyString());
        inOrder.verify(logger).info(eq("Finished at: {}"), anyString());
        inOrder.verify(logger).info("------------------------------------------------------------------------");
    }

    @Test
    public void testSessionEndedFailureMultimodule() {
        // prepare
        MavenProject project1 = aProject("artifact1");
        MavenProject project2 = aProject("artifact2");
        MavenProject project3 = aProject("artifact3");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildSuccess(project1, 1000));
        executionResult.addBuildSummary(new BuildFailure(project2, 2000, new Exception("Failure")));
        executionResult.addException(new Exception("Failure"));

        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        executionRequest.setStartTime(new Date());

        ProjectDependencyGraph projectDependencyGraph = mock(ProjectDependencyGraph.class);
        when(projectDependencyGraph.getSortedProjects()).thenReturn(Arrays.asList(project1, project2, project3));

        MavenSession mavenSession = mock(MavenSession.class);
        when(mavenSession.getResult()).thenReturn(executionResult);
        when(mavenSession.getRequest()).thenReturn(executionRequest);
        when(mavenSession.getProjects()).thenReturn(Arrays.asList(project1, project2, project3));
        when(mavenSession.getTopLevelProject()).thenReturn(project1);
        when(mavenSession.getProjectDependencyGraph()).thenReturn(projectDependencyGraph);

        ExecutionEvent event = mock(ExecutionEvent.class);
        when(event.getSession()).thenReturn(mavenSession);

        // execute
        executionEventLogger.sessionEnded(event);

        // verify
        InOrder inOrder = inOrder(logger);
        inOrder.verify(logger).info("------------------------------------------------------------------------");
        inOrder.verify(logger).info("Reactor Summary for Maven Project artifact1 1.0.0-SNAPSHOT:");
        inOrder.verify(logger).info("");
        inOrder.verify(logger).info("...");
        inOrder.verify(logger).info("Maven Project artifact2 ............................ FAILURE [  2.000 s]");
        inOrder.verify(logger).info("...");
        inOrder.verify(logger).info("------------------------------------------------------------------------");
        inOrder.verify(logger).info("BUILD FAILURE");
        inOrder.verify(logger).info("------------------------------------------------------------------------");
        inOrder.verify(logger).info(eq("Total time:  {}{}"), anyString(), anyString());
        inOrder.verify(logger).info(eq("Finished at: {}"), anyString());
        inOrder.verify(logger).info("------------------------------------------------------------------------");
    }

    @Test
    public void testSessionEndedFailureMultimoduleWithSeparatedFailures() {
        // prepare
        MavenProject project1 = aProject("artifact1");
        MavenProject project2 = aProject("artifact2");
        MavenProject project3 = aProject("artifact3");
        MavenProject project4 = aProject("artifact4");
        MavenProject project5 = aProject("artifact5");
        MavenProject project6 = aProject("artifact6");

        MavenExecutionResult executionResult = new DefaultMavenExecutionResult();
        executionResult.addBuildSummary(new BuildSuccess(project1, 1000));
        executionResult.addBuildSummary(new BuildFailure(project2, 2000, new Exception("Failure 1")));
        executionResult.addBuildSummary(new BuildSuccess(project3, 3000));
        executionResult.addBuildSummary(new BuildSuccess(project4, 4000));
        executionResult.addBuildSummary(new BuildFailure(project5, 5000, new Exception("Failure 2")));
        executionResult.addException(new Exception("Failure 1"));
        executionResult.addException(new Exception("Failure 2"));

        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        executionRequest.setStartTime(new Date());

        ProjectDependencyGraph projectDependencyGraph = mock(ProjectDependencyGraph.class);
        when(projectDependencyGraph.getSortedProjects())
                .thenReturn(Arrays.asList(project1, project2, project3, project4, project5, project6));

        MavenSession mavenSession = mock(MavenSession.class);
        when(mavenSession.getResult()).thenReturn(executionResult);
        when(mavenSession.getRequest()).thenReturn(executionRequest);
        when(mavenSession.getProjects())
                .thenReturn(Arrays.asList(project1, project2, project3, project4, project5, project6));
        when(mavenSession.getTopLevelProject()).thenReturn(project1);
        when(mavenSession.getProjectDependencyGraph()).thenReturn(projectDependencyGraph);

        ExecutionEvent event = mock(ExecutionEvent.class);
        when(event.getSession()).thenReturn(mavenSession);

        // execute
        executionEventLogger.sessionEnded(event);

        // verify
        InOrder inOrder = inOrder(logger);
        inOrder.verify(logger).info("------------------------------------------------------------------------");
        inOrder.verify(logger).info("Reactor Summary for Maven Project artifact1 1.0.0-SNAPSHOT:");
        inOrder.verify(logger).info("");
        inOrder.verify(logger).info("...");
        inOrder.verify(logger).info("Maven Project artifact2 ............................ FAILURE [  2.000 s]");
        inOrder.verify(logger).info("...");
        inOrder.verify(logger).info("Maven Project artifact5 ............................ FAILURE [  5.000 s]");
        inOrder.verify(logger).info("...");
        inOrder.verify(logger).info("------------------------------------------------------------------------");
        inOrder.verify(logger).info("BUILD FAILURE");
        inOrder.verify(logger).info("------------------------------------------------------------------------");
        inOrder.verify(logger).info(eq("Total time:  {}{}"), anyString(), anyString());
        inOrder.verify(logger).info(eq("Finished at: {}"), anyString());
        inOrder.verify(logger).info("------------------------------------------------------------------------");
    }

    private MavenProject aProject(String artifactId) {
        MavenProject project = mock(MavenProject.class);
        lenient().when(project.getGroupId()).thenReturn("org.apache.maven");
        lenient().when(project.getArtifactId()).thenReturn(artifactId);
        lenient().when(project.getName()).thenReturn("Maven Project " + artifactId);
        lenient().when(project.getVersion()).thenReturn("1.0.0-SNAPSHOT");

        return project;
    }

    private static String adaptDirSeparator(String path) {
        return FilenameUtils.separatorsToSystem(path);
    }
}
