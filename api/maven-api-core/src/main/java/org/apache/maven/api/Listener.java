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
 * Base marker interface for all Maven event listeners.
 * Specific listener sub-interfaces (such as {@link ExecutionListener}) provide typed callbacks
 * for particular event families.
 * Register listeners via {@link Session#registerListener(Listener)}.
 *
 * @see ExecutionListener
 * @since 4.0.0
 */
@Experimental
@Consumer
public interface Listener {

    /**
     * Called when an event occurs.
     * <p>
     * This generic callback is provided for backward compatibility. Prefer implementing
     * {@link ExecutionListener} or another typed sub-interface for type-safe event handling.
     *
     * @param event the event
     * @deprecated Implement {@link ExecutionListener} or a specific listener sub-interface instead.
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    default void onEvent(@Nonnull Event event) {}
}
