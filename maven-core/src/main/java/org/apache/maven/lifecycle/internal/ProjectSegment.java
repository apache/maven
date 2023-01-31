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

import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.project.MavenProject;

/**
 * A build context that matches a mavenproject to a given tasksegment, and the session to be used.
 * <p>
 * A note to the reader;
 * </p>
 * <p>
 * There are several issues/discussions regarding how "aggregator" plugins should be handled.
 * Read for instance http://docs.codehaus.org/display/MAVEN/Deterministic+Lifecycle+Planning
 * </p>
 * <p>
 * In their current implementation they are "bolted" onto the lifecycle by separating them
 * into TaskSegments. This class represents the execution context of one such task segment.
 * </p>
 * <p>
 * Wise voices have suggested that maybe aggregators shouldn't be bound to the ordinary
 * lifecycle at all, in which case we wouldn't be needing this class at all ( and
 * ProjectBuildList.getByTaskSegments). Or maybe they should be introduced in the calculation
 * of the execution plan instead, which seems much nicer.
 * </p>
 * <p>
 * Additionally this class contains a clone of the MavenSession, which is *only* needed
 * because it has as notion of a "current" project.
 * </p>
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 * @author Jason van Zyl
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold
 */
public final class ProjectSegment {
    private final MavenProject project;

    private final TaskSegment taskSegment;

    private final MavenSession session;

    private final List<MavenProject> nonTransitiveUpstreamProjects;

    private final List<MavenProject> transitiveUpstreamProjects;

    public ProjectSegment(MavenProject project, TaskSegment taskSegment, MavenSession copiedSession) {
        this.project = project;
        this.taskSegment = taskSegment;
        this.session = copiedSession;
        final ProjectDependencyGraph dependencyGraph = getSession().getProjectDependencyGraph();
        nonTransitiveUpstreamProjects = dependencyGraph.getUpstreamProjects(getProject(), false);
        transitiveUpstreamProjects = dependencyGraph.getUpstreamProjects(getProject(), true);
    }

    public MavenSession getSession() {
        return session;
    }

    public MavenProject getProject() {
        return project;
    }

    public TaskSegment getTaskSegment() {
        return taskSegment;
    }

    public List<MavenProject> getImmediateUpstreamProjects() {
        return nonTransitiveUpstreamProjects;
    }

    public List<MavenProject> getTransitiveUpstreamProjects() {
        return transitiveUpstreamProjects;
    }

    @Override
    public String toString() {
        return getProject().getId() + " -> " + getTaskSegment();
    }
}
