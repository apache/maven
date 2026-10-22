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

import java.util.List;

import org.apache.maven.api.LifecyclePlan;
import org.apache.maven.api.services.LifecycleExecutionRequest;
import org.apache.maven.api.services.LifecycleExecutionResult;
import org.apache.maven.api.services.LifecycleExecutorException;
import org.apache.maven.api.services.LifecyclePlanRequest;
import org.apache.maven.api.services.LifecyclePlanResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.lifecycle.internal.LifecycleTask;
import org.apache.maven.lifecycle.internal.LifecycleTaskSegmentCalculator;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultLifecycleExecutorTest {

    private InternalMavenSession session;
    private MavenSession mavenSession;
    private LifecycleTaskSegmentCalculator taskSegmentCalculator;
    private LifecycleExecutionPlanCalculator executionPlanCalculator;
    private MojoExecutor mojoExecutor;
    private MavenProject project;
    private MavenProject previousProject;
    private DefaultProject apiProject;
    private MojoExecution compile;
    private MojoExecution test;
    private DefaultLifecycleExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        session = mock(InternalMavenSession.class);
        mavenSession = mock(MavenSession.class);
        taskSegmentCalculator = mock(LifecycleTaskSegmentCalculator.class);
        executionPlanCalculator = mock(LifecycleExecutionPlanCalculator.class);
        mojoExecutor = mock(MojoExecutor.class);
        project = new MavenProject();
        previousProject = new MavenProject();
        apiProject = mock(DefaultProject.class);
        compile = execution("compile", "compile");
        test = execution("test", "test");
        TaskSegment segment = new TaskSegment(false, new LifecycleTask("verify"));
        MavenExecutionPlan plan = mock(MavenExecutionPlan.class);

        when(session.getMavenSession()).thenReturn(mavenSession);
        when(mavenSession.getCurrentProject()).thenReturn(previousProject);
        when(apiProject.getProject()).thenReturn(project);
        when(taskSegmentCalculator.calculateTaskSegments(mavenSession, List.of("verify")))
                .thenReturn(List.of(segment));
        when(executionPlanCalculator.calculateExecutionPlan(mavenSession, project, segment.getTasks()))
                .thenReturn(plan);
        when(plan.getMojoExecutions()).thenReturn(List.of(compile, test));

        executor = new DefaultLifecycleExecutor(taskSegmentCalculator, executionPlanCalculator, mojoExecutor);
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
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MojoExecution>> executions = ArgumentCaptor.forClass(List.class);
        verify(mojoExecutor).execute(eq(mavenSession), executions.capture());
        assertEquals(List.of(test), executions.getValue());
        verify(mavenSession, times(2)).setCurrentProject(same(project));
        verify(mavenSession, times(2)).setCurrentProject(same(previousProject));
    }

    @Test
    void executesAllMojosWhenFilterIsAbsent() throws Exception {
        LifecyclePlan plan = executor.plan(LifecyclePlanRequest.build(session, apiProject, List.of("verify")))
                .plan();

        LifecycleExecutionResult result = executor.execute(LifecycleExecutionRequest.build(session, plan));

        assertEquals(2, result.executions().size());
        verify(mojoExecutor).execute(mavenSession, List.of(compile, test));
    }

    @Test
    void restoresCurrentProjectOnExecutionFailure() throws Exception {
        LifecyclePlan plan = executor.plan(LifecyclePlanRequest.build(session, apiProject, List.of("verify")))
                .plan();
        doThrow(new org.apache.maven.lifecycle.LifecycleExecutionException("boom"))
                .when(mojoExecutor)
                .execute(eq(mavenSession), anyList());

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
        verify(mojoExecutor, never()).execute(eq(mavenSession), anyList());
        verify(mavenSession, times(2)).setCurrentProject(same(previousProject));
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
