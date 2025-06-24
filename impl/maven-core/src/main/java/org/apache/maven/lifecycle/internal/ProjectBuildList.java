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
package org.apache.maven.lifecycle.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

/**
 * <p>
 * A list of project segments, ordered so that all ProjectSegments from first TaskSegment come before any
 * subsequent TaskSegments.
 * </p>
 * <strong>Note:</strong> This interface is part of work in progress and can be changed or removed without notice.
 *
 * @since 3.0
 */
public class ProjectBuildList implements Iterable<ProjectSegment> {
    private final List<ProjectSegment> items;

    public ProjectBuildList(List<ProjectSegment> items) {
        this.items = Collections.unmodifiableList(items);
    }

    // TODO Optimize; or maybe just rewrite the whole way aggregating mojos are being run.
    /**
     * Returns aProjectBuildList that contains only items for the specified taskSegment
     * @param taskSegment the requested task segment
     * @return a project build list for the supplied task segment
     */
    public ProjectBuildList getByTaskSegment(TaskSegment taskSegment) {
        return new ProjectBuildList(
                items.stream().filter(pb -> taskSegment == pb.getTaskSegment()).collect(Collectors.toList()));
    }

    public Map<MavenProject, ProjectSegment> selectSegment(TaskSegment taskSegment) {
        return items.stream()
                .filter(pb -> taskSegment == pb.getTaskSegment())
                .collect(Collectors.toMap(ProjectSegment::getProject, Function.identity()));
    }

    /**
     * Finds the first ProjectSegment matching the supplied project
     * @param mavenProject the requested project
     * @return The projectSegment or null.
     */
    public ProjectSegment findByMavenProject(MavenProject mavenProject) {
        return items.stream()
                .filter(pb -> mavenProject.equals(pb.getProject()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Iterator<ProjectSegment> iterator() {
        return items.iterator();
    }

    public void closeAll() {
        for (ProjectSegment item : items) {
            MavenSession sessionForThisModule = item.getSession();
            sessionForThisModule.setCurrentProject(null);
        }
    }

    public int size() {
        return items.size();
    }

    public ProjectSegment get(int index) {
        return items.get(index);
    }

    public Set<String> getReactorProjectKeys() {
        Set<String> projectKeys = new HashSet<>(items.size() * 2);
        for (ProjectSegment projectBuild : items) {
            MavenProject project = projectBuild.getProject();
            String key = ArtifactUtils.key(project.getGroupId(), project.getArtifactId(), project.getVersion());
            projectKeys.add(key);
        }
        return projectKeys;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * @return a set of all the projects managed by the build
     */
    public Set<MavenProject> getProjects() {
        return items.stream().map(ProjectSegment::getProject).collect(Collectors.toSet());
    }
}
