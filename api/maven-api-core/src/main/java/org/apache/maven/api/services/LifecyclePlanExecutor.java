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

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Calculates and executes Maven lifecycle plans.
 *
 * @since 4.1.0
 */
@Experimental
public interface LifecyclePlanExecutor extends Service {

    /**
     * Calculates the execution plan for the supplied tasks, retains the executions accepted by the filter, and
     * executes the resulting plan as a single unit.
     * <p>
     * The filter receives immutable, fully configured execution snapshots in lifecycle order. Maven executes all
     * accepted executions with one dependency context, so dependency resolution and lifecycle phase tracking are
     * handled in the same way as an unfiltered plan.
     *
     * @param session the session in which to execute the plan
     * @param project the project for which to execute the plan
     * @param tasks lifecycle phases or plugin goals used to calculate the plan
     * @param filter predicate deciding which configured executions remain in the plan
     * @throws LifecyclePlanExecutionException if plan calculation or execution fails
     * @throws NullPointerException if any argument or task is {@code null}
     */
    void execute(
            @Nonnull Session session,
            @Nonnull Project project,
            @Nonnull List<String> tasks,
            @Nonnull Predicate<MojoExecution> filter);
}
