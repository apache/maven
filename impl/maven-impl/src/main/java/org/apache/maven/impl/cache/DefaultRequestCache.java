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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.cache.CacheMetadata;
import org.apache.maven.api.cache.CacheRetention;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultRequestCache extends AbstractRequestCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRequestCache.class);

    protected static final SessionData.Key<ConcurrentMap> KEY =
            SessionData.key(ConcurrentMap.class, CacheMetadata.class);
    protected static final Object ROOT = new Object();

    protected final Map<Object, CachingSupplier<?, ?>> forever = new ConcurrentHashMap<>();

    // Debug counters for memory analysis
    private static final AtomicLong REQUEST_SCOPED_CACHE_COUNT = new AtomicLong(0);
    private static final AtomicLong SESSION_SCOPED_CACHE_COUNT = new AtomicLong(0);
    private static final AtomicLong PERSISTENT_CACHE_COUNT = new AtomicLong(0);
    private static final AtomicLong TOTAL_CACHE_HITS = new AtomicLong(0);
    private static final AtomicLong TOTAL_CACHE_MISSES = new AtomicLong(0);

    static {
        // Add shutdown hook to print cache statistics
        Runtime.getRuntime()
                .addShutdownHook(new Thread(
                        () -> {
                            long totalRequests = TOTAL_CACHE_HITS.get() + TOTAL_CACHE_MISSES.get();
                            if (totalRequests > 0) {
                                System.err.println("[INFO] " + getCacheStatisticsStatic());
                            }
                        },
                        "DefaultRequestCache-Statistics"));
    }

    /**
     * Returns detailed cache statistics for monitoring and debugging (static version for shutdown hook).
     *
     * @return a string containing cache statistics
     */
    private static String getCacheStatisticsStatic() {
        StringBuilder stats = new StringBuilder();
        stats.append("DefaultRequestCache Statistics:\n");
        stats.append("  Total cache hits: ").append(TOTAL_CACHE_HITS.get()).append("\n");
        stats.append("  Total cache misses: ").append(TOTAL_CACHE_MISSES.get()).append("\n");

        long total = TOTAL_CACHE_HITS.get() + TOTAL_CACHE_MISSES.get();
        if (total > 0) {
            stats.append("  Hit ratio: ")
                    .append(String.format("%.2f%%", (TOTAL_CACHE_HITS.get() * 100.0) / total))
                    .append("\n");
        }

        stats.append("  Request scoped cache accesses: ")
                .append(REQUEST_SCOPED_CACHE_COUNT.get())
                .append("\n");
        stats.append("  Session scoped cache accesses: ")
                .append(SESSION_SCOPED_CACHE_COUNT.get())
                .append("\n");
        stats.append("  Persistent cache accesses: ").append(PERSISTENT_CACHE_COUNT.get());

        return stats.toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <REQ extends Request<?>, REP extends Result<REQ>> CachingSupplier<REQ, REP> doCache(
            REQ req, Function<REQ, REP> supplier) {
        CacheRetention retention = Objects.requireNonNullElse(
                req instanceof CacheMetadata metadata ? metadata.getCacheRetention() : null,
                CacheRetention.SESSION_SCOPED);

        Map<Object, CachingSupplier<?, ?>> cache = null;
        String cacheType = "NONE";

        if ((retention == CacheRetention.REQUEST_SCOPED || retention == CacheRetention.SESSION_SCOPED)
                && req.getSession() instanceof Session session) {
            Object key = retention == CacheRetention.REQUEST_SCOPED ? doGetOuterRequest(req) : ROOT;
            Map<Object, Map<Object, CachingSupplier<?, ?>>> caches =
                    session.getData().computeIfAbsent(KEY, ConcurrentHashMap::new);
            cache = caches.computeIfAbsent(key, k -> new SoftConcurrentMap<>());

            if (retention == CacheRetention.REQUEST_SCOPED) {
                cacheType = "REQUEST_SCOPED";
                REQUEST_SCOPED_CACHE_COUNT.incrementAndGet();
            } else {
                cacheType = "SESSION_SCOPED";
                SESSION_SCOPED_CACHE_COUNT.incrementAndGet();
            }

            // Debug logging for cache sizes
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Cache access: type={}, request={}, cacheSize={}, totalCaches={}, key={}",
                        cacheType,
                        req.getClass().getSimpleName(),
                        cache.size(),
                        caches.size(),
                        key == ROOT ? "ROOT" : key.getClass().getSimpleName());
            }

        } else if (retention == CacheRetention.PERSISTENT) {
            cache = forever;
            cacheType = "PERSISTENT";
            PERSISTENT_CACHE_COUNT.incrementAndGet();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Cache access: type={}, request={}, persistentCacheSize={}",
                        cacheType,
                        req.getClass().getSimpleName(),
                        forever.size());
            }
        }

        if (cache != null) {
            boolean isNewEntry = !cache.containsKey(req);
            CachingSupplier<REQ, REP> result =
                    (CachingSupplier<REQ, REP>) cache.computeIfAbsent(req, r -> new CachingSupplier<>(supplier));

            if (isNewEntry) {
                TOTAL_CACHE_MISSES.incrementAndGet();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(
                            "Cache MISS: type={}, request={}, newCacheSize={}",
                            cacheType,
                            req.getClass().getSimpleName(),
                            cache.size());
                }
            } else {
                TOTAL_CACHE_HITS.incrementAndGet();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(
                            "Cache HIT: type={}, request={}",
                            cacheType,
                            req.getClass().getSimpleName());
                }
            }

            // Periodic memory usage reporting
            long totalRequests = TOTAL_CACHE_HITS.get() + TOTAL_CACHE_MISSES.get();
            if (totalRequests % 1000 == 0 && LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "Cache statistics: hits={}, misses={}, hitRatio={}%, "
                                + "requestScoped={}, sessionScoped={}, persistent={}, persistentSize={}",
                        TOTAL_CACHE_HITS.get(),
                        TOTAL_CACHE_MISSES.get(),
                        String.format("%.2f", (TOTAL_CACHE_HITS.get() * 100.0) / totalRequests),
                        REQUEST_SCOPED_CACHE_COUNT.get(),
                        SESSION_SCOPED_CACHE_COUNT.get(),
                        PERSISTENT_CACHE_COUNT.get(),
                        forever.size());
            }

            return result;
        } else {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("No cache: request={}", req.getClass().getSimpleName());
            }
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

    /**
     * Get detailed cache statistics for debugging memory issues.
     * This method provides insights into cache usage patterns and potential memory leaks.
     */
    public String getCacheStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("DefaultRequestCache Statistics:\n");
        stats.append("  Total cache hits: ").append(TOTAL_CACHE_HITS.get()).append("\n");
        stats.append("  Total cache misses: ").append(TOTAL_CACHE_MISSES.get()).append("\n");

        long total = TOTAL_CACHE_HITS.get() + TOTAL_CACHE_MISSES.get();
        if (total > 0) {
            stats.append("  Hit ratio: ")
                    .append(String.format("%.2f%%", (TOTAL_CACHE_HITS.get() * 100.0) / total))
                    .append("\n");
        }

        stats.append("  Request scoped cache accesses: ")
                .append(REQUEST_SCOPED_CACHE_COUNT.get())
                .append("\n");
        stats.append("  Session scoped cache accesses: ")
                .append(SESSION_SCOPED_CACHE_COUNT.get())
                .append("\n");
        stats.append("  Persistent cache accesses: ")
                .append(PERSISTENT_CACHE_COUNT.get())
                .append("\n");
        stats.append("  Persistent cache size: ").append(forever.size()).append("\n");

        return stats.toString();
    }

    /**
     * Force garbage collection and log cache sizes to help debug memory retention issues.
     * This method should only be used for debugging purposes.
     */
    public void debugMemoryUsage() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Before GC - Persistent cache size: {}", forever.size());

            // Force garbage collection to see if SoftConcurrentMap releases entries
            System.gc();
            System.runFinalization();

            try {
                Thread.sleep(100); // Give GC time to work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            LOGGER.info("After GC - Persistent cache size: {}", forever.size());
            LOGGER.info("Cache statistics:\n{}", getCacheStatistics());
        }
    }

    /**
     * Clears REQUEST_SCOPED cache entries for a specific request.
     * <p>
     * This method should be called when a top-level request (like model building) is completed
     * to prevent memory leaks from accumulated REQUEST_SCOPED cache entries.
     * <p>
     * The method identifies the outer request using the same logic as {@link #doGetOuterRequest(Request)}
     * and removes the corresponding cache entry from the session data.
     *
     * @param req the request whose REQUEST_SCOPED cache should be cleared
     * @param <REQ> the request type
     */
    public <REQ extends Request<?>> void clearRequestScopedCache(REQ req) {
        if (req.getSession() instanceof Session session) {
            Object outerRequestKey = doGetOuterRequest(req);
            Map<Object, Map<Object, CachingSupplier<?, ?>>> caches =
                    session.getData().get(KEY);

            if (caches != null) {
                Map<Object, CachingSupplier<?, ?>> removedCache = caches.remove(outerRequestKey);
                if (removedCache != null) {
                    int removedSize = removedCache.size();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                                "Cleared REQUEST_SCOPED cache for request: {}, removed {} entries, remaining caches: {}",
                                outerRequestKey.getClass().getSimpleName(),
                                removedSize,
                                caches.size());
                    }

                    // Update statistics
                    if (LOGGER.isInfoEnabled() && removedSize > 0) {
                        LOGGER.info(
                                "Cleared REQUEST_SCOPED cache: removed {} entries for request {}",
                                removedSize,
                                req.getClass().getSimpleName());
                    }
                }
            }
        }
    }

    /**
     * Clears all REQUEST_SCOPED cache entries for a session.
     * <p>
     * This method can be used to clean up all request-scoped caches when a session ends,
     * though individual request cleanup via {@link #clearRequestScopedCache(Request)} is preferred.
     *
     * @param session the session whose REQUEST_SCOPED caches should be cleared
     */
    public void clearAllRequestScopedCaches(Session session) {
        Map<Object, Map<Object, CachingSupplier<?, ?>>> caches =
                session.getData().get(KEY);
        if (caches != null) {
            // Remove all non-ROOT entries (ROOT is for SESSION_SCOPED)
            int totalRemoved = 0;
            var iterator = caches.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getKey() != ROOT) {
                    totalRemoved += entry.getValue().size();
                    iterator.remove();
                }
            }

            if (LOGGER.isInfoEnabled() && totalRemoved > 0) {
                LOGGER.info("Cleared all REQUEST_SCOPED caches: removed {} total entries", totalRemoved);
            }
        }
    }
}
