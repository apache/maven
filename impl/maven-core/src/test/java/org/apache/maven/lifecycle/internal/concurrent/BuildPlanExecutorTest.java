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
package org.apache.maven.lifecycle.internal.concurrent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.internal.impl.DefaultLifecycleRegistry;
import org.apache.maven.internal.transformation.TransformerManager;
import org.apache.maven.lifecycle.internal.LifecycleTask;
import org.apache.maven.lifecycle.internal.ReactorBuildStatus;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.lifecycle.internal.stub.ExecutionEventCatapultStub;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.installation.InstallRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildPlanExecutorTest {

    /**
     * A build step that throws an {@link Error} must be reported as a build failure, the same way the
     * single threaded builder reports it. Otherwise the build ends with no exception at all and Maven
     * prints BUILD SUCCESS while nothing was built.
     */
    @Test
    void errorThrownByBuildStepIsRecordedAsBuildFailure() throws Exception {
        Error thrown = new NoClassDefFoundError("some/Class");
        MavenProject project = newProject();
        MavenSession session = newSession(project);

        execute(session, project, event -> {
            throw thrown;
        });

        List<Throwable> exceptions = session.getResult().getExceptions();
        assertEquals(1, exceptions.size(), "expected the error to be recorded, but got: " + exceptions);
        assertSame(thrown, exceptions.get(0));
        assertTrue(session.getResult().getBuildSummary(project) instanceof org.apache.maven.execution.BuildFailure);
    }

    /**
     * The same for an exception, which already worked. This pins the existing behaviour so the widened
     * catch does not change it.
     */
    @Test
    void exceptionThrownByBuildStepIsRecordedAsBuildFailure() throws Exception {
        RuntimeException thrown = new IllegalStateException("nope");
        MavenProject project = newProject();
        MavenSession session = newSession(project);

        execute(session, project, event -> {
            throw thrown;
        });

        List<Throwable> exceptions = session.getResult().getExceptions();
        assertEquals(1, exceptions.size(), "expected the exception to be recorded, but got: " + exceptions);
        assertSame(thrown, exceptions.get(0));
        assertTrue(session.getResult().getBuildSummary(project) instanceof org.apache.maven.execution.BuildFailure);
    }

    private void execute(MavenSession session, MavenProject project, BeforeProjectExecution listener) throws Exception {
        ReactorContext reactorContext = new ReactorContext(
                session.getResult(),
                Thread.currentThread().getContextClassLoader(),
                new ReactorBuildStatus(session.getProjectDependencyGraph()));
        TaskSegment taskSegment = new TaskSegment(false);
        taskSegment.getTasks().add(new LifecycleTask("validate"));
        newExecutor(listener).execute(session, reactorContext, List.of(taskSegment));
    }

    private BuildPlanExecutor newExecutor(ProjectExecutionListener listener) {
        return new BuildPlanExecutor(
                null,
                new ExecutionEventCatapultStub(),
                List.of(listener),
                new NoopTransformerManager(),
                new BuildPlanLogger(),
                Map.of(),
                null,
                null,
                new DefaultLifecycleRegistry(Collections.emptyList()));
    }

    private MavenProject newProject() {
        MavenProject result = new MavenProject();
        result.setArtifactId("a");
        result.setCollectedProjects(List.of());
        return result;
    }

    private MavenSession newSession(MavenProject project) {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setGoals(List.of("validate"));
        MavenSession result = new MavenSession(
                null, new DefaultRepositorySystemSession(h -> false), request, new DefaultMavenExecutionResult());
        result.setProjectDependencyGraph(new SingleProjectDependencyGraph(project));
        result.setProjects(List.of(project));
        return result;
    }

    private static final class SingleProjectDependencyGraph implements ProjectDependencyGraph {

        private final List<MavenProject> projects;

        private SingleProjectDependencyGraph(MavenProject project) {
            this.projects = List.of(project);
        }

        @Override
        public List<MavenProject> getAllProjects() {
            return projects;
        }

        @Override
        public List<MavenProject> getSortedProjects() {
            return projects;
        }

        @Override
        public List<MavenProject> getDownstreamProjects(MavenProject project, boolean transitive) {
            return List.of();
        }

        @Override
        public List<MavenProject> getUpstreamProjects(MavenProject project, boolean transitive) {
            return List.of();
        }
    }

    private static final class NoopTransformerManager implements TransformerManager {

        @Override
        public InstallRequest remapInstallArtifacts(RepositorySystemSession session, InstallRequest request) {
            return request;
        }

        @Override
        public DeployRequest remapDeployArtifacts(RepositorySystemSession session, DeployRequest request) {
            return request;
        }

        @Override
        public void injectTransformedArtifacts(RepositorySystemSession repositorySession, MavenProject project) {}
    }

    @FunctionalInterface
    private interface BeforeProjectExecution extends ProjectExecutionListener {

        @Override
        void beforeProjectExecution(ProjectExecutionEvent event);

        @Override
        default void beforeProjectLifecycleExecution(ProjectExecutionEvent event) {}

        @Override
        default void afterProjectExecutionSuccess(ProjectExecutionEvent event) {}

        @Override
        default void afterProjectExecutionFailure(ProjectExecutionEvent event) {}
    }
}
