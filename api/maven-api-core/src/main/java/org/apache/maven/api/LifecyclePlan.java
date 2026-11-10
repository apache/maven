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
package org.apache.maven.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * An immutable snapshot of the lifecycle execution plan for a single project.
 *
 * <p>A {@code LifecyclePlan} is produced by {@link org.apache.maven.api.services.LifecycleExecutor#plan(
 * org.apache.maven.api.services.LifecyclePlanRequest) LifecycleExecutor.plan()} and captures the
 * configured {@link MojoExecution mojo executions} that Maven would run for the requested
 * lifecycle phases or plugin goals. The plan is composable: it can be inspected, filtered,
 * and then passed to {@link org.apache.maven.api.services.LifecycleExecutor#execute(
 * org.apache.maven.api.services.LifecycleExecutionRequest) LifecycleExecutor.execute()}.
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 * LifecyclePlanResult result = session.planLifecycle(project, List.of("verify"));
 * LifecyclePlan plan = result.plan();
 *
 * // Inspect: display the plan to the user
 * plan.executionsByPhase().forEach((phase, mojos) ->
 *     System.out.println(phase + ": " + mojos));
 *
 * // Filter and execute: run only the compile-phase mojos
 * session.executeLifecycle(plan,
 *     mojo -> mojo.lifecyclePhase().orElse("").equals("compile"));
 * }</pre>
 *
 * <p>The {@link #executions()} list is topologically sorted: mojos appear in an order
 * consistent with the lifecycle phase graph, but the internal execution engine may
 * run independent steps concurrently.
 *
 * <p>Instances are scoped to a single project. Reactor-level (multi-project) orchestration
 * is handled separately.
 *
 * @since 4.2.0
 * @see org.apache.maven.api.services.LifecycleExecutor
 * @see Session#planLifecycle(Project, java.util.List)
 */
@Experimental
@Immutable
public interface LifecyclePlan {

    /** {@return the project for which this plan was calculated} */
    @Nonnull
    Project project();

    /** {@return the lifecycle phases and plugin goals used to calculate this plan} */
    @Nonnull
    List<String> tasks();

    /** {@return the configured mojo executions in lifecycle order} */
    @Nonnull
    List<MojoExecution> executions();

    /** {@return the executions grouped by lifecycle phase, preserving phase order} */
    @Nonnull
    default Map<String, List<MojoExecution>> executionsByPhase() {
        return executions().stream()
                .collect(Collectors.groupingBy(
                        e -> e.lifecyclePhase().orElse(""), LinkedHashMap::new, Collectors.toUnmodifiableList()));
    }
}
