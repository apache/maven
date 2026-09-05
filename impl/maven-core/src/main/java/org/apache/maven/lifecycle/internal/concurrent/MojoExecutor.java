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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.List;

import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.DependencyContext;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojosExecutionStrategy;
import org.apache.maven.project.MavenProject;

/**
 * Concurrent-builder variant of the mojo executor.
 * <p>
 * In the concurrent builder, multiple build steps for different projects
 * execute in parallel.  The shared {@code MavenSession.currentProject} field
 * is therefore unreliable — another thread can overwrite it between the
 * moment a step attaches to its thread and the moment the mojo executor
 * reads it.  This subclass uses a {@link ThreadLocal} to provide each
 * executor thread with its own, stable reference to the project being built.
 */
@Named("concurrent")
@Singleton
public class MojoExecutor extends org.apache.maven.lifecycle.internal.MojoExecutor {

    private static final ThreadLocal<MavenProject> THREAD_PROJECT = new ThreadLocal<>();

    @Inject
    public MojoExecutor(
            BuildPluginManager pluginManager,
            MavenPluginManager mavenPluginManager,
            LifecycleDependencyResolver lifeCycleDependencyResolver,
            ExecutionEventCatapult eventCatapult,
            Provider<MojosExecutionStrategy> mojosExecutionStrategy,
            MessageBuilderFactory messageBuilderFactory) {
        super(
                pluginManager,
                mavenPluginManager,
                lifeCycleDependencyResolver,
                eventCatapult,
                mojosExecutionStrategy,
                messageBuilderFactory);
    }

    @Override
    protected boolean useProjectLock(MavenSession session) {
        return false;
    }

    /**
     * Set the project for the current executor thread.  Called by
     * {@link BuildPlanExecutor} before invoking
     * {@link #execute(MavenSession, List)} so that
     * {@link #newDependencyContext} picks up the correct project
     * regardless of what {@code session.getCurrentProject()} returns.
     */
    static void setThreadProject(MavenProject project) {
        THREAD_PROJECT.set(project);
    }

    /** Clear the thread-local project reference after execution. */
    static void clearThreadProject() {
        THREAD_PROJECT.remove();
    }

    /**
     * Override to use the thread-local project instead of
     * {@code session.getCurrentProject()}, which is racy in the
     * concurrent builder.
     */
    @Override
    public DependencyContext newDependencyContext(MavenSession session, List<MojoExecution> mojoExecutions) {
        DependencyContext ctx = super.newDependencyContext(session, mojoExecutions);
        MavenProject threadProject = THREAD_PROJECT.get();
        if (threadProject != null && threadProject != ctx.getProject()) {
            // The super method captured the wrong project from the racy
            // session.currentProject — rebuild with the correct one.
            return new DependencyContext(
                    threadProject,
                    ctx.getScopesToCollectForCurrentProject(),
                    ctx.getScopesToResolveForCurrentProject());
        }
        return ctx;
    }
}
