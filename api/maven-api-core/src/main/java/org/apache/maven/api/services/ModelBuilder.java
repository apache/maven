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

import org.apache.maven.api.Service;
import org.apache.maven.api.model.Model;

/**
 * Service for building Maven project models from POM files or other sources.
 * <p>
 * The model builder processes raw POM files through the full Maven model building pipeline,
 * including inheritance assembly, profile activation, interpolation, and validation.
 *
 * @since 4.0.0
 */
public interface ModelBuilder extends Service {

    String MODEL_VERSION_4_0_0 = "4.0.0";

    String MODEL_VERSION_4_1_0 = "4.1.0";

    String MODEL_VERSION_4_2_0 = "4.2.0";

    List<String> KNOWN_MODEL_VERSIONS = List.of(MODEL_VERSION_4_0_0, MODEL_VERSION_4_1_0, MODEL_VERSION_4_2_0);

    /**
     * Creates a new model builder session for building one or more models.
     *
     * @return a new model builder session
     */
    ModelBuilderSession newSession();

    /**
     * A session for building models, allowing multiple builds to share state.
     */
    interface ModelBuilderSession {

        /**
         * Builds the effective model from the given request.
         *
         * @param request the model builder request
         * @return the model builder result
         * @throws ModelBuilderException if the model cannot be built
         */
        ModelBuilderResult build(ModelBuilderRequest request) throws ModelBuilderException;
    }

    /**
     * Builds a raw model from the given request without applying inheritance or interpolation.
     *
     * @param request the model builder request
     * @return the raw model
     * @throws ModelBuilderException if the raw model cannot be built
     */
    Model buildRawModel(ModelBuilderRequest request) throws ModelBuilderException;
}
