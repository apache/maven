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
package org.apache.maven.project;

import java.util.List;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;

/**
 * Assists the project builder. <strong>Warning:</strong> This is an internal utility interface that is only public for
 * technical reasons, it is not part of the public API. In particular, this interface can be changed or deleted without
 * prior notice.
 *
 * @author Benjamin Bentmann
 */
public interface ProjectBuildingHelper {

    /**
     * Creates the effective artifact repositories from the specified POM repositories.
     *
     * @param pomRepositories The POM repositories to create the artifact repositories from, must not be {@code null}.
     * @param externalRepositories The external (and already mirrored) repositories to merge into the result list, may
     *            be {@code null}.
     * @param request The project building request holding further settings like repository settings, must not be
     *            {@code null}.
     * @return The effective artifact repositories, never {@code null}.
     * @throws InvalidRepositoryException
     */
    List<ArtifactRepository> createArtifactRepositories(
            List<Repository> pomRepositories,
            List<ArtifactRepository> externalRepositories,
            ProjectBuildingRequest request)
            throws InvalidRepositoryException;

    /**
     * Creates the project realm that hosts the build extensions of the specified model.
     *
     * @param project The project to create the project realm for, must not be {@code null}
     * @param model The model to create the project realm for, must not be {@code null}
     * @param request The project building request holding further settings like repository settings, must not be
     *            {@code null}.
     * @return The record with the project realm and extension artifact filter, never {@code null}.
     * @throws PluginResolutionException If any build extension could not be resolved.
     */
    ProjectRealmCache.CacheRecord createProjectRealm(MavenProject project, Model model, ProjectBuildingRequest request)
            throws PluginResolutionException, PluginVersionResolutionException, PluginManagerException;

    /**
     * Updates the context class loader such that the container will search the project realm when the model builder
     * injects the lifecycle bindings from the packaging in the next step. The context class loader is to be reset by
     * the project builder when the project is fully assembled.
     *
     * @param project The project whose class realm should be selected, must not be {@code null}.
     */
    void selectProjectRealm(MavenProject project);
}
