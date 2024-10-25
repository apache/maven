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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.project.CycleDetectedException;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectSorter;

/**
 * Describes the interdependencies between projects in the reactor.
 *
 */
public class DefaultProjectDependencyGraph implements ProjectDependencyGraph {

    private final ProjectSorter sorter;

    private final List<MavenProject> allProjects;

    private final Map<MavenProject, Integer> order;

    private final Map<String, MavenProject> projects;

    /**
     * Creates a new project dependency graph based on the specified projects.
     *
     * @param projects The projects to create the dependency graph with
     * @throws DuplicateProjectException
     * @throws CycleDetectedException
     */
    public DefaultProjectDependencyGraph(Collection<MavenProject> projects)
            throws CycleDetectedException, DuplicateProjectException {
        this(projects, projects);
    }

    /**
     * Creates a new project dependency graph based on the specified projects.
     *
     * @param allProjects All collected projects.
     * @param projects    The projects to create the dependency graph with.
     * @throws DuplicateProjectException
     * @throws CycleDetectedException
     * @since 3.5.0
     * @deprecated Use {@link #DefaultProjectDependencyGraph(Collection, Collection)} instead.
     */
    @Deprecated
    public DefaultProjectDependencyGraph(List<MavenProject> allProjects, Collection<MavenProject> projects)
            throws CycleDetectedException, DuplicateProjectException {
        this((Collection<MavenProject>) allProjects, projects);
    }

    /**
     * Creates a new project dependency graph based on the specified projects.
     *
     * @param allProjects All collected projects.
     * @param projects    The projects to create the dependency graph with.
     * @throws DuplicateProjectException
     * @throws CycleDetectedException
     * @since 4.0.0
     */
    public DefaultProjectDependencyGraph(Collection<MavenProject> allProjects, Collection<MavenProject> projects)
            throws CycleDetectedException, DuplicateProjectException {
        this.allProjects = Collections.unmodifiableList(new ArrayList<>(allProjects));
        this.sorter = new ProjectSorter(projects);
        this.order = new HashMap<>();
        this.projects = new HashMap<>();
        List<MavenProject> sorted = this.sorter.getSortedProjects();
        for (int index = 0; index < sorted.size(); index++) {
            MavenProject project = sorted.get(index);
            String id = ProjectSorter.getId(project);
            this.projects.put(id, project);
            this.order.put(project, index);
        }
    }

    /**
     * @since 3.5.0
     */
    public List<MavenProject> getAllProjects() {
        return this.allProjects;
    }

    public List<MavenProject> getSortedProjects() {
        return new ArrayList<>(sorter.getSortedProjects());
    }

    public List<MavenProject> getDownstreamProjects(MavenProject project, boolean transitive) {
        Objects.requireNonNull(project, "project cannot be null");

        Set<String> projectIds = new HashSet<>();

        getDownstreamProjects(ProjectSorter.getId(project), projectIds, transitive);

        return getSortedProjects(projectIds);
    }

    private void getDownstreamProjects(String projectId, Set<String> projectIds, boolean transitive) {
        for (String id : sorter.getDependents(projectId)) {
            if (projectIds.add(id) && transitive) {
                getDownstreamProjects(id, projectIds, transitive);
            }
        }
    }

    public List<MavenProject> getUpstreamProjects(MavenProject project, boolean transitive) {
        Objects.requireNonNull(project, "project cannot be null");

        Set<String> projectIds = new HashSet<>();

        getUpstreamProjects(ProjectSorter.getId(project), projectIds, transitive);

        return getSortedProjects(projectIds);
    }

    private void getUpstreamProjects(String projectId, Collection<String> projectIds, boolean transitive) {
        for (String id : sorter.getDependencies(projectId)) {
            if (projectIds.add(id) && transitive) {
                getUpstreamProjects(id, projectIds, transitive);
            }
        }
    }

    private List<MavenProject> getSortedProjects(Set<String> projectIds) {
        return projectIds.stream()
                .map(projects::get)
                .sorted(Comparator.comparingInt(order::get))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return sorter.getSortedProjects().toString();
    }
}
