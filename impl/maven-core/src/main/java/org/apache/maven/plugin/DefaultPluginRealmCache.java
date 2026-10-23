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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.spi.CacheFactory;
import org.apache.maven.api.spi.MavenCache;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.internal.cache.MavenCacheHolder;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.sisu.Typed;

/**
 * Default PluginCache implementation. Assumes cached data does not change.
 */
@Named
@Singleton
@Typed({PluginRealmCache.class, MavenCacheHolder.class})
public class DefaultPluginRealmCache implements PluginRealmCache, MavenCacheHolder, Disposable {

    /**
     * Internal {@link org.apache.maven.api.spi.CacheRecord} wrapper for a plugin realm entry.
     * Exposes dependency paths (from artifact files) and disposes the realm on invalidation.
     */
    protected static class PluginRealmRecord implements org.apache.maven.api.spi.CacheRecord {

        private final ClassRealm realm;
        private final List<Artifact> artifacts;
        private final DependencyNode dependencyNode;
        private final PluginRealmCache.CacheRecord publicRecord;

        PluginRealmRecord(ClassRealm realm, List<Artifact> artifacts, DependencyNode dependencyNode) {
            this.realm = realm;
            this.artifacts = Collections.unmodifiableList(new ArrayList<>(artifacts));
            this.dependencyNode = dependencyNode;
            this.publicRecord = new PluginRealmCache.CacheRecord(realm, this.artifacts, dependencyNode);
        }

        public ClassRealm getRealm() {
            return realm;
        }

        public List<Artifact> getArtifacts() {
            return artifacts;
        }

        public DependencyNode getDependencyNode() {
            return dependencyNode;
        }

        @Override
        public Stream<Path> getDependencyPaths() {
            return artifacts.stream()
                    .map(Artifact::getFile)
                    .filter(Objects::nonNull)
                    .map(File::toPath);
        }

        @Override
        public void invalidate() {
            try {
                realm.getWorld().disposeRealm(realm.getId());
            } catch (NoSuchRealmException e) {
                // realm was already disposed — ignore
            }
        }

        /** Converts to the public {@link PluginRealmCache.CacheRecord} view (same instance always). */
        PluginRealmCache.CacheRecord toCacheRecord() {
            return publicRecord;
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

        private final ClassLoader parentRealm;

        private final Map<String, ClassLoader> foreignImports;

        private final DependencyFilter filter;

        private final int hashCode;

        public CacheKey(
                Plugin plugin,
                ClassLoader parentRealm,
                Map<String, ClassLoader> foreignImports,
                DependencyFilter dependencyFilter,
                List<RemoteRepository> repositories,
                RepositorySystemSession session) {
            this.plugin = plugin.clone();
            this.workspace = RepositoryUtils.getWorkspace(session);
            this.localRepo = session.getLocalRepository();
            this.repositories = new ArrayList<>(repositories.size());
            for (RemoteRepository repository : repositories) {
                if (repository.isRepositoryManager()) {
                    this.repositories.addAll(repository.getMirroredRepositories());
                } else {
                    this.repositories.add(repository);
                }
            }
            this.parentRealm = parentRealm;
            this.foreignImports = (foreignImports != null) ? foreignImports : Collections.emptyMap();
            this.filter = dependencyFilter;

            int hash = 17;
            hash = hash * 31 + CacheUtils.pluginHashCode(plugin);
            hash = hash * 31 + Objects.hashCode(workspace);
            hash = hash * 31 + Objects.hashCode(localRepo);
            hash = hash * 31 + RepositoryUtils.repositoriesHashCode(repositories);
            hash = hash * 31 + Objects.hashCode(parentRealm);
            hash = hash * 31 + this.foreignImports.hashCode();
            hash = hash * 31 + Objects.hashCode(dependencyFilter);
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
                return parentRealm == that.parentRealm
                        && CacheUtils.pluginEquals(plugin, that.plugin)
                        && Objects.equals(workspace, that.workspace)
                        && Objects.equals(localRepo, that.localRepo)
                        && RepositoryUtils.repositoriesEquals(this.repositories, that.repositories)
                        && Objects.equals(filter, that.filter)
                        && Objects.equals(foreignImports, that.foreignImports);
            } else {
                return false;
            }
        }
    }

    protected final MavenCache<Key, PluginRealmRecord> cache;

    @Inject
    public DefaultPluginRealmCache(CacheFactory cacheFactory) {
        this.cache = cacheFactory.createCache();
    }

    @Override
    public Key createKey(
            Plugin plugin,
            ClassLoader parentRealm,
            Map<String, ClassLoader> foreignImports,
            DependencyFilter dependencyFilter,
            List<RemoteRepository> repositories,
            RepositorySystemSession session) {
        return new CacheKey(plugin, parentRealm, foreignImports, dependencyFilter, repositories, session);
    }

    @Override
    public CacheRecord get(Key key) {
        PluginRealmRecord record = cache.get(key);
        return record != null ? record.toCacheRecord() : null;
    }

    @Override
    public CacheRecord get(Key key, PluginRealmSupplier supplier)
            throws PluginResolutionException, PluginContainerException {
        try {
            PluginRealmRecord record = cache.computeIfAbsent(key, k -> {
                try {
                    CacheRecord cr = supplier.load();
                    return new PluginRealmRecord(cr.getRealm(), cr.getArtifacts(), cr.getDependencyNode());
                } catch (PluginResolutionException | PluginContainerException e) {
                    throw new RuntimeException(e);
                }
            });
            return record.toCacheRecord();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof PluginResolutionException pluginResolutionException) {
                throw pluginResolutionException;
            }
            if (e.getCause() instanceof PluginContainerException pluginContainerException) {
                throw pluginContainerException;
            }
            throw e;
        }
    }

    @Override
    public CacheRecord put(Key key, ClassRealm pluginRealm, List<Artifact> pluginArtifacts) {
        return put(key, pluginRealm, pluginArtifacts, null);
    }

    @Override
    public CacheRecord put(
            Key key, ClassRealm pluginRealm, List<Artifact> pluginArtifacts, DependencyNode dependencyNode) {
        Objects.requireNonNull(pluginRealm, "pluginRealm cannot be null");
        Objects.requireNonNull(pluginArtifacts, "pluginArtifacts cannot be null");

        PluginRealmRecord newRecord = new PluginRealmRecord(pluginRealm, pluginArtifacts, dependencyNode);
        PluginRealmRecord existing = cache.put(key, newRecord);
        if (existing != null) {
            throw new IllegalStateException("Duplicate plugin realm for plugin " + key);
        }
        return newRecord.toCacheRecord();
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
    public void register(MavenProject project, Key key, CacheRecord record) {
        // default cache does not track plugin usage
    }

    @Override
    public void dispose() {
        cache.clearSilently();
    }
}
