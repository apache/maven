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

import org.apache.maven.api.spi.CacheFactory;
import org.apache.maven.api.spi.MavenCache;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.internal.cache.MavenCacheHolder;
import org.apache.maven.project.ExtensionDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.eclipse.sisu.Typed;

/**
 * Default extension realm cache implementation. Assumes cached data does not change.
 */
@Named
@Singleton
@Typed({ExtensionRealmCache.class, MavenCacheHolder.class})
public class DefaultExtensionRealmCache implements ExtensionRealmCache, MavenCacheHolder, Disposable {

    /**
     * Internal {@link CacheRecord} wrapper for an extension realm entry.
     */
    protected static class ExtensionRealmRecord implements org.apache.maven.api.spi.CacheRecord {

        private final ClassRealm realm;
        private final ExtensionDescriptor descriptor;
        private final List<Artifact> artifacts;

        ExtensionRealmRecord(ClassRealm realm, ExtensionDescriptor descriptor, List<Artifact> artifacts) {
            this.realm = realm;
            this.descriptor = descriptor;
            this.artifacts = artifacts != null ? Collections.unmodifiableList(new ArrayList<>(artifacts)) : List.of();
        }

        public ClassRealm getRealm() {
            return realm;
        }

        public ExtensionDescriptor getDescriptor() {
            return descriptor;
        }

        public List<Artifact> getArtifacts() {
            return artifacts;
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

        /** Converts to the public {@link ExtensionRealmCache.CacheRecord} view. */
        ExtensionRealmCache.CacheRecord toCacheRecord() {
            return new ExtensionRealmCache.CacheRecord(realm, descriptor, artifacts);
        }
    }

    /**
     * CacheKey
     */
    protected static class CacheKey implements Key {

        private final List<File> files;

        private final List<Long> timestamps;

        private final List<Long> sizes;

        private final List<String> ids;

        private final int hashCode;

        public CacheKey(List<Artifact> extensionArtifacts) {
            this.files = new ArrayList<>(extensionArtifacts.size());
            this.timestamps = new ArrayList<>(extensionArtifacts.size());
            this.sizes = new ArrayList<>(extensionArtifacts.size());
            this.ids = new ArrayList<>(extensionArtifacts.size());

            for (Artifact artifact : extensionArtifacts) {
                File file = artifact.getFile();
                files.add(file);
                timestamps.add((file != null) ? Long.valueOf(file.lastModified()) : Long.valueOf(0));
                sizes.add((file != null) ? Long.valueOf(file.length()) : Long.valueOf(0));
                ids.add(artifact.getVersion());
            }

            this.hashCode =
                    31 * files.hashCode() + 31 * ids.hashCode() + 31 * timestamps.hashCode() + 31 * sizes.hashCode();
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

            if (o instanceof CacheKey other) {
                return ids.equals(other.ids)
                        && files.equals(other.files)
                        && timestamps.equals(other.timestamps)
                        && sizes.equals(other.sizes);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return files.toString();
        }
    }

    protected final MavenCache<Key, ExtensionRealmRecord> cache;

    @Inject
    public DefaultExtensionRealmCache(CacheFactory cacheFactory) {
        this.cache = cacheFactory.createCache();
    }

    @Override
    public Key createKey(List<Artifact> extensionArtifacts) {
        return new CacheKey(extensionArtifacts);
    }

    @Override
    public ExtensionRealmCache.CacheRecord get(Key key) {
        ExtensionRealmRecord record = cache.get(key);
        return record != null ? record.toCacheRecord() : null;
    }

    @Override
    public ExtensionRealmCache.CacheRecord put(
            Key key, ClassRealm extensionRealm, ExtensionDescriptor extensionDescriptor, List<Artifact> artifacts) {
        Objects.requireNonNull(extensionRealm, "extensionRealm cannot be null");

        ExtensionRealmRecord newRecord = new ExtensionRealmRecord(extensionRealm, extensionDescriptor, artifacts);
        // Use computeIfAbsent for putIfAbsent semantics: preserve first-write-wins under concurrency
        ExtensionRealmRecord winner = cache.computeIfAbsent(key, k -> newRecord);
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

    @Override
    public void register(MavenProject project, Key key, ExtensionRealmCache.CacheRecord record) {
        // default cache does not track extension usage
    }

    @Override
    public void dispose() {
        cache.clearSilently();
    }
}
