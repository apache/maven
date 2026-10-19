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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.LifecyclePlanExecutionException;
import org.apache.maven.api.services.LifecyclePlanExecutor;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.lifecycle.internal.LifecycleTaskSegmentCalculator;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.Typed;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.internal.impl.CoreUtils.cast;

@Named
@Typed({LifecyclePlanExecutor.class, Service.class})
@Singleton
public class DefaultLifecyclePlanExecutor implements LifecyclePlanExecutor {

    private final LifecycleTaskSegmentCalculator taskSegmentCalculator;
    private final LifecycleExecutionPlanCalculator executionPlanCalculator;
    private final MojoExecutor mojoExecutor;

    @Inject
    public DefaultLifecyclePlanExecutor(
            LifecycleTaskSegmentCalculator taskSegmentCalculator,
            LifecycleExecutionPlanCalculator executionPlanCalculator,
            MojoExecutor mojoExecutor) {
        this.taskSegmentCalculator = taskSegmentCalculator;
        this.executionPlanCalculator = executionPlanCalculator;
        this.mojoExecutor = mojoExecutor;
    }

    @Override
    public void execute(
            @Nonnull Session requestedSession,
            @Nonnull Project project,
            @Nonnull List<String> tasks,
            @Nonnull Predicate<MojoExecution> filter) {
        requireNonNull(requestedSession, "session");
        requireNonNull(project, "project");
        requireNonNull(tasks, "tasks").forEach(task -> requireNonNull(task, "task"));
        requireNonNull(filter, "filter");

        InternalMavenSession internalSession = InternalMavenSession.from(requestedSession);
        MavenSession mavenSession = internalSession.getMavenSession();
        MavenProject mavenProject =
                cast(DefaultProject.class, project, "project").getProject();
        MavenProject previousProject = mavenSession.getCurrentProject();

        mavenSession.setCurrentProject(mavenProject);
        try {
            List<org.apache.maven.plugin.MojoExecution> plan;
            try {
                plan = calculatePlan(mavenSession, mavenProject, tasks);
            } catch (Exception e) {
                throw new LifecyclePlanExecutionException("Unable to calculate lifecycle plan", e);
            }

            List<org.apache.maven.plugin.MojoExecution> filteredPlan = new ArrayList<>();
            for (org.apache.maven.plugin.MojoExecution execution : plan) {
                if (filter.test(new DefaultMojoExecution(internalSession, execution))) {
                    filteredPlan.add(execution);
                }
            }

            try {
                mojoExecutor.execute(mavenSession, filteredPlan);
            } catch (org.apache.maven.lifecycle.LifecycleExecutionException e) {
                throw new LifecyclePlanExecutionException("Unable to execute filtered lifecycle plan", e);
            }
        } finally {
            mavenSession.setCurrentProject(previousProject);
        }
    }

    private List<org.apache.maven.plugin.MojoExecution> calculatePlan(
            MavenSession session, MavenProject project, List<String> tasks) throws Exception {
        List<TaskSegment> taskSegments = taskSegmentCalculator.calculateTaskSegments(session, tasks);
        List<org.apache.maven.plugin.MojoExecution> plan = new ArrayList<>();
        for (TaskSegment taskSegment : taskSegments) {
            MavenExecutionPlan executionPlan =
                    executionPlanCalculator.calculateExecutionPlan(session, project, taskSegment.getTasks());
            plan.addAll(executionPlan.getMojoExecutions());
        }
        return plan;
    }
}
