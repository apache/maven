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

import java.util.List;
import java.util.Map;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;

/**
 * Result of a project build call.
 *
 * @since 4.0.0
 */
@Experimental
public interface ModelBuilderResult extends Result<ModelBuilderRequest> {

    /**
     * Gets the source from which the model was read.
     *
     * @return The source from which the model was read, never {@code null}.
     */
    @Nonnull
    ModelSource getSource();

    /**
     * Gets the file model.
     *
     * @return the file model, never {@code null}.
     */
    @Nonnull
    Model getFileModel();

    /**
     * Gets the file model + build pom transformation, without inheritance nor interpolation.
     *
     * @return The raw model, never {@code null}.
     */
    @Nonnull
    Model getRawModel();

    /**
     * Gets the effective model of the parent POM.
     *
     * @return the effective model of the parent POM, never {@code null}
     */
    @Nonnull
    Model getParentModel();

    /**
     * Gets the assembled model with inheritance, interpolation and profile injection.
     *
     * @return The assembled model, never {@code null}.
     */
    @Nonnull
    Model getEffectiveModel();

    /**
     * Gets the profiles that were active during model building.
     *
     * @return The active profiles of the model or an empty list if the model has no active profiles.
     */
    @Nonnull
    List<Profile> getActivePomProfiles();

    /**
     * Gets the profiles that were active during model building for a specific model in the hierarchy.
     * This allows tracking which profiles came from which model (parent vs child).
     *
     * @param modelId The identifier of the model (groupId:artifactId:version) or empty string for the super POM.
     * @return The active profiles for the specified model or an empty list if the model has no active profiles.
     * @since 4.0.0
     */
    @Nonnull
    List<Profile> getActivePomProfiles(String modelId);

    /**
     * Gets a map of all active POM profiles organized by model ID.
     * The map keys are model IDs (groupId:artifactId:version) and values are lists of active profiles for each model.
     *
     * @return A map of model IDs to their active profiles, never {@code null}.
     * @since 4.0.0
     */
    @Nonnull
    Map<String, List<Profile>> getActivePomProfilesByModel();

    /**
     * Gets the external profiles that were active during model building. External profiles are those that were
     * contributed by {@link ModelBuilderRequest#getProfiles()}.
     *
     * @return The active external profiles or an empty list if none, never {@code null}.
     */
    @Nonnull
    List<Profile> getActiveExternalProfiles();

    /**
     * Gets the problem collector that collected problems encountered during the project building.
     *
     * @return the problem collector that collected problems encountered during the project building
     */
    @Nonnull
    ProblemCollector<ModelProblem> getProblemCollector();

    /**
     * Gets the children of this result.
     *
     * @return the children of this result, can be empty but never {@code null}
     */
    @Nonnull
    List<? extends ModelBuilderResult> getChildren();

    /**
     * Creates a human-readable representation of these errors.
     */
    @Override
    String toString();
}
