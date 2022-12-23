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
package org.apache.maven.api.services;

import java.util.Collection;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

import static org.apache.maven.api.services.BaseRequest.nonNull;

/**
 * A request for deploying one or more artifacts to a remote repository.
 *
 * @since 4.0
 */
@Experimental
@Immutable
public interface ArtifactDeployerRequest {

    @Nonnull
    Session getSession();

    @Nonnull
    RemoteRepository getRepository();

    @Nonnull
    Collection<Artifact> getArtifacts();

    int getRetryFailedDeploymentCount();

    @Nonnull
    static ArtifactDeployerRequestBuilder builder() {
        return new ArtifactDeployerRequestBuilder();
    }

    @Nonnull
    static ArtifactDeployerRequest build(
            @Nonnull Session session, @Nonnull RemoteRepository repository, @Nonnull Collection<Artifact> artifacts) {
        return builder()
                .session(nonNull(session, "session cannot be null"))
                .repository(nonNull(repository, "repository cannot be null"))
                .artifacts(nonNull(artifacts, "artifacts cannot be null"))
                .build();
    }

    class ArtifactDeployerRequestBuilder {
        Session session;
        RemoteRepository repository;
        Collection<Artifact> artifacts;
        int retryFailedDeploymentCount;

        ArtifactDeployerRequestBuilder() {}

        @Nonnull
        public ArtifactDeployerRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        @Nonnull
        public ArtifactDeployerRequestBuilder repository(RemoteRepository repository) {
            this.repository = repository;
            return this;
        }

        public ArtifactDeployerRequestBuilder artifacts(Collection<Artifact> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        public ArtifactDeployerRequestBuilder retryFailedDeploymentCount(int retryFailedDeploymentCount) {
            this.retryFailedDeploymentCount = retryFailedDeploymentCount;
            return this;
        }

        @Nonnull
        public ArtifactDeployerRequest build() {
            return new DefaultArtifactDeployerRequest(session, repository, artifacts, retryFailedDeploymentCount);
        }

        private static class DefaultArtifactDeployerRequest extends BaseRequest implements ArtifactDeployerRequest {

            private final RemoteRepository repository;
            private final Collection<Artifact> artifacts;
            private final int retryFailedDeploymentCount;

            DefaultArtifactDeployerRequest(
                    @Nonnull Session session,
                    @Nonnull RemoteRepository repository,
                    @Nonnull Collection<Artifact> artifacts,
                    int retryFailedDeploymentCount) {
                super(session);
                this.repository = nonNull(repository, "repository cannot be null");
                this.artifacts = unmodifiable(nonNull(artifacts, "artifacts cannot be null"));
                this.retryFailedDeploymentCount = retryFailedDeploymentCount;
            }

            @Nonnull
            @Override
            public RemoteRepository getRepository() {
                return repository;
            }

            @Nonnull
            @Override
            public Collection<Artifact> getArtifacts() {
                return artifacts;
            }

            @Override
            public int getRetryFailedDeploymentCount() {
                return retryFailedDeploymentCount;
            }
        }
    }
}
