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
package org.apache.maven.api.cli;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Defines the contract for a component responsible for invoking a Maven application
 * using the information provided in an {@link InvokerRequest}. This interface is central
 * to the construction and invocation of Maven commands and builds, and it fully parses arguments.
 *
 * <p>The Invoker is designed to be flexible, allowing for different implementations
 * that can handle various types of {@link InvokerRequest InvokerRequests}. It also implements
 * {@link AutoCloseable} to ensure proper resource management.</p>
 *
 * @since 4.0.0
 */
@Experimental
public interface Invoker extends AutoCloseable {
    /**
     * Invokes the Maven application using the provided {@link InvokerRequest}.
     * This method is responsible for executing the Maven command or build
     * process based on the information contained in the request.
     *
     * @param invokerRequest the request containing all necessary information for the invocation
     * @return an integer representing the exit code of the invocation (0 typically indicates success)
     * @throws InvokerException if an error occurs during the invocation process
     */
    int invoke(@Nonnull InvokerRequest invokerRequest) throws InvokerException;

    /**
     * Closes and disposes of this {@link Invoker} instance, releasing any resources it may hold.
     * This method is called automatically when using try-with-resources statements.
     *
     * <p>The default implementation does nothing. Subclasses should override this method
     * if they need to perform cleanup operations.</p>
     *
     * @throws InvokerException if an error occurs while closing the {@link Invoker}
     */
    @Override
    default void close() throws InvokerException {}
}
