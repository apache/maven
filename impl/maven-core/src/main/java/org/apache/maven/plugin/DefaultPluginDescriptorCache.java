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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.spi.CacheFactory;
import org.apache.maven.api.spi.MavenCache;
import org.apache.maven.internal.cache.MavenCacheHolder;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.sisu.Typed;

/**
 * Caches raw plugin descriptors. A raw plugin descriptor is a descriptor that has just been extracted from the plugin
 * artifact and does not contain any runtime specific data. The cache must not be used for descriptors that hold runtime
 * data like the plugin realm. <strong>Warning:</strong> This is an internal utility interface that is only public for
 * technical reasons, it is not part of the public API. In particular, this interface can be changed or deleted without
 * prior notice.
 *
 * @since 3.0
 */
@Named
@Singleton
@Typed({PluginDescriptorCache.class, MavenCacheHolder.class})
public class DefaultPluginDescriptorCache implements PluginDescriptorCache, MavenCacheHolder {

    /**
     * Internal {@link CacheRecord} wrapper for a plugin descriptor entry.
     * Descriptor caches have no direct file dependencies tracked (the descriptor is raw metadata),
     * so invalidation flushes all entries conservatively.
     */
    private static final class DescriptorRecord implements org.apache.maven.api.spi.CacheRecord {

        private final PluginDescriptor descriptor;

        DescriptorRecord(PluginDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public Stream<Path> getDependencyPaths() {
            // Plugin descriptors are read from JARs, but we do not track the JAR paths
            // at this level. Invalidation is handled conservatively by flushing all
            // descriptor entries when any plugin artifact changes.
            return Stream.empty();
        }

        @Override
        public void invalidate() {
            // No resources to release for descriptor entries
        }
    }

    private final MavenCache<Key, DescriptorRecord> cache;
    private final Map<Key, Key> keys = new ConcurrentHashMap<>();

    @Inject
    public DefaultPluginDescriptorCache(CacheFactory cacheFactory) {
        this.cache = cacheFactory.createCache();
    }

    @Override
    public void flush() {
        cache.clear();
        keys.clear();
    }

    @Override
    public Key createKey(Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session) {
        return keys.computeIfAbsent(new CacheKey(plugin, repositories, session), k -> k);
    }

    @Override
    public PluginDescriptor get(Key cacheKey) {
        DescriptorRecord record = cache.get(cacheKey);
        return record != null ? clone(record.descriptor) : null;
    }

    @Override
    public PluginDescriptor get(Key key, PluginDescriptorSupplier supplier)
            throws PluginDescriptorParsingException, PluginResolutionException, InvalidPluginDescriptorException {

        DescriptorRecord record = cache.get(key);
        if (record == null) {
            synchronized (key) {
                record = cache.get(key);
                if (record == null) {
                    PluginDescriptor desc = supplier.load();
                    DescriptorRecord newRecord = new DescriptorRecord(clone(desc));
                    cache.computeIfAbsent(key, k -> newRecord);
                    return clone(desc);
                }
            }
        }
        return clone(record.descriptor);
    }

    @Override
    public void put(Key cacheKey, PluginDescriptor pluginDescriptor) {
        cache.put(cacheKey, new DescriptorRecord(clone(pluginDescriptor)));
    }

    protected static PluginDescriptor clone(PluginDescriptor original) {
        return new PluginDescriptor(original);
    }

    // MavenCacheHolder

    @Override
    public void invalidatePath(Path path) {
        // Descriptor entries have no tracked file paths; flush all conservatively.
        flush();
    }

    @Override
    public void flushCache() {
        flush();
    }

    private static final class CacheKey implements Key {

        private final String groupId;

        private final String artifactId;

        private final String version;

        private final WorkspaceRepository workspace;

        private final LocalRepository localRepo;

        private final List<RemoteRepository> repositories;

        private final int hashCode;

        CacheKey(Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session) {
            groupId = plugin.getGroupId();
            artifactId = plugin.getArtifactId();
            version = plugin.getVersion();

            workspace = RepositoryUtils.getWorkspace(session);
            localRepo = session.getLocalRepository();
            this.repositories = new ArrayList<>(repositories.size());
            for (RemoteRepository repository : repositories) {
                if (repository.isRepositoryManager()) {
                    this.repositories.addAll(repository.getMirroredRepositories());
                } else {
                    this.repositories.add(repository);
                }
            }

            int hash = 17;
            hash = hash * 31 + groupId.hashCode();
            hash = hash * 31 + artifactId.hashCode();
            hash = hash * 31 + version.hashCode();
            hash = hash * 31 + hash(workspace);
            hash = hash * 31 + localRepo.hashCode();
            hash = hash * 31 + RepositoryUtils.repositoriesHashCode(repositories);
            this.hashCode = hash;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof CacheKey that) {
                return Objects.equals(this.artifactId, that.artifactId)
                        && Objects.equals(this.groupId, that.groupId)
                        && Objects.equals(this.version, that.version)
                        && Objects.equals(this.localRepo, that.localRepo)
                        && Objects.equals(this.workspace, that.workspace)
                        && RepositoryUtils.repositoriesEquals(this.repositories, that.repositories);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return groupId + ':' + artifactId + ':' + version;
        }

        private static int hash(Object obj) {
            return obj != null ? obj.hashCode() : 0;
        }
    }
}
