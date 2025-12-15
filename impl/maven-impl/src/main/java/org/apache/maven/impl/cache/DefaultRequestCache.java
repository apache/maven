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
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.apache.maven.api.Constants;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.cache.CacheMetadata;
import org.apache.maven.api.cache.CacheRetention;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.Result;
import org.apache.maven.impl.InternalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultRequestCache extends AbstractRequestCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRequestCache.class);

    protected static final SessionData.Key<Cache> KEY = SessionData.key(Cache.class, CacheMetadata.class);
    protected static final Object ROOT = "ROOT";

    // Comprehensive cache statistics
    private final CacheStatistics statistics = new CacheStatistics();

    private static volatile boolean shutdownHookRegistered = false;
    private static final List<CacheStatistics> ALL_STATISTICS = new ArrayList<CacheStatistics>();

    // Synchronized method to ensure shutdown hook is registered only once
    private static synchronized void ensureShutdownHookRegistered() {
        if (!shutdownHookRegistered) {
            Runtime.getRuntime()
                    .addShutdownHook(new Thread(
                            () -> {
                                // Check if cache stats should be displayed
                                for (CacheStatistics statistics : ALL_STATISTICS) {
                                    if (statistics.getTotalRequests() > 0) {
                                        System.err.println("[INFO] " + formatCacheStatistics(statistics));
                                    }
                                }
                            },
                            "DefaultRequestCache-Statistics"));
            shutdownHookRegistered = true;
        }
    }

    public DefaultRequestCache() {
        // Register cache size suppliers for different retention policies
        // Note: These provide approximate sizes since the improved cache architecture
        // uses distributed caches across sessions
        statistics.registerCacheSizeSupplier(CacheRetention.PERSISTENT, () -> 0L);
        statistics.registerCacheSizeSupplier(CacheRetention.SESSION_SCOPED, () -> 0L);
        statistics.registerCacheSizeSupplier(CacheRetention.REQUEST_SCOPED, () -> 0L);

        synchronized (ALL_STATISTICS) {
            ALL_STATISTICS.add(statistics);
        }
    }

    /**
     * Formats comprehensive cache statistics for display.
     *
     * @param stats the cache statistics to format
     * @return a formatted string containing cache statistics
     */
    static String formatCacheStatistics(CacheStatistics stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("Request Cache Statistics:\n");
        sb.append("  Total requests: ").append(stats.getTotalRequests()).append("\n");
        sb.append("  Cache hits: ").append(stats.getCacheHits()).append("\n");
        sb.append("  Cache misses: ").append(stats.getCacheMisses()).append("\n");
        sb.append("  Hit ratio: ")
                .append(String.format(Locale.ENGLISH, "%.2f%%", stats.getHitRatio()))
                .append("\n");

        // Show eviction statistics
        long totalEvictions = stats.getTotalEvictions();
        if (totalEvictions > 0) {
            sb.append("  Evictions:\n");
            sb.append("    Key evictions: ")
                    .append(stats.getKeyEvictions())
                    .append(" (")
                    .append(String.format(Locale.ENGLISH, "%.1f%%", stats.getKeyEvictionRatio()))
                    .append(")\n");
            sb.append("    Value evictions: ")
                    .append(stats.getValueEvictions())
                    .append(" (")
                    .append(String.format(Locale.ENGLISH, "%.1f%%", stats.getValueEvictionRatio()))
                    .append(")\n");
            sb.append("    Total evictions: ").append(totalEvictions).append("\n");
        }

        // Show retention policy breakdown
        var retentionStats = stats.getRetentionStatistics();
        if (!retentionStats.isEmpty()) {
            sb.append("  By retention policy:\n");
            retentionStats.forEach((retention, retStats) -> {
                sb.append("    ")
                        .append(retention)
                        .append(": ")
                        .append(retStats.getHits())
                        .append(" hits, ")
                        .append(retStats.getMisses())
                        .append(" misses (")
                        .append(String.format(Locale.ENGLISH, "%.1f%%", retStats.getHitRatio()))
                        .append(" hit ratio)");

                // Add eviction info for this retention policy
                long retKeyEvictions = retStats.getKeyEvictions();
                long retValueEvictions = retStats.getValueEvictions();
                if (retKeyEvictions > 0 || retValueEvictions > 0) {
                    sb.append(", ")
                            .append(retKeyEvictions)
                            .append(" key evictions, ")
                            .append(retValueEvictions)
                            .append(" value evictions");
                }
                sb.append("\n");
            });
        }

        // Show reference type statistics
        var refTypeStats = stats.getReferenceTypeStatistics();
        if (!refTypeStats.isEmpty()) {
            sb.append("  Reference type usage:\n");
            refTypeStats.entrySet().stream()
                    .sorted((e1, e2) ->
                            Long.compare(e2.getValue().getTotal(), e1.getValue().getTotal()))
                    .forEach(entry -> {
                        var refStats = entry.getValue();
                        sb.append("    ")
                                .append(entry.getKey())
                                .append(": ")
                                .append(refStats.getCacheCreations())
                                .append(" caches, ")
                                .append(refStats.getTotal())
                                .append(" accesses (")
                                .append(String.format(Locale.ENGLISH, "%.1f%%", refStats.getHitRatio()))
                                .append(" hit ratio)\n");
                    });
        }

        // Show top request types
        var requestStats = stats.getRequestTypeStatistics();
        if (!requestStats.isEmpty()) {
            sb.append("  Top request types:\n");
            requestStats.entrySet().stream()
                    .sorted((e1, e2) ->
                            Long.compare(e2.getValue().getTotal(), e1.getValue().getTotal()))
                    // .limit(5)
                    .forEach(entry -> {
                        var reqStats = entry.getValue();
                        sb.append("    ")
                                .append(entry.getKey())
                                .append(": ")
                                .append(reqStats.getTotal())
                                .append(" requests (")
                                .append(String.format(Locale.ENGLISH, "%.1f%%", reqStats.getHitRatio()))
                                .append(" hit ratio)\n");
                    });
        }

        return sb.toString();
    }

    public CacheStatistics getStatistics() {
        return statistics;
    }

    @Override
    @SuppressWarnings({"unchecked", "checkstyle:MethodLength"})
    protected <REQ extends Request<?>, REP extends Result<REQ>> CachingSupplier<REQ, REP> doCache(
            REQ req, Function<REQ, REP> supplier) {
        // Early return for non-Session requests (e.g., ProtoSession)
        if (!(req.getSession() instanceof Session session)) {
            // Record as a miss since no caching is performed for non-Session requests
            statistics.recordMiss(req.getClass().getSimpleName(), CacheRetention.DISABLED);
            return new CachingSupplier<>(supplier);
        }

        // Register shutdown hook for conditional statistics display
        boolean cacheStatsEnabled = isCacheStatsEnabled(session);
        if (cacheStatsEnabled) {
            ensureShutdownHookRegistered();
        }

        CacheConfig config = getCacheConfig(req, session);
        CacheRetention retention = config.scope();
        Cache.ReferenceType referenceType = config.referenceType();
        Cache.ReferenceType keyReferenceType = config.getEffectiveKeyReferenceType();
        Cache.ReferenceType valueReferenceType = config.getEffectiveValueReferenceType();

        // Debug logging to verify reference types (disabled)
        // System.err.println("DEBUG: Cache config for " + req.getClass().getSimpleName() + ": retention=" + retention
        //         + ", keyRef=" + keyReferenceType + ", valueRef=" + valueReferenceType);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "Cache config for {}: retention={}, keyRef={}, valueRef={}",
                    req.getClass().getSimpleName(),
                    retention,
                    keyReferenceType,
                    valueReferenceType);
        }

        // Handle disabled caching
        if (retention == CacheRetention.DISABLED
                || keyReferenceType == Cache.ReferenceType.NONE
                || valueReferenceType == Cache.ReferenceType.NONE) {
            // Record as a miss since no caching is performed
            statistics.recordMiss(req.getClass().getSimpleName(), retention);
            return new CachingSupplier<>(supplier);
        }

        Cache<Object, CachingSupplier<?, ?>> cache = null;
        String cacheType = "NONE";

        if (retention == CacheRetention.SESSION_SCOPED) {
            Cache<Object, Cache<Object, CachingSupplier<?, ?>>> caches = session.getData()
                    .computeIfAbsent(KEY, () -> {
                        if (config.hasSeparateKeyValueReferenceTypes()) {
                            LOGGER.debug(
                                    "Creating SESSION_SCOPED parent cache with key={}, value={}",
                                    keyReferenceType,
                                    valueReferenceType);
                            return Cache.newCache(keyReferenceType, valueReferenceType, "RequestCache-SESSION-Parent");
                        } else {
                            return Cache.newCache(Cache.ReferenceType.SOFT, "RequestCache-SESSION-Parent");
                        }
                    });

            // Use separate key/value reference types if configured
            if (config.hasSeparateKeyValueReferenceTypes()) {
                cache = caches.computeIfAbsent(ROOT, k -> {
                    LOGGER.debug(
                            "Creating SESSION_SCOPED cache with key={}, value={}",
                            keyReferenceType,
                            valueReferenceType);
                    Cache<Object, CachingSupplier<?, ?>> newCache =
                            Cache.newCache(keyReferenceType, valueReferenceType, "RequestCache-SESSION");
                    statistics.recordCacheCreation(
                            keyReferenceType.toString(), valueReferenceType.toString(), retention);
                    setupEvictionListenerIfNeeded(newCache, retention);

                    // Debug logging to verify actual reference types (disabled)
                    // if (newCache instanceof Cache.RefConcurrentMap<?, ?> refMap) {
                    //     System.err.println("DEBUG: Created cache '" + refMap.getName() + "' - requested key="
                    //             + keyReferenceType
                    //             + ", value=" + valueReferenceType + ", actual key=" + refMap.getKeyReferenceType()
                    //             + ", actual value=" + refMap.getValueReferenceType());
                    // }
                    return newCache;
                });
            } else {
                cache = caches.computeIfAbsent(ROOT, k -> {
                    Cache<Object, CachingSupplier<?, ?>> newCache =
                            Cache.newCache(referenceType, "RequestCache-SESSION");
                    statistics.recordCacheCreation(referenceType.toString(), referenceType.toString(), retention);
                    setupEvictionListenerIfNeeded(newCache, retention);
                    return newCache;
                });
            }
            cacheType = "SESSION_SCOPED";
            // Debug logging for cache sizes
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Cache access: type={}, request={}, cacheSize={}, totalCaches={}, key={}",
                        cacheType,
                        req.getClass().getSimpleName(),
                        cache.size(),
                        caches.size(),
                        ROOT);
            }
        } else if (retention == CacheRetention.REQUEST_SCOPED) {
            Object key = doGetOuterRequest(req);
            Cache<Object, Cache<Object, CachingSupplier<?, ?>>> caches = session.getData()
                    .computeIfAbsent(KEY, () -> {
                        if (config.hasSeparateKeyValueReferenceTypes()) {
                            LOGGER.debug(
                                    "Creating REQUEST_SCOPED parent cache with key={}, value={}",
                                    keyReferenceType,
                                    valueReferenceType);
                            return Cache.newCache(keyReferenceType, valueReferenceType, "RequestCache-REQUEST-Parent");
                        } else {
                            return Cache.newCache(Cache.ReferenceType.SOFT, "RequestCache-REQUEST-Parent");
                        }
                    });

            // Use separate key/value reference types if configured
            if (config.hasSeparateKeyValueReferenceTypes()) {
                cache = caches.computeIfAbsent(key, k -> {
                    LOGGER.debug(
                            "Creating REQUEST_SCOPED cache with key={}, value={}",
                            keyReferenceType,
                            valueReferenceType);
                    Cache<Object, CachingSupplier<?, ?>> newCache =
                            Cache.newCache(keyReferenceType, valueReferenceType, "RequestCache-REQUEST");
                    statistics.recordCacheCreation(
                            keyReferenceType.toString(), valueReferenceType.toString(), retention);
                    setupEvictionListenerIfNeeded(newCache, retention);
                    return newCache;
                });
            } else {
                cache = caches.computeIfAbsent(key, k -> {
                    Cache<Object, CachingSupplier<?, ?>> newCache =
                            Cache.newCache(referenceType, "RequestCache-REQUEST");
                    statistics.recordCacheCreation(referenceType.toString(), referenceType.toString(), retention);
                    setupEvictionListenerIfNeeded(newCache, retention);
                    return newCache;
                });
            }
            cacheType = "REQUEST_SCOPED";

            // Debug logging for cache sizes
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Cache access: type={}, request={}, cacheSize={}, totalCaches={}, key={}",
                        cacheType,
                        req.getClass().getSimpleName(),
                        cache.size(),
                        caches.size(),
                        key.getClass().getSimpleName());
            }

        } else if (retention == CacheRetention.PERSISTENT) {
            Cache<Object, Cache<Object, CachingSupplier<?, ?>>> caches = session.getData()
                    .computeIfAbsent(KEY, () -> {
                        if (config.hasSeparateKeyValueReferenceTypes()) {
                            LOGGER.debug(
                                    "Creating PERSISTENT parent cache with key={}, value={}",
                                    keyReferenceType,
                                    valueReferenceType);
                            return Cache.newCache(
                                    keyReferenceType, valueReferenceType, "RequestCache-PERSISTENT-Parent");
                        } else {
                            return Cache.newCache(Cache.ReferenceType.SOFT, "RequestCache-PERSISTENT-Parent");
                        }
                    });

            // Use separate key/value reference types if configured
            if (config.hasSeparateKeyValueReferenceTypes()) {
                cache = caches.computeIfAbsent(KEY, k -> {
                    LOGGER.debug(
                            "Creating PERSISTENT cache with key={}, value={}", keyReferenceType, valueReferenceType);
                    Cache<Object, CachingSupplier<?, ?>> newCache =
                            Cache.newCache(keyReferenceType, valueReferenceType, "RequestCache-PERSISTENT");
                    statistics.recordCacheCreation(
                            keyReferenceType.toString(), valueReferenceType.toString(), retention);
                    setupEvictionListenerIfNeeded(newCache, retention);
                    return newCache;
                });
            } else {
                cache = caches.computeIfAbsent(KEY, k -> {
                    Cache<Object, CachingSupplier<?, ?>> newCache =
                            Cache.newCache(referenceType, "RequestCache-PERSISTENT");
                    statistics.recordCacheCreation(referenceType.toString(), referenceType.toString(), retention);
                    setupEvictionListenerIfNeeded(newCache, retention);
                    return newCache;
                });
            }
            cacheType = "PERSISTENT";

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Cache access: type={}, request={}, cacheSize={}",
                        cacheType,
                        req.getClass().getSimpleName(),
                        cache.size());
            }
        }

        if (cache != null) {
            // Set up eviction listener if this is a RefConcurrentMap
            setupEvictionListenerIfNeeded(cache, retention);

            boolean isNewEntry = !cache.containsKey(req);
            CachingSupplier<REQ, REP> result = (CachingSupplier<REQ, REP>)
                    cache.computeIfAbsent(req, r -> new CachingSupplier<>(supplier), referenceType);

            // Record statistics using the comprehensive system
            String requestType = req.getClass().getSimpleName();

            // Record reference type statistics
            if (cache instanceof Cache.RefConcurrentMap<?, ?> refMap) {
                statistics.recordCacheAccess(
                        refMap.getKeyReferenceType().toString(),
                        refMap.getValueReferenceType().toString(),
                        !isNewEntry);
            }

            if (isNewEntry) {
                statistics.recordMiss(requestType, retention);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(
                            "Cache MISS: type={}, request={}, newCacheSize={}", cacheType, requestType, cache.size());
                }
            } else {
                statistics.recordHit(requestType, retention);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Cache HIT: type={}, request={}", cacheType, requestType);
                }
            }
            return result;
        } else {
            // Record as a miss since no cache was available
            statistics.recordMiss(req.getClass().getSimpleName(), retention);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("No cache: request={}", req.getClass().getSimpleName());
            }
            return new CachingSupplier<>(supplier);
        }
    }

    private static boolean isCacheStatsEnabled(Session session) {
        String showStats = session.getUserProperties().get(Constants.MAVEN_CACHE_STATS);
        return Boolean.parseBoolean(showStats);
    }

    /**
     * Sets up eviction listener for the cache if it's a RefConcurrentMap.
     * This avoids memory leaks by having the cache push events to statistics
     * instead of statistics holding references to caches.
     */
    private void setupEvictionListenerIfNeeded(Cache<Object, CachingSupplier<?, ?>> cache, CacheRetention retention) {
        if (cache instanceof Cache.RefConcurrentMap<?, ?> refMap) {
            // Set up the eviction listener (it's safe to set multiple times)
            refMap.setEvictionListener(new Cache.EvictionListener() {
                @Override
                public void onKeyEviction() {
                    statistics.recordKeyEviction(retention);
                }

                @Override
                public void onValueEviction() {
                    statistics.recordValueEviction(retention);
                }
            });
        }
    }

    private <REQ extends Request<?>> Object doGetOuterRequest(REQ req) {
        RequestTrace trace = req.getTrace();
        if (trace == null && req.getSession() instanceof Session session) {
            trace = InternalSession.from(session).getCurrentTrace();
        }
        while (trace != null && trace.parent() != null) {
            trace = trace.parent();
        }
        return trace != null && trace.data() != null ? trace.data() : req;
    }

    /**
     * Gets the cache configuration for the given request and session.
     *
     * @param req the request to get configuration for
     * @param session the session containing user properties
     * @return the resolved cache configuration
     */
    private <REQ extends Request<?>> CacheConfig getCacheConfig(REQ req, Session session) {
        return CacheConfigurationResolver.resolveConfig(req, session);
    }
}
