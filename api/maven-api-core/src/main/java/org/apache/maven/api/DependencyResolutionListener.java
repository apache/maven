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
 * Receives events emitted while the dependencies of a project are resolved during a build.
 * Each method corresponds to a specific {@link DependencyResolutionEventType} and has a no-op default
 * implementation, so implementations only need to override the methods they care about.
 * <p>
 * Register a {@code DependencyResolutionListener} via {@link Session#registerListener(Listener)}.
 * Implementations must support concurrent notification during parallel builds.
 * Runtime exceptions thrown by a listener do not stop the build or notification of other listeners.
 *
 * @see DependencyResolutionEvent
 * @see DependencyResolutionEventType
 * @since 4.1.0
 */
@Experimental
@Consumer
public interface DependencyResolutionListener extends Listener {

    /**
     * Dispatches the given event to the appropriate typed callback.
     * This default implementation routes {@link DependencyResolutionEvent}s to the matching method
     * and ignores other event types.
     *
     * @param event the event to dispatch
     */
    @Override
    default void onEvent(@Nonnull Event event) {
        if (event instanceof DependencyResolutionEvent de) {
            switch (de.type()) {
                case STARTED -> dependencyResolutionStarted(de);
                case SUCCEEDED -> dependencyResolutionSucceeded(de);
                case FAILED -> dependencyResolutionFailed(de);
                default -> {}
            }
        }
    }

    /**
     * Called before the dependencies of a project are resolved.
     *
     * @param event the event containing the project
     */
    default void dependencyResolutionStarted(@Nonnull DependencyResolutionEvent event) {}

    /**
     * Called after the dependencies of a project have been resolved.
     *
     * @param event the event containing the project and the resolved dependencies
     */
    default void dependencyResolutionSucceeded(@Nonnull DependencyResolutionEvent event) {}

    /**
     * Called after the resolution of the dependencies of a project failed.
     *
     * @param event the event containing the project, the dependencies which were not resolved and the failure
     */
    default void dependencyResolutionFailed(@Nonnull DependencyResolutionEvent event) {}
}
