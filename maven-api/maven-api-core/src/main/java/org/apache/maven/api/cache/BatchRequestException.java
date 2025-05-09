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

import java.util.List;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.Result;

/**
 * Exception thrown when a batch request operation fails. This exception contains the results
 * of all requests that were attempted, including both successful and failed operations.
 * <p>
 * The exception provides access to detailed results through {@link #getResults()}, allowing
 * callers to determine which specific requests failed and why.
 *
 * @since 4.0.0
 */
@Experimental
public class BatchRequestException extends RuntimeException {

    private final List<RequestResult<?, ?>> results;

    /**
     * Constructs a new BatchRequestException with the specified message and results.
     *
     * @param <REQ> The type of the request
     * @param <REP> The type of the response
     * @param message The error message describing the batch operation failure
     * @param allResults List of results from all attempted requests in the batch
     */
    public <REQ extends Request<?>, REP extends Result<REQ>> BatchRequestException(
            String message, List<RequestResult<REQ, REP>> allResults) {
        super(message);
        this.results = List.copyOf(allResults);
    }

    /**
     * Returns the list of results from all requests that were part of the batch operation.
     * Each result contains the original request, the response (if successful), and any error
     * that occurred during processing.
     *
     * @return An unmodifiable list of request results
     */
    public List<RequestResult<?, ?>> getResults() {
        return results;
    }
}
