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
package org.apache.maven.api.spi;

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;

/**
 * SPI for providing {@link MavenCache} instances with pluggable invalidation strategies.
 *
 * <p>Maven's default implementation ({@code DefaultCacheFactory}) creates simple
 * {@link java.util.concurrent.ConcurrentHashMap}-backed caches suitable for single-shot
 * CLI builds, where no invalidation is ever needed.
 *
 * <p>Embedders such as mvnd or m2e can replace this factory with one that wraps each
 * created cache with file-watching logic: when a JAR or POM that a {@link CacheRecord}
 * depends on changes on disk, the watcher calls {@link CacheRecord#invalidate()} on the
 * affected entry so that the next build starts with a fresh classloader.
 *
 * <p>There must be exactly one active {@code CacheFactory} in the container. Embedders
 * should register their implementation with a higher priority than the default so it
 * takes precedence.
 *
 * @since 4.1.0
 * @see MavenCache
 * @see CacheRecord
 */
@Experimental
@Consumer
@Named
@Singleton
public interface CacheFactory extends SpiService {

    /**
     * Creates a new, empty cache instance.
     *
     * <p>The returned cache must be thread-safe. The factory is free to wrap the
     * underlying storage with any invalidation strategy (e.g. file watchers, weak
     * references, TTL eviction).
     *
     * @param <K> the type of cache keys
     * @param <V> the type of cache values; extends {@link CacheRecord}
     * @return a new, empty {@link MavenCache} instance; never {@code null}
     */
    @Nonnull
    <K, V extends CacheRecord> MavenCache<K, V> createCache();
}
