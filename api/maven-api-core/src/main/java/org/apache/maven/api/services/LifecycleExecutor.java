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
 * Calculates, inspects, and executes Maven lifecycle plans for a single project.
 *
 * <p>This is the Maven API v4 lifecycle executor service. The legacy
 * {@code org.apache.maven.lifecycle.LifecycleExecutor} interface serves a different role in the compatibility layer.
 *
 * @since 4.1.0
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
     * @since 4.1.0
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
     * @since 4.1.0
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
     * @since 4.1.0
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
     * @since 4.1.0
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
     * @since 4.1.0
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
