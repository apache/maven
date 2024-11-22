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
 * Defines the contract for a component responsible for executing a Maven tool
 * using the information provided in an {@link ExecutorRequest}. This interface is central
 * to the execution of Maven commands and builds, but it does not construct nor fully parses arguments.
 *
 * @since 4.0.0
 */
@Experimental
public interface Executor extends AutoCloseable {
    // Logic borrowed from Commons-Lang3
    boolean IS_WINDOWS = System.getProperty("os.name", "unknown").startsWith("Windows");

    /**
     * Maven version string returned when the actual version of Maven cannot be determinet.
     */
    String UNKNOWN_VERSION = "unknown";

    /**
     * Invokes the tool application using the provided {@link ExecutorRequest}.
     * This method is responsible for executing the command or build
     * process based on the information contained in the request.
     *
     * @param executorRequest the request containing all necessary information for the execution
     * @return an integer representing the exit code of the execution (0 typically indicates success)
     * @throws ExecutorException if an error occurs during the execution process
     */
    int execute(@Nonnull ExecutorRequest executorRequest) throws ExecutorException;

    /**
     * Returns the Maven version that provided {@link ExecutorRequest} point at (would use). Please not, that this
     * operation, depending on underlying implementation may be costly. If caller use this method often, it is
     * caller responsibility to properly cache returned values (key can be {@link ExecutorRequest#installationDirectory()}.
     *
     * @param executorRequest the request containing all necessary information for the execution
     * @return a string representing the Maven version or {@link #UNKNOWN_VERSION}
     * @throws ExecutorException if an error occurs during the execution process
     */
    @Nonnull
    String mavenVersion(@Nonnull ExecutorRequest executorRequest) throws ExecutorException;

    /**
     * Closes and disposes of this {@link Executor} instance, releasing any resources it may hold.
     * This method is called automatically when using try-with-resources statements.
     *
     * <p>The default implementation does nothing. Subclasses should override this method
     * if they need to perform cleanup operations.</p>
     *
     * @throws ExecutorException if an error occurs while closing the {@link Executor}
     */
    @Override
    default void close() throws ExecutorException {}
}
