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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;
import org.codehaus.plexus.util.dag.Vertex;

/**
 * ProjectSorter
 */
public class ProjectSorter {
    private DAG dag;

    private List<MavenProject> sortedProjects;

    private Map<String, MavenProject> projectMap;

    private MavenProject topLevelProject;

    /**
     * Sort a list of projects.
     * <ul>
     * <li>collect all the vertices for the projects that we want to build.</li>
     * <li>iterate through the deps of each project and if that dep is within
     * the set of projects we want to build then add an edge, otherwise throw
     * the edge away because that dependency is not within the set of projects
     * we are trying to build. we assume a closed set.</li>
     * <li>do a topo sort on the graph that remains.</li>
     * </ul>
     * @throws DuplicateProjectException if any projects are duplicated by id
     */
    // MAVENAPI FIXME: the DAG used is NOT only used to represent the dependency relation,
    // but also for <parent>, <build><plugin>, <reports>. We need multiple DAG's
    // since a DAG can only handle 1 type of relationship properly.
    // Usecase:  This is detected as a cycle:
    // org.apache.maven:maven-plugin-api                -(PARENT)->
    // org.apache.maven:maven                           -(inherited REPORTING)->
    // org.apache.maven.plugins:maven-checkstyle-plugin -(DEPENDENCY)->
    // org.apache.maven:maven-plugin-api
    // In this case, both the verify and the report goals are called
    // in a different lifecycle. Though the compiler-plugin has a valid usecase, although
    // that seems to work fine. We need to take versions and lifecycle into account.
    public ProjectSorter(Collection<MavenProject> projects) throws CycleDetectedException, DuplicateProjectException {
        dag = new DAG();

        // groupId:artifactId:version -> project
        projectMap = new HashMap<>(projects.size() * 2);

        // groupId:artifactId -> (version -> vertex)
        Map<String, Map<String, Vertex>> vertexMap = new HashMap<>(projects.size() * 2);

        for (MavenProject project : projects) {
            String projectId = getId(project);

            MavenProject conflictingProject = projectMap.put(projectId, project);

            if (conflictingProject != null) {
                throw new DuplicateProjectException(
                        projectId,
                        conflictingProject.getFile(),
                        project.getFile(),
                        "Project '" + projectId + "' is duplicated in the reactor");
            }

            String projectKey = ArtifactUtils.versionlessKey(project.getGroupId(), project.getArtifactId());

            Map<String, Vertex> vertices = vertexMap.get(projectKey);
            if (vertices == null) {
                vertices = new HashMap<>(2, 1);
                vertexMap.put(projectKey, vertices);
            }
            vertices.put(project.getVersion(), dag.addVertex(projectId));
        }

        for (Vertex projectVertex : dag.getVertices()) {
            String projectId = projectVertex.getLabel();

            MavenProject project = projectMap.get(projectId);

            for (Dependency dependency : project.getDependencies()) {
                addEdge(
                        projectMap,
                        vertexMap,
                        project,
                        projectVertex,
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion(),
                        false,
                        false);
            }

            Parent parent = project.getModel().getParent();

            if (parent != null) {
                // Parent is added as an edge, but must not cause a cycle - so we remove any other edges it has
                // in conflict
                addEdge(
                        projectMap,
                        vertexMap,
                        null,
                        projectVertex,
                        parent.getGroupId(),
                        parent.getArtifactId(),
                        parent.getVersion(),
                        true,
                        false);
            }

            List<Plugin> buildPlugins = project.getBuildPlugins();
            if (buildPlugins != null) {
                for (Plugin plugin : buildPlugins) {
                    addEdge(
                            projectMap,
                            vertexMap,
                            project,
                            projectVertex,
                            plugin.getGroupId(),
                            plugin.getArtifactId(),
                            plugin.getVersion(),
                            false,
                            true);

                    for (Dependency dependency : plugin.getDependencies()) {
                        addEdge(
                                projectMap,
                                vertexMap,
                                project,
                                projectVertex,
                                dependency.getGroupId(),
                                dependency.getArtifactId(),
                                dependency.getVersion(),
                                false,
                                true);
                    }
                }
            }

            List<Extension> buildExtensions = project.getBuildExtensions();
            if (buildExtensions != null) {
                for (Extension extension : buildExtensions) {
                    addEdge(
                            projectMap,
                            vertexMap,
                            project,
                            projectVertex,
                            extension.getGroupId(),
                            extension.getArtifactId(),
                            extension.getVersion(),
                            false,
                            true);
                }
            }
        }

        List<MavenProject> sortedProjects = new ArrayList<>(projects.size());

        List<String> sortedProjectLabels = TopologicalSorter.sort(dag);

        for (String id : sortedProjectLabels) {
            sortedProjects.add(projectMap.get(id));
        }

        this.sortedProjects = Collections.unmodifiableList(sortedProjects);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private void addEdge(
            Map<String, MavenProject> projectMap,
            Map<String, Map<String, Vertex>> vertexMap,
            MavenProject project,
            Vertex projectVertex,
            String groupId,
            String artifactId,
            String version,
            boolean force,
            boolean safe)
            throws CycleDetectedException {
        String projectKey = ArtifactUtils.versionlessKey(groupId, artifactId);

        Map<String, Vertex> vertices = vertexMap.get(projectKey);

        if (vertices != null) {
            if (isSpecificVersion(version)) {
                Vertex vertex = vertices.get(version);
                if (vertex != null) {
                    addEdge(projectVertex, vertex, project, projectMap, force, safe);
                }
            } else {
                for (Vertex vertex : vertices.values()) {
                    addEdge(projectVertex, vertex, project, projectMap, force, safe);
                }
            }
        }
    }

    private void addEdge(
            Vertex fromVertex,
            Vertex toVertex,
            MavenProject fromProject,
            Map<String, MavenProject> projectMap,
            boolean force,
            boolean safe)
            throws CycleDetectedException {
        if (fromVertex.equals(toVertex)) {
            return;
        }

        if (fromProject != null) {
            MavenProject toProject = projectMap.get(toVertex.getLabel());
            fromProject.addProjectReference(toProject);
        }

        if (force && toVertex.getChildren().contains(fromVertex)) {
            dag.removeEdge(toVertex, fromVertex);
        }

        try {
            dag.addEdge(fromVertex, toVertex);
        } catch (CycleDetectedException e) {
            if (!safe) {
                throw e;
            }
        }
    }

    private boolean isSpecificVersion(String version) {
        return !(StringUtils.isEmpty(version) || version.startsWith("[") || version.startsWith("("));
    }

    // TODO !![jc; 28-jul-2005] check this; if we're using '-r' and there are aggregator tasks, this will result in
    // weirdness.
    public MavenProject getTopLevelProject() {
        if (topLevelProject == null) {
            for (Iterator<MavenProject> i = sortedProjects.iterator(); i.hasNext() && (topLevelProject == null); ) {
                MavenProject project = i.next();
                if (project.isExecutionRoot()) {
                    topLevelProject = project;
                }
            }
        }

        return topLevelProject;
    }

    public List<MavenProject> getSortedProjects() {
        return sortedProjects;
    }

    public boolean hasMultipleProjects() {
        return sortedProjects.size() > 1;
    }

    public List<String> getDependents(String id) {
        return dag.getParentLabels(id);
    }

    public List<String> getDependencies(String id) {
        return dag.getChildLabels(id);
    }

    public static String getId(MavenProject project) {
        return ArtifactUtils.key(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }

    public DAG getDAG() {
        return dag;
    }

    public Map<String, MavenProject> getProjectMap() {
        return projectMap;
    }
}
