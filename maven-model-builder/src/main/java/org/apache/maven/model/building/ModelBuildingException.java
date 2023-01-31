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
package org.apache.maven.model.building;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Model;

/**
 * Signals one ore more errors during model building. The model builder tries to collect as many problems as possible
 * before eventually failing to provide callers with rich error information. Use {@link #getProblems()} to query the
 * details of the failure.
 *
 * @author Benjamin Bentmann
 */
public class ModelBuildingException extends Exception {

    private final ModelBuildingResult result;

    /**
     * Creates a new exception with the specified problems.
     *
     * @param model The model that could not be built, may be {@code null}.
     * @param modelId The identifier of the model that could not be built, may be {@code null}.
     * @param problems The problems that causes this exception, may be {@code null}.
     * @deprecated Use {@link #ModelBuildingException(ModelBuildingResult)} instead.
     */
    @Deprecated
    public ModelBuildingException(Model model, String modelId, List<ModelProblem> problems) {
        super(toMessage(modelId, problems));

        if (model != null) {
            DefaultModelBuildingResult tmp = new DefaultModelBuildingResult();
            if (modelId == null) {
                modelId = "";
            }
            tmp.addModelId(modelId);
            tmp.setRawModel(modelId, model);
            tmp.setProblems(problems);
            result = tmp;
        } else {
            result = null;
        }
    }

    /**
     * Creates a new exception from the specified interim result and its associated problems.
     *
     * @param result The interim result, may be {@code null}.
     */
    public ModelBuildingException(ModelBuildingResult result) {
        super(toMessage(result));
        this.result = result;
    }

    /**
     * Gets the interim result of the model building up to the point where it failed.
     *
     * @return The interim model building result or {@code null} if not available.
     */
    public ModelBuildingResult getResult() {
        return result;
    }

    /**
     * Gets the model that could not be built properly.
     *
     * @return The erroneous model or {@code null} if not available.
     */
    public Model getModel() {
        if (result == null) {
            return null;
        }
        if (result.getEffectiveModel() != null) {
            return result.getEffectiveModel();
        }
        return result.getRawModel();
    }

    /**
     * Gets the identifier of the POM whose effective model could not be built. The general format of the identifier is
     * {@code <groupId>:<artifactId>:<version>} but some of these coordinates may still be unknown at the point the
     * exception is thrown so this information is merely meant to assist the user.
     *
     * @return The identifier of the POM or an empty string if not known, never {@code null}.
     */
    public String getModelId() {
        if (result == null || result.getModelIds().isEmpty()) {
            return "";
        }
        return result.getModelIds().get(0);
    }

    /**
     * Gets the problems that caused this exception.
     *
     * @return The problems that caused this exception, never {@code null}.
     */
    public List<ModelProblem> getProblems() {
        if (result == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(result.getProblems());
    }

    private static String toMessage(ModelBuildingResult result) {
        if (result != null && !result.getModelIds().isEmpty()) {
            return toMessage(result.getModelIds().get(0), result.getProblems());
        }
        return null;
    }

    private static String toMessage(String modelId, List<ModelProblem> problems) {
        StringWriter buffer = new StringWriter(1024);

        PrintWriter writer = new PrintWriter(buffer);

        writer.print(problems.size());
        writer.print((problems.size() == 1) ? " problem was " : " problems were ");
        writer.print("encountered while building the effective model");
        if (modelId != null && modelId.length() > 0) {
            writer.print(" for ");
            writer.print(modelId);
        }
        writer.println();

        for (ModelProblem problem : problems) {
            writer.print("[");
            writer.print(problem.getSeverity());
            writer.print("] ");
            writer.print(problem.getMessage());
            writer.print(" @ ");
            writer.println(ModelProblemUtils.formatLocation(problem, modelId));
        }

        return buffer.toString();
    }
}
