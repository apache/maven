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
package org.apache.maven.api.spi;

import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.ThreadSafe;
import org.apache.maven.api.di.Named;

/**
 * Allows build extensions to participate in the Maven build lifecycle.
 * <p>
 * The same participant instance may be called concurrently for different projects during a parallel build.
 * Implementations must therefore be thread-safe and must use the project supplied by the callback instead of
 * relying on mutable session-scoped current-project state.
 *
 * @since 4.1.0
 */
@Experimental
@Consumer
@ThreadSafe
@Named
public interface LifecycleParticipant extends SpiService {

    /**
     * Invoked after all reactor projects have been read and before the reactor is sorted for execution.
     * Extensions may update project state through services such as
     * {@link org.apache.maven.api.services.ProjectManager}.
     *
     * @param session the build session
     * @throws LifecycleParticipantException if the build cannot continue
     */
    default void afterProjectsRead(@Nonnull Session session) throws LifecycleParticipantException {}

    /**
     * Invoked immediately before execution starts for a project, after its execution plan has been calculated.
     *
     * @param event the project, session, and immutable execution plan
     * @throws LifecycleParticipantException if the project build cannot continue
     */
    default void beforeProjectExecution(@Nonnull ProjectExecutionEvent event) throws LifecycleParticipantException {}

    /**
     * Invoked after project execution finishes, successfully or with a failure.
     *
     * @param event the project, session, execution plan, and optional failure
     * @throws LifecycleParticipantException if the build cannot continue
     */
    default void afterProjectExecution(@Nonnull ProjectExecutionEvent event) throws LifecycleParticipantException {}

    /**
     * Invoked when the build session ends. This callback is intended for cleanup of resources owned by the extension.
     *
     * @param session the build session
     * @throws LifecycleParticipantException if cleanup fails
     */
    default void afterSessionEnd(@Nonnull Session session) throws LifecycleParticipantException {}
}
