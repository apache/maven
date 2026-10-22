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
 * @since 4.1.0
 */
@Experimental
public interface LifecycleExecutor extends Service {

    /**
     * Calculates a lifecycle plan without executing it.
     *
     * @param request the planning request
     * @return the planning result
     */
    @Nonnull
    LifecyclePlanResult plan(@Nonnull LifecyclePlanRequest request);

    /**
     * Executes all or part of a previously calculated lifecycle plan.
     *
     * @param request the execution request
     * @return the execution result
     */
    @Nonnull
    LifecycleExecutionResult execute(@Nonnull LifecycleExecutionRequest request);

    @Nonnull
    default LifecyclePlanResult plan(@Nonnull Session session, @Nonnull Project project, @Nonnull List<String> tasks) {
        return plan(LifecyclePlanRequest.build(session, project, tasks));
    }

    @Nonnull
    default LifecycleExecutionResult execute(@Nonnull Session session, @Nonnull LifecyclePlan plan) {
        return execute(LifecycleExecutionRequest.build(session, plan));
    }

    @Nonnull
    default LifecycleExecutionResult execute(
            @Nonnull Session session, @Nonnull LifecyclePlan plan, @Nonnull Predicate<MojoExecution> filter) {
        return execute(LifecycleExecutionRequest.build(session, plan, filter));
    }

    @Nonnull
    default LifecycleExecutionResult planAndExecute(
            @Nonnull Session session, @Nonnull Project project, @Nonnull List<String> tasks) {
        return execute(session, plan(session, project, tasks).plan());
    }

    @Nonnull
    default LifecycleExecutionResult planAndExecute(
            @Nonnull Session session,
            @Nonnull Project project,
            @Nonnull List<String> tasks,
            @Nonnull Predicate<MojoExecution> filter) {
        return execute(session, plan(session, project, tasks).plan(), filter);
    }
}
