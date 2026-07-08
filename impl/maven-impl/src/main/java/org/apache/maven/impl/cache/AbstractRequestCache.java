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
package org.apache.maven.impl.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.maven.api.cache.BatchRequestException;
import org.apache.maven.api.cache.MavenExecutionException;
import org.apache.maven.api.cache.RequestCache;
import org.apache.maven.api.cache.RequestResult;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.Result;

/**
 * Abstract implementation of the {@link RequestCache} interface, providing common caching mechanisms
 * for executing and caching request results in Maven.
 * <p>
 * This class implements caching strategies for individual and batch requests, ensuring that results
 * are stored and reused where appropriate to optimize performance.
 * </p>
 *
 * @since 4.0.0
 */
public abstract class AbstractRequestCache implements RequestCache {

    /**
     * Executes and optionally caches a single request.
     * <p>
     * The caching behavior is determined by the specific implementation of {@link #doCache(Request, Function)}.
     * If caching is enabled, the result is retrieved from the cache or computed using the supplier function.
     * </p>
     *
     * @param <REQ> The request type
     * @param <REP> The response type
     * @param req The request object used as the cache key
     * @param supplier The function that provides the response if not cached
     * @return The cached or computed response
     */
    @Override
    @SuppressWarnings("all")
    public <REQ extends Request<?>, REP extends Result<REQ>> REP request(REQ req, Function<REQ, REP> supplier) {
        CachingSupplier<REQ, REP> cs = doCache(req, supplier);
        return cs.apply(req);
    }

    /**
     * Executes and optionally caches a batch of requests.
     * <p>
     * This method processes a list of requests, utilizing caching where applicable and executing
     * only the non-cached requests using the provided supplier function.
     * </p>
     * <p>
     * If any request in the batch fails, a {@link BatchRequestException} is thrown, containing
     * details of all failed requests.
     * </p>
     *
     * @param <REQ> The request type
     * @param <REP> The response type
     * @param reqs List of requests to process
     * @param supplier Function to execute the batch of requests
     * @return List of results corresponding to the input requests
     * @throws BatchRequestException if any request in the batch fails
     */
    /**
     * {@inheritDoc}
     * <p>
     * This implementation avoids the use of {@code Object.wait()/notify()} to prevent deadlocks
     * in re-entrant scenarios. When batch resolution triggers nested calls (e.g., parent POM
     * resolution during artifact resolution), a cached {@link CachingSupplier} from an outer call
     * could reference a stale wait-based supplier, causing the thread to wait on a
     * {@code HashMap} that will never be notified.
     * <p>
     * Instead, this method:
     * <ol>
     *   <li>Passes a single-item fallback supplier to {@link #doCache} so any newly created
     *       {@link CachingSupplier} can independently resolve its request</li>
     *   <li>Performs batch resolution for efficiency</li>
     *   <li>Directly sets results on {@link CachingSupplier} instances via {@link CachingSupplier#complete}</li>
     * </ol>
     */
    @Override
    @SuppressWarnings("unchecked")
    public <REQ extends Request<?>, REP extends Result<REQ>> List<REP> requests(
            List<REQ> reqs, Function<List<REQ>, List<REP>> supplier) {
        // Create a fallback supplier that can resolve individual requests independently.
        // This is stored in newly created CachingSupplier instances and ensures that
        // re-entrant calls can resolve without deadlocking on a shared wait/notify map.
        Function<REQ, REP> singleSupplier = req -> supplier.apply(List.of(req)).get(0);

        List<CachingSupplier<REQ, REP>> suppliers = new ArrayList<>(reqs.size());
        List<REQ> nonCached = new ArrayList<>();
        for (REQ req : reqs) {
            CachingSupplier<REQ, REP> cs = doCache(req, singleSupplier);
            suppliers.add(cs);
            if (cs.getValue() == null) {
                nonCached.add(req);
            }
        }

        // Resolve non-cached requests in batch and directly set results on CachingSuppliers
        if (!nonCached.isEmpty()) {
            // Build a map from request to its CachingSupplier index for efficient lookup
            Map<REQ, Integer> reqToIndex = new HashMap<>();
            for (int i = 0; i < reqs.size(); i++) {
                if (suppliers.get(i).getValue() == null) {
                    reqToIndex.put(reqs.get(i), i);
                }
            }
            try {
                List<REP> reps = supplier.apply(nonCached);
                for (int i = 0; i < reps.size(); i++) {
                    Integer idx = reqToIndex.get(nonCached.get(i));
                    if (idx != null) {
                        suppliers.get(idx).complete(reps.get(i));
                    }
                }
            } catch (MavenExecutionException e) {
                // If batch request fails, mark all non-cached requests as failed
                CachingSupplier.AltRes failure = new CachingSupplier.AltRes(e.getCause());
                for (REQ req : nonCached) {
                    Integer idx = reqToIndex.get(req);
                    if (idx != null) {
                        suppliers.get(idx).complete(failure);
                    }
                }
            }
        }

        // Collect results in original order
        List<RequestResult<REQ, REP>> allResults = new ArrayList<>(reqs.size());
        boolean hasFailures = false;
        for (int i = 0; i < reqs.size(); i++) {
            REQ req = reqs.get(i);
            CachingSupplier<REQ, REP> cs = suppliers.get(i);
            try {
                REP value = cs.apply(req);
                allResults.add(new RequestResult<>(req, value, null));
            } catch (Throwable t) {
                hasFailures = true;
                allResults.add(new RequestResult<>(req, null, t));
            }
        }

        if (hasFailures) {
            BatchRequestException exception = new BatchRequestException("One or more requests failed", allResults);
            // Add all individual exceptions as suppressed exceptions to preserve stack traces
            for (RequestResult<REQ, REP> result : allResults) {
                if (result.error() != null) {
                    exception.addSuppressed(result.error());
                }
            }
            throw exception;
        }

        return allResults.stream().map(RequestResult::result).toList();
    }

    /**
     * Abstract method to be implemented by subclasses to handle caching logic.
     * <p>
     * This method is responsible for determining whether a request result should be cached,
     * retrieving it from cache if available, or executing the supplier function if necessary.
     * </p>
     *
     * @param <REQ> The request type
     * @param <REP> The response type
     * @param req The request object
     * @param supplier The function that provides the response
     * @return A caching supplier that handles caching logic for the request
     */
    protected abstract <REQ extends Request<?>, REP extends Result<REQ>> CachingSupplier<REQ, REP> doCache(
            REQ req, Function<REQ, REP> supplier);

    @SuppressWarnings("unchecked")
    protected static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        throw (T) t; // rely on vacuous cast
    }
}
