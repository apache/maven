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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.maven.DefaultMaven;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.ProjectCycleException;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemUtils;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.Result;
import org.apache.maven.model.building.UrlModelSource;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * Builds the {@link ProjectDependencyGraph inter-dependencies graph} between projects in the reactor.
 */
@Component(role = GraphBuilder.class, hint = GraphBuilder.HINT)
public class DefaultGraphBuilder implements GraphBuilder {

    @Requirement
    private Logger logger;

    @Requirement
    protected ProjectBuilder projectBuilder;

    @Override
    public Result<ProjectDependencyGraph> build(MavenSession session) {
        try {
            Result<ProjectDependencyGraph> result = sessionDependencyGraph(session);

            if (result == null) {
                final List<MavenProject> projects = getProjectsForMavenReactor(session);
                validateProjects(projects);
                result = reactorDependencyGraph(session, projects);
            }

            return result;
        } catch (final ProjectBuildingException | DuplicateProjectException | MavenExecutionException e) {
            return Result.error(Collections.singletonList(new DefaultModelProblem(null, null, null, null, 0, 0, e)));
        } catch (final CycleDetectedException e) {
            String message = "The projects in the reactor contain a cyclic reference: " + e.getMessage();
            ProjectCycleException error = new ProjectCycleException(message, e);
            return Result.error(
                    Collections.singletonList(new DefaultModelProblem(null, null, null, null, 0, 0, error)));
        }
    }

    private Result<ProjectDependencyGraph> sessionDependencyGraph(final MavenSession session)
            throws CycleDetectedException, DuplicateProjectException {
        Result<ProjectDependencyGraph> result = null;

        if (session.getProjectDependencyGraph() != null || session.getProjects() != null) {
            final ProjectDependencyGraph graph =
                    new DefaultProjectDependencyGraph(session.getAllProjects(), session.getProjects());

            result = Result.success(graph);
        }

        return result;
    }

    private Result<ProjectDependencyGraph> reactorDependencyGraph(MavenSession session, List<MavenProject> projects)
            throws CycleDetectedException, DuplicateProjectException, MavenExecutionException {
        ProjectDependencyGraph projectDependencyGraph = new DefaultProjectDependencyGraph(projects);
        List<MavenProject> activeProjects = projectDependencyGraph.getSortedProjects();
        activeProjects = trimSelectedProjects(activeProjects, projectDependencyGraph, session.getRequest());
        activeProjects = trimExcludedProjects(activeProjects, session.getRequest());
        activeProjects = trimResumedProjects(activeProjects, session.getRequest());

        if (activeProjects.size() != projectDependencyGraph.getSortedProjects().size()) {
            projectDependencyGraph = new FilteredProjectDependencyGraph(projectDependencyGraph, activeProjects);
        }

        return Result.success(projectDependencyGraph);
    }

    private List<MavenProject> trimSelectedProjects(
            List<MavenProject> projects, ProjectDependencyGraph graph, MavenExecutionRequest request)
            throws MavenExecutionException {
        List<MavenProject> result = projects;

        if (!request.getSelectedProjects().isEmpty()) {
            File reactorDirectory = null;
            if (request.getBaseDirectory() != null) {
                reactorDirectory = new File(request.getBaseDirectory());
            }

            Collection<MavenProject> selectedProjects = new LinkedHashSet<>(projects.size());

            for (String selector : request.getSelectedProjects()) {
                MavenProject selectedProject = null;

                for (MavenProject project : projects) {
                    if (isMatchingProject(project, selector, reactorDirectory)) {
                        selectedProject = project;
                        break;
                    }
                }

                if (selectedProject != null) {
                    selectedProjects.add(selectedProject);
                } else {
                    throw new MavenExecutionException(
                            "Could not find the selected project in the reactor: " + selector, request.getPom());
                }
            }

            boolean makeUpstream = false;
            boolean makeDownstream = false;

            if (MavenExecutionRequest.REACTOR_MAKE_UPSTREAM.equals(request.getMakeBehavior())) {
                makeUpstream = true;
            } else if (MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM.equals(request.getMakeBehavior())) {
                makeDownstream = true;
            } else if (MavenExecutionRequest.REACTOR_MAKE_BOTH.equals(request.getMakeBehavior())) {
                makeUpstream = true;
                makeDownstream = true;
            } else if (StringUtils.isNotEmpty(request.getMakeBehavior())) {
                throw new MavenExecutionException(
                        "Invalid reactor make behavior: " + request.getMakeBehavior(), request.getPom());
            }

            if (makeUpstream || makeDownstream) {
                for (MavenProject selectedProject : new ArrayList<>(selectedProjects)) {
                    if (makeUpstream) {
                        selectedProjects.addAll(graph.getUpstreamProjects(selectedProject, true));
                    }
                    if (makeDownstream) {
                        selectedProjects.addAll(graph.getDownstreamProjects(selectedProject, true));
                    }
                }
            }

            result = new ArrayList<>(selectedProjects.size());

            for (MavenProject project : projects) {
                if (selectedProjects.contains(project)) {
                    result.add(project);
                }
            }
        }

        return result;
    }

    private List<MavenProject> trimExcludedProjects(List<MavenProject> projects, MavenExecutionRequest request)
            throws MavenExecutionException {
        List<MavenProject> result = projects;

        if (!request.getExcludedProjects().isEmpty()) {
            File reactorDirectory = null;

            if (request.getBaseDirectory() != null) {
                reactorDirectory = new File(request.getBaseDirectory());
            }

            Collection<MavenProject> excludedProjects = new LinkedHashSet<>(projects.size());

            for (String selector : request.getExcludedProjects()) {
                MavenProject excludedProject = null;

                for (MavenProject project : projects) {
                    if (isMatchingProject(project, selector, reactorDirectory)) {
                        excludedProject = project;
                        break;
                    }
                }

                if (excludedProject != null) {
                    excludedProjects.add(excludedProject);
                } else {
                    throw new MavenExecutionException(
                            "Could not find the selected project in the reactor: " + selector, request.getPom());
                }
            }

            result = new ArrayList<>(projects.size());
            for (MavenProject project : projects) {
                if (!excludedProjects.contains(project)) {
                    result.add(project);
                }
            }
        }

        return result;
    }

    private List<MavenProject> trimResumedProjects(List<MavenProject> projects, MavenExecutionRequest request)
            throws MavenExecutionException {
        List<MavenProject> result = projects;

        if (StringUtils.isNotEmpty(request.getResumeFrom())) {
            File reactorDirectory = null;
            if (request.getBaseDirectory() != null) {
                reactorDirectory = new File(request.getBaseDirectory());
            }

            String selector = request.getResumeFrom();

            result = new ArrayList<>(projects.size());

            boolean resumed = false;

            for (MavenProject project : projects) {
                if (!resumed && isMatchingProject(project, selector, reactorDirectory)) {
                    resumed = true;
                }

                if (resumed) {
                    result.add(project);
                }
            }

            if (!resumed) {
                throw new MavenExecutionException(
                        "Could not find project to resume reactor build from: " + selector + " vs "
                                + formatProjects(projects),
                        request.getPom());
            }
        }

        return result;
    }

    private String formatProjects(List<MavenProject> projects) {
        StringBuilder projectNames = new StringBuilder();
        Iterator<MavenProject> iterator = projects.iterator();
        while (iterator.hasNext()) {
            MavenProject project = iterator.next();
            projectNames.append(project.getGroupId()).append(":").append(project.getArtifactId());
            if (iterator.hasNext()) {
                projectNames.append(", ");
            }
        }
        return projectNames.toString();
    }

    private boolean isMatchingProject(MavenProject project, String selector, File reactorDirectory) {
        // [groupId]:artifactId
        if (selector.indexOf(':') >= 0) {
            String id = ':' + project.getArtifactId();

            if (id.equals(selector)) {
                return true;
            }

            id = project.getGroupId() + id;

            if (id.equals(selector)) {
                return true;
            }
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

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Project collection
    //
    // ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private List<MavenProject> getProjectsForMavenReactor(MavenSession session) throws ProjectBuildingException {
        MavenExecutionRequest request = session.getRequest();

        request.getProjectBuildingRequest().setRepositorySession(session.getRepositorySession());

        List<MavenProject> projects = new ArrayList<>();

        // We have no POM file.
        //
        if (request.getPom() == null) {
            ModelSource modelSource = new UrlModelSource(DefaultMaven.class.getResource("project/standalone.xml"));
            MavenProject project = projectBuilder
                    .build(modelSource, request.getProjectBuildingRequest())
                    .getProject();
            project.setExecutionRoot(true);
            projects.add(project);
            request.setProjectPresent(false);
            return projects;
        }

        List<File> files = Arrays.asList(request.getPom().getAbsoluteFile());
        collectProjects(projects, files, request);
        return projects;
    }

    private void collectProjects(List<MavenProject> projects, List<File> files, MavenExecutionRequest request)
            throws ProjectBuildingException {
        ProjectBuildingRequest projectBuildingRequest = request.getProjectBuildingRequest();

        List<ProjectBuildingResult> results =
                projectBuilder.build(files, request.isRecursive(), projectBuildingRequest);

        boolean problems = false;

        for (ProjectBuildingResult result : results) {
            projects.add(result.getProject());

            if (!result.getProblems().isEmpty() && logger.isWarnEnabled()) {
                logger.warn("");
                logger.warn("Some problems were encountered while building the effective model for "
                        + result.getProject().getId());

                for (ModelProblem problem : result.getProblems()) {
                    String loc = ModelProblemUtils.formatLocation(problem, result.getProjectId());
                    logger.warn(problem.getMessage() + (StringUtils.isNotEmpty(loc) ? " @ " + loc : ""));
                }

                problems = true;
            }
        }

        if (problems) {
            logger.warn("");
            logger.warn("It is highly recommended to fix these problems"
                    + " because they threaten the stability of your build.");
            logger.warn("");
            logger.warn("For this reason, future Maven versions might no"
                    + " longer support building such malformed projects.");
            logger.warn("");
        }
    }

    private void validateProjects(List<MavenProject> projects) {
        Map<String, MavenProject> projectsMap = new HashMap<>();

        for (MavenProject p : projects) {
            String projectKey = ArtifactUtils.key(p.getGroupId(), p.getArtifactId(), p.getVersion());

            projectsMap.put(projectKey, p);
        }

        for (MavenProject project : projects) {
            // MNG-1911 / MNG-5572: Building plugins with extensions cannot be part of reactor
            for (Plugin plugin : project.getBuildPlugins()) {
                if (plugin.isExtensions()) {
                    String pluginKey =
                            ArtifactUtils.key(plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion());

                    if (projectsMap.containsKey(pluginKey)) {
                        logger.warn(project.getName() + " uses " + plugin.getKey()
                                + " as extensions, which is not possible within the same reactor build. "
                                + "This plugin was pulled from the local repository!");
                    }
                }
            }
        }
    }
}
