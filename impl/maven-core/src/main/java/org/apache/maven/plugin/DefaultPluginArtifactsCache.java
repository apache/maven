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
package org.apache.maven.plugin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.spi.CacheFactory;
import org.apache.maven.api.spi.MavenCache;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.internal.cache.MavenCacheHolder;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.sisu.Typed;

/**
 * Default cache for resolved plugin artifacts.
 */
@Named
@Singleton
@Typed({PluginArtifactsCache.class, MavenCacheHolder.class})
public class DefaultPluginArtifactsCache implements PluginArtifactsCache, MavenCacheHolder {

    /**
     * Internal {@link CacheRecord} wrapper for a plugin artifacts entry.
     * Exposes artifact file paths as dependency paths; no realm disposal on invalidation.
     */
    protected static class PluginArtifactsRecord implements org.apache.maven.api.spi.CacheRecord {

        private final List<Artifact> artifacts;
        private final PluginResolutionException exception;

        PluginArtifactsRecord(List<Artifact> artifacts) {
            this.artifacts = Collections.unmodifiableList(new ArrayList<>(artifacts));
            this.exception = null;
        }

        PluginArtifactsRecord(PluginResolutionException exception) {
            this.artifacts = null;
            this.exception = exception;
        }

        public List<Artifact> getArtifacts() {
            return artifacts;
        }

        public PluginResolutionException getException() {
            return exception;
        }

        @Override
        public Stream<Path> getDependencyPaths() {
            if (artifacts == null) {
                return Stream.empty();
            }
            return artifacts.stream()
                    .map(Artifact::getFile)
                    .filter(Objects::nonNull)
                    .map(File::toPath);
        }

        @Override
        public void invalidate() {
            // No resources to release for artifact resolution entries
        }

        /** Converts to the public {@link PluginArtifactsCache.CacheRecord} view. */
        PluginArtifactsCache.CacheRecord toCacheRecord() {
            if (exception != null) {
                return new PluginArtifactsCache.CacheRecord(exception);
            }
            return new PluginArtifactsCache.CacheRecord(artifacts);
        }
    }

    /**
     * CacheKey
     */
    protected static class CacheKey implements Key {
        private final Plugin plugin;

        private final WorkspaceRepository workspace;

        private final LocalRepository localRepo;

        private final List<RemoteRepository> repositories;

        private final DependencyFilter filter;

        private final int hashCode;

        public CacheKey(
                Plugin plugin,
                DependencyFilter extensionFilter,
                List<RemoteRepository> repositories,
                RepositorySystemSession session) {
            this.plugin = plugin.clone();
            workspace = RepositoryUtils.getWorkspace(session);
            this.localRepo = session.getLocalRepository();
            this.repositories = new ArrayList<>(repositories.size());
            for (RemoteRepository repository : repositories) {
                if (repository.isRepositoryManager()) {
                    this.repositories.addAll(repository.getMirroredRepositories());
                } else {
                    this.repositories.add(repository);
                }
            }
            this.filter = extensionFilter;

            int hash = 17;
            hash = hash * 31 + CacheUtils.pluginHashCode(plugin);
            hash = hash * 31 + Objects.hashCode(workspace);
            hash = hash * 31 + Objects.hashCode(localRepo);
            hash = hash * 31 + RepositoryUtils.repositoriesHashCode(repositories);
            hash = hash * 31 + Objects.hashCode(extensionFilter);
            this.hashCode = hash;
        }

        @Override
        public String toString() {
            return plugin.getId();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (o instanceof CacheKey that) {
                return CacheUtils.pluginEquals(plugin, that.plugin)
                        && Objects.equals(workspace, that.workspace)
                        && Objects.equals(localRepo, that.localRepo)
                        && RepositoryUtils.repositoriesEquals(repositories, that.repositories)
                        && Objects.equals(filter, that.filter);
            } else {
                return false;
            }
        }
    }

    protected final MavenCache<Key, PluginArtifactsRecord> cache;

    @Inject
    public DefaultPluginArtifactsCache(CacheFactory cacheFactory) {
        this.cache = cacheFactory.createCache();
    }

    @Override
    public Key createKey(
            Plugin plugin,
            DependencyFilter extensionFilter,
            List<RemoteRepository> repositories,
            RepositorySystemSession session) {
        return new CacheKey(plugin, extensionFilter, repositories, session);
    }

    @Override
    public PluginArtifactsCache.CacheRecord get(Key key) throws PluginResolutionException {
        PluginArtifactsRecord record = cache.get(key);

        if (record != null && record.getException() != null) {
            throw record.getException();
        }

        return record != null ? record.toCacheRecord() : null;
    }

    @Override
    public PluginArtifactsCache.CacheRecord put(Key key, List<Artifact> pluginArtifacts) {
        Objects.requireNonNull(pluginArtifacts, "pluginArtifacts cannot be null");

        PluginArtifactsRecord newRecord = new PluginArtifactsRecord(pluginArtifacts);
        PluginArtifactsRecord winner = cache.computeIfAbsent(key, k -> newRecord);
        return winner.toCacheRecord();
    }

    @Override
    public PluginArtifactsCache.CacheRecord put(Key key, PluginResolutionException exception) {
        Objects.requireNonNull(exception, "exception cannot be null");

        PluginArtifactsRecord newRecord = new PluginArtifactsRecord(exception);
        PluginArtifactsRecord winner = cache.computeIfAbsent(key, k -> newRecord);
        return winner.toCacheRecord();
    }

    @Override
    public void flush() {
        cache.clear();
    }

    @Override
    public void invalidatePath(Path path) {
        cache.removeIf((k, v) -> v.getDependencyPaths().anyMatch(path::equals));
    }

    @Override
    public void flushCache() {
        cache.clear();
    }

    protected static int pluginHashCode(Plugin plugin) {
        return CacheUtils.pluginHashCode(plugin);
    }

    protected static boolean pluginEquals(Plugin a, Plugin b) {
        return CacheUtils.pluginEquals(a, b);
    }

    @Override
    public void register(MavenProject project, Key cacheKey, PluginArtifactsCache.CacheRecord record) {
        // default cache does not track record usage
    }
}
