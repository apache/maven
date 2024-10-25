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
package org.apache.maven.model.building;

import java.io.File;
import java.nio.file.Path;

import org.apache.maven.model.Model;

/**
 * Builds the effective model from a POM.
 *
 * @deprecated use {@link org.apache.maven.api.services.ModelBuilder} instead
 */
@Deprecated(since = "4.0.0")
public interface ModelBuilder {

    /**
     * Builds the effective model of the specified POM.
     *
     * @param request The model building request that holds the parameters, must not be {@code null}.
     * @return The result of the model building, never {@code null}.
     * @throws ModelBuildingException If the effective model could not be built.
     */
    ModelBuildingResult build(ModelBuildingRequest request) throws ModelBuildingException;

    /**
     * Builds the effective model by completing the specified interim result which was produced by a previous call to
     * {@link #build(ModelBuildingRequest)} with {@link ModelBuildingRequest#isTwoPhaseBuilding()} being {@code true}.
     * The model building request passed to this method must be the same as the one used for the first phase of the
     * model building.
     *
     * @param request The model building request that holds the parameters, must not be {@code null}.
     * @param result The interim result of the first phase of model building, must not be {@code null}.
     * @return The result of the model building, never {@code null}.
     * @throws ModelBuildingException If the effective model could not be built.
     */
    ModelBuildingResult build(ModelBuildingRequest request, ModelBuildingResult result) throws ModelBuildingException;

    /**
     * Performs only the part of {@link ModelBuilder#build(ModelBuildingRequest)} that loads the raw model
     *
     * @deprecated Use {@link #buildRawModel(Path, int, boolean)} instead.
     */
    @Deprecated
    Result<? extends Model> buildRawModel(File pomFile, int validationLevel, boolean locationTracking);

    /**
     * Performs only the part of {@link ModelBuilder#build(ModelBuildingRequest)} that loads the raw model
     *
     * @since 4.0.0
     */
    Result<? extends Model> buildRawModel(Path pomFile, int validationLevel, boolean locationTracking);

    /**
     * @deprecated Use {@link #buildRawModel(Path, int, boolean, TransformerContext)} instead.
     */
    @Deprecated
    Result<? extends Model> buildRawModel(
            File pomFile, int validationLevel, boolean locationTracking, TransformerContext context);

    /**
     * @since 4.0.0
     */
    Result<? extends Model> buildRawModel(
            Path pomFile, int validationLevel, boolean locationTracking, TransformerContext context);

    TransformerContextBuilder newTransformerContextBuilder();
}
