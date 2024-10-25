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

import java.util.List;
import java.util.Map;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.MissingProjectException;
import org.apache.maven.lifecycle.NoGoalSpecifiedException;
import org.apache.maven.lifecycle.internal.builder.Builder;
import org.apache.maven.lifecycle.internal.builder.BuilderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts the build life cycle
 *
 */
@Named
@Singleton
public class DefaultLifecycleStarter implements LifecycleStarter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ExecutionEventCatapult eventCatapult;

    private final DefaultLifecycles defaultLifeCycles;

    private final BuildListCalculator buildListCalculator;

    private final LifecycleDebugLogger lifecycleDebugLogger;

    private final LifecycleTaskSegmentCalculator lifecycleTaskSegmentCalculator;

    private final Map<String, Builder> builders;

    @Inject
    public DefaultLifecycleStarter(
            ExecutionEventCatapult eventCatapult,
            DefaultLifecycles defaultLifeCycles,
            BuildListCalculator buildListCalculator,
            LifecycleDebugLogger lifecycleDebugLogger,
            LifecycleTaskSegmentCalculator lifecycleTaskSegmentCalculator,
            Map<String, Builder> builders) {
        this.eventCatapult = eventCatapult;
        this.defaultLifeCycles = defaultLifeCycles;
        this.buildListCalculator = buildListCalculator;
        this.lifecycleDebugLogger = lifecycleDebugLogger;
        this.lifecycleTaskSegmentCalculator = lifecycleTaskSegmentCalculator;
        this.builders = builders;
    }

    @Override
    public void execute(MavenSession session) {
        eventCatapult.fire(ExecutionEvent.Type.SessionStarted, session, null);

        ReactorContext reactorContext = null;
        ProjectBuildList projectBuilds = null;
        MavenExecutionResult result = session.getResult();

        try {
            if (buildExecutionRequiresProject(session) && projectIsNotPresent(session)) {
                throw new MissingProjectException("The goal you specified requires a project to execute"
                        + " but there is no POM in this directory (" + session.getExecutionRootDirectory() + ")."
                        + " Please verify you invoked Maven from the correct directory.");
            }

            List<TaskSegment> taskSegments = lifecycleTaskSegmentCalculator.calculateTaskSegments(session);
            projectBuilds = buildListCalculator.calculateProjectBuilds(session, taskSegments);

            if (projectBuilds.isEmpty()) {
                throw new NoGoalSpecifiedException("No goals have been specified for this build."
                        + " You must specify a valid lifecycle phase or a goal in the format <plugin-prefix>:<goal> or"
                        + " <plugin-group-id>:<plugin-artifact-id>[:<plugin-version>]:<goal>."
                        + " Available lifecycle phases are: " + defaultLifeCycles.getLifecyclePhaseList() + ".");
            }

            if (logger.isDebugEnabled()) {
                lifecycleDebugLogger.debugReactorPlan(projectBuilds);
            }

            ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
            ReactorBuildStatus reactorBuildStatus = new ReactorBuildStatus(session.getProjectDependencyGraph());
            reactorContext = new ReactorContext(result, oldContextClassLoader, reactorBuildStatus);

            String builderId = session.getRequest().getBuilderId();
            Builder builder = builders.get(builderId);
            if (builder == null) {
                throw new BuilderNotFoundException(
                        String.format("The builder requested using id = %s cannot be" + " found", builderId));
            }

            int degreeOfConcurrency = session.getRequest().getDegreeOfConcurrency();
            if (degreeOfConcurrency > 1) {
                logger.info("");
                logger.info(String.format(
                        "Using the %s implementation with a thread count of %d",
                        builder.getClass().getSimpleName(), degreeOfConcurrency));
            }
            builder.build(session, reactorContext, projectBuilds, taskSegments, reactorBuildStatus);

        } catch (Exception e) {
            result.addException(e);
        } finally {
            eventCatapult.fire(ExecutionEvent.Type.SessionEnded, session, null);
        }
    }

    private boolean buildExecutionRequiresProject(MavenSession session) {
        return lifecycleTaskSegmentCalculator.requiresProject(session);
    }

    private boolean projectIsNotPresent(MavenSession session) {
        return !session.getRequest().isProjectPresent();
    }
}
