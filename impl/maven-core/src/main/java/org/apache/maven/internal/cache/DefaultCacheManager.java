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

import javax.inject.Inject;
import javax.inject.Named;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Service;
import org.apache.maven.api.di.SessionScoped;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.CacheManager;
import org.apache.maven.impl.InternalSession;
import org.eclipse.sisu.Typed;

/**
 * Default implementation of {@link CacheManager}.
 *
 * <p>Delegates invalidation to all {@link MavenCacheHolder} components discovered
 * in the DI container (one per internal cache). Each holder knows how to remove
 * entries whose {@link org.apache.maven.api.spi.CacheRecord#getDependencyPaths()
 * dependency paths} match the requested path.
 *
 * <p>This service is session-scoped so that it can access session-level services
 * (e.g. {@link ArtifactManager}) while the underlying cache holders are singletons
 * shared across sessions.
 *
 * @since 4.1.0
 */
@Named
@Typed({CacheManager.class, Service.class})
@SessionScoped
public class DefaultCacheManager implements CacheManager {

    private final List<MavenCacheHolder> holders;
    private final InternalSession session;

    @Inject
    public DefaultCacheManager(List<MavenCacheHolder> holders, InternalSession session) {
        this.holders = holders;
        this.session = session;
    }

    @Override
    public void invalidate(Artifact artifact) {
        ArtifactManager artifactManager = session.getService(ArtifactManager.class);
        Optional<Path> artifactPath = artifactManager.getPath(artifact);
        artifactPath.ifPresent(p -> holders.forEach(h -> h.invalidatePath(p)));
    }

    @Override
    public void invalidateProject(Path pom) {
        holders.forEach(h -> h.invalidatePath(pom));
    }

    @Override
    public void flush() {
        holders.forEach(MavenCacheHolder::flushCache);
    }
}
