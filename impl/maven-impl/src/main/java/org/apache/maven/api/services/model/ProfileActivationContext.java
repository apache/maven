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
package org.apache.maven.api.services.model;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.InterpolatorException;
import org.apache.maven.api.services.ModelBuilderException;

/**
 * Describes the environmental context used to determine the activation status of profiles.
 * <p>
 * The {@link Model} is available from the activation context, but only static parts of it
 * are allowed to be used, i.e. those that do not change between file model and effective model.
 *
 */
public interface ProfileActivationContext {

    /**
     * Checks if the specified profile has been explicitly activated.
     *
     * @param profileId the profile id
     * @return whether the profile has been activated
     */
    boolean isProfileActive(@Nonnull String profileId);

    /**
     * Checks if the specified profile has been explicitly deactivated.
     *
     * @param profileId the profile id
     * @return whether the profile has been deactivated
     */
    boolean isProfileInactive(@Nonnull String profileId);

    /**
     * Gets the system property to use for interpolation and profile activation. The system properties are collected
     * from the runtime environment like {@link System#getProperties()} and environment variables.
     *
     * @param key the name of the system property
     * @return the system property for the specified key, or {@code null}
     */
    @Nullable
    String getSystemProperty(@Nonnull String key);

    /**
     * Gets the user property to use for interpolation and profile activation. The user properties have been
     * configured directly by the user on his discretion, e.g. via the {@code -Dkey=value} parameter on the command line.
     *
     * @param key the name of the user property
     * @return The user property for the specified key, or {@code null}.
     */
    @Nullable
    String getUserProperty(@Nonnull String key);

    /**
     * Gets the model property to use for interpolation and profile activation.
     *
     * @param key the name of the model property
     * @return The model property for the specified key, or {@code null};
     */
    @Nullable
    String getModelProperty(@Nonnull String key);

    /**
     * Gets the artifactId from the current model.
     *
     * @return The artifactId of the current model, or {@code null} if not set.
     */
    @Nullable
    String getModelArtifactId();

    /**
     * Gets the packaging type from the current model.
     *
     * @return The packaging type of the current model, or {@code null} if not set.
     */
    @Nullable
    String getModelPackaging();

    /**
     * Gets the root directory of the current model.
     *
     * @return The root directory path of the current model, or {@code null} if not set.
     */
    @Nullable
    String getModelRootDirectory();

    /**
     * Gets the base directory of the current model.
     *
     * @return The base directory path of the current model, or {@code null} if not set.
     */
    @Nullable
    String getModelBaseDirectory();

    /**
     * Interpolates the given path string using the current context's properties.
     *
     * @param path The path string to interpolate
     * @return The interpolated path string
     * @throws InterpolatorException if an error occurs during interpolation
     */
    @Nullable
    String interpolatePath(@Nullable String path);

    /**
     * Checks if a file or directory matching the given glob pattern exists at the specified path.
     *
     * @param path the base path to check
     * @param glob whether the path can be a glob expression
     * @return {@code true} if a matching file exists, {@code false} otherwise
     * @throws ModelBuilderException if an error occurs while checking the path
     * @throws InterpolatorException if an error occurs during interpolation
     */
    boolean exists(@Nullable String path, boolean glob);
}
