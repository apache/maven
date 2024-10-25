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
package org.apache.maven.plugin.version.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.MetadataReader;
import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.PluginIncompatibleException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.SessionData;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves a version for a plugin.
 *
 * @since 3.0
 */
@Named
@Singleton
public class DefaultPluginVersionResolver implements PluginVersionResolver {
    private static final String REPOSITORY_CONTEXT = "plugin";

    private static final Object CACHE_KEY = new Object();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RepositorySystem repositorySystem;
    private final MetadataReader metadataReader;
    private final MavenPluginManager pluginManager;
    private final VersionScheme versionScheme;

    @Inject
    public DefaultPluginVersionResolver(
            RepositorySystem repositorySystem,
            MetadataReader metadataReader,
            MavenPluginManager pluginManager,
            VersionScheme versionScheme) {
        this.repositorySystem = repositorySystem;
        this.metadataReader = metadataReader;
        this.pluginManager = pluginManager;
        this.versionScheme = versionScheme;
    }

    @Override
    public PluginVersionResult resolve(PluginVersionRequest request) throws PluginVersionResolutionException {
        PluginVersionResult result = resolveFromProject(request);

        if (result == null) {
            ConcurrentMap<Key, PluginVersionResult> cache = getCache(request);
            Key key = getKey(request);
            result = cache.get(key);

            if (result == null) {
                result = resolveFromRepository(request);

                logger.debug(
                        "Resolved plugin version for {}:{} to {} from repository {}",
                        request.getGroupId(),
                        request.getArtifactId(),
                        result.getVersion(),
                        result.getRepository());

                cache.putIfAbsent(key, result);
            } else {
                logger.debug(
                        "Reusing cached resolved plugin version for {}:{} to {} from POM {}",
                        request.getGroupId(),
                        request.getArtifactId(),
                        result.getVersion(),
                        request.getPom());
            }
        } else {
            logger.debug(
                    "Reusing cached resolved plugin version for {}:{} to {} from POM {}",
                    request.getGroupId(),
                    request.getArtifactId(),
                    result.getVersion(),
                    request.getPom());
        }

        return result;
    }

    private PluginVersionResult resolveFromRepository(PluginVersionRequest request)
            throws PluginVersionResolutionException {
        RequestTrace trace = RequestTrace.newChild(null, request);

        DefaultPluginVersionResult result = new DefaultPluginVersionResult();

        org.eclipse.aether.metadata.Metadata metadata = new DefaultMetadata(
                request.getGroupId(),
                request.getArtifactId(),
                "maven-metadata.xml",
                DefaultMetadata.Nature.RELEASE_OR_SNAPSHOT);

        List<MetadataRequest> requests = new ArrayList<>();

        requests.add(new MetadataRequest(metadata, null, REPOSITORY_CONTEXT).setTrace(trace));

        for (RemoteRepository repository : request.getRepositories()) {
            requests.add(new MetadataRequest(metadata, repository, REPOSITORY_CONTEXT).setTrace(trace));
        }

        List<MetadataResult> results = repositorySystem.resolveMetadata(request.getRepositorySession(), requests);

        Versions versions = new Versions();

        for (MetadataResult res : results) {
            ArtifactRepository repository = res.getRequest().getRepository();
            if (repository == null) {
                repository = request.getRepositorySession().getLocalRepository();
            }

            mergeMetadata(request.getRepositorySession(), trace, versions, res.getMetadata(), repository);
        }

        selectVersion(result, request, versions);

        return result;
    }

    private void selectVersion(DefaultPluginVersionResult result, PluginVersionRequest request, Versions versions)
            throws PluginVersionResolutionException {
        String version = null;
        ArtifactRepository repo = null;
        boolean resolvedPluginVersions = !versions.versions.isEmpty();
        boolean searchPerformed = false;

        if (versions.releaseVersion != null && !versions.releaseVersion.isEmpty()) {
            version = versions.releaseVersion;
            repo = versions.releaseRepository;
        } else if (versions.latestVersion != null && !versions.latestVersion.isEmpty()) {
            version = versions.latestVersion;
            repo = versions.latestRepository;
        }
        if (version != null && !isCompatible(request, version)) {
            logger.info(
                    "Latest version of plugin {}:{} failed compatibility check",
                    request.getGroupId(),
                    request.getArtifactId());
            versions.versions.remove(version);
            version = null;
            searchPerformed = true;
        }

        if (version == null) {
            TreeSet<Version> releases = new TreeSet<>(Collections.reverseOrder());
            TreeSet<Version> snapshots = new TreeSet<>(Collections.reverseOrder());

            for (String ver : versions.versions.keySet()) {
                try {
                    Version v = versionScheme.parseVersion(ver);

                    if (ver.endsWith("-SNAPSHOT")) {
                        snapshots.add(v);
                    } else {
                        releases.add(v);
                    }
                } catch (InvalidVersionSpecificationException e) {
                    // ignore
                }
            }

            if (!releases.isEmpty()) {
                logger.info(
                        "Looking for compatible RELEASE version of plugin {}:{}",
                        request.getGroupId(),
                        request.getArtifactId());
                for (Version v : releases) {
                    String ver = v.toString();
                    if (isCompatible(request, ver)) {
                        version = ver;
                        repo = versions.versions.get(version);
                        break;
                    }
                }
            }

            if (version == null && !snapshots.isEmpty()) {
                logger.info(
                        "Looking for compatible SNAPSHOT version of plugin {}:{}",
                        request.getGroupId(),
                        request.getArtifactId());
                for (Version v : snapshots) {
                    String ver = v.toString();
                    if (isCompatible(request, ver)) {
                        version = ver;
                        repo = versions.versions.get(version);
                        break;
                    }
                }
            }
        }

        if (version != null) {
            // if LATEST worked out of the box, remain silent as today, otherwise inform user about search result
            if (searchPerformed) {
                logger.info("Selected plugin {}:{}:{}", request.getGroupId(), request.getArtifactId(), version);
            }
            result.setVersion(version);
            result.setRepository(repo);
        } else {
            logger.warn(
                    resolvedPluginVersions
                            ? "Could not find compatible version of plugin {}:{} in any plugin repository"
                            : "Plugin {}:{} not found in any plugin repository",
                    request.getGroupId(),
                    request.getArtifactId());
            throw new PluginVersionResolutionException(
                    request.getGroupId(),
                    request.getArtifactId(),
                    request.getRepositorySession().getLocalRepository(),
                    request.getRepositories(),
                    resolvedPluginVersions
                            ? "Could not find compatible plugin version in any plugin repository"
                            : "Plugin not found in any plugin repository");
        }
    }

    private boolean isCompatible(PluginVersionRequest request, String version) {
        Plugin plugin = new Plugin();
        plugin.setGroupId(request.getGroupId());
        plugin.setArtifactId(request.getArtifactId());
        plugin.setVersion(version);

        PluginDescriptor pluginDescriptor;

        try {
            pluginDescriptor = pluginManager.getPluginDescriptor(
                    plugin, request.getRepositories(), request.getRepositorySession());
        } catch (PluginResolutionException e) {
            logger.debug("Ignoring unresolvable plugin version {}", version, e);
            return false;
        } catch (Exception e) {
            // ignore for now and delay failure to higher level processing
            return true;
        }

        try {
            pluginManager.checkPrerequisites(pluginDescriptor);
        } catch (PluginIncompatibleException e) {
            if (logger.isDebugEnabled()) {
                logger.warn("Ignoring incompatible plugin version {}:", version, e);
            } else {
                logger.warn("Ignoring incompatible plugin version {}: {}", version, e.getMessage());
            }
            return false;
        }

        return true;
    }

    private void mergeMetadata(
            RepositorySystemSession session,
            RequestTrace trace,
            Versions versions,
            org.eclipse.aether.metadata.Metadata metadata,
            ArtifactRepository repository) {
        if (metadata != null && metadata.getFile() != null && metadata.getFile().isFile()) {
            try {
                Map<String, ?> options = Collections.singletonMap(MetadataReader.IS_STRICT, Boolean.FALSE);

                Metadata repoMetadata = metadataReader.read(metadata.getFile(), options);

                mergeMetadata(versions, repoMetadata, repository);
            } catch (IOException e) {
                invalidMetadata(session, trace, metadata, repository, e);
            }
        }
    }

    private void invalidMetadata(
            RepositorySystemSession session,
            RequestTrace trace,
            org.eclipse.aether.metadata.Metadata metadata,
            ArtifactRepository repository,
            Exception exception) {
        RepositoryListener listener = session.getRepositoryListener();
        if (listener != null) {
            RepositoryEvent.Builder event = new RepositoryEvent.Builder(session, EventType.METADATA_INVALID);
            event.setTrace(trace);
            event.setMetadata(metadata);
            event.setException(exception);
            event.setRepository(repository);
            listener.metadataInvalid(event.build());
        }
    }

    private void mergeMetadata(Versions versions, Metadata source, ArtifactRepository repository) {
        Versioning versioning = source.getVersioning();
        if (versioning != null) {
            String timestamp = versioning.getLastUpdated() == null
                    ? ""
                    : versioning.getLastUpdated().trim();

            if (versioning.getRelease() != null
                    && !versioning.getRelease().isEmpty()
                    && timestamp.compareTo(versions.releaseTimestamp) > 0) {
                versions.releaseVersion = versioning.getRelease();
                versions.releaseTimestamp = timestamp;
                versions.releaseRepository = repository;
            }

            if (versioning.getLatest() != null
                    && !versioning.getLatest().isEmpty()
                    && timestamp.compareTo(versions.latestTimestamp) > 0) {
                versions.latestVersion = versioning.getLatest();
                versions.latestTimestamp = timestamp;
                versions.latestRepository = repository;
            }

            for (String version : versioning.getVersions()) {
                if (!versions.versions.containsKey(version)) {
                    versions.versions.put(version, repository);
                }
            }
        }
    }

    private PluginVersionResult resolveFromProject(PluginVersionRequest request) {
        PluginVersionResult result = null;

        if (request.getPom() != null && request.getPom().getBuild() != null) {
            Build build = request.getPom().getBuild();

            result = resolveFromProject(request, build.getPlugins());

            if (result == null && build.getPluginManagement() != null) {
                result = resolveFromProject(request, build.getPluginManagement().getPlugins());
            }
        }

        return result;
    }

    private PluginVersionResult resolveFromProject(PluginVersionRequest request, List<Plugin> plugins) {
        for (Plugin plugin : plugins) {
            if (request.getGroupId().equals(plugin.getGroupId())
                    && request.getArtifactId().equals(plugin.getArtifactId())) {
                if (plugin.getVersion() != null) {
                    return new DefaultPluginVersionResult(plugin.getVersion());
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private ConcurrentMap<Key, PluginVersionResult> getCache(PluginVersionRequest request) {
        SessionData data = request.getRepositorySession().getData();
        return (ConcurrentMap<Key, PluginVersionResult>)
                data.computeIfAbsent(CACHE_KEY, () -> new ConcurrentHashMap<>(256));
    }

    private static Key getKey(PluginVersionRequest request) {
        return new Key(request.getGroupId(), request.getArtifactId(), request.getRepositories());
    }

    static class Key {
        final String groupId;
        final String artifactId;
        final List<RemoteRepository> repositories;
        final int hash;

        Key(String groupId, String artifactId, List<RemoteRepository> repositories) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.repositories = repositories;
            this.hash = Objects.hash(groupId, artifactId, repositories);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key key = (Key) o;
            return groupId.equals(key.groupId)
                    && artifactId.equals(key.artifactId)
                    && repositories.equals(key.repositories);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    static class Versions {

        String releaseVersion = "";

        String releaseTimestamp = "";

        ArtifactRepository releaseRepository;

        String latestVersion = "";

        String latestTimestamp = "";

        ArtifactRepository latestRepository;

        Map<String, ArtifactRepository> versions = new LinkedHashMap<>();
    }
}
