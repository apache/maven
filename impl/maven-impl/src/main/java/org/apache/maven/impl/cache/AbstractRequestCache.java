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
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
     * Tracks which {@link CachingSupplier} instances are currently being batch-resolved
     * on the current thread. Used by {@link CachingSupplier#apply} to detect re-entrant
     * calls and avoid deadlock: if the current thread is already resolving a CachingSupplier,
     * {@code apply()} will invoke the fallback supplier instead of waiting for
     * {@link CachingSupplier#complete} (which would never arrive on the same thread).
     */
    private static final ThreadLocal<Set<CachingSupplier<?, ?>>> RESOLVING_ON_THREAD =
            ThreadLocal.withInitial(HashSet::new);

    /**
     * Checks whether the given CachingSupplier is currently being batch-resolved
     * on the calling thread.
     *
     * @param cs the CachingSupplier to check
     * @return {@code true} if a batch resolution for {@code cs} is in progress on this thread
     */
    static boolean isResolvingOnCurrentThread(CachingSupplier<?, ?> cs) {
        return RESOLVING_ON_THREAD.get().contains(cs);
    }

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
     * This implementation uses {@link CachingSupplier#complete} with {@code wait/notifyAll}
     * to coordinate batch results, and a {@link ThreadLocal} re-entrancy guard to prevent
     * deadlocks when batch resolution triggers nested calls (e.g., parent POM resolution
     * during artifact resolution).
     * </p>
     * <p>
     * <b>Concurrent calls</b> for the same request on different threads: the first thread
     * performs the resolution; the second thread's {@link CachingSupplier#apply} blocks
     * (via {@code Object.wait()}) until {@code complete()} is called, avoiding duplicate work.
     * </p>
     * <p>
     * <b>Re-entrant calls</b> on the same thread: detected via {@link #RESOLVING_ON_THREAD}.
     * {@link CachingSupplier#apply} skips the wait and invokes the fallback supplier directly
     * so the inner call can complete without waiting for the outer call's batch result.
     * </p>
     *
     * @param <REQ> The request type
     * @param <REP> The response type
     * @param reqs List of requests to process
     * @param supplier Function to execute the batch of requests
     * @return List of results corresponding to the input requests
     * @throws BatchRequestException if any request in the batch fails
     */
    @Override
    @SuppressWarnings("unchecked")
    public <REQ extends Request<?>, REP extends Result<REQ>> List<REP> requests(
            List<REQ> reqs, Function<List<REQ>, List<REP>> supplier) {
        // Create a fallback supplier that can resolve individual requests independently.
        // This is stored in newly created CachingSupplier instances and used only when
        // a re-entrant call on the same thread needs to resolve a request whose batch
        // resolution is still in progress higher up the call stack.
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
            // Use IdentityHashMap: request objects may have unstable hashCode() due to
            // mutable RequestTrace/ModelBuilderRequest data that changes during batch resolution.
            // Since we always use the same object references for put and get, identity is safe.
            Map<REQ, Integer> reqToIndex = new IdentityHashMap<>();
            List<CachingSupplier<REQ, REP>> nonCachedSuppliers = new ArrayList<>(nonCached.size());
            for (int i = 0; i < reqs.size(); i++) {
                if (suppliers.get(i).getValue() == null) {
                    reqToIndex.put(reqs.get(i), i);
                    nonCachedSuppliers.add(suppliers.get(i));
                }
            }

            // Mark these CachingSuppliers as being batch-resolved, and register them
            // on this thread so that re-entrant apply() calls skip the wait.
            Set<CachingSupplier<?, ?>> resolving = RESOLVING_ON_THREAD.get();
            for (CachingSupplier<REQ, REP> cs : nonCachedSuppliers) {
                cs.setBatchResolving(true);
                resolving.add(cs);
            }
            try {
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
                } catch (Throwable e) {
                    // Ensure waiting concurrent threads are unblocked on unexpected errors.
                    // We mark all non-cached requests as failed and fall through to the
                    // collection loop, which produces a consistent BatchRequestException
                    // with per-request RequestResult details — same as MavenExecutionException.
                    CachingSupplier.AltRes failure = new CachingSupplier.AltRes(e);
                    for (REQ req : nonCached) {
                        Integer idx = reqToIndex.get(req);
                        if (idx != null) {
                            suppliers.get(idx).complete(failure);
                        }
                    }
                }
            } finally {
                for (CachingSupplier<REQ, REP> cs : nonCachedSuppliers) {
                    cs.setBatchResolving(false);
                    resolving.remove(cs);
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
