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
 * Base for listeners notified through event-specific callbacks.
 * Implement {@link ExecutionListener}, {@link RepositoryListener}, or both.
 * The shared default implementation allows both interfaces to be implemented without conflicting defaults.
 *
 * @since 4.1.0
 */
@Experimental
@Consumer
public interface TypedListener extends Listener {
    /**
     * Does nothing. Typed listeners are notified through their event-specific callbacks.
     *
     * @param event the legacy execution event
     */
    @Override
    default void onEvent(@Nonnull Event event) {}
}
