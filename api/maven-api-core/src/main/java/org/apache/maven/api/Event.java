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

import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Event sent by maven during various phases of the build process.
 * Such events can be listened to using {@link Listener}s objects
 * registered in the {@link Session}.
 *
 * @since 4.0.0
 */
@Experimental
public interface Event {

    /**
     * Gets the type of the event.
     *
     * @return the type of the event, never {@code null}
     */
    @Nonnull
    EventType getType();

    /**
     * Gets the session from which this event originates.
     *
     * @return the current session, never {@code null}
     */
    @Nonnull
    Session getSession();

    /**
     * Gets the current project (if any).
     *
     * @return the current project or {@code empty()} if not applicable
     */
    @Nonnull
    Optional<Project> getProject();

    /**
     * Gets the current mojo execution (if any).
     *
     * @return the current mojo execution or {@code empty()} if not applicable
     */
    @Nonnull
    Optional<MojoExecution> getMojoExecution();

    /**
     * Gets the exception that caused the event (if any).
     *
     * @return the exception or {@code empty()} if none
     */
    Optional<Exception> getException();
}
