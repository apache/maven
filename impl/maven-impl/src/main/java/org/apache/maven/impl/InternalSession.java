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

import java.util.Collection;
import java.util.List;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.Node;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.services.RequestTrace;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import static org.apache.maven.impl.Utils.cast;

public interface InternalSession extends Session {

    static InternalSession from(Session session) {
        return cast(InternalSession.class, session, "session should be an " + InternalSession.class);
    }

    static InternalSession from(org.eclipse.aether.RepositorySystemSession session) {
        return cast(InternalSession.class, session.getData().get(InternalSession.class), "session");
    }

    static void associate(org.eclipse.aether.RepositorySystemSession rsession, Session session) {
        if (!rsession.getData().set(InternalSession.class, null, from(session))) {
            throw new IllegalStateException("A maven session is already associated with the repository session");
        }
    }

    RemoteRepository getRemoteRepository(org.eclipse.aether.repository.RemoteRepository repository);

    Node getNode(org.eclipse.aether.graph.DependencyNode node);

    Node getNode(org.eclipse.aether.graph.DependencyNode node, boolean verbose);

    @Nonnull
    Artifact getArtifact(@Nonnull org.eclipse.aether.artifact.Artifact artifact);

    @Nonnull
    <T extends Artifact> T getArtifact(@Nonnull Class<T> clazz, @Nonnull org.eclipse.aether.artifact.Artifact artifact);

    @Nonnull
    Dependency getDependency(@Nonnull org.eclipse.aether.graph.Dependency dependency);

    List<org.eclipse.aether.repository.RemoteRepository> toRepositories(List<RemoteRepository> repositories);

    org.eclipse.aether.repository.RemoteRepository toRepository(RemoteRepository repository);

    org.eclipse.aether.repository.LocalRepository toRepository(LocalRepository repository);

    List<org.eclipse.aether.graph.Dependency> toDependencies(
            Collection<DependencyCoordinates> dependencies, boolean managed);

    org.eclipse.aether.graph.Dependency toDependency(DependencyCoordinates dependency, boolean managed);

    List<org.eclipse.aether.artifact.Artifact> toArtifacts(Collection<? extends Artifact> artifacts);

    org.eclipse.aether.artifact.Artifact toArtifact(Artifact artifact);

    org.eclipse.aether.artifact.Artifact toArtifact(ArtifactCoordinates coords);

    RepositorySystemSession getSession();

    RepositorySystem getRepositorySystem();

    /**
     * Sets the current request trace for the session.
     * The request trace provides contextual information about the current operation
     * being performed and can be used for debugging and monitoring purposes.
     * The trace is stored in thread-local storage, allowing for concurrent operations
     * with different traces.
     *
     * @param trace the trace to set as current, may be null to clear the trace
     * @see RequestTraceHelper#enter(Session, Object) For the recommended way to manage traces
     */
    void setCurrentTrace(@Nullable RequestTrace trace);

    /**
     * Gets the current request trace for the session from thread-local storage.
     * Each thread maintains its own trace context, ensuring thread-safety for
     * concurrent operations.
     *
     * @return the current request trace, or null if no trace is set
     * @see RequestTraceHelper#enter(Session, Object) For the recommended way to manage traces
     */
    RequestTrace getCurrentTrace();
}
