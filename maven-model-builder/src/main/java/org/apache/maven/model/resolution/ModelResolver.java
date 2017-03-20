package org.apache.maven.model.resolution;

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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelSource;

/**
 * Resolves a POM from its coordinates. During the build process, the
 * {@link org.apache.maven.model.building.ModelBuilder} will add any relevant repositories to the model resolver. In
 * other words, the model resolver is stateful and should not be reused across multiple model building requests.
 *
 * @author Benjamin Bentmann
 */
public interface ModelResolver
{

    /**
     * Tries to resolve the POM for the specified coordinates.
     *
     * @param groupId The group identifier of the POM, must not be {@code null}.
     * @param artifactId The artifact identifier of the POM, must not be {@code null}.
     * @param version The version of the POM, must not be {@code null}.
     * @return The source of the requested POM, never {@code null}.
     * @throws UnresolvableModelException If the POM could not be resolved from any configured repository.
     */
    ModelSource resolveModel( String groupId, String artifactId, String version )
        throws UnresolvableModelException;

    /**
     * Tries to resolve the POM for the specified parent coordinates possibly updating {@code parent}.
     * <p>
     * Unlike the {@link #resolveModel(java.lang.String, java.lang.String, java.lang.String)} method, this method
     * supports version ranges and updates the given {@code parent} instance to match the returned {@code ModelSource}.
     * If {@code parent} declares a version range, the version corresponding to the returned {@code ModelSource} will
     * be set on the given {@code parent}.
     * </p>
     *
     * @param parent The parent coordinates to resolve, must not be {@code null}.
     *
     * @return The source of the requested POM, never {@code null}.
     *
     * @throws UnresolvableModelException If the POM could not be resolved from any configured repository.
     * @since 3.2.2
     *
     * @see Parent#clone()
     */
    ModelSource resolveModel( Parent parent )
        throws UnresolvableModelException;

    /**
     * Tries to resolve the POM for the specified dependency coordinates possibly updating {@code dependency}.
     * <p>
     * Unlike the {@link #resolveModel(java.lang.String, java.lang.String, java.lang.String)} method, this method
     * supports version ranges and updates the given {@code dependency} instance to match the returned
     * {@code ModelSource}. If {@code dependency} declares a version range, the version corresponding to the returned
     * {@code ModelSource} will be set on the given {@code dependency}.
     * </p>
     *
     * @param dependency The dependency coordinates to resolve, must not be {@code null}.
     *
     * @return The source of the requested POM, never {@code null}.
     *
     * @throws UnresolvableModelException If the POM could not be resolved from any configured repository.
     * @since 3.5.0
     *
     * @see Dependency#clone()
     */
    ModelSource resolveModel( Dependency dependency )
        throws UnresolvableModelException;

    /**
     * Adds a repository to use for subsequent resolution requests. The order in which repositories are added matters,
     * repositories that were added first should also be searched first. When multiple repositories with the same
     * identifier are added, only the first repository being added will be used.
     *
     * @param repository The repository to add to the internal search chain, must not be {@code null}.
     * @throws InvalidRepositoryException If the repository could not be added (e.g. due to invalid URL or layout).
     */
    void addRepository( Repository repository )
        throws InvalidRepositoryException;

    /**
     * Adds a repository to use for subsequent resolution requests. The order in which repositories are added matters,
     * repositories that were added first should also be searched first. When multiple repositories with the same
     * identifier are added, then the value of the replace argument is determines the behaviour.
     *
     * If replace is false than any existing repository with the same Id will remain in use. If replace
     * is true the new repository replaces the original.
     *
     * @param repository The repository to add to the internal search chain, must not be {@code null}.
     * @throws InvalidRepositoryException If the repository could not be added (e.g. due to invalid URL or layout).
     */
    void addRepository( Repository repository, boolean replace )
            throws InvalidRepositoryException;

    /**
     * Clones this resolver for usage in a forked resolution process. In general, implementors need not provide a deep
     * clone. The only requirement is that invocations of {@link #addRepository(Repository)} on the clone do not affect
     * the state of the original resolver and vice versa.
     *
     * @return The cloned resolver, never {@code null}.
     */
    ModelResolver newCopy();

}
