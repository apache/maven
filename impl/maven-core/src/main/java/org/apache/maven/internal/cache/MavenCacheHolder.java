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
package org.apache.maven.internal.cache;

import java.nio.file.Path;

import org.apache.maven.api.spi.CacheRecord;
import org.apache.maven.api.spi.MavenCache;

/**
 * Internal marker interface that exposes a component's underlying {@link MavenCache}
 * to the {@link DefaultCacheManager}.
 *
 * <p>Each Maven cache component ({@code DefaultPluginRealmCache},
 * {@code DefaultExtensionRealmCache}, etc.) implements this interface so that
 * {@code DefaultCacheManager} can perform cross-cutting operations such as path-based
 * invalidation and full flush without knowing the specific key and value types of
 * each individual cache.
 *
 * @since 4.1.0
 */
public interface MavenCacheHolder {

    /**
     * Removes all entries from this component's cache whose
     * {@link CacheRecord#getDependencyPaths() dependency paths} contain the given path,
     * calling {@link CacheRecord#invalidate()} on each removed entry.
     *
     * @param path the file path that has changed
     */
    void invalidatePath(Path path);

    /**
     * Removes all entries from this component's cache, calling
     * {@link CacheRecord#invalidate()} on each entry.
     */
    void flushCache();
}
