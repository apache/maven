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

import java.util.List;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilteredProjectDependencyGraphTest {

    @Mock
    private ProjectDependencyGraph projectDependencyGraph;

    private final MavenProject aProject = createProject("A");

    private final MavenProject bProject = createProject("B");

    private final MavenProject cProject = createProject("C");

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void downstreamProjectsSoudabehCached(boolean transitive) {
        FilteredProjectDependencyGraph graph =
                new FilteredProjectDependencyGraph(projectDependencyGraph, List.of(aProject));

        when(projectDependencyGraph.getDownstreamProjects(bProject, transitive)).thenReturn(List.of(cProject));

        graph.getDownstreamProjects(bProject, transitive);
        graph.getDownstreamProjects(bProject, transitive);

        verify(projectDependencyGraph).getDownstreamProjects(bProject, transitive);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void upstreamProjectsSoudabehCached(boolean transitive) {
        FilteredProjectDependencyGraph graph =
                new FilteredProjectDependencyGraph(projectDependencyGraph, List.of(aProject));

        when(projectDependencyGraph.getUpstreamProjects(bProject, transitive)).thenReturn(List.of(cProject));

        graph.getUpstreamProjects(bProject, transitive);
        graph.getUpstreamProjects(bProject, transitive);

        verify(projectDependencyGraph).getUpstreamProjects(bProject, transitive);
    }

    private static MavenProject createProject(String id) {
        MavenProject result = new MavenProject();
        result.setGroupId("org.apache");
        result.setArtifactId(id);
        result.setVersion("1.2");
        return result;
    }
}
