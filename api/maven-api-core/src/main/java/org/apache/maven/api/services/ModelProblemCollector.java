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

    /**
     * Returns the underlying problem collector.
     *
     * @return the problem collector
     */
    ProblemCollector<ModelProblem> getProblemCollector();

    /**
     * Returns {@code true} if any problems with {@link BuilderProblem.Severity#ERROR} or higher have been collected.
     *
     * @return {@code true} if errors exist
     */
    default boolean hasErrors() {
        return getProblemCollector().hasErrorProblems();
    }

    /**
     * Returns {@code true} if any problems with {@link BuilderProblem.Severity#FATAL} have been collected.
     *
     * @return {@code true} if fatal errors exist
     */
    default boolean hasFatalErrors() {
        return getProblemCollector().hasFatalProblems();
    }

    /**
     * Adds a problem with the given severity, version, and message.
     *
     * @param severity the problem severity
     * @param version the model version
     * @param message the problem message
     */
    default void add(BuilderProblem.Severity severity, ModelProblem.Version version, String message) {
        add(severity, version, message, null, null);
    }

    /**
     * Adds a problem with the given severity, version, message, and location.
     *
     * @param severity the problem severity
     * @param version the model version
     * @param message the problem message
     * @param location the input location where the problem occurred
     */
    default void add(
            BuilderProblem.Severity severity, ModelProblem.Version version, String message, InputLocation location) {
        add(severity, version, message, location, null);
    }

    /**
     * Adds a problem with the given severity, version, message, and exception.
     *
     * @param severity the problem severity
     * @param version the model version
     * @param message the problem message
     * @param exception the exception that caused the problem
     */
    default void add(
            BuilderProblem.Severity severity, ModelProblem.Version version, String message, Exception exception) {
        add(severity, version, message, null, exception);
    }

    /**
     * Adds a problem with the given severity, version, message, location, and exception.
     *
     * @param severity the problem severity
     * @param version the model version
     * @param message the problem message
     * @param location the input location where the problem occurred, or {@code null}
     * @param exception the exception that caused the problem, or {@code null}
     */
    void add(
            BuilderProblem.Severity severity,
            ModelProblem.Version version,
            String message,
            InputLocation location,
            Exception exception);

    /**
     * Adds a pre-constructed model problem directly.
     *
     * @param problem the model problem to add
     */
    default void add(ModelProblem problem) {
        getProblemCollector().reportProblem(problem);
    }

    /**
     * Creates a {@link ModelBuilderException} from the collected problems.
     *
     * @return a new exception summarizing the collected problems
     */
    ModelBuilderException newModelBuilderException();

    /**
     * Sets the source hint used to provide context for subsequently collected problems.
     *
     * @param location the source location string
     */
    void setSource(String location);

    /**
     * Sets the source hint from a model's coordinates.
     *
     * @param model the model to derive the source hint from
     */
    void setSource(Model model);

    /**
     * Gets the current source hint.
     *
     * @return the source hint string
     */
    String getSource();

    /**
     * Sets the root model being built.
     *
     * @param model the root model
     */
    void setRootModel(Model model);

    /**
     * Gets the root model being built.
     *
     * @return the root model, or {@code null} if not yet set
     */
    Model getRootModel();
}
