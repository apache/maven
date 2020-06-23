package org.apache.maven.model.validation;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;

/**
 * Checks the model for missing or invalid values.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
public interface ModelValidator
{
    /**
     * Checks the specified file model for missing or invalid values. This model is directly created from the POM
     * file and has not been subjected to inheritance, interpolation or profile/default injection.
     *
     * @param model The model to validate, must not be {@code null}.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    default void validateFileModel( Model model, ModelBuildingRequest request, ModelProblemCollector problems )
    {
        // do nothing
    }

    /**
     * Checks the specified (raw) model for missing or invalid values. The raw model is the file model + buildpom filter
     * transformation and has not been subjected to inheritance, interpolation or profile/default injection.
     *
     * @param model The model to validate, must not be {@code null}.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    void validateRawModel( Model model, ModelBuildingRequest request, ModelProblemCollector problems );

    /**
     * Checks the specified (effective) model for missing or invalid values. The effective model is fully assembled and
     * has undergone inheritance, interpolation and other model operations.
     *
     * @param model The model to validate, must not be {@code null}.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    void validateEffectiveModel( Model model, ModelBuildingRequest request, ModelProblemCollector problems );

}
