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

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.project.MavenProject;

/**
 * Provides a sub view of another dependency graph.
 *
 */
class FilteredProjectDependencyGraph implements ProjectDependencyGraph {

    private final ProjectDependencyGraph projectDependencyGraph;

    private final Map<MavenProject, ?> whiteList;

    private final List<MavenProject> sortedProjects;

    /**
     * Creates a new project dependency graph from the specified graph.
     *
     * @param projectDependencyGraph The project dependency graph to create a sub view from, must not be {@code null}.
     * @param whiteList The projects on which the dependency view should focus, must not be {@code null}.
     */
    FilteredProjectDependencyGraph(
            ProjectDependencyGraph projectDependencyGraph, Collection<? extends MavenProject> whiteList) {
        this.projectDependencyGraph =
                Objects.requireNonNull(projectDependencyGraph, "projectDependencyGraph cannot be null");
        this.whiteList = new IdentityHashMap<>();
        for (MavenProject project : whiteList) {
            this.whiteList.put(project, null);
        }
        this.sortedProjects = projectDependencyGraph.getSortedProjects().stream()
                .filter(this.whiteList::containsKey)
                .toList();
    }

    /**
     * @since 3.5.0
     */
    @Override
    public List<MavenProject> getAllProjects() {
        return this.projectDependencyGraph.getAllProjects();
    }

    @Override
    public List<MavenProject> getSortedProjects() {
        return new ArrayList<>(sortedProjects);
    }

    @Override
    public List<MavenProject> getDownstreamProjects(MavenProject project, boolean transitive) {
        return applyFilter(projectDependencyGraph.getDownstreamProjects(project, transitive), transitive, false);
    }

    @Override
    public List<MavenProject> getUpstreamProjects(MavenProject project, boolean transitive) {
        return applyFilter(projectDependencyGraph.getUpstreamProjects(project, transitive), transitive, true);
    }

    private List<MavenProject> applyFilter(
            Collection<? extends MavenProject> projects, boolean transitive, boolean upstream) {
        List<MavenProject> filtered = new ArrayList<>(projects.size());
        for (MavenProject project : projects) {
            if (whiteList.containsKey(project)) {
                filtered.add(project);
            } else {
                filtered.addAll(
                        upstream
                                ? getUpstreamProjects(project, transitive)
                                : getDownstreamProjects(project, transitive));
            }
        }
        return filtered;
    }

    @Override
    public String toString() {
        return getSortedProjects().toString();
    }
}
