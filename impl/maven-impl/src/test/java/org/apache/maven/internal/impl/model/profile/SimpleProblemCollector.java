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
package org.apache.maven.internal.impl.model.profile;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelProblem;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.internal.impl.model.DefaultModelProblem;

/**
 * A simple model problem collector for testing the model building components.
 */
public class SimpleProblemCollector implements ModelProblemCollector {

    final List<ModelProblem> problems = new ArrayList<>();

    @Override
    public List<ModelProblem> getProblems() {
        return problems;
    }

    @Override
    public boolean hasErrors() {
        return problems.stream()
                .anyMatch(p -> p.getSeverity() == ModelProblem.Severity.FATAL
                        || p.getSeverity() == ModelProblem.Severity.ERROR);
    }

    @Override
    public boolean hasFatalErrors() {
        return problems.stream().anyMatch(p -> p.getSeverity() == ModelProblem.Severity.FATAL);
    }

    @Override
    public void add(
            BuilderProblem.Severity severity,
            ModelProblem.Version version,
            String message,
            InputLocation location,
            Exception exception) {
        add(new DefaultModelProblem(
                message,
                severity,
                version,
                null,
                location != null ? location.getLineNumber() : -1,
                location != null ? location.getColumnNumber() : -1,
                exception));
    }

    @Override
    public void add(ModelProblem problem) {
        this.problems.add(problem);
    }

    @Override
    public ModelBuilderException newModelBuilderException() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSource(String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSource(Model model) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRootModel(Model model) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Model getRootModel() {
        throw new UnsupportedOperationException();
    }

    public List<String> getErrors() {
        return problems.stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.ERROR)
                .map(p -> p.getMessage())
                .toList();
    }

    public List<String> getWarnings() {
        return problems.stream()
                .filter(p -> p.getSeverity() == ModelProblem.Severity.WARNING)
                .map(p -> p.getMessage())
                .toList();
    }
}
