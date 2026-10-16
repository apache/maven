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

import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.maven.api.annotations.Experimental;

/**
 * Contract for cache entries that track their file dependencies.
 *
 * <p>Each {@code CacheRecord} knows which files on disk it depends on (JARs, POMs, etc.).
 * A {@link CacheFactory} implementation — typically supplied by an embedder such as
 * mvnd or m2e — can watch those paths and call {@link #invalidate()} whenever a file
 * changes, ensuring that stale classloaders are discarded before the next build.
 *
 * <p>The default Maven CLI implementation creates simple, non-invalidating caches, so
 * both methods are never called during a single-shot build. Embedders that keep daemons
 * running across multiple builds should provide a custom {@link CacheFactory} that wraps
 * records with file-watching logic.
 *
 * @since 4.1.0
 * @see CacheFactory
 * @see MavenCache
 */
@Experimental
public interface CacheRecord {

    /**
     * Returns the file paths this cache entry depends on.
     *
     * <p>Typically these are the JAR and POM files whose content was used to build
     * the cached entry (e.g. the JARs loaded into a {@code ClassRealm}).  An embedder
     * watches these paths and calls {@link #invalidate()} when any of them changes.
     *
     * @return a stream of file (not directory) paths; never {@code null}, may be empty
     */
    Stream<Path> getDependencyPaths();

    /**
     * Releases any resources held by this cache entry.
     *
     * <p>Called by the cache when this record is removed due to invalidation or a
     * full cache flush.  Implementations should dispose {@code ClassRealm} instances
     * here to avoid classloader leaks.  The method must be idempotent.
     */
    void invalidate();
}
