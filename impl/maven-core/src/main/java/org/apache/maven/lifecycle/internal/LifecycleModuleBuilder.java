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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.HashSet;
import java.util.List;

import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.internal.transformation.ConsumerPomArtifactTransformer;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.builder.BuilderCommon;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

/**
 * <p>
 * Builds one or more lifecycles for a full module
 * </p>
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 */
@Named
@Singleton
public class LifecycleModuleBuilder {

    private final MojoExecutor mojoExecutor;
    private final BuilderCommon builderCommon;
    private final ExecutionEventCatapult eventCatapult;
    private final ProjectExecutionListener projectExecutionListener;
    private final ConsumerPomArtifactTransformer consumerPomArtifactTransformer;

    @Inject
    public LifecycleModuleBuilder(
            MojoExecutor mojoExecutor,
            BuilderCommon builderCommon,
            ExecutionEventCatapult eventCatapult,
            List<ProjectExecutionListener> listeners,
            ConsumerPomArtifactTransformer consumerPomArtifactTransformer) {
        this.mojoExecutor = mojoExecutor;
        this.builderCommon = builderCommon;
        this.eventCatapult = eventCatapult;
        this.projectExecutionListener = new CompoundProjectExecutionListener(listeners);
        this.consumerPomArtifactTransformer = consumerPomArtifactTransformer;
    }

    public void buildProject(
            MavenSession session, ReactorContext reactorContext, MavenProject currentProject, TaskSegment taskSegment) {
        buildProject(session, session, reactorContext, currentProject, taskSegment);
    }

    public void buildProject(
            MavenSession session,
            MavenSession rootSession,
            ReactorContext reactorContext,
            MavenProject currentProject,
            TaskSegment taskSegment) {
        session.setCurrentProject(currentProject);

        long buildStartTime = System.currentTimeMillis();

        try {

            if (reactorContext.getReactorBuildStatus().isHaltedOrBlacklisted(currentProject)) {
                eventCatapult.fire(ExecutionEvent.Type.ProjectSkipped, session, null);
                return;
            }

            consumerPomArtifactTransformer.injectTransformedArtifacts(session.getRepositorySession(), currentProject);

            BuilderCommon.attachToThread(currentProject);

            projectExecutionListener.beforeProjectExecution(new ProjectExecutionEvent(session, currentProject));

            eventCatapult.fire(ExecutionEvent.Type.ProjectStarted, session, null);

            MavenExecutionPlan executionPlan =
                    builderCommon.resolveBuildPlan(session, currentProject, taskSegment, new HashSet<>());
            List<MojoExecution> mojoExecutions = executionPlan.getMojoExecutions();

            projectExecutionListener.beforeProjectLifecycleExecution(
                    new ProjectExecutionEvent(session, currentProject, mojoExecutions));
            mojoExecutor.execute(session, mojoExecutions);

            long buildEndTime = System.currentTimeMillis();

            projectExecutionListener.afterProjectExecutionSuccess(
                    new ProjectExecutionEvent(session, currentProject, mojoExecutions));

            reactorContext.getResult().addBuildSummary(new BuildSuccess(currentProject, buildEndTime - buildStartTime));

            eventCatapult.fire(ExecutionEvent.Type.ProjectSucceeded, session, null);
        } catch (Throwable t) {
            builderCommon.handleBuildError(reactorContext, rootSession, session, currentProject, t, buildStartTime);

            projectExecutionListener.afterProjectExecutionFailure(
                    new ProjectExecutionEvent(session, currentProject, t));

            // rethrow original errors and runtime exceptions
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
        } finally {
            session.setCurrentProject(null);

            Thread.currentThread().setContextClassLoader(reactorContext.getOriginalContextClassLoader());
        }
    }
}
