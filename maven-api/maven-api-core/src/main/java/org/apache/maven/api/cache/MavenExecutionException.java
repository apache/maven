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
package org.apache.maven.api.cache;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.services.MavenException;

/**
 * Exception thrown when an error occurs during Maven execution.
 * This exception wraps the original cause of the execution failure.
 *
 * @since 4.0.0
 */
@Experimental
public class MavenExecutionException extends MavenException {

    /**
     * Constructs a new MavenExecutionException with the specified cause.
     *
     * @param cause The underlying exception that caused the execution failure
     */
    public MavenExecutionException(Throwable cause) {
        super(cause);
    }
}
