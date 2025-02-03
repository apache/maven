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
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.Result;

/**
 * A record representing the result of a single request operation, containing the original request,
 * the result (if successful), and any error that occurred during processing.
 * <p>
 * This class is immutable and thread-safe, suitable for use in concurrent operations.
 *
 * @param <REQ> The type of the request
 * @param <REP> The type of the response, which must extend {@code Result<REQ>}
 * @param request The original request that was processed
 * @param result The result of the request, if successful; may be null if an error occurred
 * @param error Any error that occurred during processing; null if the request was successful
 * @since 4.0.0
 */
@Experimental
public record RequestResult<REQ extends Request<?>, REP extends Result<REQ>>(
        /**
         * The original request that was processed
         */
        REQ request,

        /**
         * The result of the request, if successful; may be null if an error occurred
         */
        REP result,

        /**
         * Any error that occurred during processing; null if the request was successful
         */
        Throwable error) {

    /**
     * Determines if the request was processed successfully.
     *
     * @return true if no error occurred during processing (error is null), false otherwise
     */
    public boolean isSuccess() {
        return error == null;
    }
}
