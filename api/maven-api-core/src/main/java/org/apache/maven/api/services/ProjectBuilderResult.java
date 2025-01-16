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
import java.util.Optional;

import org.apache.maven.api.Project;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Result of a project build call.
 *
 * @since 4.0.0
 */
@Experimental
public interface ProjectBuilderResult extends Result<ProjectBuilderRequest> {

    /**
     * Gets the identifier of the project that could not be built. The general format of the identifier is {@code
     * <groupId>:<artifactId>:<version>} but some of these coordinates may still be unknown at the point the exception
     * is thrown so this information is merely meant to assist the user.
     *
     * @return the identifier of the project or an empty string if not known, never {@code null}
     */
    @Nonnull
    String getProjectId();

    /**
     * Gets the POM file from which the project was built.
     *
     * @return the optional POM file
     */
    @Nonnull
    Optional<Path> getPomFile();

    /**
     * Gets the project that was built.
     *
     * @return The project that was built or {@code null} if an error occurred and this result accompanies a
     *         {@link ProjectBuilderException}.
     */
    @Nonnull
    Optional<Project> getProject();

    /**
     * Gets the problems that were encountered during the project building.
     *
     * @return the problems that were encountered during the project building, can be empty but never {@code null}
     */
    @Nonnull
    Collection<BuilderProblem> getProblems();

    /**
     * Gets the result of the dependency resolution for the project.
     *
     * @return the result of the dependency resolution for the project
     */
    @Nonnull
    Optional<DependencyResolverResult> getDependencyResolverResult();
}
