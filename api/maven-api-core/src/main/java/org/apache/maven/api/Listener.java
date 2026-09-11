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
 * A listener for session events.
 * Existing implementations and lambdas receive execution events through {@link #onEvent(Event)}.
 * Implement {@link ExecutionListener} or {@link RepositoryListener} for typed callbacks.
 * <p>
 * A listener implementing both interfaces must override {@link #onEvent(Event)} to resolve
 * their conflicting default methods. Delegate to both defaults to receive typed callbacks
 * for both event families; each default ignores events from the other family:
 * <pre>{@code
 * class CombinedListener implements ExecutionListener, RepositoryListener {
 *     @Override
 *     public void onEvent(Event event) {
 *         ExecutionListener.super.onEvent(event);
 *         RepositoryListener.super.onEvent(event);
 *     }
 * }
 * }</pre>
 *
 * @since 4.0.0
 */
@Experimental
@FunctionalInterface
@Consumer
public interface Listener {
    /**
     * Receives a build execution event.
     *
     * @param event the execution event
     */
    void onEvent(@Nonnull Event event);
}
