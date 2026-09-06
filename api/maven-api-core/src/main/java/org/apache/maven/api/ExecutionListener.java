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
 * A typed listener for Maven build lifecycle execution events.
 * Each method corresponds to a specific {@link ExecutionEventType} and has a no-op default implementation,
 * so implementations only need to override the methods they care about.
 * <p>
 * Register an {@code ExecutionListener} via {@link Session#registerListener(Listener)}.
 *
 * @see ExecutionEvent
 * @see ExecutionEventType
 * @since 4.1.0
 */
@Experimental
@Consumer
public interface ExecutionListener extends Listener {

    /**
     * Called when project discovery has started.
     *
     * @param event the execution event, never {@code null}
     */
    default void projectDiscoveryStarted(@Nonnull ExecutionEvent event) {}

    /**
     * Called when the Maven session has started.
     *
     * @param event the execution event, never {@code null}
     */
    default void sessionStarted(@Nonnull ExecutionEvent event) {}

    /**
     * Called when the Maven session has ended.
     *
     * @param event the execution event, never {@code null}
     */
    default void sessionEnded(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a project has been skipped.
     *
     * @param event the execution event, never {@code null}
     */
    default void projectSkipped(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a project build has started.
     *
     * @param event the execution event, never {@code null}
     */
    default void projectStarted(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a project build has succeeded.
     *
     * @param event the execution event, never {@code null}
     */
    default void projectSucceeded(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a project build has failed.
     *
     * @param event the execution event, never {@code null}
     */
    default void projectFailed(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a mojo execution has been skipped.
     *
     * @param event the execution event, never {@code null}
     */
    default void mojoSkipped(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a mojo execution has started.
     *
     * @param event the execution event, never {@code null}
     */
    default void mojoStarted(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a mojo execution has succeeded.
     *
     * @param event the execution event, never {@code null}
     */
    default void mojoSucceeded(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a mojo execution has failed.
     *
     * @param event the execution event, never {@code null}
     */
    default void mojoFailed(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a forked execution has started.
     *
     * @param event the execution event, never {@code null}
     */
    default void forkStarted(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a forked execution has succeeded.
     *
     * @param event the execution event, never {@code null}
     */
    default void forkSucceeded(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a forked execution has failed.
     *
     * @param event the execution event, never {@code null}
     */
    default void forkFailed(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a forked project build has started.
     *
     * @param event the execution event, never {@code null}
     */
    default void forkedProjectStarted(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a forked project build has succeeded.
     *
     * @param event the execution event, never {@code null}
     */
    default void forkedProjectSucceeded(@Nonnull ExecutionEvent event) {}

    /**
     * Called when a forked project build has failed.
     *
     * @param event the execution event, never {@code null}
     */
    default void forkedProjectFailed(@Nonnull ExecutionEvent event) {}
}
