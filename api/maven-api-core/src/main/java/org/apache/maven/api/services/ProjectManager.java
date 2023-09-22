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

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.ResolutionScope;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Interface to manage the project during its lifecycle.
 *
 * @since 4.0.0
 */
@Experimental
public interface ProjectManager extends Service {
    /**
     * Returns the path to the resolved file in the local repository
     * if the artifact has been resolved.
     *
     * @return the path of the resolved artifact
     */
    @Nonnull
    Optional<Path> getPath(Project project);

    @Nonnull
    Collection<Artifact> getAttachedArtifacts(Project project);

    default void attachArtifact(Session session, Project project, Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String ext = dot >= 1 ? name.substring(dot + 1) : "";
        Artifact artifact =
                session.createArtifact(project.getGroupId(), project.getArtifactId(), project.getVersion(), ext);
        attachArtifact(project, artifact, path);
    }

    default void attachArtifact(Session session, Project project, String type, Path path) {
        Artifact artifact = session.createArtifact(
                project.getGroupId(), project.getArtifactId(), project.getVersion(), null, null, type);
        attachArtifact(project, artifact, path);
    }

    void attachArtifact(Project project, Artifact artifact, Path path);

    List<String> getCompileSourceRoots(Project project);

    void addCompileSourceRoot(Project project, String sourceRoot);

    List<String> getTestCompileSourceRoots(Project project);

    void addTestCompileSourceRoot(Project project, String sourceRoot);

    List<RemoteRepository> getRepositories(Project project);

    List<Artifact> getResolvedDependencies(Project project, ResolutionScope scope);

    Node getCollectedDependencies(Project project, ResolutionScope scope);

    void setProperty(Project project, String key, String value);
}
