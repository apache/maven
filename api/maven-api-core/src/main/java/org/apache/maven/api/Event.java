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
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Base interface for all Maven events.
 * Specific event families extend this interface to provide typed event data.
 * Events can be listened to using {@link Listener} objects registered in the {@link Session}.
 *
 * @see ExecutionEvent
 * @see RepositoryEvent
 * @see Listener
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface Event {

    /**
     * Returns the session from which this event originates.
     *
     * @return the current session, never {@code null}
     */
    @Nonnull
    Session session();

    /**
     * Gets the session from which this event originates.
     *
     * @return the current session, never {@code null}
     * @deprecated Use {@link #session()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Session getSession() {
        return session();
    }

    /**
     * Gets the type of the event.
     *
     * @return the type of the event, never {@code null}
     * @throws UnsupportedOperationException if this event is not an {@link ExecutionEvent}
     * @deprecated Use {@link ExecutionEvent#type()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default EventType getType() {
        if (this instanceof ExecutionEvent ee) {
            return EventType.valueOf(ee.type().name());
        }
        throw new UnsupportedOperationException("getType() is only supported on ExecutionEvent instances");
    }

    /**
     * Gets the current project (if any).
     *
     * @return the current project or {@code empty()} if not applicable
     * @deprecated Use {@link ExecutionEvent#project()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Optional<Project> getProject() {
        if (this instanceof ExecutionEvent ee) {
            return ee.project();
        }
        return Optional.empty();
    }

    /**
     * Gets the current mojo execution (if any).
     *
     * @return the current mojo execution or {@code empty()} if not applicable
     * @deprecated Use {@link ExecutionEvent#mojoExecution()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Nonnull
    default Optional<MojoExecution> getMojoExecution() {
        if (this instanceof ExecutionEvent ee) {
            return ee.mojoExecution();
        }
        return Optional.empty();
    }

    /**
     * Gets the exception that caused the event (if any).
     *
     * @return the exception or {@code empty()} if none
     * @deprecated Use {@link ExecutionEvent#exception()} instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    default Optional<Exception> getException() {
        if (this instanceof ExecutionEvent ee) {
            return ee.exception();
        }
        return Optional.empty();
    }
}
