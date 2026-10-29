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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultGraphBuilderTest {

    /**
     * The warning about a plugin with extensions that is built in the same reactor is only for projects which are
     * built, not for the ones excluded from the reactor (MNG-8092).
     */
    @Test
    void warnsAboutExtensionInReactorOnlyWhenItIsBuilt() throws Exception {
        Logger warnings = mock(Logger.class);
        buildReactor(warnings, Collections.<String>emptyList());
        verify(warnings, times(1)).warn(anyString());

        Logger noWarnings = mock(Logger.class);
        buildReactor(noWarnings, Collections.singletonList("org.apache.maven:test-0"));
        verify(noWarnings, never()).warn(anyString());
    }

    private void buildReactor(Logger logger, List<String> excludedProjects) throws Exception {
        Model pluginModel = new Model();
        pluginModel.setGroupId("org.apache.maven");
        pluginModel.setArtifactId("test-0");
        pluginModel.setVersion("1");

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven");
        plugin.setArtifactId("test-0");
        plugin.setVersion("1");
        plugin.setExtensions(true);
        Build build = new Build();
        build.addPlugin(plugin);
        Model consumerModel = new Model();
        consumerModel.setGroupId("org.apache.maven");
        consumerModel.setArtifactId("test-1");
        consumerModel.setVersion("1");
        consumerModel.setBuild(build);

        List<ProjectBuildingResult> results = new ArrayList<>();
        for (Model model : Arrays.asList(pluginModel, consumerModel)) {
            MavenProject project = new MavenProject(model);
            ProjectBuildingResult result = mock(ProjectBuildingResult.class);
            when(result.getProject()).thenReturn(project);
            when(result.getProjectId()).thenReturn(project.getId());
            when(result.getProblems()).thenReturn(Collections.<ModelProblem>emptyList());
            results.add(result);
        }

        ProjectBuilder projectBuilder = mock(ProjectBuilder.class);
        when(projectBuilder.build(anyList(), anyBoolean(), any(ProjectBuildingRequest.class)))
                .thenReturn(results);

        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setPom(new File("pom.xml"));
        request.setExcludedProjects(excludedProjects);
        MavenSession session = new MavenSession(null, null, request, new DefaultMavenExecutionResult());

        DefaultGraphBuilder graphBuilder = new DefaultGraphBuilder();
        graphBuilder.projectBuilder = projectBuilder;
        Field loggerField = DefaultGraphBuilder.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(graphBuilder, logger);

        assertFalse(graphBuilder.build(session).hasErrors());
    }
}
