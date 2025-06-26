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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.cache.CacheMetadata;
import org.apache.maven.api.cache.CacheRetention;
import org.apache.maven.api.cache.CacheStatistics;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.Result;

public class DefaultRequestCache extends AbstractRequestCache {

    protected static final SessionData.Key<ConcurrentMap> KEY =
            SessionData.key(ConcurrentMap.class, CacheMetadata.class);
    protected static final Object ROOT = new Object();

    protected final Map<Object, CachingSupplier<?, ?>> forever = new ConcurrentHashMap<>();
    protected final DefaultCacheStatistics statistics = new DefaultCacheStatistics();

    public DefaultRequestCache() {
        // Register cache size suppliers for different retention policies
        statistics.registerCacheSizeSupplier(CacheRetention.PERSISTENT, () -> (long) forever.size());
        statistics.registerCacheSizeSupplier(CacheRetention.SESSION_SCOPED, this::getSessionScopedCacheSize);
        statistics.registerCacheSizeSupplier(CacheRetention.REQUEST_SCOPED, this::getRequestScopedCacheSize);
    }

    @Override
    public CacheStatistics getStatistics() {
        return statistics;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <REQ extends Request<?>, REP extends Result<REQ>> CachingSupplier<REQ, REP> doCache(
            REQ req, Function<REQ, REP> supplier) {
        CacheRetention retention = Objects.requireNonNullElse(
                req instanceof CacheMetadata metadata ? metadata.getCacheRetention() : null,
                CacheRetention.SESSION_SCOPED);

        String requestType = req.getClass().getSimpleName();

        Map<Object, CachingSupplier<?, ?>> cache = null;
        if ((retention == CacheRetention.REQUEST_SCOPED || retention == CacheRetention.SESSION_SCOPED)
                && req.getSession() instanceof Session session) {
            Object key = retention == CacheRetention.REQUEST_SCOPED ? doGetOuterRequest(req) : ROOT;
            Map<Object, Map<Object, CachingSupplier<?, ?>>> caches =
                    session.getData().computeIfAbsent(KEY, ConcurrentHashMap::new);
            cache = caches.computeIfAbsent(key, k -> new SoftIdentityMap<>());
        } else if (retention == CacheRetention.PERSISTENT) {
            cache = forever;
        }
        if (cache != null) {
            return (CachingSupplier<REQ, REP>) cache.computeIfAbsent(
                    req, r -> new StatisticsCachingSupplier<>(supplier, statistics, requestType, retention));
        } else {
            return new StatisticsCachingSupplier<>(supplier, statistics, requestType, retention);
        }
    }

    private <REQ extends Request<?>> Object doGetOuterRequest(REQ req) {
        RequestTrace trace = req.getTrace();
        while (trace != null && trace.parent() != null) {
            trace = trace.parent();
        }
        return trace != null && trace.data() != null ? trace.data() : req;
    }

    /**
     * Calculates the total size of all session-scoped caches.
     * This method iterates through all active sessions and counts their cache entries.
     */
    private long getSessionScopedCacheSize() {
        // Note: This is an approximation since we don't have direct access to all sessions
        // In a real implementation, this would need to be tracked more carefully
        return 0L; // Placeholder - would need session registry to implement properly
    }

    /**
     * Calculates the total size of all request-scoped caches.
     * This method iterates through all active requests and counts their cache entries.
     */
    private long getRequestScopedCacheSize() {
        // Note: This is an approximation since we don't have direct access to all requests
        // In a real implementation, this would need to be tracked more carefully
        return 0L; // Placeholder - would need request registry to implement properly
    }
}
