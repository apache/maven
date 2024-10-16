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
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Resource;

/**
 * Interface to manage the project during its lifecycle.
 *
 * @since 4.0.0
 */
@Experimental
public interface ProjectManager extends Service {
    /**
     * Returns the path to the built project artifact file, if the project has been built.
     *
     * @return the path of the built project artifact
     */
    @Nonnull
    Optional<Path> getPath(Project project);

    /**
     * Returns an immutable collection of attached artifacts for given project.
     */
    @Nonnull
    Collection<ProducedArtifact> getAttachedArtifacts(Project project);

    /**
     * Returns project's all artifacts as immutable collection. The list contains all artifacts, even the attached ones,
     * if any. Hence, the list returned by this method depends on which lifecycle step of the build was it invoked.
     * The head of returned list is result of {@link Project#getArtifacts()} method, so same applies here: the list can have
     * minimum of one element. The maximum number of elements is in turn dependent on build configuration and lifecycle
     * phase when this method was invoked (i.e. is javadoc jar built and attached, is sources jar built attached, are
     * all the artifact signed, etc.).
     * <p>
     * This method is shorthand for {@link Project#getArtifacts()} and {@link #getAttachedArtifacts(Project)} methods.
     *
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     */
    Collection<ProducedArtifact> getAllArtifacts(Project project);

    default void attachArtifact(Session session, Project project, Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String ext = dot >= 1 ? name.substring(dot + 1) : "";
        ProducedArtifact artifact = session.createProducedArtifact(
                project.getGroupId(), project.getArtifactId(), project.getVersion(), ext);
        attachArtifact(project, artifact, path);
    }

    default void attachArtifact(Session session, Project project, String type, Path path) {
        ProducedArtifact artifact = session.createProducedArtifact(
                project.getGroupId(), project.getArtifactId(), project.getVersion(), null, null, type);
        attachArtifact(project, artifact, path);
    }

    void attachArtifact(Project project, ProducedArtifact artifact, Path path);

    /**
     * Obtain an immutable list of compile source roots for the given project and scope.
     * Paths are absolute.
     *
     * @param project the project
     * @param scope the scope, i.e. usually main or test
     * @return the list of compile source roots
     */
    @Nonnull
    List<Path> getCompileSourceRoots(@Nonnull Project project, @Nonnull ProjectScope scope);

    /**
     * Add a compilation source root to the given project for the given scope.
     * The path will be transformed into an absolute path and added to the list for the given scope,
     * if not already present.
     *
     * @param project the project
     * @param scope the scope, i.e. usually main or test
     * @param sourceRoot the new source root
     */
    void addCompileSourceRoot(@Nonnull Project project, @Nonnull ProjectScope scope, @Nonnull Path sourceRoot);

    /**
     * Get the list of resources for the given project and scope
     *
     * @param project the project
     * @param scope the scope, i.e. usually main or test
     * @return the list of resources
     */
    List<Resource> getResources(@Nonnull Project project, @Nonnull ProjectScope scope);

    /**
     * Add a resource set to the given project for the given scope.
     *
     * @param project the project
     * @param scope the scope, i.e. usually main or test
     * @param resource the resource set to add
     */
    void addResource(@Nonnull Project project, @Nonnull ProjectScope scope, @Nonnull Resource resource);

    /**
     * Returns an immutable list of project remote repositories (directly specified or inherited).
     *
     * @param project the project
     */
    @Nonnull
    List<RemoteRepository> getRemoteProjectRepositories(@Nonnull Project project);

    /**
     * Returns an immutable list of project remote plugin repositories (directly specified or inherited).
     *
     * @param project the project
     */
    @Nonnull
    List<RemoteRepository> getRemotePluginRepositories(@Nonnull Project project);

    /**
     * Returns an immutable map of the project properties.
     *
     * @see #setProperty(Project, String, String)
     */
    @Nonnull
    Map<String, String> getProperties(@Nonnull Project project);

    /**
     * Set a given project property.
     *
     * @param project the project to modify
     * @param key they property's key
     * @param value the value or {@code null} to unset the property
     */
    void setProperty(@Nonnull Project project, @Nonnull String key, @Nullable String value);

    @Nonnull
    Optional<Project> getExecutionProject(@Nonnull Project project);
}
