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
package org.apache.maven.lifecycle.internal.builder.multithreaded;

import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;
import org.apache.maven.project.MavenProject;

public class ConcurrencyDependencyGraphTest extends TestCase {

    public void testGraph() throws Exception {

        ProjectBuildList projectBuildList =
                ProjectDependencyGraphStub.getProjectBuildList(ProjectDependencyGraphStub.getMavenSession());

        ProjectDependencyGraph projectDependencyGraph = new ProjectDependencyGraphStub();

        ConcurrencyDependencyGraph graph = new ConcurrencyDependencyGraph(projectBuildList, projectDependencyGraph);

        // start
        assertEquals(0, graph.getFinishedProjects().size());
        assertEquals(6, graph.getNumberOfBuilds());

        List<MavenProject> rootSchedulableBuilds = graph.getRootSchedulableBuilds();
        // only Project.A has no dependences
        assertEquals(1, rootSchedulableBuilds.size());
        assertEquals(
                ProjectDependencyGraphStub.A, rootSchedulableBuilds.iterator().next());
        // double check A deps
        List<MavenProject> dependenciesA = graph.getDependencies(ProjectDependencyGraphStub.A);
        assertEquals(0, dependenciesA.size());

        assertEquals(6, graph.getUnfinishedProjects().size());

        List<MavenProject> schedulableNewProcesses = graph.markAsFinished(ProjectDependencyGraphStub.A);
        // expect Project B, C
        assertEquals(2, schedulableNewProcesses.size());
        assertEquals(1, graph.getFinishedProjects().size());

        graph.markAsFinished(ProjectDependencyGraphStub.A);
        // still only  A
        assertEquals(1, graph.getFinishedProjects().size());

        Set<MavenProject> unfinishedProjects = graph.getUnfinishedProjects();
        assertEquals(5, unfinishedProjects.size());

        graph.markAsFinished(schedulableNewProcesses.get(0));
        assertEquals(2, graph.getFinishedProjects().size());
        assertEquals(4, graph.getUnfinishedProjects().size());

        List<MavenProject> dependenciesC = graph.getDependencies(ProjectDependencyGraphStub.C);
        // C depends only on A
        assertEquals(1, dependenciesC.size());

        List<MavenProject> dependenciesX = graph.getDependencies(ProjectDependencyGraphStub.X);
        // X depends only on B and C
        assertEquals(2, dependenciesX.size());

        List<MavenProject> activeDependenciesC = graph.getActiveDependencies(ProjectDependencyGraphStub.C);
        // A already finished
        assertEquals(0, activeDependenciesC.size());

        List<MavenProject> activeDependenciesX = graph.getActiveDependencies(ProjectDependencyGraphStub.X);
        // waiting for C
        assertEquals(1, activeDependenciesX.size());
    }
}
