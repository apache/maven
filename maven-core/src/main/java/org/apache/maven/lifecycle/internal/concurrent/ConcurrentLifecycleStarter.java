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
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.MissingProjectException;
import org.apache.maven.lifecycle.NoGoalSpecifiedException;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.GoalTask;
import org.apache.maven.lifecycle.internal.LifecyclePluginResolver;
import org.apache.maven.lifecycle.internal.LifecycleStarter;
import org.apache.maven.lifecycle.internal.LifecycleTask;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.lifecycle.internal.ReactorBuildStatus;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * Starts the build life cycle
 */
@Named("concurrent")
@Singleton
public class ConcurrentLifecycleStarter implements LifecycleStarter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ExecutionEventCatapult eventCatapult;
    private final DefaultLifecycles defaultLifeCycles;
    private final BuildPlanExecutor executor;
    private final LifecyclePluginResolver lifecyclePluginResolver;
    private final MojoDescriptorCreator mojoDescriptorCreator;

    @Inject
    public ConcurrentLifecycleStarter(
            ExecutionEventCatapult eventCatapult,
            DefaultLifecycles defaultLifeCycles,
            BuildPlanExecutor executor,
            LifecyclePluginResolver lifecyclePluginResolver,
            MojoDescriptorCreator mojoDescriptorCreator) {
        this.eventCatapult = eventCatapult;
        this.defaultLifeCycles = defaultLifeCycles;
        this.executor = executor;
        this.lifecyclePluginResolver = lifecyclePluginResolver;
        this.mojoDescriptorCreator = mojoDescriptorCreator;
    }

    public void execute(MavenSession session) {
        eventCatapult.fire(ExecutionEvent.Type.SessionStarted, session, null);

        try {
            if (requiresProject(session) && projectIsNotPresent(session)) {
                throw new MissingProjectException("The goal you specified requires a project to execute"
                        + " but there is no POM in this directory (" + session.getTopDirectory() + ")."
                        + " Please verify you invoked Maven from the correct directory.");
            }

            List<TaskSegment> taskSegments = calculateTaskSegments(session);
            if (taskSegments.isEmpty()) {
                throw new NoGoalSpecifiedException("No goals have been specified for this build."
                        + " You must specify a valid lifecycle phase or a goal in the format <plugin-prefix>:<goal> or"
                        + " <plugin-group-id>:<plugin-artifact-id>[:<plugin-version>]:<goal>."
                        + " Available lifecycle phases are: " + defaultLifeCycles.getLifecyclePhaseList() + ".");
            }

            int degreeOfConcurrency = session.getRequest().getDegreeOfConcurrency();
            if (degreeOfConcurrency > 1) {
                logger.info("");
                logger.info(String.format(
                        "Using the %s implementation with a thread count of %d",
                        executor.getClass().getSimpleName(), degreeOfConcurrency));
            }

            ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
            ReactorBuildStatus reactorBuildStatus = new ReactorBuildStatus(session.getProjectDependencyGraph());
            ReactorContext reactorContext =
                    new ReactorContext(session.getResult(), oldContextClassLoader, reactorBuildStatus);
            executor.execute(session, reactorContext, taskSegments);

        } catch (Exception e) {
            session.getResult().addException(e);
        } finally {
            eventCatapult.fire(ExecutionEvent.Type.SessionEnded, session, null);
        }
    }

    public List<TaskSegment> calculateTaskSegments(MavenSession session) throws Exception {

        MavenProject rootProject = session.getTopLevelProject();

        List<String> tasks = requireNonNull(session.getGoals()); // session never returns null, but empty list

        if (tasks.isEmpty()
                && (rootProject.getDefaultGoal() != null
                        && !rootProject.getDefaultGoal().isEmpty())) {
            tasks = Stream.of(rootProject.getDefaultGoal().split("\\s+"))
                    .filter(g -> !g.isEmpty())
                    .collect(Collectors.toList());
        }

        return calculateTaskSegments(session, tasks);
    }

    public List<TaskSegment> calculateTaskSegments(MavenSession session, List<String> tasks) throws Exception {
        List<TaskSegment> taskSegments = new ArrayList<>(tasks.size());

        TaskSegment currentSegment = null;

        for (String task : tasks) {
            if (isGoalSpecification(task)) {
                // "pluginPrefix[:version]:goal" or "groupId:artifactId[:version]:goal"

                lifecyclePluginResolver.resolveMissingPluginVersions(session.getTopLevelProject(), session);

                MojoDescriptor mojoDescriptor =
                        mojoDescriptorCreator.getMojoDescriptor(task, session, session.getTopLevelProject());

                boolean aggregating = mojoDescriptor.isAggregator() || !mojoDescriptor.isProjectRequired();

                if (currentSegment == null || currentSegment.isAggregating() != aggregating) {
                    currentSegment = new TaskSegment(aggregating);
                    taskSegments.add(currentSegment);
                }

                currentSegment.getTasks().add(new GoalTask(task));
            } else {
                // lifecycle phase

                if (currentSegment == null || currentSegment.isAggregating()) {
                    currentSegment = new TaskSegment(false);
                    taskSegments.add(currentSegment);
                }

                currentSegment.getTasks().add(new LifecycleTask(task));
            }
        }

        return taskSegments;
    }

    private boolean projectIsNotPresent(MavenSession session) {
        return !session.getRequest().isProjectPresent();
    }

    private boolean requiresProject(MavenSession session) {
        List<String> goals = session.getGoals();
        if (goals != null) {
            for (String goal : goals) {
                if (!isGoalSpecification(goal)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isGoalSpecification(String task) {
        return task.indexOf(':') >= 0;
    }
}
