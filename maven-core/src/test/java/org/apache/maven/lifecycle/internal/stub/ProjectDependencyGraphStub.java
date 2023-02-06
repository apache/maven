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
package org.apache.maven.lifecycle.internal.stub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.lifecycle.LifecycleNotFoundException;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.internal.GoalTask;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ProjectSegment;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;

/**
 * A stub dependency graph that is custom made for testing concurrent build graph evaluations.
 * <p>
 * Implements a graph as follows:
 * A has no dependencies
 * B depends on A
 * C depends on A
 * X depends on B &amp; C
 * Y depends on B
 * Z depends on C
 * </p>
 *
 * @author Kristian Rosenvold
 */
public class ProjectDependencyGraphStub implements ProjectDependencyGraph {
    public static final MavenProject A = new MavenProject();

    public static final MavenProject B = new MavenProject();

    public static final MavenProject C = new MavenProject();

    public static final MavenProject X = new MavenProject();

    public static final MavenProject Y = new MavenProject();

    public static final MavenProject Z = new MavenProject();

    public static final MavenProject UNKNOWN = new MavenProject();

    static {
        A.setArtifactId("A");
        B.setArtifactId("B");
        C.setArtifactId("C");
        X.setArtifactId("X");
        Y.setArtifactId("Y");
        Z.setArtifactId("Z");
    }

    // This should probably be moved to a separate stub

    public static ProjectBuildList getProjectBuildList(MavenSession session)
            throws InvalidPluginDescriptorException, PluginVersionResolutionException, PluginDescriptorParsingException,
                    NoPluginFoundForPrefixException, MojoNotFoundException, PluginNotFoundException,
                    PluginResolutionException, LifecyclePhaseNotFoundException, LifecycleNotFoundException {
        final List<ProjectSegment> list = getProjectBuilds(session);
        return new ProjectBuildList(list);
    }

    public static List<ProjectSegment> getProjectBuilds(MavenSession session)
            throws InvalidPluginDescriptorException, PluginVersionResolutionException, PluginDescriptorParsingException,
                    NoPluginFoundForPrefixException, PluginNotFoundException, MojoNotFoundException,
                    PluginResolutionException, LifecyclePhaseNotFoundException, LifecycleNotFoundException {
        List<ProjectSegment> projectBuilds = new ArrayList<>();

        TaskSegment segment = createTaskSegment();
        projectBuilds.add(createProjectBuild(A, session, segment));
        projectBuilds.add(createProjectBuild(B, session, segment));
        projectBuilds.add(createProjectBuild(C, session, segment));
        projectBuilds.add(createProjectBuild(X, session, segment));
        projectBuilds.add(createProjectBuild(Y, session, segment));
        projectBuilds.add(createProjectBuild(Z, session, segment));
        return projectBuilds;
    }

    private static ProjectSegment createProjectBuild(
            MavenProject project, MavenSession session, TaskSegment taskSegment)
            throws InvalidPluginDescriptorException, PluginVersionResolutionException, PluginDescriptorParsingException,
                    NoPluginFoundForPrefixException, MojoNotFoundException, PluginNotFoundException,
                    PluginResolutionException, LifecyclePhaseNotFoundException, LifecycleNotFoundException {
        final MavenSession session1 = session.clone();
        return new ProjectSegment(project, taskSegment, session1);
    }

    private static TaskSegment createTaskSegment() {
        TaskSegment result = new TaskSegment(false);
        result.getTasks().add(new GoalTask("t1"));
        result.getTasks().add(new GoalTask("t2"));
        return result;
    }

    class Dependency {
        MavenProject dependant;

        MavenProject dependency;

        Dependency(MavenProject dependant, MavenProject dependency) {
            this.dependant = dependant;
            this.dependency = dependency;
        }

        void addIfDownstream(MavenProject mavenProject, List<MavenProject> result) {
            if (dependency == mavenProject) {
                result.add(dependant);
            }
        }

        void addIfUpstreamOf(MavenProject mavenProject, List<MavenProject> result) {
            if (dependant == mavenProject) {
                result.add(dependency); // All projects are the statics from this class
            }
        }
    }

    private List<Dependency> getDependencies() {
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(new Dependency(B, A));
        dependencies.add(new Dependency(C, A));
        dependencies.add(new Dependency(X, B));
        dependencies.add(new Dependency(X, C));
        dependencies.add(new Dependency(Y, B));
        dependencies.add(new Dependency(Z, C));
        return dependencies;
    }

    public List<MavenProject> getAllProjects() {
        return Arrays.asList(A, B, C, X, Y, Z, UNKNOWN);
    }

    public List<MavenProject> getSortedProjects() {
        return Arrays.asList(A, B, C, X, Y, Z); // I'm not entirely sure about the order but this should do...
    }

    public List<MavenProject> getDownstreamProjects(MavenProject project, boolean transitive) {
        if (transitive) {
            throw new RuntimeException("Not implemented yet");
        }
        List<MavenProject> result = new ArrayList<>();
        for (Dependency dependency : getDependencies()) {
            dependency.addIfDownstream(project, result);
        }
        return result;
    }

    public List<MavenProject> getUpstreamProjects(MavenProject project, boolean transitive) {
        /*  if ( transitive )
        {
            throw new RuntimeException( "Not implemented yet" );
        }*/
        List<MavenProject> result = new ArrayList<>();
        final List<Dependency> dependencies = getDependencies();
        for (Dependency dependency : dependencies) {
            dependency.addIfUpstreamOf(project, result);
        }
        return result;
    }

    public static MavenSession getMavenSession(MavenProject mavenProject) {
        final MavenSession session = getMavenSession();
        session.setCurrentProject(mavenProject);
        return session;
    }

    public static MavenSession getMavenSession() {
        final DefaultMavenExecutionResult defaultMavenExecutionResult = new DefaultMavenExecutionResult();
        MavenExecutionRequest mavenExecutionRequest = new DefaultMavenExecutionRequest();
        mavenExecutionRequest.setExecutionListener(new AbstractExecutionListener());
        mavenExecutionRequest.setGoals(Arrays.asList("clean", "aggr", "install"));
        mavenExecutionRequest.setDegreeOfConcurrency(1);
        final MavenSession session = new MavenSession(null, null, mavenExecutionRequest, defaultMavenExecutionResult);
        final ProjectDependencyGraphStub dependencyGraphStub = new ProjectDependencyGraphStub();
        session.setProjectDependencyGraph(dependencyGraphStub);
        session.setProjects(dependencyGraphStub.getSortedProjects());
        return session;
    }
}
