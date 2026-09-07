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
 * A build execution event with noun-style accessors.
 * Extends {@link Event} so existing execution listeners can consume the same event.
 *
 * @since 4.1.0
 */
@Experimental
@Immutable
public interface ExecutionEvent extends Event {
    /** {@return the kind of execution operation} */
    @Nonnull
    ExecutionEventType type();

    /** {@return the current project, if applicable} */
    @Nonnull
    Optional<Project> project();

    /** {@return the current mojo execution, if applicable} */
    @Nonnull
    Optional<MojoExecution> mojoExecution();

    /** {@return the failure associated with this event, if any} */
    @Nonnull
    Optional<Exception> exception();

    @Override
    @Nonnull
    Session session();

    @Override
    default EventType getType() {
        return EventType.valueOf(type().name());
    }

    @Override
    default Session getSession() {
        return session();
    }

    @Override
    default Optional<Project> getProject() {
        return project();
    }

    @Override
    default Optional<MojoExecution> getMojoExecution() {
        return mojoExecution();
    }

    @Override
    default Optional<Exception> getException() {
        return exception();
    }
}
