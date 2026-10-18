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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultLifecycleExecutorTest {

    @Test
    void executesFilteredMojosAsOnePlanAndRestoresCurrentProject() throws Exception {
        InternalMavenSession session = mock(InternalMavenSession.class);
        MavenSession mavenSession = mock(MavenSession.class);
        LifecycleTaskSegmentCalculator taskSegmentCalculator = mock(LifecycleTaskSegmentCalculator.class);
        LifecycleExecutionPlanCalculator executionPlanCalculator = mock(LifecycleExecutionPlanCalculator.class);
        MojoExecutor mojoExecutor = mock(MojoExecutor.class);
        MavenProject project = new MavenProject();
        MavenProject previousProject = new MavenProject();
        DefaultProject apiProject = mock(DefaultProject.class);
        TaskSegment segment = new TaskSegment(false, new LifecycleTask("verify"));
        MojoExecution compile = execution("compile", "compile");
        MojoExecution test = execution("test", "test");
        MavenExecutionPlan plan = mock(MavenExecutionPlan.class);

        when(session.getMavenSession()).thenReturn(mavenSession);
        when(mavenSession.getCurrentProject()).thenReturn(previousProject);
        when(apiProject.getProject()).thenReturn(project);
        when(taskSegmentCalculator.calculateTaskSegments(mavenSession, List.of("verify")))
                .thenReturn(List.of(segment));
        when(executionPlanCalculator.calculateExecutionPlan(mavenSession, project, segment.getTasks()))
                .thenReturn(plan);
        when(plan.getMojoExecutions()).thenReturn(List.of(compile, test));

        DefaultLifecycleExecutor executor =
                new DefaultLifecycleExecutor(taskSegmentCalculator, executionPlanCalculator, mojoExecutor);

        executor.execute(
                session,
                apiProject,
                List.of("verify"),
                execution -> execution.goal().equals("test"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MojoExecution>> executions = ArgumentCaptor.forClass(List.class);
        verify(mojoExecutor).execute(eq(mavenSession), executions.capture());
        assertEquals(List.of(test), executions.getValue());
        verify(mavenSession).setCurrentProject(same(project));
        verify(mavenSession).setCurrentProject(same(previousProject));
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
