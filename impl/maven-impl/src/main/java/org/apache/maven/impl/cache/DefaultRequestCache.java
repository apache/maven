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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.cache.BatchRequestException;
import org.apache.maven.api.cache.CacheMetadata;
import org.apache.maven.api.cache.CacheRetention;
import org.apache.maven.api.cache.MavenExecutionException;
import org.apache.maven.api.cache.RequestCache;
import org.apache.maven.api.cache.RequestResult;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.Result;

public class DefaultRequestCache implements RequestCache {

    private static final SessionData.Key<ConcurrentMap> KEY = SessionData.key(ConcurrentMap.class, CacheMetadata.class);
    private static final Object ROOT = new Object();

    private final Map<Object, CachingSupplier<?, ?>> forever = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("all")
    public <REQ extends Request<?>, REP extends Result<REQ>> REP request(REQ req, Function<REQ, REP> supplier) {
        CachingSupplier<REQ, REP> cs = doCache(req, supplier);
        return cs.apply(req);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <REQ extends Request<?>, REP extends Result<REQ>> List<REP> requests(
            List<REQ> reqs, Function<List<REQ>, List<REP>> supplier) {
        final Map<REQ, Object> nonCachedResults = new HashMap<>();
        List<RequestResult<REQ, REP>> allResults = new ArrayList<>(reqs.size());

        Function<REQ, REP> individualSupplier = req -> {
            synchronized (nonCachedResults) {
                while (!nonCachedResults.containsKey(req)) {
                    try {
                        nonCachedResults.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
                Object val = nonCachedResults.get(req);
                if (val instanceof CachingSupplier.AltRes altRes) {
                    uncheckedThrow(altRes.t);
                }
                return (REP) val;
            }
        };

        List<CachingSupplier<REQ, REP>> suppliers = new ArrayList<>(reqs.size());
        List<REQ> nonCached = new ArrayList<>();
        for (REQ req : reqs) {
            CachingSupplier<REQ, REP> cs = doCache(req, individualSupplier);
            suppliers.add(cs);
            if (cs.getValue() == null) {
                nonCached.add(req);
            }
        }

        if (!nonCached.isEmpty()) {
            synchronized (nonCachedResults) {
                try {
                    List<REP> reps = supplier.apply(nonCached);
                    for (int i = 0; i < reps.size(); i++) {
                        nonCachedResults.put(nonCached.get(i), reps.get(i));
                    }
                } catch (MavenExecutionException e) {
                    // If batch request fails, mark all non-cached requests as failed
                    for (REQ req : nonCached) {
                        nonCachedResults.put(
                                req, new CachingSupplier.AltRes(e.getCause())); // Mark as processed but failed
                    }
                } finally {
                    nonCachedResults.notifyAll();
                }
            }
        }

        // Collect results in original order
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
            throw new BatchRequestException("One or more requests failed", allResults);
        }

        return allResults.stream().map(RequestResult::result).toList();
    }

    @SuppressWarnings("unchecked")
    private <REQ extends Request<?>, REP extends Result<REQ>> CachingSupplier<REQ, REP> doCache(
            REQ req, Function<REQ, REP> supplier) {
        CacheRetention retention = Objects.requireNonNullElse(
                req instanceof CacheMetadata metadata ? metadata.getCacheRetention() : null,
                CacheRetention.SESSION_SCOPED);

        Map<Object, CachingSupplier<?, ?>> cache = null;
        if ((retention == CacheRetention.REQUEST_SCOPED || retention == CacheRetention.SESSION_SCOPED)
                && req.getSession() instanceof Session session) {
            Object key = retention == CacheRetention.REQUEST_SCOPED ? doGetOuterRequest(req) : ROOT;
            Map<Object, Map<Object, CachingSupplier<?, ?>>> caches =
                    session.getData().computeIfAbsent(KEY, ConcurrentHashMap::new);
            cache = caches.computeIfAbsent(key, k -> new WeakIdentityMap<>());
        } else if (retention == CacheRetention.PERSISTENT) {
            cache = forever;
        }
        if (cache != null) {
            return (CachingSupplier<REQ, REP>) cache.computeIfAbsent(req, r -> new CachingSupplier<>(supplier));
        } else {
            return new CachingSupplier<>(supplier);
        }
    }

    private <REQ extends Request<?>> Object doGetOuterRequest(REQ req) {
        RequestTrace trace = req.getTrace();
        while (trace != null && trace.parent() != null) {
            trace = trace.parent();
        }
        return trace != null && trace.data() != null ? trace.data() : req;
    }

    @SuppressWarnings("unchecked")
    static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        throw (T) t; // rely on vacuous cast
    }
}
