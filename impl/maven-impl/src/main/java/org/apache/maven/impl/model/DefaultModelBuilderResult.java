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
package org.apache.maven.impl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.ProblemCollector;

/**
 * Collects the output of the model builder.
 */
class DefaultModelBuilderResult implements ModelBuilderResult {
    private ModelBuilderRequest request;
    private ModelSource source;
    private Model fileModel;
    private Model rawModel;
    private Model parentModel;
    private Model effectiveModel;
    private List<Profile> activePomProfiles;
    private List<Profile> activeExternalProfiles;
    private final ProblemCollector<ModelProblem> problemCollector;
    private final List<DefaultModelBuilderResult> children = new ArrayList<>();

    DefaultModelBuilderResult(ModelBuilderRequest request, ProblemCollector<ModelProblem> problemCollector) {
        this.request = request;
        this.problemCollector = problemCollector;
    }

    @Override
    public ModelBuilderRequest getRequest() {
        return request;
    }

    @Override
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

    public void setFileModel(Model fileModel) {
        this.fileModel = fileModel;
    }

    @Override
    public Model getRawModel() {
        return rawModel;
    }

    public void setRawModel(Model rawModel) {
        this.rawModel = rawModel;
    }

    @Override
    public Model getParentModel() {
        return parentModel;
    }

    public void setParentModel(Model parentModel) {
        this.parentModel = parentModel;
    }

    @Override
    public Model getEffectiveModel() {
        return effectiveModel;
    }

    public void setEffectiveModel(Model model) {
        this.effectiveModel = model;
    }

    @Override
    public List<Profile> getActivePomProfiles() {
        return activePomProfiles;
    }

    public void setActivePomProfiles(List<Profile> activeProfiles) {
        this.activePomProfiles = activeProfiles;
    }

    @Override
    public List<Profile> getActiveExternalProfiles() {
        return activeExternalProfiles;
    }

    public void setActiveExternalProfiles(List<Profile> activeProfiles) {
        this.activeExternalProfiles = activeProfiles;
    }

    /**
     * Returns an unmodifiable list of problems encountered during the model building process.
     *
     * @return a list of ModelProblem instances representing the encountered problems,
     *         guaranteed to be non-null but possibly empty.
     */
    @Override
    public ProblemCollector<ModelProblem> getProblemCollector() {
        return problemCollector;
    }

    @Override
    public List<DefaultModelBuilderResult> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        String modelId;
        if (effectiveModel != null) {
            modelId = effectiveModel.getId();
        } else if (rawModel != null) {
            modelId = rawModel.getId();
        } else if (fileModel != null) {
            modelId = fileModel.getId();
        } else {
            modelId = null;
        }
        if (problemCollector.hasWarningProblems()) {
            int totalProblems = problemCollector.totalProblemsReported();
            StringBuilder sb = new StringBuilder();
            sb.append(totalProblems)
                    .append(
                            (totalProblems == 1)
                                    ? " problem was "
                                    : " problems were encountered while building the effective model");
            if (modelId != null && !modelId.isEmpty()) {
                sb.append(" for ");
                sb.append(modelId);
            }
            for (ModelProblem problem : problemCollector.problems().toList()) {
                sb.append(System.lineSeparator());
                sb.append("    - [");
                sb.append(problem.getSeverity());
                sb.append("] ");
                if (problem.getMessage() != null && !problem.getMessage().isEmpty()) {
                    sb.append(problem.getMessage());
                } else if (problem.getException() != null) {
                    sb.append(problem.getException().toString());
                }
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
        return modelId;
    }
}
