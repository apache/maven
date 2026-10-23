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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.api.spi.CacheFactory;
import org.apache.maven.api.spi.CacheRecord;
import org.apache.maven.api.spi.MavenCache;

/**
 * Default {@link CacheFactory} that creates simple, non-invalidating
 * {@link ConcurrentHashMap}-backed caches.
 *
 * <p>This implementation is appropriate for the standard Maven CLI where each
 * JVM process runs exactly one build. Since the process exits after the build,
 * cached entries are never stale within a single invocation, and no file-watching
 * is needed.
 *
 * <p>Embedders such as mvnd or m2e that keep a Maven session alive across
 * multiple builds should replace this factory by providing a higher-priority
 * implementation via the DI container.
 *
 * @since 4.1.0
 * @see DefaultMavenCache
 */
@Named
@Singleton
public class DefaultCacheFactory implements CacheFactory {

    @Override
    public <K, V extends CacheRecord> MavenCache<K, V> createCache() {
        return new DefaultMavenCache<>();
    }
}
