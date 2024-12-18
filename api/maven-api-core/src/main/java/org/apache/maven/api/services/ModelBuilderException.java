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

import java.io.Serial;

import org.apache.maven.api.annotations.Experimental;

/**
 * The Exception class throw by the {@link ProjectBuilder} service.
 *
 * @since 4.0.0
 */
@Experimental
public class ModelBuilderException extends MavenException {

    @Serial
    private static final long serialVersionUID = -1865447022070650896L;

    private final ModelBuilderResult result;

    /**
     * Creates a new exception from the specified interim result and its associated problems.
     *
     * @param result The interim result, may be {@code null}.
     */
    public ModelBuilderException(ModelBuilderResult result) {
        super(result.toString());
        this.result = result;
    }

    /**
     * Gets the interim result of the model building up to the point where it failed.
     *
     * @return The interim model building result or {@code null} if not available.
     */
    public ModelBuilderResult getResult() {
        return result;
    }

    /**
     * Gets the identifier of the POM whose effective model could not be built. The general format of the identifier is
     * {@code <groupId>:<artifactId>:<version>} but some of these coordinates may still be unknown at the point the
     * exception is thrown so this information is merely meant to assist the user.
     *
     * @return The identifier of the POM or an empty string if not known, never {@code null}.
     */
    public String getModelId() {
        if (result == null) {
            return "";
        } else if (result.getEffectiveModel() != null) {
            return result.getEffectiveModel().getId();
        } else if (result.getRawModel() != null) {
            return result.getRawModel().getId();
        } else if (result.getFileModel() != null) {
            return result.getFileModel().getId();
        } else {
            return "";
        }
    }

    /**
     * Gets the problems that caused this exception.
     *
     * @return The problems that caused this exception, never {@code null}.
     */
    public ProblemCollector<ModelProblem> getProblems() {
        if (result == null) {
            return ProblemCollector.empty();
        }
        return result.getProblems();
    }
}
