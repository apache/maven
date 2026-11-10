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
package org.apache.maven.internal.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.LifecyclePlan;
import org.apache.maven.api.services.LifecycleExecutionRequest;
import org.apache.maven.api.services.LifecycleExecutionResult;
import org.apache.maven.api.services.LifecycleExecutorException;
import org.apache.maven.api.services.LifecyclePlanRequest;
import org.apache.maven.api.services.LifecyclePlanResult;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.LifecycleTask;
import org.apache.maven.lifecycle.internal.LifecycleTaskSegmentCalculator;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.lifecycle.internal.concurrent.BuildPlan;
import org.apache.maven.lifecycle.internal.concurrent.BuildPlanExecutor;
import org.apache.maven.lifecycle.internal.concurrent.BuildStep;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultLifecycleExecutorTest {

    private InternalMavenSession session;
    private MavenSession mavenSession;
    private LifecycleTaskSegmentCalculator taskSegmentCalculator;
    private BuildPlanExecutor buildPlanExecutor;
    private MavenProject project;
    private MavenProject previousProject;
    private DefaultProject apiProject;
    private MojoExecution compile;
    private MojoExecution test;
    private DefaultLifecycleExecutor executor;
    private BuildPlan buildPlan;
    private BuildPlanExecutor.BuildContext buildContext;

    @BeforeEach
    void setUp() throws Exception {
        session = mock(InternalMavenSession.class);
        mavenSession = mock(MavenSession.class);
        taskSegmentCalculator = mock(LifecycleTaskSegmentCalculator.class);
        buildPlanExecutor = mock(BuildPlanExecutor.class);
        project = new MavenProject();
        previousProject = new MavenProject();
        apiProject = mock(DefaultProject.class);
        compile = execution("compile", "compile");
        test = execution("test", "test");
        TaskSegment segment = new TaskSegment(false, new LifecycleTask("verify"));

        when(session.getMavenSession()).thenReturn(mavenSession);
        when(session.getService(Lookup.class)).thenReturn(mock(Lookup.class));
        when(mavenSession.getCurrentProject()).thenReturn(previousProject);
        when(apiProject.getProject()).thenReturn(project);
        when(taskSegmentCalculator.calculateTaskSegments(mavenSession, List.of("verify")))
                .thenReturn(List.of(segment));

        // Create a BuildPlan with steps containing the mojos
        buildPlan = new BuildPlan(Collections.singletonMap(project, Collections.emptyList()));
        Map<String, BuildStep> steps = new java.util.LinkedHashMap<>();
        BuildStep compileStep = new BuildStep("compile", project, null);
        compileStep.addMojo(compile, 0);
        steps.put("compile", compileStep);
        BuildStep testStep = new BuildStep("test", project, null);
        testStep.addMojo(test, 0);
        compileStep.executeBefore(testStep);
        steps.put("test", testStep);
        buildPlan.addProject(project, steps);

        buildContext = mock(BuildPlanExecutor.BuildContext.class);
        when(buildContext.getPlan()).thenReturn(buildPlan);
        when(buildPlanExecutor.createProjectContext(same(mavenSession), same(project), anyList()))
                .thenReturn(buildContext);

        executor = new DefaultLifecycleExecutor(taskSegmentCalculator, buildPlanExecutor);
    }

    @Test
    void plansInspectsAndExecutesFilteredMojos() throws Exception {
        LifecyclePlanResult planResult =
                executor.plan(LifecyclePlanRequest.build(session, apiProject, List.of("verify")));
        LifecyclePlan plan = planResult.plan();

        assertSame(apiProject, plan.project());
        assertEquals(List.of("verify"), plan.tasks());
        assertEquals(
                List.of("compile", "test"),
                plan.executions().stream()
                        .map(org.apache.maven.api.MojoExecution::goal)
                        .toList());

        LifecycleExecutionResult result = executor.execute(LifecycleExecutionRequest.build(
                session, plan, execution -> execution.goal().equals("test")));

        assertEquals(
                List.of("test"),
                result.executions().stream()
                        .map(org.apache.maven.api.MojoExecution::goal)
                        .toList());
        verify(mavenSession, times(2)).setCurrentProject(same(project));
        verify(mavenSession, times(2)).setCurrentProject(same(previousProject));
    }

    @Test
    void executesAllMojosWhenFilterIsAbsent() throws Exception {
        LifecyclePlan plan = executor.plan(LifecyclePlanRequest.build(session, apiProject, List.of("verify")))
                .plan();

        LifecycleExecutionResult result = executor.execute(LifecycleExecutionRequest.build(session, plan));

        assertEquals(2, result.executions().size());
        assertEquals(2, result.executedSuccessfully().size());
        assertTrue(result.failedExecution().isEmpty());
        assertTrue(result.failureCause().isEmpty());
    }

    @Test
    void restoresCurrentProjectOnExecutionFailure() throws Exception {
        LifecyclePlan plan = executor.plan(LifecyclePlanRequest.build(session, apiProject, List.of("verify")))
                .plan();
        doThrow(new LifecycleExecutionException("boom"))
                .when(buildContext)
                .configureAndExecuteStep(eq(project), anyList());

        assertThrows(
                LifecycleExecutorException.class,
                () -> executor.execute(LifecycleExecutionRequest.build(session, plan)));

        verify(mavenSession, times(2)).setCurrentProject(same(previousProject));
    }

    @Test
    void doesNotWrapFilterFailures() throws Exception {
        LifecyclePlan plan = executor.plan(LifecyclePlanRequest.build(session, apiProject, List.of("verify")))
                .plan();
        IllegalStateException filterFailure = new IllegalStateException("broken filter");

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> executor.execute(LifecycleExecutionRequest.build(session, plan, execution -> {
                    throw filterFailure;
                })));

        assertSame(filterFailure, thrown);
        verify(buildContext, never()).configureAndExecuteStep(any(), anyList());
        verify(mavenSession, times(2)).setCurrentProject(same(previousProject));
    }

    @Test
    void rejectsPlansNotProducedByThisServiceWithoutLeakingImplementationDetails() {
        LifecyclePlan externalPlan = mock(LifecyclePlan.class);

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(LifecycleExecutionRequest.build(session, externalPlan)));

        assertEquals("plan was not produced by this lifecycle executor", thrown.getMessage());
        assertFalse(thrown.getMessage().contains(DefaultLifecyclePlan.class.getName()));
        verify(mavenSession, never()).setCurrentProject(project);
    }

    @Test
    void reportsPerExecutionStatus() throws Exception {
        LifecyclePlan plan = executor.plan(LifecyclePlanRequest.build(session, apiProject, List.of("verify")))
                .plan();

        // Make the second step (test) fail
        doAnswer(inv -> {
                    List<MojoExecution> execs = inv.getArgument(1);
                    if (execs.stream().anyMatch(e -> "test".equals(e.getGoal()))) {
                        throw new LifecycleExecutionException("test failed");
                    }
                    return null;
                })
                .when(buildContext)
                .configureAndExecuteStep(eq(project), anyList());

        LifecycleExecutorException ex = assertThrows(
                LifecycleExecutorException.class,
                () -> executor.execute(LifecycleExecutionRequest.build(session, plan)));

        assertTrue(ex.getMessage().contains("Unable to execute lifecycle plan"));
        LifecycleExecutionResult partialResult = ex.result().orElseThrow();
        assertEquals(
                List.of("compile"),
                partialResult.executedSuccessfully().stream()
                        .map(org.apache.maven.api.MojoExecution::goal)
                        .toList());
        assertTrue(partialResult.failedExecution().isPresent());
        assertEquals("test", partialResult.failedExecution().get().goal());
        assertTrue(partialResult.failureCause().isPresent());
    }

    @Test
    void phaseGrouping() throws Exception {
        LifecyclePlan plan = executor.plan(LifecyclePlanRequest.build(session, apiProject, List.of("verify")))
                .plan();

        var byPhase = plan.executionsByPhase();
        assertEquals(2, byPhase.size());
        assertTrue(byPhase.containsKey("compile"));
        assertTrue(byPhase.containsKey("test"));
    }

    private static MojoExecution execution(String goal, String phase) {
        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.setGroupId("org.apache.maven.plugins");
        pluginDescriptor.setArtifactId("maven-test-plugin");
        pluginDescriptor.setVersion("1.0");

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal(goal);
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);

        MojoExecution execution = new MojoExecution(mojoDescriptor, "default-" + goal);
        execution.setLifecyclePhase(phase);
        return execution;
    }
}
