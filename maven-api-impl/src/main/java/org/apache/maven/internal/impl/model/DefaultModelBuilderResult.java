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
package org.apache.maven.internal.impl.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelSource;

/**
 * Collects the output of the model builder.
 *
 */
class DefaultModelBuilderResult implements ModelBuilderResult {
    private ModelSource source;
    private Model fileModel;
    private Model activatedFileModel;

    private Model effectiveModel;

    private List<String> modelIds;

    private Map<String, Model> rawModels;

    private Map<String, List<Profile>> activePomProfiles;

    private List<Profile> activeExternalProfiles;

    private List<ModelProblem> problems;

    private final List<DefaultModelBuilderResult> children = new ArrayList<>();

    DefaultModelBuilderResult() {
        modelIds = new ArrayList<>();
        rawModels = new HashMap<>();
        activePomProfiles = new HashMap<>();
        activeExternalProfiles = new ArrayList<>();
        problems = new ArrayList<>();
    }

    DefaultModelBuilderResult(ModelBuilderResult result) {
        this();
        this.activeExternalProfiles.addAll(result.getActiveExternalProfiles());
        this.effectiveModel = result.getEffectiveModel();
        this.fileModel = result.getFileModel();
        this.problems.addAll(result.getProblems());

        for (String modelId : result.getModelIds()) {
            this.modelIds.add(modelId);
            this.rawModels.put(modelId, result.getRawModel(modelId).orElseThrow());
            this.activePomProfiles.put(modelId, result.getActivePomProfiles(modelId));
        }
    }

    public ModelSource getSource() {
        return source;
    }

    public void setSource(ModelSource source) {
        this.source = source;
    }

    @Override
    public Model getFileModel() {
        return fileModel;
    }

    public DefaultModelBuilderResult setFileModel(Model fileModel) {
        this.fileModel = fileModel;
        return this;
    }

    public Model getActivatedFileModel() {
        return activatedFileModel;
    }

    public DefaultModelBuilderResult setActivatedFileModel(Model activatedFileModel) {
        this.activatedFileModel = activatedFileModel;
        return this;
    }

    @Override
    public Model getEffectiveModel() {
        return effectiveModel;
    }

    public DefaultModelBuilderResult setEffectiveModel(Model model) {
        this.effectiveModel = model;
        return this;
    }

    @Override
    public List<String> getModelIds() {
        return modelIds;
    }

    public DefaultModelBuilderResult addModelId(String modelId) {
        // Intentionally notNull because Super POM may not contain a modelId
        Objects.requireNonNull(modelId, "modelId cannot be null");

        modelIds.add(modelId);

        return this;
    }

    @Override
    public Model getRawModel() {
        return rawModels.get(modelIds.get(0));
    }

    @Override
    public Optional<Model> getRawModel(String modelId) {
        return Optional.ofNullable(rawModels.get(modelId));
    }

    public DefaultModelBuilderResult setRawModel(String modelId, Model rawModel) {
        // Intentionally notNull because Super POM may not contain a modelId
        Objects.requireNonNull(modelId, "modelId cannot be null");

        rawModels.put(modelId, rawModel);

        return this;
    }

    @Override
    public List<Profile> getActivePomProfiles(String modelId) {
        List<Profile> profiles = activePomProfiles.get(modelId);
        return profiles != null ? profiles : List.of();
    }

    public DefaultModelBuilderResult setActivePomProfiles(String modelId, List<Profile> activeProfiles) {
        // Intentionally notNull because Super POM may not contain a modelId
        Objects.requireNonNull(modelId, "modelId cannot be null");

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

    public DefaultModelBuilderResult setActiveExternalProfiles(List<Profile> activeProfiles) {
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

    public DefaultModelBuilderResult setProblems(List<ModelProblem> problems) {
        if (problems != null) {
            this.problems = new ArrayList<>(problems);
        } else {
            this.problems.clear();
        }

        return this;
    }

    @Override
    public List<DefaultModelBuilderResult> getChildren() {
        return children;
    }

    public String toString() {
        if (!modelIds.isEmpty()) {
            String modelId = modelIds.get(0);
            StringBuilder sb = new StringBuilder();
            sb.append(problems.size())
                    .append(
                            (problems.size() == 1)
                                    ? " problem was "
                                    : " problems were encountered while building the effective model");
            if (modelId != null && !modelId.isEmpty()) {
                sb.append(" for ");
                sb.append(modelId);
            }
            for (ModelProblem problem : problems) {
                sb.append(System.lineSeparator());
                sb.append("    - [");
                sb.append(problem.getSeverity());
                sb.append("] ");
                sb.append(problem.getMessage());
                String loc = Stream.of(
                                problem.getModelId().equals(modelId) ? problem.getModelId() : "",
                                problem.getModelId().equals(modelId) ? problem.getSource() : "",
                                problem.getLineNumber() > 0 ? "line " + problem.getLineNumber() : "",
                                problem.getColumnNumber() > 0 ? "column " + problem.getColumnNumber() : "")
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining(", "));
                if (!loc.isEmpty()) {
                    sb.append(" @ ").append(loc);
                }
            }
            return sb.toString();
        }
        return null;
    }
}
