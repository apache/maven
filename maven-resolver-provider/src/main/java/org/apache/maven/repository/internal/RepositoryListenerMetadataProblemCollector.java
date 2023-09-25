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

import java.io.IOException;
import java.util.Objects;

import org.apache.maven.artifact.repository.metadata.validator.MetadataProblemCollector;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.ArtifactRepository;

public class RepositoryListenerMetadataProblemCollector implements MetadataProblemCollector {

    private final RepositorySystemSession session;
    private final ArtifactRepository repository;
    private final RequestTrace trace;
    private final Metadata metadata;

    public RepositoryListenerMetadataProblemCollector(
            RepositorySystemSession session, ArtifactRepository repository, RequestTrace trace, Metadata metadata) {
        this.session = Objects.requireNonNull(session, "session cannot be null");
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.trace = trace;
        this.metadata = Objects.requireNonNull(metadata, "metadata cannot be null");
    }

    private RepositoryEvent createMetadataInvalidEvent(Exception exception) {
        RepositoryEvent.Builder event = new RepositoryEvent.Builder(session, EventType.METADATA_INVALID);
        event.setTrace(trace);
        event.setMetadata(metadata);
        event.setException(exception);
        event.setRepository(repository);
        return event.build();
    }

    void dispatchEvent(RepositoryEvent event) {
        RepositoryListener listener = session.getRepositoryListener();
        if (listener != null) {
            listener.metadataInvalid(event);
        }
    }

    @Override
    public void add(Severity severity, String message, int line, int column, Exception cause) {
        // TODO: when to break build?
        // TODO: which exception to use?
        Exception e = new IOException("Invalid metadata: " + message, cause);
        RepositoryEvent event = createMetadataInvalidEvent(e);
        dispatchEvent(event);
    }
}
