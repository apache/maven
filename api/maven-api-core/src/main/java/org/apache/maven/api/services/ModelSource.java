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

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Represents a source for loading Maven Project Object Model (POM) files. This interface
 * extends the basic {@link Source} interface with specific functionality for handling
 * Maven POM files and resolving related project POMs.
 *
 * <p>The interface provides two types of sources:</p>
 * <ul>
 *   <li>Build sources: Used for POM files of projects being built by Maven in the filesystem.
 *       These sources support resolving related POMs using the {@link ModelLocator}.</li>
 *   <li>Resolved sources: Used for artifacts that have been resolved by Maven from repositories
 *       (using groupId:artifactId:version coordinates) and downloaded to the local repository.
 *       These sources do not support resolving other sources.</li>
 * </ul>
 *
 * @since 4.0.0
 * @see Source
 */
public interface ModelSource extends Source {

    /**
     * Creates a new build source for the specified path.
     * Build sources are used for POM files of projects being built by Maven
     * in the filesystem and support resolving related POMs.
     *
     * @param path the path to the POM file or project directory
     * @return a new ModelSource instance configured as a build source
     * @throws NullPointerException if path is null
     */
    @Nonnull
    static ModelSource buildSource(@Nonnull Path path) {
        return new PathSource(true, requireNonNull(path, "path"), null);
    }

    /**
     * Creates a new resolved source for the specified path and location.
     * Resolved sources are used for artifacts that have been resolved by Maven
     * from repositories (using groupId:artifactId:version coordinates) and
     * downloaded to the local repository. These sources do not support resolving
     * other sources.
     *
     * @param path the path to the POM file or project directory
     * @param location optional logical location of the source, used for reporting purposes
     * @return a new ModelSource instance configured as a resolved source
     * @throws NullPointerException if path is null
     */
    @Nonnull
    static ModelSource resolvedSource(@Nonnull Path path, @Nullable String location) {
        return new PathSource(false, requireNonNull(path, "path"), location);
    }

    /**
     * Interface for locating POM files within a project structure.
     * Implementations of this interface provide the ability to find POM files
     * in various project contexts.
     *
     * @since 4.0.0
     */
    interface ModelLocator {
        /**
         * Attempts to locate an existing POM file at or within the specified project path.
         *
         * <p>This method is used to find POM files in various contexts, such as:</p>
         * <ul>
         *   <li>Directly at the specified path</li>
         *   <li>Within a directory at the specified path</li>
         *   <li>In standard Maven project locations relative to the specified path</li>
         * </ul>
         *
         * @param project the path to search for a POM file
         * @return the path to the located POM file, or null if no POM can be found
         * @throws NullPointerException if project is null
         */
        @Nullable
        Path locateExistingPom(@Nonnull Path project);
    }

    /**
     * Resolves a relative path to another POM file using the provided model locator.
     * This method is specifically used to locate POM files for subprojects or related
     * projects referenced from the current POM.
     *
     * <p>The resolution process typically involves:</p>
     * <ul>
     *   <li>Normalizing the relative path for the current platform</li>
     *   <li>Resolving the path against the current POM's location</li>
     *   <li>Using the model locator to find an existing POM at the resolved location</li>
     * </ul>
     *
     * @param modelLocator the locator to use for finding the related POM file
     * @param relative the relative path to resolve
     * @return a new ModelSource for the resolved POM, or null if:
     *         <ul>
     *           <li>This is not a build source</li>
     *           <li>No POM can be found at the resolved location</li>
     *         </ul>
     * @throws NullPointerException if modelLocator or relative is null
     */
    @Nullable
    ModelSource resolve(@Nonnull ModelLocator modelLocator, @Nonnull String relative);
}