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
package org.apache.maven.api.services;

import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Signals that lifecycle planning or execution failed.
 *
 * @since 4.2.0
 */
@Experimental
public class LifecycleExecutorException extends MavenException {

    private final LifecycleExecutionResult result;

    public LifecycleExecutorException(String message) {
        super(message);
        this.result = null;
    }

    public LifecycleExecutorException(String message, Throwable cause) {
        super(message, cause);
        this.result = null;
    }

    public LifecycleExecutorException(String message, Throwable cause, LifecycleExecutionResult result) {
        super(message, cause);
        this.result = result;
    }

    /** {@return the partial execution result available at the time of failure, or empty} */
    @Nonnull
    public Optional<LifecycleExecutionResult> result() {
        return Optional.ofNullable(result);
    }
}
