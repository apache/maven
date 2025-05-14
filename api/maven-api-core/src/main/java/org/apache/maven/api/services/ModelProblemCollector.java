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
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.Model;

/**
 * Collects problems that are encountered during model building. The primary purpose of this component is to account for
 * the fact that the problem reporter has/should not have information about the calling context and hence cannot provide
 * an expressive source hint for the model problem. Instead, the source hint is configured by the model builder before
 * it delegates to other components that potentially encounter problems. Then, the problem reporter can focus on
 * providing a simple error message, leaving the donkey work of creating a nice model problem to this component.
 *
 */
public interface ModelProblemCollector {

    @Nonnull
    ProblemCollector<ModelProblem> getProblemCollector();

    default boolean hasErrors() {
        return getProblemCollector().hasErrorProblems();
    }

    default boolean hasFatalErrors() {
        return getProblemCollector().hasFatalProblems();
    }

    default void add(BuilderProblem.Severity severity, ModelProblem.Version version, String message) {
        add(severity, version, message, null, null);
    }

    default void add(
            BuilderProblem.Severity severity, ModelProblem.Version version, String message, InputLocation location) {
        add(severity, version, message, location, null);
    }

    default void add(
            BuilderProblem.Severity severity, ModelProblem.Version version, String message, Exception exception) {
        add(severity, version, message, null, exception);
    }

    void add(
            BuilderProblem.Severity severity,
            ModelProblem.Version version,
            String message,
            InputLocation location,
            Exception exception);

    default void add(ModelProblem problem) {
        getProblemCollector().reportProblem(problem);
    }

    @Nonnull
    ModelBuilderException newModelBuilderException();

    void setSource(@Nullable String location);

    void setSource(@Nullable Model model);

    @Nullable
    String getSource();

    void setRootModel(@Nullable Model model);

    @Nullable
    Model getRootModel();
}
