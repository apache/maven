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
package org.apache.maven.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.impl.resolver.ArtifactDescriptorUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;

/**
 * A model resolver to assist building of projects. This resolver gives priority to those repositories that have been
 * declared in the POM.
 */
public class ProjectModelResolver implements ModelResolver {

    private final RepositorySystemSession session;

    private final RequestTrace trace;

    private final String context = "project";

    private List<RemoteRepository> repositories;

    private List<RemoteRepository> pomRepositories;

    private final List<RemoteRepository> externalRepositories;

    private final RepositorySystem resolver;

    private final RemoteRepositoryManager remoteRepositoryManager;

    private final Set<String> repositoryIds;

    private final ReactorModelPool modelPool;

    private final ProjectBuildingRequest.RepositoryMerging repositoryMerging;

    public ProjectModelResolver(
            RepositorySystemSession session,
            RequestTrace trace,
            RepositorySystem resolver,
            RemoteRepositoryManager remoteRepositoryManager,
            List<RemoteRepository> repositories,
            ProjectBuildingRequest.RepositoryMerging repositoryMerging,
            ReactorModelPool modelPool) {
        this.session = session;
        this.trace = trace;
        this.resolver = resolver;
        this.remoteRepositoryManager = remoteRepositoryManager;
        this.pomRepositories = new ArrayList<>();
        this.externalRepositories = Collections.unmodifiableList(new ArrayList<>(repositories));
        this.repositories = new ArrayList<>();
        this.repositories.addAll(externalRepositories);
        this.repositoryMerging = repositoryMerging;
        this.repositoryIds = new HashSet<>();
        this.modelPool = modelPool;
    }

    private ProjectModelResolver(ProjectModelResolver original) {
        this.session = original.session;
        this.trace = original.trace;
        this.resolver = original.resolver;
        this.remoteRepositoryManager = original.remoteRepositoryManager;
        this.pomRepositories = new ArrayList<>(original.pomRepositories);
        this.externalRepositories = original.externalRepositories;
        this.repositories = new ArrayList<>(original.repositories);
        this.repositoryMerging = original.repositoryMerging;
        this.repositoryIds = new HashSet<>(original.repositoryIds);
        this.modelPool = original.modelPool;
    }

    public void addRepository(Repository repository) throws InvalidRepositoryException {
        addRepository(repository, false);
    }

    @Override
    public void addRepository(final Repository repository, boolean replace) throws InvalidRepositoryException {
        if (!repositoryIds.add(repository.getId())) {
            if (!replace) {
                return;
            }

            // Remove any previous repository with this Id
            removeMatchingRepository(repositories, repository.getId());
            removeMatchingRepository(pomRepositories, repository.getId());
        }

        List<RemoteRepository> newRepositories =
                Collections.singletonList(ArtifactDescriptorUtils.toRemoteRepository(repository.getDelegate()));

        if (ProjectBuildingRequest.RepositoryMerging.REQUEST_DOMINANT.equals(repositoryMerging)) {
            repositories = remoteRepositoryManager.aggregateRepositories(session, repositories, newRepositories, true);
        } else {
            pomRepositories =
                    remoteRepositoryManager.aggregateRepositories(session, pomRepositories, newRepositories, true);
            repositories = remoteRepositoryManager.aggregateRepositories(
                    session, pomRepositories, externalRepositories, false);
        }
    }

    private static void removeMatchingRepository(Iterable<RemoteRepository> repositories, final String id) {
        Iterator iterator = repositories.iterator();
        while (iterator.hasNext()) {
            RemoteRepository next = (RemoteRepository) iterator.next();
            if (next.getId().equals(id)) {
                iterator.remove();
            }
        }
    }

    public ModelResolver newCopy() {
        return new ProjectModelResolver(this);
    }

    public ModelSource resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        File pomFile = null;

        if (modelPool != null) {
            pomFile = modelPool.get(groupId, artifactId, version);
        }

        if (pomFile == null) {
            Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, "", "pom", version);

            try {
                ArtifactRequest request = new ArtifactRequest(pomArtifact, repositories, context);
                request.setTrace(trace);
                pomArtifact = resolver.resolveArtifact(session, request).getArtifact();
            } catch (ArtifactResolutionException e) {
                throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
            }

            pomFile = pomArtifact.getFile();
        }

        return new FileModelSource(pomFile);
    }

    @Override
    public ModelSource resolveModel(final Parent parent) throws UnresolvableModelException {
        try {
            final Artifact artifact =
                    new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), "", "pom", parent.getVersion());

            final VersionRangeRequest versionRangeRequest = new VersionRangeRequest(artifact, repositories, context);
            versionRangeRequest.setTrace(trace);

            final VersionRangeResult versionRangeResult = resolver.resolveVersionRange(session, versionRangeRequest);

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

            parent.setVersion(versionRangeResult.getHighestVersion().toString());

            return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
        } catch (final VersionRangeResolutionException e) {
            throw new UnresolvableModelException(
                    e.getMessage(), parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), e);
        }
    }

    @Override
    public ModelSource resolveModel(final Dependency dependency) throws UnresolvableModelException {
        try {
            final Artifact artifact = new DefaultArtifact(
                    dependency.getGroupId(), dependency.getArtifactId(), "", "pom", dependency.getVersion());

            final VersionRangeRequest versionRangeRequest = new VersionRangeRequest(artifact, repositories, context);
            versionRangeRequest.setTrace(trace);

            final VersionRangeResult versionRangeResult = resolver.resolveVersionRange(session, versionRangeRequest);

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

            dependency.setVersion(versionRangeResult.getHighestVersion().toString());

            return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        } catch (VersionRangeResolutionException e) {
            throw new UnresolvableModelException(
                    e.getMessage(), dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), e);
        }
    }
}
