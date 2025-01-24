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

import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Base interface for service requests in Maven. This interface defines the common contract
 * for all request types within the Maven service layer, providing access to session context
 * and request tracing capabilities.
 *
 * <p>Each request is associated with a {@link ProtoSession} that contains the configuration
 * and context necessary for request processing, including:
 * <ul>
 *   <li>User and system properties for interpolation</li>
 *   <li>Session start time information</li>
 *   <li>Project directory structures</li>
 * </ul>
 *
 * <p>Requests can optionally carry trace information through {@link RequestTrace} to support:
 * <ul>
 *   <li>Debugging and troubleshooting of request flows</li>
 *   <li>Audit logging of operations</li>
 *   <li>Performance monitoring of nested operations</li>
 * </ul>
 *
 * <p>This interface is designed to be extended by specific request types that handle
 * different Maven operations. All implementations must be immutable to ensure thread safety
 * and predictable behavior in concurrent environments.
 *
 * @param <S> the type of ProtoSession associated with this request, allowing for
 *           type-safe session handling in specific request implementations
 *
 * @see ProtoSession
 * @see RequestTrace
 * @see Result
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface Request<S extends ProtoSession> {

    /**
     * Returns the session associated with this request.
     *
     * @return the session instance, never {@code null}
     */
    @Nonnull
    S getSession();

    /**
     * Returns the trace information associated with this request, if any.
     * The trace provides context about the request's position in the operation
     * hierarchy and can carry additional diagnostic information.
     *
     * @return the request trace, or {@code null} if no trace information is available
     */
    @Nullable
    RequestTrace getTrace();

    /**
     * Returns a hashcode value for this request, based on all significant fields.
     * Implementations must ensure that if two requests are equal according to
     * {@link #equals(Object)}, they have the same hashcode.
     *
     * @return a hash code value for this request
     */
    @Override
    int hashCode();

    /**
     * Returns {@code true} if the specified object is equal to this request.
     * Two requests are considered equal if they have the same type and all
     * significant fields are equal.
     *
     * @param obj the object to compare with this request
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    boolean equals(Object obj);

    /**
     * Returns a string representation of this request, used for debugging and logging purposes.
     * The format should include the request type and any significant attributes that define the
     * request's state.
     *
     * @return a string representation of this request, never {@code null}
     */
    @Override
    @Nonnull
    String toString();
}
