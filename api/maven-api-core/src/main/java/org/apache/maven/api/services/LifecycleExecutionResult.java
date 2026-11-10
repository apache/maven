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
import java.util.Optional;

import org.apache.maven.api.LifecyclePlan;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * The result of executing a lifecycle plan.
 *
 * <p>On the success path this object is returned from
 * {@link LifecycleExecutor#execute(LifecycleExecutionRequest)}. On the failure path
 * it is attached to the thrown {@link LifecycleExecutorException} and accessible via
 * {@link LifecycleExecutorException#result()}, so callers can always determine which
 * mojos succeeded before the failure.
 *
 * @since 4.2.0
 * @see LifecycleExecutor#execute(LifecycleExecutionRequest)
 * @see LifecycleExecutorException#result()
 */
@Experimental
public interface LifecycleExecutionResult extends Result<LifecycleExecutionRequest> {

    /** {@return the lifecycle plan that was executed} */
    @Nonnull
    LifecyclePlan plan();

    /** {@return the executions retained by the request and submitted for execution} */
    @Nonnull
    List<MojoExecution> executions();

    /** {@return the executions that completed successfully} */
    @Nonnull
    List<MojoExecution> executedSuccessfully();

    /** {@return the execution that failed, if any} */
    @Nonnull
    Optional<MojoExecution> failedExecution();

    /** {@return the cause of the failure, if any} */
    @Nonnull
    Optional<Throwable> failureCause();
}
