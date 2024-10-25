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
package org.apache.maven.execution;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;

public class DefaultMavenExecutionResult implements MavenExecutionResult {
    private MavenProject project;

    private List<MavenProject> topologicallySortedProjects = Collections.emptyList();

    private DependencyResolutionResult dependencyResolutionResult;

    private final List<Throwable> exceptions = new CopyOnWriteArrayList<>();

    private final Map<MavenProject, BuildSummary> buildSummaries = Collections.synchronizedMap(new IdentityHashMap<>());

    private boolean canResume = false;

    public MavenExecutionResult setProject(MavenProject project) {
        this.project = project;

        return this;
    }

    public MavenProject getProject() {
        return project;
    }

    public MavenExecutionResult setTopologicallySortedProjects(List<MavenProject> topologicallySortedProjects) {
        this.topologicallySortedProjects = topologicallySortedProjects;

        return this;
    }

    public List<MavenProject> getTopologicallySortedProjects() {
        return null == topologicallySortedProjects
                ? Collections.emptyList()
                : Collections.unmodifiableList(topologicallySortedProjects);
    }

    public DependencyResolutionResult getDependencyResolutionResult() {
        return dependencyResolutionResult;
    }

    public MavenExecutionResult setDependencyResolutionResult(DependencyResolutionResult dependencyResolutionResult) {
        this.dependencyResolutionResult = dependencyResolutionResult;

        return this;
    }

    public List<Throwable> getExceptions() {
        return exceptions;
    }

    public MavenExecutionResult addException(Throwable t) {
        exceptions.add(t);

        return this;
    }

    public boolean hasExceptions() {
        return !getExceptions().isEmpty();
    }

    public BuildSummary getBuildSummary(MavenProject project) {
        return buildSummaries.get(project);
    }

    public void addBuildSummary(BuildSummary summary) {
        buildSummaries.put(summary.getProject(), summary);
    }

    @Override
    public boolean canResume() {
        return canResume;
    }

    @Override
    public void setCanResume(boolean canResume) {
        this.canResume = canResume;
    }
}
