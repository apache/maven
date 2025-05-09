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

/**
 * Provides a caching infrastructure for Maven requests and their results.
 * <p>
 * This package contains the core components for implementing and managing caches in Maven:
 * <ul>
 *   <li>{@link org.apache.maven.api.cache.RequestCache} - The main interface for caching request results</li>
 *   <li>{@link org.apache.maven.api.cache.RequestCacheFactory} - Factory for creating cache instances</li>
 *   <li>{@link org.apache.maven.api.cache.CacheMetadata} - Configuration for cache behavior and lifecycle</li>
 * </ul>
 * <p>
 * The caching system supports different retention periods through {@link org.apache.maven.api.cache.CacheRetention}:
 * <ul>
 *   <li>PERSISTENT - Data persists across Maven invocations</li>
 *   <li>SESSION_SCOPED - Data retained for the duration of a Maven session</li>
 *   <li>REQUEST_SCOPED - Data retained only for the current build request</li>
 *   <li>DISABLED - No caching performed</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * RequestCache cache = cacheFactory.createCache();
 * Result result = cache.request(myRequest, req -> {
 *     // Expensive operation to compute result
 *     return computedResult;
 * });
 * </pre>
 * <p>
 * The package also provides support for batch operations through {@link org.apache.maven.api.cache.BatchRequestException}
 * and {@link org.apache.maven.api.cache.RequestResult} which help manage multiple requests and their results.
 *
 * @since 4.0.0
 */
@Experimental
package org.apache.maven.api.cache;

import org.apache.maven.api.annotations.Experimental;
