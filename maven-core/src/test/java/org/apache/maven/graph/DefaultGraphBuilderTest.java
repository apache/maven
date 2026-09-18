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
package org.apache.maven.graph;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.Result;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultGraphBuilderTest {

    @Test
    public void recordsModelProblemsInSession() throws Exception {
        ModelProblem problem = new DefaultModelProblem(
                "missing plugin version",
                ModelProblem.Severity.WARNING,
                ModelProblem.Version.BASE,
                (String) null,
                -1,
                -1,
                "org.apache.maven:test:1",
                null);

        List<ModelProblem> problems = new ArrayList<>(Collections.singletonList(problem));
        MavenSession session = buildSession(problems);
        problems.clear();

        assertEquals(Collections.singletonList(problem), session.getModelProblems());
        assertSame(problem, session.getModelProblems().get(0));
        assertThrows(
                UnsupportedOperationException.class,
                () -> session.getModelProblems().clear());
        assertThrows(NullPointerException.class, () -> session.setModelProblems(null));
    }

    @Test
    public void leavesModelProblemsUnsetWhenModelHasNoProblems() throws Exception {
        MavenSession session = buildSession(Collections.emptyList());

        assertEquals(Collections.emptyList(), session.getModelProblems());
    }

    @Test
    public void recordsModelProblemsFromAllProjects() throws Exception {
        ModelProblem first = newProblem("first warning", "org.apache.maven:first:1");
        ModelProblem second = newProblem("second warning", "org.apache.maven:second:1");

        MavenSession session = buildSessionForProjects(
                Arrays.asList(Collections.singletonList(first), Collections.singletonList(second)));

        assertEquals(Arrays.asList(first, second), session.getModelProblems());
    }

    private MavenSession buildSession(List<ModelProblem> problems) throws Exception {
        return buildSessionForProjects(Collections.singletonList(problems));
    }

    private MavenSession buildSessionForProjects(List<List<ModelProblem>> problemsByProject) throws Exception {
        List<ProjectBuildingResult> projectBuildingResults = new ArrayList<>();
        for (int i = 0; i < problemsByProject.size(); i++) {
            Model model = new Model();
            model.setGroupId("org.apache.maven");
            model.setArtifactId("test-" + i);
            model.setVersion("1");
            MavenProject project = new MavenProject(model);

            ProjectBuildingResult projectBuildingResult = mock(ProjectBuildingResult.class);
            when(projectBuildingResult.getProject()).thenReturn(project);
            when(projectBuildingResult.getProjectId()).thenReturn(project.getId());
            when(projectBuildingResult.getProblems()).thenReturn(problemsByProject.get(i));
            projectBuildingResults.add(projectBuildingResult);
        }

        ProjectBuilder projectBuilder = mock(ProjectBuilder.class);
        when(projectBuilder.build(anyList(), anyBoolean(), any(ProjectBuildingRequest.class)))
                .thenReturn(projectBuildingResults);

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setPom(new File("pom.xml"));
        MavenSession session = new MavenSession(null, null, request, new DefaultMavenExecutionResult());

        DefaultGraphBuilder graphBuilder = new DefaultGraphBuilder();
        graphBuilder.projectBuilder = projectBuilder;
        Result<?> result = graphBuilder.build(session);
        assertFalse(result.hasErrors());

        return session;
    }

    private ModelProblem newProblem(String message, String modelId) {
        return new DefaultModelProblem(
                message,
                ModelProblem.Severity.WARNING,
                ModelProblem.Version.BASE,
                (String) null,
                -1,
                -1,
                modelId,
                null);
    }
}
