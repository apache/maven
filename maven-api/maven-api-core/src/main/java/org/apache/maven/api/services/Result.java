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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Base interface for service operation results in Maven. This interface defines the common contract
 * for operation results, providing access to the original request that generated this result.
 *
 * <p>Each result is linked to its originating {@link Request}, allowing for:
 * <ul>
 *   <li>Traceability between requests and their outcomes</li>
 *   <li>Access to the session context used during processing</li>
 *   <li>Correlation of results with their initiating parameters</li>
 * </ul>
 *
 * @param <REQ> the type of Request that produced this result, ensuring type-safe
 *              access to the original request parameters
 *
 * @see Request
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface Result<REQ extends Request<?>> {

    /**
     * Returns the request that produced this result.
     *
     * @return the originating request instance, never {@code null}
     */
    @Nonnull
    REQ getRequest();
}
