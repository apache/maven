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
package org.apache.maven.impl;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Repository;
import org.apache.maven.api.RepositoryEvent;
import org.apache.maven.api.RepositoryEventType;
import org.apache.maven.api.RepositoryMetadata;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.RequestTrace;

final class DefaultRepositoryEvent implements RepositoryEvent {

    private final RepositoryEventType type;
    private final Session session;
    private final Artifact artifact;
    private final RepositoryMetadata metadata;
    private final Path path;
    private final Repository repository;
    private final Exception exception;
    private final List<Exception> exceptions;
    private final RequestTrace trace;

    DefaultRepositoryEvent(InternalSession session, org.eclipse.aether.RepositoryEvent event) {
        this.type = RepositoryEventType.valueOf(event.getType().name());
        this.session = session;
        this.artifact = event.getArtifact() != null ? session.getArtifact(event.getArtifact()) : null;
        this.metadata = event.getMetadata() != null ? new DefaultRepositoryMetadata(event.getMetadata()) : null;
        this.path = event.getPath();
        this.repository = event.getRepository() != null
                ? session.getRepository(event.getRepository()).orElse(null)
                : null;
        this.exception = event.getException();
        this.exceptions = event.getExceptions() != null ? List.copyOf(event.getExceptions()) : List.of();
        RequestTrace currentTrace = session.getCurrentTrace();
        this.trace = RequestTraceHelper.toMaven(currentTrace != null ? currentTrace.context() : null, event.getTrace());
    }

    @Override
    public RepositoryEventType getType() {
        return type;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public Optional<Artifact> getArtifact() {
        return Optional.ofNullable(artifact);
    }

    @Override
    public Optional<RepositoryMetadata> getMetadata() {
        return Optional.ofNullable(metadata);
    }

    @Override
    public Optional<Path> getPath() {
        return Optional.ofNullable(path);
    }

    @Override
    public Optional<Repository> getRepository() {
        return Optional.ofNullable(repository);
    }

    @Override
    public Optional<Exception> getException() {
        return Optional.ofNullable(exception);
    }

    @Override
    public List<Exception> getExceptions() {
        return exceptions;
    }

    @Override
    public Optional<RequestTrace> getTrace() {
        return Optional.ofNullable(trace);
    }

    private static final class DefaultRepositoryMetadata implements RepositoryMetadata {

        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String type;
        private final Nature nature;
        private final Path path;
        private final Map<String, String> properties;

        private DefaultRepositoryMetadata(org.eclipse.aether.metadata.Metadata metadata) {
            this.groupId = metadata.getGroupId();
            this.artifactId = metadata.getArtifactId();
            this.version = metadata.getVersion();
            this.type = metadata.getType();
            this.nature = Nature.valueOf(metadata.getNature().name());
            this.path = metadata.getPath();
            this.properties = Map.copyOf(metadata.getProperties());
        }

        @Override
        public String getGroupId() {
            return groupId;
        }

        @Override
        public String getArtifactId() {
            return artifactId;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public Nature getNature() {
            return nature;
        }

        @Override
        public Optional<Path> getPath() {
            return Optional.ofNullable(path);
        }

        @Override
        public Map<String, String> getProperties() {
            return properties;
        }
    }
}
