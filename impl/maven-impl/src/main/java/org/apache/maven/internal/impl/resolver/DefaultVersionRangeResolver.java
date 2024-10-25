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
package org.apache.maven.internal.impl.resolver;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.metadata.Versioning;
import org.apache.maven.internal.impl.DefaultModelVersionParser;
import org.apache.maven.metadata.v4.MetadataStaxReader;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.impl.MetadataResolver;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionRange;
import org.eclipse.aether.version.VersionScheme;

/**
 */
@Named
@Singleton
public class DefaultVersionRangeResolver implements VersionRangeResolver {

    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    private final MetadataResolver metadataResolver;
    private final SyncContextFactory syncContextFactory;
    private final RepositoryEventDispatcher repositoryEventDispatcher;
    private final VersionScheme versionScheme;

    @Inject
    public DefaultVersionRangeResolver(
            MetadataResolver metadataResolver,
            SyncContextFactory syncContextFactory,
            RepositoryEventDispatcher repositoryEventDispatcher,
            VersionScheme versionScheme) {
        this.metadataResolver = Objects.requireNonNull(metadataResolver, "metadataResolver cannot be null");
        this.syncContextFactory = Objects.requireNonNull(syncContextFactory, "syncContextFactory cannot be null");
        this.repositoryEventDispatcher =
                Objects.requireNonNull(repositoryEventDispatcher, "repositoryEventDispatcher cannot be null");
        this.versionScheme = Objects.requireNonNull(versionScheme, "versionScheme cannot be null");
    }

    @Override
    public VersionRangeResult resolveVersionRange(RepositorySystemSession session, VersionRangeRequest request)
            throws VersionRangeResolutionException {
        VersionRangeResult result = new VersionRangeResult(request);

        VersionConstraint versionConstraint;
        try {
            versionConstraint =
                    versionScheme.parseVersionConstraint(request.getArtifact().getVersion());
        } catch (InvalidVersionSpecificationException e) {
            result.addException(e);
            throw new VersionRangeResolutionException(result);
        }

        result.setVersionConstraint(versionConstraint);

        if (versionConstraint.getRange() == null) {
            result.addVersion(versionConstraint.getVersion());
        } else {
            VersionRange.Bound lowerBound = versionConstraint.getRange().getLowerBound();
            if (lowerBound != null
                    && lowerBound.equals(versionConstraint.getRange().getUpperBound())) {
                result.addVersion(lowerBound.getVersion());
            } else {
                Map<String, ArtifactRepository> versionIndex = getVersions(session, result, request);

                List<Version> versions = new ArrayList<>();
                for (Map.Entry<String, ArtifactRepository> v : versionIndex.entrySet()) {
                    try {
                        Version ver = versionScheme.parseVersion(v.getKey());
                        if (versionConstraint.containsVersion(ver)) {
                            versions.add(ver);
                            result.setRepository(ver, v.getValue());
                        }
                    } catch (InvalidVersionSpecificationException e) {
                        result.addException(e);
                    }
                }

                Collections.sort(versions);
                result.setVersions(versions);
            }
        }

        return result;
    }

    private Map<String, ArtifactRepository> getVersions(
            RepositorySystemSession session, VersionRangeResult result, VersionRangeRequest request) {
        RequestTrace trace = RequestTrace.newChild(request.getTrace(), request);

        Map<String, ArtifactRepository> versionIndex = new HashMap<>();

        Metadata metadata = new DefaultMetadata(
                request.getArtifact().getGroupId(),
                request.getArtifact().getArtifactId(),
                MAVEN_METADATA_XML,
                Metadata.Nature.RELEASE_OR_SNAPSHOT);

        List<MetadataRequest> metadataRequests =
                new ArrayList<>(request.getRepositories().size());

        metadataRequests.add(new MetadataRequest(metadata, null, request.getRequestContext()));

        for (RemoteRepository repository : request.getRepositories()) {
            MetadataRequest metadataRequest = new MetadataRequest(metadata, repository, request.getRequestContext());
            metadataRequest.setDeleteLocalCopyIfMissing(true);
            metadataRequest.setTrace(trace);
            metadataRequests.add(metadataRequest);
        }

        List<MetadataResult> metadataResults = metadataResolver.resolveMetadata(session, metadataRequests);

        WorkspaceReader workspace = session.getWorkspaceReader();
        if (workspace != null) {
            List<String> versions = workspace.findVersions(request.getArtifact());
            for (String version : versions) {
                versionIndex.put(version, workspace.getRepository());
            }
        }

        for (MetadataResult metadataResult : metadataResults) {
            result.addException(metadataResult.getException());

            ArtifactRepository repository = metadataResult.getRequest().getRepository();
            if (repository == null) {
                repository = session.getLocalRepository();
            }

            Versioning versioning = readVersions(session, trace, metadataResult.getMetadata(), repository, result);

            versioning = filterVersionsByRepositoryType(
                    versioning, metadataResult.getRequest().getRepository());

            for (String version : versioning.getVersions()) {
                if (!versionIndex.containsKey(version)) {
                    versionIndex.put(version, repository);
                }
            }
        }

        return versionIndex;
    }

    private Versioning readVersions(
            RepositorySystemSession session,
            RequestTrace trace,
            Metadata metadata,
            ArtifactRepository repository,
            VersionRangeResult result) {
        Versioning versioning = null;
        try {
            if (metadata != null) {
                try (SyncContext syncContext = syncContextFactory.newInstance(session, true)) {
                    syncContext.acquire(null, Collections.singleton(metadata));

                    if (metadata.getPath() != null && Files.exists(metadata.getPath())) {
                        try (InputStream in = Files.newInputStream(metadata.getPath())) {
                            versioning =
                                    new MetadataStaxReader().read(in, false).getVersioning();
                        }
                    }
                }
            }
        } catch (Exception e) {
            invalidMetadata(session, trace, metadata, repository, e);
            result.addException(e);
        }

        return (versioning != null) ? versioning : Versioning.newInstance();
    }

    private Versioning filterVersionsByRepositoryType(Versioning versioning, RemoteRepository remoteRepository) {
        if (remoteRepository == null) {
            return versioning;
        }
        return versioning.withVersions(versioning.getVersions().stream()
                .filter(version -> remoteRepository
                        .getPolicy(DefaultModelVersionParser.checkSnapshot(version))
                        .isEnabled())
                .toList());
    }

    private void invalidMetadata(
            RepositorySystemSession session,
            RequestTrace trace,
            Metadata metadata,
            ArtifactRepository repository,
            Exception exception) {
        RepositoryEvent.Builder event = new RepositoryEvent.Builder(session, EventType.METADATA_INVALID);
        event.setTrace(trace);
        event.setMetadata(metadata);
        event.setException(exception);
        event.setRepository(repository);

        repositoryEventDispatcher.dispatch(event.build());
    }
}
