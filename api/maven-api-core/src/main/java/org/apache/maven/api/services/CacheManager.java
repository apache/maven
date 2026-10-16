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
package org.apache.maven.api.services;

import java.nio.file.Path;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * High-level facade for invalidating Maven's internal component caches.
 *
 * <p>Maven maintains several caches for plugin realms, extension realms, project realms,
 * plugin artifacts, and plugin descriptors. Embedders (IDEs, daemons) that keep a Maven
 * session alive across multiple builds can use this service to evict stale entries
 * without knowing the details of each individual cache.
 *
 * <p>The default implementation delegates to each registered cache via their
 * {@link org.apache.maven.api.spi.MavenCache#removeIf} method, matching entries whose
 * {@link org.apache.maven.api.spi.CacheRecord#getDependencyPaths() dependency paths}
 * include the changed file. Embedders that rely on a custom
 * {@link org.apache.maven.api.spi.CacheFactory} (e.g. mvnd with file-watching) may
 * not need to call this service directly — their factory will trigger invalidation
 * automatically.
 *
 * @since 4.1.0
 * @see org.apache.maven.api.spi.CacheFactory
 * @see org.apache.maven.api.spi.CacheRecord
 */
@Experimental
public interface CacheManager extends Service {

    /**
     * Invalidates all cache entries that depend on any artifact file produced by the
     * given artifact.
     *
     * <p>Use this when a workspace artifact has been rebuilt: all plugin realms,
     * extension realms, and artifact resolution results that loaded a JAR or POM
     * from this artifact will be evicted so the next build picks up the new binaries.
     *
     * @param artifact the artifact whose files have changed; never {@code null}
     */
    void invalidate(@Nonnull Artifact artifact);

    /**
     * Invalidates all cache entries that depend on the given POM file.
     *
     * <p>Use this when a project POM changes in the workspace: cached entries that
     * loaded plugin descriptors or resolved dependencies using this POM will be evicted.
     *
     * @param pom the path to the POM file that changed; never {@code null}
     */
    void invalidateProject(@Nonnull Path pom);

    /**
     * Flushes all entries from all caches managed by this service.
     *
     * <p>This is the nuclear option — use it only when a targeted invalidation is not
     * possible (e.g. when the local repository contents have changed wholesale).
     * All {@link org.apache.maven.api.spi.CacheRecord#invalidate()} callbacks will
     * be invoked before the entries are removed.
     */
    void flush();
}
