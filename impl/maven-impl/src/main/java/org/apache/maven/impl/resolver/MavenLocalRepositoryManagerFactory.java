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
package org.apache.maven.impl.resolver;

import java.nio.file.Path;

import org.apache.maven.api.Constants;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.LocalPathPrefixComposerFactorySupport;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalArtifactRegistration;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalMetadataRegistration;
import org.eclipse.aether.repository.LocalMetadataRequest;
import org.eclipse.aether.repository.LocalMetadataResult;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.aether.util.repository.ChainedLocalRepositoryManager;

import static java.util.Objects.requireNonNull;

/**
 * Keeps enhanced local repository bookkeeping without making remote origin part of artifact identity
 * in an unsplit cache. Each chain member is decorated independently, preserving chain lookup and write routing.
 */
@Named("maven")
@Singleton
public final class MavenLocalRepositoryManagerFactory implements LocalRepositoryManagerFactory {
    private final EnhancedLocalRepositoryManagerFactory delegate;

    @Inject
    public MavenLocalRepositoryManagerFactory(
            @Named(EnhancedLocalRepositoryManagerFactory.NAME) final EnhancedLocalRepositoryManagerFactory delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public float getPriority() {
        return Math.nextUp(delegate.getPriority());
    }

    @Override
    public LocalRepositoryManager newInstance(final RepositorySystemSession session, final LocalRepository repository)
            throws NoLocalRepositoryManagerException {
        final var manager = delegate.newInstance(session, repository);
        final var split = ConfigUtils.getBoolean(
                session,
                LocalPathPrefixComposerFactorySupport.DEFAULT_SPLIT,
                LocalPathPrefixComposerFactorySupport.CONFIG_PROP_SPLIT,
                "aether.enhancedLocalRepository.split");
        // Honor an explicitly strict tail policy in automatic mode. The Maven alias takes precedence,
        // as it does in DefaultRepositorySystemSessionFactory.
        final var tailAvailability = ConfigUtils.getBoolean(
                session,
                true,
                Constants.MAVEN_REPO_LOCAL_TAIL_IGNORE_AVAILABILITY,
                ChainedLocalRepositoryManager.CONFIG_PROP_IGNORE_TAIL_AVAILABILITY);
        final var ignoreAvailability = ConfigUtils.getBoolean(
                session, !split && tailAvailability, Constants.MAVEN_REPO_LOCAL_IGNORE_AVAILABILITY);
        return ignoreAvailability ? new OriginIndependentLocalRepositoryManager(manager) : manager;
    }

    private record OriginIndependentLocalRepositoryManager(LocalRepositoryManager delegate)
            implements LocalRepositoryManager {
        @Override
        public LocalRepository getRepository() {
            return delegate.getRepository();
        }

        @Override
        public Path getAbsolutePathForLocalArtifact(final Artifact artifact) {
            return delegate.getAbsolutePathForLocalArtifact(artifact);
        }

        @Override
        public Path getAbsolutePathForRemoteArtifact(
                final Artifact artifact, final RemoteRepository repository, final String context) {
            return delegate.getAbsolutePathForRemoteArtifact(artifact, repository, context);
        }

        @Override
        public Path getAbsolutePathForLocalMetadata(final Metadata metadata) {
            return delegate.getAbsolutePathForLocalMetadata(metadata);
        }

        @Override
        public Path getAbsolutePathForRemoteMetadata(
                final Metadata metadata, final RemoteRepository repository, final String context) {
            return delegate.getAbsolutePathForRemoteMetadata(metadata, repository, context);
        }

        @Override
        public String getPathForLocalArtifact(final Artifact artifact) {
            return delegate.getPathForLocalArtifact(artifact);
        }

        @Override
        public String getPathForRemoteArtifact(
                final Artifact artifact, final RemoteRepository repository, final String context) {
            return delegate.getPathForRemoteArtifact(artifact, repository, context);
        }

        @Override
        public String getPathForLocalMetadata(final Metadata metadata) {
            return delegate.getPathForLocalMetadata(metadata);
        }

        @Override
        public String getPathForRemoteMetadata(
                final Metadata metadata, final RemoteRepository repository, final String context) {
            return delegate.getPathForRemoteMetadata(metadata, repository, context);
        }

        @Override
        public LocalArtifactResult find(final RepositorySystemSession session, final LocalArtifactRequest request) {
            final var result = delegate.find(session, request);
            // Enhanced LRM exposes a path only after file existence and real-path validation. A missing
            // path must remain a miss: do not rediscover a file that the delegate deliberately rejected.
            if (!result.isAvailable() && result.getPath() != null) {
                result.setAvailable(true);
            }
            return result;
        }

        @Override
        public void add(final RepositorySystemSession session, final LocalArtifactRegistration request) {
            delegate.add(session, request);
        }

        @Override
        public LocalMetadataResult find(final RepositorySystemSession session, final LocalMetadataRequest request) {
            return delegate.find(session, request);
        }

        @Override
        public void add(final RepositorySystemSession session, final LocalMetadataRegistration request) {
            delegate.add(session, request);
        }
    }
}
