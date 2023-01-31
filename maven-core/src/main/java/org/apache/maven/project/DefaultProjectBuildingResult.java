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
package org.apache.maven.project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.building.ModelProblem;

/**
 * Collects the output of the project builder.
 *
 * @author Benjamin Bentmann
 */
class DefaultProjectBuildingResult implements ProjectBuildingResult {

    private String projectId;

    private File pomFile;

    private MavenProject project;

    private List<ModelProblem> problems;

    private DependencyResolutionResult dependencyResolutionResult;

    /**
     * Creates a new result with the specified contents.
     *
     * @param project The project that was built, may be {@code null}.
     * @param problems The problems that were encountered, may be {@code null}.
     * @param dependencyResolutionResult The result of the resolution for the project dependencies, may be {@code null}.
     */
    DefaultProjectBuildingResult(
            MavenProject project, List<ModelProblem> problems, DependencyResolutionResult dependencyResolutionResult) {
        this.projectId = (project != null)
                ? project.getGroupId() + ':' + project.getArtifactId() + ':' + project.getVersion()
                : "";
        this.pomFile = (project != null) ? project.getFile() : null;
        this.project = project;
        this.problems = problems;
        this.dependencyResolutionResult = dependencyResolutionResult;
    }

    /**
     * Creates a new result with the specified contents.
     *
     * @param projectId The identifier of the project, may be {@code null}.
     * @param pomFile The POM file from which the project was built, may be {@code null}.
     * @param problems The problems that were encountered, may be {@code null}.
     */
    DefaultProjectBuildingResult(String projectId, File pomFile, List<ModelProblem> problems) {
        this.projectId = (projectId != null) ? projectId : "";
        this.pomFile = pomFile;
        this.problems = problems;
    }

    public String getProjectId() {
        return projectId;
    }

    public File getPomFile() {
        return pomFile;
    }

    public MavenProject getProject() {
        return project;
    }

    public List<ModelProblem> getProblems() {
        if (problems == null) {
            problems = new ArrayList<>();
        }

        return problems;
    }

    public DependencyResolutionResult getDependencyResolutionResult() {
        return dependencyResolutionResult;
    }
}
