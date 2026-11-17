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
package org.apache.maven.api.services;

import java.util.List;
import java.util.function.Predicate;

import org.apache.maven.api.LifecyclePlan;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Service for calculating and executing Maven lifecycle plans for a single project.
 *
 * <p>This service separates lifecycle <em>planning</em> from <em>execution</em>, enabling
 * callers to inspect and filter the plan before deciding what to execute. This is the
 * primary entry point for IDE embedders and tools that need programmatic lifecycle control.
 *
 * <h2>Core workflow</h2>
 * The service exposes two abstract operations:
 * <ol>
 *   <li>{@link #plan(LifecyclePlanRequest)} — calculates an immutable {@link LifecyclePlan}
 *       containing the configured mojo executions for the requested phases/goals</li>
 *   <li>{@link #execute(LifecycleExecutionRequest)} — runs all or a filtered subset of a
 *       previously calculated plan</li>
 * </ol>
 *
 * <p>Convenience defaults ({@link #plan(Session, Project, java.util.List)},
 * {@link #execute(Session, LifecyclePlan)}, {@link #planAndExecute(Session, Project, java.util.List)})
 * and {@link Session} shortcuts ({@link Session#planLifecycle(Project, java.util.List)},
 * {@link Session#executeLifecycle(LifecyclePlan)}) cover the common cases without
 * constructing request objects directly.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Plan the "resources" phase for a project
 * LifecyclePlanResult planResult = session.planLifecycle(project, "resources");
 * LifecyclePlan plan = planResult.plan();
 *
 * // Inspect what would run
 * for (MojoExecution mojo : plan.executions()) {
 *     System.out.println(mojo.lifecyclePhase().orElse("") + " : " + mojo.goal());
 * }
 *
 * // Execute the full plan
 * LifecycleExecutionResult result = session.executeLifecycle(plan);
 *
 * // Or execute only specific mojos (e.g. skip resource copying)
 * result = session.executeLifecycle(plan,
 *     mojo -> !mojo.goal().equals("resources"));
 * }</pre>
 *
 * <h2>Scope and limitations</h2>
 * <ul>
 *   <li>This service operates on a <strong>single project</strong>. Reactor-level
 *       (multi-project) orchestration is handled by a separate mechanism.</li>
 *   <li>The plan uses the same graph-based calculation as the concurrent build
 *       infrastructure, but plugin resolution happens eagerly during {@code plan()},
 *       which requires that all plugin artifacts are already resolvable.</li>
 *   <li>On execution failure, the thrown {@link LifecycleExecutorException} carries
 *       a {@linkplain LifecycleExecutorException#result() partial result} with per-mojo
 *       success/failure status.</li>
 * </ul>
 *
 * <p>This is the Maven 4 API lifecycle executor. The legacy
 * {@code org.apache.maven.lifecycle.LifecycleExecutor} in the compatibility layer
 * serves a different role (full reactor execution).
 *
 * @since 4.2.0
 * @see LifecyclePlan
 * @see Session#planLifecycle(Project, java.util.List)
 * @see Session#executeLifecycle(LifecyclePlan)
 */
@Experimental
public interface LifecycleExecutor extends Service {

    /**
     * Calculates a lifecycle plan without executing it.
     *
     * @param request the planning request
     * @return the planning result
     * @throws LifecycleExecutorException if the lifecycle plan cannot be calculated
     */
    @Nonnull
    LifecyclePlanResult plan(@Nonnull LifecyclePlanRequest request);

    /**
     * Executes all or part of a previously calculated lifecycle plan.
     *
     * @param request the execution request
     * @return the execution result
     * @throws LifecycleExecutorException if lifecycle execution fails
     * @throws IllegalArgumentException if the plan was not produced by this service
     */
    @Nonnull
    LifecycleExecutionResult execute(@Nonnull LifecycleExecutionRequest request);

    /**
     * Calculates a lifecycle plan without executing it.
     *
     * @param session the current session
     * @param project the project for which to calculate the plan
     * @param tasks lifecycle phases or plugin goals used to calculate the plan
     * @return the lifecycle planning result
     * @throws LifecycleExecutorException if the lifecycle plan cannot be calculated
     * @see #plan(LifecyclePlanRequest)
     */
    @Nonnull
    default LifecyclePlanResult plan(@Nonnull Session session, @Nonnull Project project, @Nonnull List<String> tasks) {
        return plan(LifecyclePlanRequest.build(session, project, tasks));
    }

    /**
     * Executes a previously calculated lifecycle plan.
     *
     * @param session the current session
     * @param plan the lifecycle plan to execute
     * @return the lifecycle execution result
     * @throws LifecycleExecutorException if lifecycle execution fails
     * @throws IllegalArgumentException if the plan was not produced by this service
     * @see #execute(LifecycleExecutionRequest)
     */
    @Nonnull
    default LifecycleExecutionResult execute(@Nonnull Session session, @Nonnull LifecyclePlan plan) {
        return execute(LifecycleExecutionRequest.build(session, plan));
    }

    /**
     * Executes the entries of a previously calculated lifecycle plan that are accepted by the supplied filter.
     *
     * @param session the current session
     * @param plan the lifecycle plan to execute
     * @param filter predicate deciding which configured mojo executions are executed
     * @return the lifecycle execution result
     * @throws LifecycleExecutorException if lifecycle execution fails
     * @throws IllegalArgumentException if the plan was not produced by this service
     * @see #execute(LifecycleExecutionRequest)
     */
    @Nonnull
    default LifecycleExecutionResult execute(
            @Nonnull Session session, @Nonnull LifecyclePlan plan, @Nonnull Predicate<MojoExecution> filter) {
        return execute(LifecycleExecutionRequest.build(session, plan, filter));
    }

    /**
     * Calculates and executes a lifecycle plan.
     *
     * @param session the current session
     * @param project the project for which to execute the lifecycle
     * @param tasks lifecycle phases or plugin goals used to calculate the plan
     * @return the lifecycle execution result
     * @throws LifecycleExecutorException if the lifecycle plan cannot be calculated or executed
     * @throws IllegalArgumentException if the calculated plan cannot be executed by this service
     * @see #plan(LifecyclePlanRequest)
     * @see #execute(LifecycleExecutionRequest)
     */
    @Nonnull
    default LifecycleExecutionResult planAndExecute(
            @Nonnull Session session, @Nonnull Project project, @Nonnull List<String> tasks) {
        return execute(session, plan(session, project, tasks).plan());
    }

    /**
     * Calculates a lifecycle plan and executes the entries accepted by the supplied filter.
     *
     * @param session the current session
     * @param project the project for which to execute the lifecycle
     * @param tasks lifecycle phases or plugin goals used to calculate the plan
     * @param filter predicate deciding which configured mojo executions are executed
     * @return the lifecycle execution result
     * @throws LifecycleExecutorException if the lifecycle plan cannot be calculated or executed
     * @throws IllegalArgumentException if the calculated plan cannot be executed by this service
     * @see #plan(LifecyclePlanRequest)
     * @see #execute(LifecycleExecutionRequest)
     */
    @Nonnull
    default LifecycleExecutionResult planAndExecute(
            @Nonnull Session session,
            @Nonnull Project project,
            @Nonnull List<String> tasks,
            @Nonnull Predicate<MojoExecution> filter) {
        return execute(session, plan(session, project, tasks).plan(), filter);
    }
}
