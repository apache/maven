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
package org.apache.maven.api.cli.mvn;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cli.Invoker;
import org.apache.maven.api.cli.InvokerException;

/**
 * Defines the contract for a component responsible for invoking Maven using information provided in an invoker request.
 * This interface extends the general {@link Invoker} interface, specializing it for Maven-specific operations.
 *
 * @param <R> The specific type of {@link MavenInvokerRequest} this invoker can handle
 *
 * @since 4.0.0
 */
@Experimental
public interface MavenInvoker<R extends MavenInvokerRequest<? extends MavenOptions>> extends Invoker<R> {
    /**
     * Invokes Maven using the provided MavenInvokerRequest.
     * This method is responsible for executing the Maven build process
     * based on the information contained in the request.
     *
     * @param invokerRequest the request containing all necessary information for the Maven invocation
     * @return an integer representing the exit code of the Maven invocation (0 typically indicates success)
     * @throws InvokerException if an error occurs during the Maven invocation process
     */
    @Override
    int invoke(@Nonnull R invokerRequest) throws InvokerException;
}
