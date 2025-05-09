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

import java.io.Serial;

import org.apache.maven.api.annotations.Experimental;

/**
 * Exception thrown by {@link Interpolator} implementations when an error occurs during interpolation.
 * This can include syntax errors in variable placeholders or recursive variable references.
 *
 * @since 4.0.0
 */
@Experimental
public class InterpolatorException extends MavenException {

    @Serial
    private static final long serialVersionUID = -1219149033636851813L;

    /**
     * Constructs a new InterpolatorException with {@code null} as its
     * detail message. The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public InterpolatorException() {}

    /**
     * Constructs a new InterpolatorException with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public InterpolatorException(String message) {
        super(message);
    }

    /**
     * Constructs a new InterpolatorException with the specified detail message and cause.
     *
     * <p>Note that the detail message associated with {@code cause} is <i>not</i>
     * automatically incorporated in this exception's detail message.</p>
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method). A {@code null} value is
     *                permitted, and indicates that the cause is nonexistent or unknown.
     */
    public InterpolatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
