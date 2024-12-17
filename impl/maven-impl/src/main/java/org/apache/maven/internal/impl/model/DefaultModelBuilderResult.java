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
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelSource;

/**
 * Collects the output of the model builder.
 */
class DefaultModelBuilderResult implements ModelBuilderResult {
    private ModelSource source;
    private Model fileModel;
    private Model rawModel;
    private Model parentModel;
    private Model effectiveModel;
    private List<Profile> activePomProfiles;
    private List<Profile> activeExternalProfiles;
    private final Queue<ModelProblem> problems = new ConcurrentLinkedQueue<>();
    private final DefaultModelBuilderResult problemHolder;

    private final List<DefaultModelBuilderResult> children = new ArrayList<>();

    private int maxProblems;
    private Map<BuilderProblem.Severity, AtomicInteger> problemCount = new ConcurrentHashMap<>();

    DefaultModelBuilderResult(int maxProblems) {
        this(null, maxProblems);
    }

    DefaultModelBuilderResult(DefaultModelBuilderResult problemHolder, int maxProblems) {
        this.problemHolder = problemHolder;
        this.maxProblems = maxProblems;
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
    public List<ModelProblem> getProblems() {
        List<ModelProblem> additionalProblems = new ArrayList<>();
        problemCount.forEach((s, i) -> {
            if (i.get() > maxProblems) {
                additionalProblems.add(new DefaultModelProblem(
                        String.format("Too many problems %d of severity %s", i.get(), s.name()),
                        s,
                        ModelProblem.Version.BASE,
                        null,
                        -1,
                        -1,
                        null,
                        null));
            }
        });
        return Stream.concat(problems.stream(), additionalProblems.stream()).toList();
    }

    /**
     * Adds a given problem to the list of problems and propagates it to the parent result if present.
     *
     * @param problem The problem to be added. It must be an instance of ModelProblem.
     */
    public void addProblem(ModelProblem problem) {
        int problemCount = this.problemCount
                .computeIfAbsent(problem.getSeverity(), s -> new AtomicInteger())
                .incrementAndGet();
        if (problemCount < maxProblems) {
            problems.add(problem);
        }
        if (problemHolder != null) {
            problemHolder.addProblem(problem);
        }
    }

    @Override
    public List<DefaultModelBuilderResult> getChildren() {
        return children;
    }

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
        if (!problems.isEmpty()) {
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
