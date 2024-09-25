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
import java.util.Optional;

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
public interface ModelBuilderResult {

    /**
     * Gets the sequence of model identifiers that denote the lineage of models from which the effective model was
     * constructed. Model identifiers have the form {@code <groupId>:<artifactId>:<version>}. The first identifier from
     * the list denotes the model on which the model builder was originally invoked. The last identifier will always be
     * an empty string that by definition denotes the super POM.
     *
     * @return The model identifiers from the lineage of models, never {@code null}.
     */
    @Nonnull
    List<String> getModelIds();

    /**
     * Gets the file model.
     *
     * @return the file model, never {@code null}.
     */
    @Nonnull
    Model getFileModel();

    /**
     * Returns the file model + profile injection.
     *
     * @return the activated file model, never {@code null}.
     */
    @Nonnull
    Model getActivatedFileModel();

    /**
     * Gets the file model + build pom transformation, without inheritance nor interpolation.
     *
     * @return The raw model, never {@code null}.
     */
    @Nonnull
    Model getRawModel();

    /**
     * Gets the assembled model with inheritance, interpolation and profile injection.
     *
     * @return The assembled model, never {@code null}.
     */
    @Nonnull
    Model getEffectiveModel();

    /**
     * Gets the specified raw model as it was read from a model source. Apart from basic validation, a raw model has not
     * undergone any updates by the model builder, e.g. reflects neither inheritance nor interpolation. The model
     * identifier should be from the collection obtained by {@link #getModelIds()}. As a special case, an empty string
     * can be used as the identifier for the super POM.
     *
     * @param modelId The identifier of the desired raw model, must not be {@code null}.
     * @return The raw model or {@code null} if the specified model id does not refer to a known model.
     */
    @Nonnull
    Optional<Model> getRawModel(@Nonnull String modelId);

    /**
     * Gets the profiles from the specified model that were active during model building. The model identifier should be
     * from the collection obtained by {@link #getModelIds()}. As a special case, an empty string can be used as the
     * identifier for the super POM.
     *
     * @param modelId The identifier of the model whose active profiles should be retrieved, must not be {@code null}.
     * @return The active profiles of the model or an empty list if the specified model id does
     *         not refer to a known model or has no active profiles.
     */
    @Nonnull
    List<Profile> getActivePomProfiles(@Nonnull String modelId);

    /**
     * Gets the external profiles that were active during model building. External profiles are those that were
     * contributed by {@link ModelBuilderRequest#getProfiles()}.
     *
     * @return The active external profiles or an empty list if none, never {@code null}.
     */
    @Nonnull
    List<Profile> getActiveExternalProfiles();

    /**
     * Gets the problems that were encountered during the project building.
     *
     * @return the problems that were encountered during the project building, can be empty but never {@code null}
     */
    @Nonnull
    List<ModelProblem> getProblems();

    @Nonnull
    List<? extends ModelBuilderResult> getChildren();

    @Nonnull
    ModelSource getSource();

    /**
     * Creates a human-readable representation of these errors.
     */
    String toString();
}
