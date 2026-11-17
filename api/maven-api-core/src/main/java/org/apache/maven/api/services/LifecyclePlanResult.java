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

import org.apache.maven.api.LifecyclePlan;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * The result of calculating a lifecycle plan.
 *
 * <p>The {@link #plan()} accessor returns the immutable {@link LifecyclePlan} that can be
 * inspected and then passed to
 * {@link LifecycleExecutor#execute(LifecycleExecutionRequest)}.
 *
 * @since 4.2.0
 * @see LifecycleExecutor#plan(LifecyclePlanRequest)
 */
@Experimental
public interface LifecyclePlanResult extends Result<LifecyclePlanRequest> {

    /** {@return the calculated lifecycle plan} */
    @Nonnull
    LifecyclePlan plan();
}
