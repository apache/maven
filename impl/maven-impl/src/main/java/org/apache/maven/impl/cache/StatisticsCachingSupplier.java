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

import java.util.function.Function;

import org.apache.maven.api.cache.CacheRetention;

/**
 * A statistics-aware caching supplier that tracks cache hits and misses.
 * Extends the basic CachingSupplier to provide detailed metrics about cache performance.
 *
 * @param <REQ> The request type
 * @param <REP> The response type
 */
public class StatisticsCachingSupplier<REQ, REP> extends CachingSupplier<REQ, REP> {
    private final DefaultCacheStatistics statistics;
    private final String requestType;
    private final CacheRetention retention;

    public StatisticsCachingSupplier(
            Function<REQ, REP> supplier,
            DefaultCacheStatistics statistics,
            String requestType,
            CacheRetention retention) {
        super(supplier);
        this.statistics = statistics;
        this.requestType = requestType;
        this.retention = retention;
    }

    @Override
    @SuppressWarnings({"unchecked", "checkstyle:InnerAssignment"})
    public REP apply(REQ req) {
        Object v;

        if ((v = value) == null) {
            synchronized (this) {
                if ((v = value) == null) {
                    // This is a cache miss - we need to compute the value
                    statistics.recordMiss(requestType, retention);
                    try {
                        v = value = supplier.apply(req);
                    } catch (Exception e) {
                        v = value = new AltRes(e);
                        statistics.recordCachedException();
                    }
                } else {
                    // Another thread computed the value while we were waiting
                    statistics.recordHit(requestType, retention);
                }
            }
        } else {
            // This is a cache hit - value was already computed
            statistics.recordHit(requestType, retention);
        }

        if (v instanceof AltRes altRes) {
            DefaultRequestCache.uncheckedThrow(altRes.throwable);
        }
        return (REP) v;
    }
}
