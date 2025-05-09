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
import java.util.function.Function;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.Result;

/**
 * Interface for caching request results in Maven. This cache implementation provides
 * methods for executing and optionally caching both single requests and batches of requests.
 * <p>
 * The cache behavior is determined by the cache retention specified in the request's metadata.
 * Results can be cached at different policies (forever, session, request, or not at all)
 * based on the {@link CacheRetention} associated with the request.
 *
 * @since 4.0.0
 * @see CacheMetadata
 * @see RequestCacheFactory
 */
@Experimental
public interface RequestCache {

    /**
     * Executes and optionally caches a request using the provided supplier function. If caching is enabled
     * for this session, the result will be cached and subsequent identical requests will return the cached
     * value without re-executing the supplier.
     * <p>
     * The caching behavior is determined by the cache retention specified in the request's metadata.
     * If an error occurs during execution, it will be cached and re-thrown for subsequent identical requests.
     *
     * @param <REQ> The request type
     * @param <REP> The response type
     * @param req The request object used as the cache key
     * @param supplier The function to execute and cache the result
     * @return The result from the supplier (either fresh or cached)
     * @throws RuntimeException Any exception thrown by the supplier will be cached and re-thrown on subsequent calls
     */
    <REQ extends Request<?>, REP extends Result<REQ>> REP request(REQ req, Function<REQ, REP> supplier);

    /**
     * Executes and optionally caches a batch of requests using the provided supplier function.
     * This method allows for efficient batch processing of multiple requests.
     * <p>
     * The implementation may optimize the execution by:
     * <ul>
     *   <li>Returning cached results for previously executed requests</li>
     *   <li>Grouping similar requests for batch processing</li>
     *   <li>Processing requests in parallel where appropriate</li>
     * </ul>
     *
     * @param <REQ> The request type
     * @param <REP> The response type
     * @param req List of requests to process
     * @param supplier Function to execute the batch of requests
     * @return List of results corresponding to the input requests
     * @throws BatchRequestException if any request in the batch fails
     */
    <REQ extends Request<?>, REP extends Result<REQ>> List<REP> requests(
            List<REQ> req, Function<List<REQ>, List<REP>> supplier);
}
