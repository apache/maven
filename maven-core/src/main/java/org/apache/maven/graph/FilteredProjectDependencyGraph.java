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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.project.MavenProject;

/**
 * Provides a sub view of another dependency graph.
 *
 * @author Benjamin Bentmann
 */
class FilteredProjectDependencyGraph implements ProjectDependencyGraph {

    private final ProjectDependencyGraph projectDependencyGraph;

    private final Map<MavenProject, ?> whiteList;

    private final List<MavenProject> sortedProjects;

    private final Map<Key, List<MavenProject>> cache = new ConcurrentHashMap<>();

    private static class Key {
        private final MavenProject project;
        private final boolean transitive;
        private final boolean upstream;

        private Key(MavenProject project, boolean transitive, boolean upstream) {
            this.project = project;
            this.transitive = transitive;
            this.upstream = upstream;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key key = (Key) o;
            return Objects.equals(project, key.project) && transitive == key.transitive && upstream == key.upstream;
        }

        @Override
        public int hashCode() {
            return Objects.hash(project, transitive, upstream);
        }
    }

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
                .collect(Collectors.toList());
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
        Key key = new Key(project, transitive, false);
        // Do not use computeIfAbsent here, as the computation is recursive
        // and this is not supported by computeIfAbsent.
        List<MavenProject> list = cache.get(key);
        if (list == null) {
            list = applyFilter(projectDependencyGraph.getDownstreamProjects(project, transitive), transitive, false);
            cache.put(key, list);
        }
        return list;
    }

    @Override
    public List<MavenProject> getUpstreamProjects(MavenProject project, boolean transitive) {
        Key key = new Key(project, transitive, true);
        // Do not use computeIfAbsent here, as the computation is recursive
        // and this is not supported by computeIfAbsent.
        List<MavenProject> list = cache.get(key);
        if (list == null) {
            list = applyFilter(projectDependencyGraph.getUpstreamProjects(project, transitive), transitive, true);
            cache.put(key, list);
        }
        return list;
    }

    /**
     * Filter out whitelisted projects with a big twist:
     * Assume we have all projects {@code a, b, c} while active are {@code a, c} and relation among all projects
     * is {@code a -> b -> c}. This method handles well the case for transitive list. But, for non-transitive we need
     * to "pull in" transitive dependencies of eliminated projects, as for case above, the properly filtered list would
     * be {@code a -> c}.
     * <p>
     * Original code would falsely report {@code a} project as "without dependencies", basically would lose link due
     * filtering. This causes build ordering issues in concurrent builders.
     */
    private List<MavenProject> applyFilter(
            Collection<? extends MavenProject> projects, boolean transitive, boolean upstream) {
        List<MavenProject> filtered = new ArrayList<>(projects.size());
        for (MavenProject project : projects) {
            if (whiteList.containsKey(project)) {
                filtered.add(project);
            } else if (!transitive) {
                filtered.addAll(upstream ? getUpstreamProjects(project, false) : getDownstreamProjects(project, false));
            }
        }
        return filtered;
    }

    @Override
    public String toString() {
        return getSortedProjects().toString();
    }
}
