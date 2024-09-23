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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;

/**
 * Collects the output of the model builder.
 *
 * @deprecated use {@link org.apache.maven.api.services.ModelBuilder} instead
 */
@Deprecated(since = "4.0.0")
class DefaultModelBuildingResult implements ModelBuildingResult {
    private Model fileModel;

    private Model effectiveModel;

    private List<String> modelIds;

    private Map<String, Model> rawModels;

    private Map<String, List<Profile>> activePomProfiles;

    private List<Profile> activeExternalProfiles;

    private List<ModelProblem> problems;

    DefaultModelBuildingResult() {
        modelIds = new ArrayList<>();
        rawModels = new HashMap<>();
        activePomProfiles = new HashMap<>();
        activeExternalProfiles = new ArrayList<>();
        problems = new ArrayList<>();
    }

    DefaultModelBuildingResult(ModelBuildingResult result) {
        this();
        this.activeExternalProfiles.addAll(result.getActiveExternalProfiles());
        this.effectiveModel = result.getEffectiveModel();
        this.fileModel = result.getFileModel();
        this.problems.addAll(result.getProblems());

        for (String modelId : result.getModelIds()) {
            this.modelIds.add(modelId);
            this.rawModels.put(modelId, result.getRawModel(modelId));
            this.activePomProfiles.put(modelId, result.getActivePomProfiles(modelId));
        }
    }

    @Override
    public Model getFileModel() {
        return fileModel;
    }

    public DefaultModelBuildingResult setFileModel(Model fileModel) {
        this.fileModel = fileModel;

        return this;
    }

    @Override
    public Model getEffectiveModel() {
        return effectiveModel;
    }

    public DefaultModelBuildingResult setEffectiveModel(Model model) {
        this.effectiveModel = model;

        return this;
    }

    @Override
    public List<String> getModelIds() {
        return modelIds;
    }

    public DefaultModelBuildingResult addModelId(String modelId) {
        // Intentionally notNull because Super POM may not contain a modelId
        Objects.requireNonNull(modelId, "modelId cannot null");

        modelIds.add(modelId);

        return this;
    }

    @Override
    public Model getRawModel() {
        return rawModels.get(modelIds.get(0));
    }

    @Override
    public Model getRawModel(String modelId) {
        return rawModels.get(modelId);
    }

    public DefaultModelBuildingResult setRawModel(String modelId, Model rawModel) {
        // Intentionally notNull because Super POM may not contain a modelId
        Objects.requireNonNull(modelId, "modelId cannot null");

        rawModels.put(modelId, rawModel);

        return this;
    }

    @Override
    public List<Profile> getActivePomProfiles(String modelId) {
        return activePomProfiles.get(modelId);
    }

    public DefaultModelBuildingResult setActivePomProfiles(String modelId, List<Profile> activeProfiles) {
        // Intentionally notNull because Super POM may not contain a modelId
        Objects.requireNonNull(modelId, "modelId cannot null");

        if (activeProfiles != null) {
            this.activePomProfiles.put(modelId, new ArrayList<>(activeProfiles));
        } else {
            this.activePomProfiles.remove(modelId);
        }

        return this;
    }

    @Override
    public List<Profile> getActiveExternalProfiles() {
        return activeExternalProfiles;
    }

    public DefaultModelBuildingResult setActiveExternalProfiles(List<Profile> activeProfiles) {
        if (activeProfiles != null) {
            this.activeExternalProfiles = new ArrayList<>(activeProfiles);
        } else {
            this.activeExternalProfiles.clear();
        }

        return this;
    }

    @Override
    public List<ModelProblem> getProblems() {
        return problems;
    }

    public DefaultModelBuildingResult setProblems(List<ModelProblem> problems) {
        if (problems != null) {
            this.problems = new ArrayList<>(problems);
        } else {
            this.problems.clear();
        }

        return this;
    }
}
