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
import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.LifecycleExecutionRequest;
import org.apache.maven.api.services.LifecycleExecutionResult;
import org.apache.maven.api.services.LifecycleExecutor;
import org.apache.maven.api.services.LifecycleExecutorException;
import org.apache.maven.api.services.LifecyclePlanRequest;
import org.apache.maven.api.services.LifecyclePlanResult;
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
@Typed({LifecycleExecutor.class, Service.class})
@Singleton
public class DefaultLifecycleExecutor implements LifecycleExecutor {

    private final LifecycleTaskSegmentCalculator taskSegmentCalculator;
    private final LifecycleExecutionPlanCalculator executionPlanCalculator;
    private final MojoExecutor mojoExecutor;

    @Inject
    public DefaultLifecycleExecutor(
            LifecycleTaskSegmentCalculator taskSegmentCalculator,
            LifecycleExecutionPlanCalculator executionPlanCalculator,
            MojoExecutor mojoExecutor) {
        this.taskSegmentCalculator = taskSegmentCalculator;
        this.executionPlanCalculator = executionPlanCalculator;
        this.mojoExecutor = mojoExecutor;
    }

    @Nonnull
    @Override
    public LifecyclePlanResult plan(@Nonnull LifecyclePlanRequest request) {
        requireNonNull(request, "request");
        InternalMavenSession internalSession = InternalMavenSession.from(request.getSession());
        MavenSession mavenSession = internalSession.getMavenSession();
        MavenProject mavenProject =
                cast(DefaultProject.class, request.project(), "project").getProject();
        MavenProject previousProject = mavenSession.getCurrentProject();

        mavenSession.setCurrentProject(mavenProject);
        try {
            List<org.apache.maven.plugin.MojoExecution> internalExecutions =
                    calculatePlan(mavenSession, mavenProject, request.tasks());
            List<MojoExecution> executions = internalExecutions.stream()
                    .map(execution -> (MojoExecution) new DefaultMojoExecution(internalSession, execution))
                    .toList();
            DefaultLifecyclePlan plan =
                    new DefaultLifecyclePlan(request.project(), request.tasks(), executions, internalExecutions);
            return new DefaultLifecyclePlanResult(request, plan);
        } catch (Exception e) {
            throw new LifecycleExecutorException("Unable to calculate lifecycle plan", e);
        } finally {
            mavenSession.setCurrentProject(previousProject);
        }
    }

    @Nonnull
    @Override
    public LifecycleExecutionResult execute(@Nonnull LifecycleExecutionRequest request) {
        requireNonNull(request, "request");
        InternalMavenSession internalSession = InternalMavenSession.from(request.getSession());
        MavenSession mavenSession = internalSession.getMavenSession();
        DefaultLifecyclePlan plan = cast(DefaultLifecyclePlan.class, request.plan(), "plan");
        MavenProject mavenProject =
                cast(DefaultProject.class, plan.project(), "project").getProject();
        MavenProject previousProject = mavenSession.getCurrentProject();

        mavenSession.setCurrentProject(mavenProject);
        try {
            List<org.apache.maven.plugin.MojoExecution> internalExecutions = new ArrayList<>();
            List<MojoExecution> executions = new ArrayList<>();
            Predicate<MojoExecution> filter = request.filter();
            for (int i = 0; i < plan.executions().size(); i++) {
                MojoExecution execution = plan.executions().get(i);
                if (filter == null || filter.test(execution)) {
                    executions.add(execution);
                    internalExecutions.add(plan.internalExecutions().get(i));
                }
            }

            try {
                mojoExecutor.execute(mavenSession, internalExecutions);
            } catch (org.apache.maven.lifecycle.LifecycleExecutionException e) {
                throw new LifecycleExecutorException("Unable to execute lifecycle plan", e);
            }
            return new DefaultLifecycleExecutionResult(request, executions);
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

    private record DefaultLifecyclePlanResult(LifecyclePlanRequest request, org.apache.maven.api.LifecyclePlan plan)
            implements LifecyclePlanResult {
        @Override
        public LifecyclePlanRequest getRequest() {
            return request;
        }
    }

    private record DefaultLifecycleExecutionResult(LifecycleExecutionRequest request, List<MojoExecution> executions)
            implements LifecycleExecutionResult {
        private DefaultLifecycleExecutionResult {
            executions = List.copyOf(executions);
        }

        @Override
        public LifecycleExecutionRequest getRequest() {
            return request;
        }
    }
}
