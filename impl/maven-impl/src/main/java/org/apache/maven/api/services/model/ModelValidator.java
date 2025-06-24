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

import org.apache.maven.api.Session;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.ModelProblemCollector;

/**
 * Checks the model for missing or invalid values.
 *
 */
public interface ModelValidator {
    /**
     * Denotes minimal validation of POMs. This validation level is meant for processing of POMs from repositories
     * during metadata retrieval.
     */
    int VALIDATION_LEVEL_MINIMAL = 0;
    /**
     * Denotes validation as performed by Maven 2.0. This validation level is meant as a compatibility mode to allow
     * users to migrate their projects.
     */
    int VALIDATION_LEVEL_MAVEN_2_0 = 20;
    /**
     * Denotes validation as performed by Maven 3.0. This validation level is meant for existing projects.
     */
    int VALIDATION_LEVEL_MAVEN_3_0 = 30;
    /**
     * Denotes validation as performed by Maven 3.1. This validation level is meant for existing projects.
     */
    int VALIDATION_LEVEL_MAVEN_3_1 = 31;
    /**
     * Denotes validation as performed by Maven 4.0. This validation level is meant for new projects.
     */
    int VALIDATION_LEVEL_MAVEN_4_0 = 40;
    /**
     * Denotes strict validation as recommended by the current Maven version.
     */
    int VALIDATION_LEVEL_STRICT = VALIDATION_LEVEL_MAVEN_4_0;

    /**
     * Checks the specified file model for missing or invalid values. This model is directly created from the POM
     * file and has not been subjected to inheritance, interpolation or profile/default injection.
     *
     * @param model The model to validate, must not be {@code null}.
     * @param validationLevel The validation level.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    void validateFileModel(Session session, Model model, int validationLevel, ModelProblemCollector problems);

    /**
     * Checks the specified (raw) model for missing or invalid values. The raw model is the file model + buildpom filter
     * transformation and has not been subjected to inheritance, interpolation or profile/default injection.
     *
     * @param model The model to validate, must not be {@code null}.
     * @param validationLevel The validation level.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    void validateRawModel(Session session, Model model, int validationLevel, ModelProblemCollector problems);

    /**
     * Checks the specified (effective) model for missing or invalid values. The effective model is fully assembled and
     * has undergone inheritance, interpolation and other model operations.
     *
     * @param model The model to validate, must not be {@code null}.
     * @param validationLevel The validation level.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    void validateEffectiveModel(Session session, Model model, int validationLevel, ModelProblemCollector problems);
}
