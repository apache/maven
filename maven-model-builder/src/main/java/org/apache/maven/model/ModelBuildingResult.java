package org.apache.maven.model;

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

import java.util.List;

/**
 * Collects the output of the model builder.
 * 
 * @author Benjamin Bentmann
 */
public interface ModelBuildingResult
{

    /**
     * Gets the fully assembled model.
     * 
     * @return The fully assembled model, never {@code null}.
     */
    Model getEffectiveModel();

    /**
     * Gets the raw model as it was read from the model source. Apart from basic validation, the raw model has not
     * undergone any updates by the model builder, e.g. reflects neither inheritance or interpolation.
     * 
     * @return The raw model, never {@code null}.
     */
    Model getRawModel();

    /**
     * Gets the lineage of raw models from which the effective model was constructed. The first model is the model on
     * which the model builder was originally invoked, the last model is the super POM.
     * 
     * @return The lineage of raw models, never {@code null}.
     */
    List<Model> getRawModels();

    /**
     * Gets the profiles from the specified (raw) model that were active during model building. The input parameter
     * should be a model from the collection obtained by {@link #getRawModels()}.
     * 
     * @param rawModel The (raw) model for whose active profiles should be retrieved, must not be {@code null}.
     * @return The active profiles of the model or an empty list if none, never {@code null}.
     */
    List<Profile> getActivePomProfiles( Model rawModel );

    /**
     * Gets the external profiles that were active during model building. External profiles are those that were
     * contributed by {@link ModelBuildingRequest#getProfiles()}.
     * 
     * @return The active external profiles or an empty list if none, never {@code null}.
     */
    List<Profile> getActiveExternalProfiles();

}
