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
package org.apache.maven.repository.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Repository;
import org.apache.maven.model.building.ArtifactModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;

/**
 * A model resolver to assist building of dependency POMs. This resolver gives priority to those repositories that have
 * been initially specified and repositories discovered in dependency POMs are recessively merged into the search chain.
 *
 * @see DefaultArtifactDescriptorReader
 * @deprecated since 4.0.0, use {@code maven-api-impl} jar instead
 */
@Deprecated(since = "4.0.0")
class DefaultModelResolver implements ModelResolver {

    private final RepositorySystemSession session;

    private final RequestTrace trace;

    private final String context;

    private List<RemoteRepository> repositories;

    private final List<RemoteRepository> externalRepositories;

    private final ArtifactResolver resolver;

    private final VersionRangeResolver versionRangeResolver;

    private final RemoteRepositoryManager remoteRepositoryManager;

    private final Set<String> repositoryIds;

    DefaultModelResolver(
            RepositorySystemSession session,
            RequestTrace trace,
            String context,
            ArtifactResolver resolver,
            VersionRangeResolver versionRangeResolver,
            RemoteRepositoryManager remoteRepositoryManager,
            List<RemoteRepository> repositories) {
        this.session = session;
        this.trace = trace;
        this.context = context;
        this.resolver = resolver;
        this.versionRangeResolver = versionRangeResolver;
        this.remoteRepositoryManager = remoteRepositoryManager;
        this.repositories = repositories;
        this.externalRepositories = Collections.unmodifiableList(new ArrayList<>(repositories));

        this.repositoryIds = new HashSet<>();
    }

    private DefaultModelResolver(DefaultModelResolver original) {
        this.session = original.session;
        this.trace = original.trace;
        this.context = original.context;
        this.resolver = original.resolver;
        this.versionRangeResolver = original.versionRangeResolver;
        this.remoteRepositoryManager = original.remoteRepositoryManager;
        this.repositories = new ArrayList<>(original.repositories);
        this.externalRepositories = original.externalRepositories;
        this.repositoryIds = new HashSet<>(original.repositoryIds);
    }

    @Override
    public void addRepository(Repository repository) throws InvalidRepositoryException {
        addRepository(repository, false);
    }

    @Override
    public void addRepository(final Repository repository, boolean replace) throws InvalidRepositoryException {
        if (session.isIgnoreArtifactDescriptorRepositories()) {
            return;
        }

        if (!repositoryIds.add(repository.getId())) {
            if (!replace) {
                return;
            }

            removeMatchingRepository(repositories, repository.getId());
        }

        List<RemoteRepository> newRepositories = Collections.singletonList(
                ArtifactDescriptorUtils.toRemoteRepository(new org.apache.maven.model.Repository(repository)));

        this.repositories = remoteRepositoryManager.aggregateRepositories(session, repositories, newRepositories, true);
    }

    private static void removeMatchingRepository(Iterable<RemoteRepository> repositories, final String id) {
        Iterator<RemoteRepository> iterator = repositories.iterator();
        while (iterator.hasNext()) {
            RemoteRepository remoteRepository = iterator.next();
            if (remoteRepository.getId().equals(id)) {
                iterator.remove();
            }
        }
    }

    @Override
    public ModelResolver newCopy() {
        return new DefaultModelResolver(this);
    }

    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, "", "pom", version);

        try {
            ArtifactRequest request = new ArtifactRequest(pomArtifact, repositories, context);
            request.setTrace(trace);
            pomArtifact = resolver.resolveArtifact(session, request).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
        }

        return new ArtifactModelSource(pomArtifact.getPath(), groupId, artifactId, version);
    }

    @Override
    public ModelSource resolveModel(final Parent parent, final AtomicReference<Parent> modified)
            throws UnresolvableModelException {
        try {
            final Artifact artifact =
                    new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), "", "pom", parent.getVersion());

            final VersionRangeRequest versionRangeRequest = new VersionRangeRequest(artifact, repositories, context);
            versionRangeRequest.setTrace(trace);

            final VersionRangeResult versionRangeResult =
                    versionRangeResolver.resolveVersionRange(session, versionRangeRequest);

            if (versionRangeResult.getHighestVersion() == null) {
                throw new UnresolvableModelException(
                        String.format(
                                "No versions matched the requested parent version range '%s'", parent.getVersion()),
                        parent.getGroupId(),
                        parent.getArtifactId(),
                        parent.getVersion());
            }

            if (versionRangeResult.getVersionConstraint() != null
                    && versionRangeResult.getVersionConstraint().getRange() != null
                    && versionRangeResult.getVersionConstraint().getRange().getUpperBound() == null) {
                // Message below is checked for in the MNG-2199 core IT.
                throw new UnresolvableModelException(
                        String.format(
                                "The requested parent version range '%s' does not specify an upper bound",
                                parent.getVersion()),
                        parent.getGroupId(),
                        parent.getArtifactId(),
                        parent.getVersion());
            }

            String newVersion = versionRangeResult.getHighestVersion().toString();
            if (!parent.getVersion().equals(newVersion)) {
                modified.set(parent.withVersion(newVersion));
            }

            return resolveModel(parent.getGroupId(), parent.getArtifactId(), newVersion);
        } catch (final VersionRangeResolutionException e) {
            throw new UnresolvableModelException(
                    e.getMessage(), parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), e);
        }
    }

    @Override
    public ModelSource resolveModel(final Dependency dependency, AtomicReference<Dependency> modified)
            throws UnresolvableModelException {
        try {
            final Artifact artifact = new DefaultArtifact(
                    dependency.getGroupId(), dependency.getArtifactId(), "", "pom", dependency.getVersion());

            final VersionRangeRequest versionRangeRequest = new VersionRangeRequest(artifact, repositories, context);
            versionRangeRequest.setTrace(trace);

            final VersionRangeResult versionRangeResult =
                    versionRangeResolver.resolveVersionRange(session, versionRangeRequest);

            if (versionRangeResult.getHighestVersion() == null) {
                throw new UnresolvableModelException(
                        String.format(
                                "No versions matched the requested dependency version range '%s'",
                                dependency.getVersion()),
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion());
            }

            if (versionRangeResult.getVersionConstraint() != null
                    && versionRangeResult.getVersionConstraint().getRange() != null
                    && versionRangeResult.getVersionConstraint().getRange().getUpperBound() == null) {
                // Message below is checked for in the MNG-4463 core IT.
                throw new UnresolvableModelException(
                        String.format(
                                "The requested dependency version range '%s' does not specify an upper bound",
                                dependency.getVersion()),
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion());
            }

            String newVersion = versionRangeResult.getHighestVersion().toString();
            if (!dependency.getVersion().equals(newVersion)) {
                modified.set(dependency.withVersion(newVersion));
            }

            return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), newVersion);
        } catch (VersionRangeResolutionException e) {
            throw new UnresolvableModelException(
                    e.getMessage(), dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), e);
        }
    }

    @Override
    public ModelSource resolveModel(org.apache.maven.model.Parent parent) throws UnresolvableModelException {
        AtomicReference<org.apache.maven.api.model.Parent> resolvedParent = new AtomicReference<>();
        ModelSource result = resolveModel(parent.getDelegate(), resolvedParent);
        if (resolvedParent.get() != null) {
            parent.setVersion(resolvedParent.get().getVersion());
        }
        return result;
    }

    @Override
    public ModelSource resolveModel(org.apache.maven.model.Dependency dependency) throws UnresolvableModelException {
        AtomicReference<org.apache.maven.api.model.Dependency> resolvedDependency = new AtomicReference<>();
        ModelSource result = resolveModel(dependency.getDelegate(), resolvedDependency);
        if (resolvedDependency.get() != null) {
            dependency.setVersion(resolvedDependency.get().getVersion());
        }
        return result;
    }

    @Override
    public void addRepository(org.apache.maven.model.Repository repository) throws InvalidRepositoryException {
        addRepository(repository.getDelegate());
    }

    @Override
    public void addRepository(org.apache.maven.model.Repository repository, boolean replace)
            throws InvalidRepositoryException {
        addRepository(repository.getDelegate(), replace);
    }
}
