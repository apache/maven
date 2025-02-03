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
package org.apache.maven.api.services.model;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.Result;

/**
 * Resolves a POM from its coordinates.
 */
public interface ModelResolver extends Service {

    /**
     * Tries to resolve the POM for the specified parent coordinates possibly updating {@code parent}.
     *
     * @param session The session to use to resolve the model, must not be {@code null}.
     * @param repositories The repositories to use to resolve the model, may be {@code null} in which case the {@code Session} repositories will be used.
     * @param parent The parent coordinates to resolve, must not be {@code null}.
     * @param modified a holder for the updated parent, must not be {@code null}.
     * @return The source of the requested POM, never {@code null}.
     * @throws ModelResolverException If the POM could not be resolved from any configured repository.
     */
    @Nonnull
    ModelSource resolveModel(
            @Nonnull Session session,
            @Nullable List<RemoteRepository> repositories,
            @Nonnull Parent parent,
            @Nonnull AtomicReference<Parent> modified)
            throws ModelResolverException;

    /**
     * Tries to resolve the POM for the specified dependency coordinates possibly updating {@code dependency}.
     *
     * @param session The session to use to resolve the model, must not be {@code null}.
     * @param repositories The repositories to use to resolve the model, may be {@code null} in which case the {@code Session} repositories will be used.
     * @param dependency The dependency coordinates to resolve, must not be {@code null}.
     * @param modified a holder for the updated dependency, must not be {@code null}.
     * @return The source of the requested POM, never {@code null}.
     * @throws ModelResolverException If the POM could not be resolved from any configured repository.
     */
    @Nonnull
    ModelSource resolveModel(
            @Nonnull Session session,
            @Nullable List<RemoteRepository> repositories,
            @Nonnull Dependency dependency,
            @Nonnull AtomicReference<Dependency> modified)
            throws ModelResolverException;

    @Nonnull
    ModelResolverResult resolveModel(@Nonnull ModelResolverRequest request) throws ModelResolverException;

    record ModelResolverRequest(
            @Nonnull Session session,
            @Nullable RequestTrace trace,
            @Nullable List<RemoteRepository> repositories,
            @Nonnull String groupId,
            @Nonnull String artifactId,
            @Nonnull String version,
            @Nullable String classifier)
            implements Request<Session> {
        @Nonnull
        @Override
        public Session getSession() {
            return session;
        }

        @Nullable
        @Override
        public RequestTrace getTrace() {
            return trace;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ModelResolverRequest that
                    && repositories == that.repositories
                    && Objects.equals(groupId, that.groupId)
                    && Objects.equals(artifactId, that.artifactId)
                    && Objects.equals(version, that.version)
                    && Objects.equals(classifier, that.classifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repositories, groupId, artifactId, version, classifier);
        }

        @Override
        @Nonnull
        public String toString() {
            return getClass().getSimpleName() + "[" + "repositories="
                    + repositories + ", groupId="
                    + groupId
                    + ", artifactId=" + artifactId
                    + ", version=" + version
                    + ", classifier=" + classifier
                    + ']';
        }
    }

    record ModelResolverResult(ModelResolverRequest request, ModelSource source, String version)
            implements Result<ModelResolverRequest> {
        @Nonnull
        @Override
        public ModelResolverRequest getRequest() {
            return request;
        }
    }
}
