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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.maven.api.Constants;
import org.apache.maven.api.Session;
import org.apache.maven.api.cache.CacheMetadata;
import org.apache.maven.api.cache.CacheRetention;
import org.apache.maven.api.services.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves cache configuration for requests based on user-defined selectors.
 */
public class CacheConfigurationResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfigurationResolver.class);

    /**
     * Cache for parsed selectors per session to avoid re-parsing.
     */
    private static final ConcurrentMap<String, List<CacheSelector>> SELECTOR_CACHE = new ConcurrentHashMap<>();

    /**
     * Resolves cache configuration for the given request and session.
     *
     * @param req the request to resolve configuration for
     * @param session the session containing user properties
     * @return the resolved cache configuration
     */
    public static CacheConfig resolveConfig(Request<?> req, Session session) {
        // First check if request implements CacheMetadata for backward compatibility
        CacheRetention legacyRetention = null;
        if (req instanceof CacheMetadata metadata) {
            legacyRetention = metadata.getCacheRetention();
        }

        // Get user-defined configuration
        String configString = session.getUserProperties().get(Constants.MAVEN_CACHE_CONFIG_PROPERTY);
        if (configString == null || configString.trim().isEmpty()) {
            // No user configuration, use legacy behavior or defaults
            if (legacyRetention != null) {
                return new CacheConfig(legacyRetention, getDefaultReferenceType(legacyRetention));
            }
            return CacheConfig.DEFAULT;
        }

        // Parse and cache selectors
        List<CacheSelector> selectors = SELECTOR_CACHE.computeIfAbsent(configString, CacheSelectorParser::parse);

        // Find all matching selectors and merge them (most specific first)
        PartialCacheConfig mergedConfig = null;
        for (CacheSelector selector : selectors) {
            if (selector.matches(req)) {
                if (mergedConfig == null) {
                    mergedConfig = selector.config();
                    LOGGER.debug(
                            "Cache config for {}: matched selector '{}' with config {}",
                            req.getClass().getSimpleName(),
                            selector,
                            selector.config());
                } else {
                    PartialCacheConfig previousConfig = mergedConfig;
                    mergedConfig = mergedConfig.mergeWith(selector.config());
                    LOGGER.debug(
                            "Cache config for {}: merged selector '{}' with previous config {} -> {}",
                            req.getClass().getSimpleName(),
                            selector,
                            previousConfig,
                            mergedConfig);
                }

                // If we have a complete configuration, we can stop
                if (mergedConfig.isComplete()) {
                    break;
                }
            }
        }

        // Convert merged partial config to complete config
        if (mergedConfig != null && !mergedConfig.isEmpty()) {
            CacheConfig finalConfig = mergedConfig.toComplete();
            LOGGER.debug("Final cache config for {}: {}", req.getClass().getSimpleName(), finalConfig);
            return finalConfig;
        }

        // No selector matched, use legacy behavior or defaults
        if (legacyRetention != null) {
            CacheConfig config = new CacheConfig(legacyRetention, getDefaultReferenceType(legacyRetention));
            LOGGER.debug(
                    "Cache config for {}: {} (legacy CacheMetadata)",
                    req.getClass().getSimpleName(),
                    config);
            return config;
        }

        LOGGER.debug("Cache config for {}: {} (default)", req.getClass().getSimpleName(), CacheConfig.DEFAULT);
        return CacheConfig.DEFAULT;
    }

    /**
     * Gets the default reference type for a given cache retention.
     * This maintains backward compatibility with the original hardcoded behavior.
     */
    private static Cache.ReferenceType getDefaultReferenceType(CacheRetention retention) {
        return switch (retention) {
            case SESSION_SCOPED -> Cache.ReferenceType.SOFT;
            case REQUEST_SCOPED -> Cache.ReferenceType.SOFT; // Changed from HARD to SOFT for consistency
            case PERSISTENT -> Cache.ReferenceType.HARD;
            case DISABLED -> Cache.ReferenceType.NONE;
        };
    }

    /**
     * Clears the selector cache. Useful for testing.
     */
    public static void clearCache() {
        SELECTOR_CACHE.clear();
    }
}
