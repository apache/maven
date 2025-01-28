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
package org.apache.maven.api.plugin;

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.ThreadSafe;

/**
 * Represents the contract for Mojos to interact with the Maven infrastructure.
 * Implementations of this interface define specific build-process behaviors
 * that are triggered during a Maven build lifecycle.
 *
 * The primary entry point is the {@link #execute()} method, which encapsulates
 * the behavior of the Mojo and serves as the integration point with Maven. This
 * method may throw an {@link Exception} to signal any issues that prevent
 * successful execution of the Mojo.
 *
 * <p>
 * Annotations:
 * </p>
 * <ul>
 * <li>{@link Experimental}: Indicates that this interface or its implementation
 * may still be evolving and could change in future versions.</li>
 * <li>{@link FunctionalInterface}: Denotes that this is a functional interface,
 * allowing implementations as lambda expressions or method references.</li>
 * <li>{@link Consumer}: Signifies that this type is intended to be implemented
 * or extended by Maven plugins or extensions and consumed by Maven itself.</li>
 * <li>{@link ThreadSafe}: Implies that implementations of this interface must
 * be safe to invoke from multiple threads concurrently.</li>
 * </ul>
 *
 * @since 4.0.0
 */
@Experimental
@FunctionalInterface
@Consumer
@ThreadSafe
public interface Mojo {
    /**
     * Perform whatever build-process behavior this {@code Mojo} implements. This is
     * the main trigger for the {@code Mojo} inside the Maven system, and allows the
     * {@code Mojo} to communicate errors.
     *
     * @throws Exception if any problem occurs that prevents the mojo from its
     *                   execution
     */
    void execute() throws Exception;
}
