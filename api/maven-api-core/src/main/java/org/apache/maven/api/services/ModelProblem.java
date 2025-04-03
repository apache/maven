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

import org.apache.maven.api.annotations.Nonnull;

/**
 * Describes a problem that was encountered during model building. A problem can either be an exception that was thrown
 * or a simple string message. In addition, a problem carries a hint about its source, e.g. the POM file that exhibits
 * the problem.
 *
 */
public interface ModelProblem extends BuilderProblem {

    /**
     * Enumeration of model versions that can be validated.
     * These versions correspond to different levels of validation that can be applied
     * during model building, based on the POM schema version.
     * <p>
     * The validation levels are cumulative, with higher versions including all validations
     * from lower versions plus additional checks specific to that version.
     */
    enum Version {
        /**
         * Base validation level that applies to all POM versions.
         * Includes fundamental structural validations.
         */
        BASE,

        /**
         * Validation for Maven 2.0 POM format.
         */
        V20,

        /**
         * Validation for Maven 3.0 POM format.
         */
        V30,

        /**
         * Validation for Maven 3.1 POM format.
         */
        V31,

        /**
         * Validation for Maven 4.0 POM format.
         */
        V40,

        /**
         * Validation for Maven 4.1 POM format.
         */
        V41
    }

    /**
     * Gets the identifier of the model from which the problem originated. The identifier is derived from the
     * information that is available at the point the problem occurs and as such merely serves as best effort
     * to provide information to the user to track the problem back to its origin.
     *
     * @return The identifier of the model from which the problem originated or an empty string if unknown, never
     *         {@code null}.
     */
    @Nonnull
    String getModelId();

    /**
     * Gets the applicable maven version/validation level of this problem
     * @return The version, never {@code null}.
     */
    @Nonnull
    Version getVersion();
}
