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
package org.apache.maven.internal.impl;

import java.util.Collection;
import java.util.List;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.DependencyCoordinate;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import static org.apache.maven.internal.impl.Utils.cast;

public interface InternalSession extends Session {

    static InternalSession from(Session session) {
        return cast(InternalSession.class, session, "session should be an " + InternalSession.class);
    }

    RemoteRepository getRemoteRepository(org.eclipse.aether.repository.RemoteRepository repository);

    Node getNode(org.eclipse.aether.graph.DependencyNode node);

    Node getNode(org.eclipse.aether.graph.DependencyNode node, boolean verbose);

    @Nonnull
    Artifact getArtifact(@Nonnull org.eclipse.aether.artifact.Artifact artifact);

    @Nonnull
    Dependency getDependency(@Nonnull org.eclipse.aether.graph.Dependency dependency);

    List<Project> getProjects(List<org.apache.maven.project.MavenProject> projects);

    Project getProject(org.apache.maven.project.MavenProject project);

    List<org.eclipse.aether.repository.RemoteRepository> toRepositories(List<RemoteRepository> repositories);

    org.eclipse.aether.repository.RemoteRepository toRepository(RemoteRepository repository);

    org.eclipse.aether.repository.LocalRepository toRepository(LocalRepository repository);

    List<org.apache.maven.artifact.repository.ArtifactRepository> toArtifactRepositories(
            List<RemoteRepository> repositories);

    org.apache.maven.artifact.repository.ArtifactRepository toArtifactRepository(RemoteRepository repository);

    List<org.eclipse.aether.graph.Dependency> toDependencies(
            Collection<DependencyCoordinate> dependencies, boolean managed);

    org.eclipse.aether.graph.Dependency toDependency(DependencyCoordinate dependency, boolean managed);

    List<org.eclipse.aether.artifact.Artifact> toArtifacts(Collection<Artifact> artifacts);

    org.eclipse.aether.artifact.Artifact toArtifact(Artifact artifact);

    org.eclipse.aether.artifact.Artifact toArtifact(ArtifactCoordinate coord);

    MavenSession getMavenSession();

    RepositorySystemSession getSession();

    RepositorySystem getRepositorySystem();
}
