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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.maven.api.LifecyclePlan;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.LifecycleExecutionRequest;
import org.apache.maven.api.services.LifecycleExecutionResult;
import org.apache.maven.api.services.LifecycleExecutor;
import org.apache.maven.api.services.LifecycleExecutorException;
import org.apache.maven.api.services.LifecyclePlanRequest;
import org.apache.maven.api.services.LifecyclePlanResult;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.LifecycleTaskSegmentCalculator;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.lifecycle.internal.concurrent.BuildPlan;
import org.apache.maven.lifecycle.internal.concurrent.BuildPlanExecutor;
import org.apache.maven.lifecycle.internal.concurrent.BuildStep;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.Typed;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.internal.impl.CoreUtils.cast;

@Named
@Typed({LifecycleExecutor.class, Service.class})
@Singleton
public class DefaultLifecycleExecutor implements LifecycleExecutor {

    private final LifecycleTaskSegmentCalculator taskSegmentCalculator;
    private final BuildPlanExecutor buildPlanExecutor;

    @Inject
    public DefaultLifecycleExecutor(
            LifecycleTaskSegmentCalculator taskSegmentCalculator, BuildPlanExecutor buildPlanExecutor) {
        this.taskSegmentCalculator = taskSegmentCalculator;
        this.buildPlanExecutor = buildPlanExecutor;
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
            List<TaskSegment> taskSegments = taskSegmentCalculator.calculateTaskSegments(mavenSession, request.tasks());
            BuildPlan buildPlan = buildPlanExecutor.calculateProjectPlan(mavenSession, mavenProject, taskSegments);

            List<org.apache.maven.plugin.MojoExecution> internalExecutions = buildPlan.sortedNodes().stream()
                    .flatMap(BuildStep::executions)
                    .toList();
            List<MojoExecution> executions = internalExecutions.stream()
                    .map(execution -> (MojoExecution) new DefaultMojoExecution(internalSession, execution))
                    .toList();
            Map<org.apache.maven.plugin.MojoExecution, MojoExecution> mapping =
                    DefaultLifecyclePlan.buildMapping(internalExecutions, executions);

            DefaultLifecyclePlan plan =
                    new DefaultLifecyclePlan(request.project(), request.tasks(), executions, buildPlan, mapping);
            return new DefaultLifecyclePlanResult(request, plan);
        } catch (MavenException e) {
            throw new LifecycleExecutorException("Unable to calculate lifecycle plan", e);
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
        if (!(request.plan() instanceof DefaultLifecyclePlan plan)) {
            throw new IllegalArgumentException("plan was not produced by this lifecycle executor");
        }
        MavenProject mavenProject =
                cast(DefaultProject.class, plan.project(), "project").getProject();
        MavenProject previousProject = mavenSession.getCurrentProject();

        mavenSession.setCurrentProject(mavenProject);
        try {
            Predicate<MojoExecution> filter = request.filter();
            Map<org.apache.maven.plugin.MojoExecution, MojoExecution> mapping = plan.internalToApiMap();

            // Determine which v4 executions pass the filter
            Set<MojoExecution> accepted = new HashSet<>();
            List<MojoExecution> submittedExecutions = new ArrayList<>();
            for (MojoExecution apiExecution : plan.executions()) {
                if (filter == null || filter.test(apiExecution)) {
                    accepted.add(apiExecution);
                    submittedExecutions.add(apiExecution);
                }
            }

            // Walk the BuildPlan steps in topological order, executing filtered mojos
            List<MojoExecution> succeeded = new ArrayList<>();
            MojoExecution failed = null;
            Throwable failureCause = null;

            for (BuildStep step : plan.buildPlan().sortedNodes()) {
                List<org.apache.maven.plugin.MojoExecution> stepExecutions = step.executions()
                        .filter(internal -> accepted.contains(mapping.get(internal)))
                        .toList();
                if (!stepExecutions.isEmpty()) {
                    try {
                        buildPlanExecutor.configureAndExecute(mavenSession, mavenProject, stepExecutions);
                        for (org.apache.maven.plugin.MojoExecution internal : stepExecutions) {
                            succeeded.add(mapping.get(internal));
                        }
                    } catch (LifecycleExecutionException e) {
                        // Determine which mojo failed (last in the batch that was executing)
                        failed = mapping.get(stepExecutions.get(stepExecutions.size() - 1));
                        failureCause = e;
                        throw new LifecycleExecutorException("Unable to execute lifecycle plan", e);
                    }
                }
            }
            return new DefaultLifecycleExecutionResult(
                    request, plan, submittedExecutions, succeeded, failed, failureCause);
        } finally {
            mavenSession.setCurrentProject(previousProject);
        }
    }

    private record DefaultLifecyclePlanResult(LifecyclePlanRequest request, LifecyclePlan plan)
            implements LifecyclePlanResult {
        @Override
        public LifecyclePlanRequest getRequest() {
            return request;
        }
    }

    private record DefaultLifecycleExecutionResult(
            LifecycleExecutionRequest request,
            LifecyclePlan plan,
            List<MojoExecution> executions,
            List<MojoExecution> executedSuccessfully,
            MojoExecution failedMojo,
            Throwable failure)
            implements LifecycleExecutionResult {
        private DefaultLifecycleExecutionResult {
            executions = List.copyOf(executions);
            executedSuccessfully = List.copyOf(executedSuccessfully);
        }

        @Override
        public LifecycleExecutionRequest getRequest() {
            return request;
        }

        @Override
        public Optional<MojoExecution> failedExecution() {
            return Optional.ofNullable(failedMojo);
        }

        @Override
        public Optional<Throwable> failureCause() {
            return Optional.ofNullable(failure);
        }
    }
}
