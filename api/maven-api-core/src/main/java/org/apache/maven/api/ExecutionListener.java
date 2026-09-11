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

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Receives build execution events through typed callbacks.
 * Each method corresponds to a specific {@link ExecutionEventType} and has a no-op default implementation,
 * so implementations only need to override the methods they care about.
 * <p>
 * Register an {@code ExecutionListener} via {@link Session#registerListener(Listener)}.
 * Implementations must support concurrent notification during parallel builds.
 *
 * @see ExecutionEvent
 * @see ExecutionEventType
 * @since 4.1.0
 */
@Experimental
@Consumer
public interface ExecutionListener extends Listener {

    /**
     * Dispatches the given event to the appropriate typed callback.
     * This default implementation routes {@link ExecutionEvent}s to the matching method
     * and ignores other event types.
     *
     * @param event the event to dispatch
     */
    @Override
    default void onEvent(@Nonnull Event event) {
        if (event instanceof ExecutionEvent ee) {
            switch (ee.type()) {
                case PROJECT_DISCOVERY_STARTED -> projectDiscoveryStarted(ee);
                case SESSION_STARTED -> sessionStarted(ee);
                case SESSION_ENDED -> sessionEnded(ee);
                case PROJECT_SKIPPED -> projectSkipped(ee);
                case PROJECT_STARTED -> projectStarted(ee);
                case PROJECT_SUCCEEDED -> projectSucceeded(ee);
                case PROJECT_FAILED -> projectFailed(ee);
                case MOJO_SKIPPED -> mojoSkipped(ee);
                case MOJO_STARTED -> mojoStarted(ee);
                case MOJO_SUCCEEDED -> mojoSucceeded(ee);
                case MOJO_FAILED -> mojoFailed(ee);
                case FORK_STARTED -> forkStarted(ee);
                case FORK_SUCCEEDED -> forkSucceeded(ee);
                case FORK_FAILED -> forkFailed(ee);
                case FORKED_PROJECT_STARTED -> forkedProjectStarted(ee);
                case FORKED_PROJECT_SUCCEEDED -> forkedProjectSucceeded(ee);
                case FORKED_PROJECT_FAILED -> forkedProjectFailed(ee);
                default -> {}
            }
        }
    }

    /**
     * Called when project discovery has started.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void projectDiscoveryStarted(@Nonnull ExecutionEvent event) {}

    /**
     * Called when the build session starts.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void sessionStarted(@Nonnull ExecutionEvent event) {}

    /**
     * Called when the build session ends.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void sessionEnded(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a project is skipped.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void projectSkipped(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a project starts.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void projectStarted(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a project completes successfully.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void projectSucceeded(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a project fails.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void projectFailed(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a mojo is skipped.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void mojoSkipped(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a mojo starts.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void mojoStarted(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a mojo completes successfully.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void mojoSucceeded(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a mojo fails.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void mojoFailed(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a forked execution starts.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void forkStarted(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a forked execution completes successfully.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void forkSucceeded(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a forked execution fails.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void forkFailed(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a forked project starts.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void forkedProjectStarted(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a forked project completes successfully.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void forkedProjectSucceeded(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a forked project fails.
     *
     * @param event the execution details, including the session and any project, mojo, or failure
     */
    default void forkedProjectFailed(@Nonnull ExecutionEvent event) {}
}
