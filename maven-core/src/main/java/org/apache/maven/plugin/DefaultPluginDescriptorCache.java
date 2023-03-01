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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceRepository;

/**
 * Caches raw plugin descriptors. A raw plugin descriptor is a descriptor that has just been extracted from the plugin
 * artifact and does not contain any runtime specific data. The cache must not be used for descriptors that hold runtime
 * data like the plugin realm. <strong>Warning:</strong> This is an internal utility interface that is only public for
 * technical reasons, it is not part of the public API. In particular, this interface can be changed or deleted without
 * prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
@Component(role = PluginDescriptorCache.class)
public class DefaultPluginDescriptorCache implements PluginDescriptorCache {

    private Map<Key, PluginDescriptor> descriptors = new ConcurrentHashMap<>(128);

    public void flush() {
        descriptors.clear();
    }

    public Key createKey(Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session) {
        return new CacheKey(plugin, repositories, session);
    }

    public PluginDescriptor get(Key cacheKey) {
        return clone(descriptors.get(cacheKey));
    }

    @Override
    public PluginDescriptor get(Key key, PluginDescriptorSupplier supplier)
            throws PluginDescriptorParsingException, PluginResolutionException, InvalidPluginDescriptorException {
        try {
            return clone(descriptors.computeIfAbsent(key, k -> {
                try {
                    return clone(supplier.load());
                } catch (PluginDescriptorParsingException
                        | PluginResolutionException
                        | InvalidPluginDescriptorException e) {
                    throw new RuntimeException(e);
                }
            }));
        } catch (RuntimeException e) {
            if (e.getCause() instanceof PluginDescriptorParsingException) {
                throw (PluginDescriptorParsingException) e.getCause();
            }
            if (e.getCause() instanceof PluginResolutionException) {
                throw (PluginResolutionException) e.getCause();
            }
            if (e.getCause() instanceof InvalidPluginDescriptorException) {
                throw (InvalidPluginDescriptorException) e.getCause();
            }
            throw e;
        }
    }

    public void put(Key cacheKey, PluginDescriptor pluginDescriptor) {
        descriptors.put(cacheKey, clone(pluginDescriptor));
    }

    protected static PluginDescriptor clone(PluginDescriptor original) {
        PluginDescriptor clone = null;

        if (original != null) {
            clone = new PluginDescriptor();

            clone.setGroupId(original.getGroupId());
            clone.setArtifactId(original.getArtifactId());
            clone.setVersion(original.getVersion());
            clone.setGoalPrefix(original.getGoalPrefix());
            clone.setInheritedByDefault(original.isInheritedByDefault());

            clone.setName(original.getName());
            clone.setDescription(original.getDescription());
            clone.setRequiredMavenVersion(original.getRequiredMavenVersion());

            clone.setPluginArtifact(ArtifactUtils.copyArtifactSafe(original.getPluginArtifact()));

            clone.setComponents(clone(original.getMojos(), clone));
            clone.setId(original.getId());
            clone.setIsolatedRealm(original.isIsolatedRealm());
            clone.setSource(original.getSource());

            clone.setDependencies(original.getDependencies());
        }

        return clone;
    }

    private static List<ComponentDescriptor<?>> clone(List<MojoDescriptor> mojos, PluginDescriptor pluginDescriptor) {
        List<ComponentDescriptor<?>> clones = null;

        if (mojos != null) {
            clones = new ArrayList<>(mojos.size());

            for (MojoDescriptor mojo : mojos) {
                MojoDescriptor clone = mojo.clone();
                clone.setPluginDescriptor(pluginDescriptor);
                clones.add(clone);
            }
        }

        return clones;
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

            if (!(obj instanceof CacheKey)) {
                return false;
            }

            CacheKey that = (CacheKey) obj;

            return Objects.equals(this.artifactId, that.artifactId)
                    && Objects.equals(this.groupId, that.groupId)
                    && Objects.equals(this.version, that.version)
                    && Objects.equals(this.localRepo, that.localRepo)
                    && Objects.equals(this.workspace, that.workspace)
                    && RepositoryUtils.repositoriesEquals(this.repositories, that.repositories);
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
