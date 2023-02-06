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
package org.apache.maven.lifecycle.internal.builder;

import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.MultilineMessageHelper;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleNotFoundException;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.LifecycleDebugLogger;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Common code that is shared by the LifecycleModuleBuilder and the LifeCycleWeaveBuilder
 *
 * @since 3.0
 * @author Kristian Rosenvold
 *         Builds one or more lifecycles for a full module
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
@Component(role = BuilderCommon.class)
public class BuilderCommon {
    @Requirement
    private LifecycleDebugLogger lifecycleDebugLogger;

    @Requirement
    private LifecycleExecutionPlanCalculator lifeCycleExecutionPlanCalculator;

    @Requirement
    private ExecutionEventCatapult eventCatapult;

    @Requirement
    private Logger logger;

    public BuilderCommon() {}

    public BuilderCommon(
            LifecycleDebugLogger lifecycleDebugLogger,
            LifecycleExecutionPlanCalculator lifeCycleExecutionPlanCalculator,
            Logger logger) {
        this.lifecycleDebugLogger = lifecycleDebugLogger;
        this.lifeCycleExecutionPlanCalculator = lifeCycleExecutionPlanCalculator;
        this.logger = logger;
    }

    public MavenExecutionPlan resolveBuildPlan(
            MavenSession session, MavenProject project, TaskSegment taskSegment, Set<Artifact> projectArtifacts)
            throws PluginNotFoundException, PluginResolutionException, LifecyclePhaseNotFoundException,
                    PluginDescriptorParsingException, MojoNotFoundException, InvalidPluginDescriptorException,
                    NoPluginFoundForPrefixException, LifecycleNotFoundException, PluginVersionResolutionException,
                    LifecycleExecutionException {
        MavenExecutionPlan executionPlan =
                lifeCycleExecutionPlanCalculator.calculateExecutionPlan(session, project, taskSegment.getTasks());

        lifecycleDebugLogger.debugProjectPlan(project, executionPlan);

        if (session.getRequest().getDegreeOfConcurrency() > 1
                && session.getProjects().size() > 1) {
            final Set<Plugin> unsafePlugins = executionPlan.getNonThreadSafePlugins();
            if (!unsafePlugins.isEmpty()) {
                for (String s : MultilineMessageHelper.format(
                        "Your build is requesting parallel execution, but this project contains the following "
                                + "plugin(s) that have goals not marked as thread-safe to support parallel execution.",
                        "While this /may/ work fine, please look for plugin updates and/or "
                                + "request plugins be made thread-safe.",
                        "If reporting an issue, report it against the plugin in question, not against Apache Maven.")) {
                    logger.warn(s);
                }
                if (logger.isDebugEnabled()) {
                    final Set<MojoDescriptor> unsafeGoals = executionPlan.getNonThreadSafeMojos();
                    logger.warn("The following goals are not marked as thread-safe in " + project.getName() + ":");
                    for (MojoDescriptor unsafeGoal : unsafeGoals) {
                        logger.warn("  " + unsafeGoal.getId());
                    }
                } else {
                    logger.warn("The following plugins are not marked as thread-safe in " + project.getName() + ":");
                    for (Plugin unsafePlugin : unsafePlugins) {
                        logger.warn("  " + unsafePlugin.getId());
                    }
                    logger.warn("");
                    logger.warn("Enable debug to see precisely which goals are not marked as thread-safe.");
                }
                logger.warn(MultilineMessageHelper.separatorLine());
            }
        }

        return executionPlan;
    }

    public void handleBuildError(
            final ReactorContext buildContext,
            final MavenSession rootSession,
            final MavenSession currentSession,
            final MavenProject mavenProject,
            Throwable t,
            final long buildStartTime) {
        // record the error and mark the project as failed
        long buildEndTime = System.currentTimeMillis();
        buildContext.getResult().addException(t);
        buildContext.getResult().addBuildSummary(new BuildFailure(mavenProject, buildEndTime - buildStartTime, t));

        // notify listeners about "soft" project build failures only
        if (t instanceof Exception && !(t instanceof RuntimeException)) {
            eventCatapult.fire(ExecutionEvent.Type.ProjectFailed, currentSession, null, (Exception) t);
        }

        // reactor failure modes
        if (t instanceof RuntimeException || !(t instanceof Exception)) {
            // fail fast on RuntimeExceptions, Errors and "other" Throwables
            // assume these are system errors and further build is meaningless
            buildContext.getReactorBuildStatus().halt();
        } else if (MavenExecutionRequest.REACTOR_FAIL_NEVER.equals(rootSession.getReactorFailureBehavior())) {
            // continue the build
        } else if (MavenExecutionRequest.REACTOR_FAIL_AT_END.equals(rootSession.getReactorFailureBehavior())) {
            // continue the build but ban all projects that depend on the failed one
            buildContext.getReactorBuildStatus().blackList(mavenProject);
        } else if (MavenExecutionRequest.REACTOR_FAIL_FAST.equals(rootSession.getReactorFailureBehavior())) {
            buildContext.getReactorBuildStatus().halt();
        } else {
            logger.error("invalid reactor failure behavior " + rootSession.getReactorFailureBehavior());
            buildContext.getReactorBuildStatus().halt();
        }
    }

    public static void attachToThread(MavenProject currentProject) {
        ClassRealm projectRealm = currentProject.getClassRealm();
        if (projectRealm != null) {
            Thread.currentThread().setContextClassLoader(projectRealm);
        }
    }

    // TODO I'm really wondering where this method belongs; smells like it should be on MavenProject, but for some
    // reason it isn't ? This localization is kind-of a code smell.

    public static String getKey(MavenProject project) {
        return project.getGroupId() + ':' + project.getArtifactId() + ':' + project.getVersion();
    }
}
