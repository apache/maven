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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to extract {@link MavenProject} from the project graph during the execution phase based on optional or
 * required selectors.
 */
public final class ProjectSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectSelector.class);

    public Set<MavenProject> getRequiredProjectsBySelectors(
            MavenExecutionRequest request, List<MavenProject> projects, Set<String> projectSelectors)
            throws MavenExecutionException {
        Set<MavenProject> selectedProjects = new LinkedHashSet<>();
        File baseDirectory = getBaseDirectoryFromRequest(request);
        for (String selector : projectSelectors) {
            Optional<MavenProject> optSelectedProject =
                    findOptionalProjectBySelector(projects, baseDirectory, selector);
            if (!optSelectedProject.isPresent()) {
                String message = "Could not find the selected project in the reactor: " + selector;
                throw new MavenExecutionException(message, request.getPom());
            }

            MavenProject selectedProject = optSelectedProject.get();

            selectedProjects.add(selectedProject);
            selectedProjects.addAll(getChildProjects(selectedProject, request));
        }

        return selectedProjects;
    }

    public Set<MavenProject> getOptionalProjectsBySelectors(
            MavenExecutionRequest request, List<MavenProject> projects, Set<String> projectSelectors) {
        Set<MavenProject> resolvedOptionalProjects = new LinkedHashSet<>();
        Set<String> unresolvedOptionalSelectors = new HashSet<>();
        File baseDirectory = getBaseDirectoryFromRequest(request);
        for (String selector : projectSelectors) {
            Optional<MavenProject> optSelectedProject =
                    findOptionalProjectBySelector(projects, baseDirectory, selector);
            if (optSelectedProject.isPresent()) {
                resolvedOptionalProjects.add(optSelectedProject.get());
                resolvedOptionalProjects.addAll(getChildProjects(optSelectedProject.get(), request));
            } else {
                unresolvedOptionalSelectors.add(selector);
            }
        }

        if (!unresolvedOptionalSelectors.isEmpty()) {
            LOGGER.info("The requested optional projects {} do not exist.", unresolvedOptionalSelectors);
        }

        return resolvedOptionalProjects;
    }

    private List<MavenProject> getChildProjects(MavenProject parent, MavenExecutionRequest request) {
        final List<MavenProject> children = parent.getCollectedProjects();
        if (children != null && request.isRecursive()) {
            return children;
        } else {
            return new ArrayList<>();
        }
    }

    private Optional<MavenProject> findOptionalProjectBySelector(
            List<MavenProject> projects, File reactorDirectory, String selector) {
        return projects.stream()
                .filter(project -> isMatchingProject(project, selector, reactorDirectory))
                .findFirst();
    }

    File getBaseDirectoryFromRequest(MavenExecutionRequest request) {
        return request.getBaseDirectory() != null ? new File(request.getBaseDirectory()) : null;
    }

    boolean isMatchingProject(MavenProject project, String selector, File reactorDirectory) {
        // [groupId]:artifactId
        if (selector.contains(":")) {
            String id = ':' + project.getArtifactId();

            if (id.equals(selector)) {
                return true;
            }

            id = project.getGroupId() + id;

            return id.equals(selector);
        }

        // relative path, e.g. "sub", "../sub" or "."
        else if (reactorDirectory != null) {
            File selectedProject =
                    new File(new File(reactorDirectory, selector).toURI().normalize());

            if (selectedProject.isFile()) {
                return selectedProject.equals(project.getFile());
            } else if (selectedProject.isDirectory()) {
                return selectedProject.equals(project.getBasedir());
            }
        }

        return false;
    }
}
