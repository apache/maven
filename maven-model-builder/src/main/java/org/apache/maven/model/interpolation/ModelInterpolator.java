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
package org.apache.maven.model.interpolation;

import java.io.File;
import java.nio.file.Path;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;

/**
 * Replaces expressions of the form <code>${token}</code> with their effective values. Effective values are basically
 * calculated from the elements of the model itself and the execution properties from the building request.
 *
 * @deprecated use {@link org.apache.maven.api.services.ModelBuilder} instead
 */
@Deprecated(since = "4.0.0")
public interface ModelInterpolator {

    /**
     * Interpolates expressions in the specified model. Note that implementations are free to either interpolate the
     * provided model directly or to create a clone of the model and interpolate the clone. Callers should always use
     * the returned model and must not rely on the input model being updated.
     *
     * @param model The model to interpolate, must not be {@code null}.
     * @param projectDir The project directory, may be {@code null} if the model does not belong to a local project but
     *            to some artifact's metadata.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     * @return The interpolated model, never {@code null}.
     * @deprecated Use {@link #interpolateModel(Model, Path, ModelBuildingRequest, ModelProblemCollector)} instead.
     */
    @Deprecated
    Model interpolateModel(Model model, File projectDir, ModelBuildingRequest request, ModelProblemCollector problems);

    @Deprecated
    org.apache.maven.api.model.Model interpolateModel(
            org.apache.maven.api.model.Model model,
            File projectDir,
            ModelBuildingRequest request,
            ModelProblemCollector problems);

    /**
     * Interpolates expressions in the specified model. Note that implementations are free to either interpolate the
     * provided model directly or to create a clone of the model and interpolate the clone. Callers should always use
     * the returned model and must not rely on the input model being updated.
     *
     * @param model The model to interpolate, must not be {@code null}.
     * @param projectDir The project directory, may be {@code null} if the model does not belong to a local project but
     *            to some artifact's metadata.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     * @return The interpolated model, never {@code null}.
     * @since 4.0.0
     */
    Model interpolateModel(Model model, Path projectDir, ModelBuildingRequest request, ModelProblemCollector problems);

    /**
     * @since 4.0.0
     */
    org.apache.maven.api.model.Model interpolateModel(
            org.apache.maven.api.model.Model model,
            Path projectDir,
            ModelBuildingRequest request,
            ModelProblemCollector problems);
}
