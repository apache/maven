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

/**
 * Describes a problem that was encountered during model building. A problem can either be an exception that was thrown
 * or a simple string message. In addition, a problem carries a hint about its source, e.g. the POM file that exhibits
 * the problem.
 *
 */
public interface ModelProblem extends BuilderProblem {

    /**
     * Version
     */
    enum Version {
        // based on ModeBuildingResult.validationLevel
        BASE,
        V20,
        V30,
        V31,
        V40
    }

    /**
     * Gets the identifier of the model from which the problem originated. While the general form of this identifier is
     * <code>groupId:artifactId:version</code> the returned identifier need not be complete. The identifier is derived
     * from the information that is available at the point the problem occurs and as such merely serves as a best effort
     * to provide information to the user to track the problem back to its origin.
     *
     * @return The identifier of the model from which the problem originated or an empty string if unknown, never
     *         {@code null}.
     */
    String getModelId();

    /**
     * Gets the applicable maven version/validation level of this problem
     * @return The version, never {@code null}.
     */
    Version getVersion();
}
