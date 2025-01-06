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
import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.services.LifecycleRegistry;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.internal.MultilineMessageHelper;
import org.apache.maven.internal.impl.DefaultLifecycleRegistry;
import org.apache.maven.internal.impl.util.PhasingExecutor;
import org.apache.maven.internal.transformation.ConsumerPomArtifactTransformer;
import org.apache.maven.internal.xml.XmlNodeUtil;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleNotFoundException;
import org.apache.maven.lifecycle.LifecyclePhaseNotFoundException;
import org.apache.maven.lifecycle.MojoExecutionConfigurator;
import org.apache.maven.lifecycle.internal.BuildThreadFactory;
import org.apache.maven.lifecycle.internal.CompoundProjectExecutionListener;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.GoalTask;
import org.apache.maven.lifecycle.internal.LifecycleTask;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.Task;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.maven.api.Lifecycle.AFTER;
import static org.apache.maven.api.Lifecycle.AT;
import static org.apache.maven.api.Lifecycle.BEFORE;
import static org.apache.maven.api.Lifecycle.Phase.PACKAGE;
import static org.apache.maven.api.Lifecycle.Phase.READY;
import static org.apache.maven.lifecycle.internal.concurrent.BuildStep.CREATED;
import static org.apache.maven.lifecycle.internal.concurrent.BuildStep.EXECUTED;
import static org.apache.maven.lifecycle.internal.concurrent.BuildStep.FAILED;
import static org.apache.maven.lifecycle.internal.concurrent.BuildStep.PLAN;
import static org.apache.maven.lifecycle.internal.concurrent.BuildStep.PLANNING;
import static org.apache.maven.lifecycle.internal.concurrent.BuildStep.SCHEDULED;
import static org.apache.maven.lifecycle.internal.concurrent.BuildStep.SETUP;
import static org.apache.maven.lifecycle.internal.concurrent.BuildStep.TEARDOWN;

/**
 * Builds the full lifecycle in weave-mode (phase by phase as opposed to project-by-project).
 * <p>
 * This builder uses a number of threads equal to the minimum of the degree of concurrency (which is the thread count
 * set with <code>-T</code> on the command-line) and the number of projects to build. As such, building a single project
 * will always result in a sequential build, regardless of the thread count.
 * </p>
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @since 3.0
 *         Builds one or more lifecycles for a full module
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
@Named
public class BuildPlanExecutor {

    private static final Object GLOBAL = new Object();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MojoExecutor mojoExecutor;
    private final ExecutionEventCatapult eventCatapult;
    private final ProjectExecutionListener projectExecutionListener;
    private final ConsumerPomArtifactTransformer consumerPomArtifactTransformer;
    private final BuildPlanLogger buildPlanLogger;
    private final Map<String, MojoExecutionConfigurator> mojoExecutionConfigurators;
    private final MavenPluginManager mavenPluginManager;
    private final MojoDescriptorCreator mojoDescriptorCreator;
    private final LifecycleRegistry lifecycles;

    @Inject
    @SuppressWarnings("checkstyle:ParameterNumber")
    public BuildPlanExecutor(
            @Named("concurrent") MojoExecutor mojoExecutor,
            ExecutionEventCatapult eventCatapult,
            List<ProjectExecutionListener> listeners,
            ConsumerPomArtifactTransformer consumerPomArtifactTransformer,
            BuildPlanLogger buildPlanLogger,
            Map<String, MojoExecutionConfigurator> mojoExecutionConfigurators,
            MavenPluginManager mavenPluginManager,
            MojoDescriptorCreator mojoDescriptorCreator,
            LifecycleRegistry lifecycles) {
        this.mojoExecutor = mojoExecutor;
        this.eventCatapult = eventCatapult;
        this.projectExecutionListener = new CompoundProjectExecutionListener(listeners);
        this.consumerPomArtifactTransformer = consumerPomArtifactTransformer;
        this.buildPlanLogger = buildPlanLogger;
        this.mojoExecutionConfigurators = mojoExecutionConfigurators;
        this.mavenPluginManager = mavenPluginManager;
        this.mojoDescriptorCreator = mojoDescriptorCreator;
        this.lifecycles = lifecycles;
    }

    public void execute(MavenSession session, ReactorContext reactorContext, List<TaskSegment> taskSegments)
            throws ExecutionException, InterruptedException {
        try (BuildContext ctx = new BuildContext(session, reactorContext, taskSegments)) {
            ctx.execute();
        }
    }

    class BuildContext implements AutoCloseable {
        final MavenSession session;
        final ReactorContext reactorContext;
        final PhasingExecutor executor;
        final Map<Object, Clock> clocks = new ConcurrentHashMap<>();
        final ReadWriteLock lock = new ReentrantReadWriteLock();
        final int threads;
        BuildPlan plan;

        BuildContext(MavenSession session, ReactorContext reactorContext, List<TaskSegment> taskSegments) {
            this.session = session;
            this.reactorContext = reactorContext;
            this.threads = Math.min(
                    session.getRequest().getDegreeOfConcurrency(),
                    session.getProjects().size());
            // Propagate the parallel flag to the root session
            session.setParallel(threads > 1);
            this.executor = new PhasingExecutor(Executors.newFixedThreadPool(threads, new BuildThreadFactory()));

            // build initial plan
            this.plan = buildInitialPlan(taskSegments);
        }

        BuildContext() {
            this.session = null;
            this.reactorContext = null;
            this.threads = 1;
            this.executor = null;
            this.plan = null;
        }

        public BuildPlan buildInitialPlan(List<TaskSegment> taskSegments) {
            int nThreads = Math.min(
                    session.getRequest().getDegreeOfConcurrency(),
                    session.getProjects().size());
            boolean parallel = nThreads > 1;
            // Propagate the parallel flag to the root session
            session.setParallel(parallel);

            ProjectDependencyGraph dependencyGraph = session.getProjectDependencyGraph();
            MavenProject rootProject = session.getTopLevelProject();

            Map<MavenProject, List<MavenProject>> allProjects = new LinkedHashMap<>();
            dependencyGraph
                    .getSortedProjects()
                    .forEach(p -> allProjects.put(p, dependencyGraph.getUpstreamProjects(p, false)));

            BuildPlan plan = new BuildPlan(allProjects);
            for (TaskSegment taskSegment : taskSegments) {
                Map<MavenProject, List<MavenProject>> projects = taskSegment.isAggregating()
                        ? Collections.singletonMap(rootProject, allProjects.get(rootProject))
                        : allProjects;

                BuildPlan segment = calculateMojoExecutions(projects, taskSegment.getTasks());
                plan.then(segment);
            }

            // Create plan, setup and teardown
            for (MavenProject project : plan.getAllProjects().keySet()) {
                BuildStep pplan = new BuildStep(PLAN, project, null);
                pplan.status.set(PLANNING); // the plan step always need planning
                BuildStep setup = new BuildStep(SETUP, project, null);
                BuildStep teardown = new BuildStep(TEARDOWN, project, null);
                setup.executeAfter(pplan);
                plan.steps(project).forEach(step -> {
                    if (step.predecessors.isEmpty()) {
                        step.executeAfter(setup);
                    } else if (step.successors.isEmpty()) {
                        teardown.executeAfter(step);
                    }
                });
                Stream.of(pplan, setup, teardown).forEach(step -> plan.addStep(project, step.name, step));
            }

            return plan;
        }

        private void checkUnboundVersions(BuildPlan buildPlan) {
            String defaulModelId = DefaultLifecycleRegistry.DEFAULT_LIFECYCLE_MODELID;
            List<String> unversionedPlugins = buildPlan
                    .allSteps()
                    .flatMap(step -> step.mojos.values().stream().flatMap(map -> map.values().stream()))
                    .map(MojoExecution::getPlugin)
                    .filter(p -> p.getLocation("version") != null
                            && p.getLocation("version").getSource() != null
                            && defaulModelId.equals(
                                    p.getLocation("version").getSource().getModelId()))
                    .distinct()
                    .map(Plugin::getArtifactId) // managed by us, groupId is always o.a.m.plugins
                    .toList();
            if (!unversionedPlugins.isEmpty()) {
                logger.warn("Version not locked for default bindings plugins " + unversionedPlugins
                        + ", you should define versions in pluginManagement section of your " + "pom.xml or parent");
            }
        }

        private void checkThreadSafety(BuildPlan buildPlan) {
            if (threads > 1) {
                Set<MojoExecution> unsafeExecutions = buildPlan
                        .allSteps()
                        .flatMap(step -> step.mojos.values().stream().flatMap(map -> map.values().stream()))
                        .filter(execution -> !execution.getMojoDescriptor().isV4Api())
                        .collect(Collectors.toSet());
                if (!unsafeExecutions.isEmpty()) {
                    for (String s : MultilineMessageHelper.format(
                            """
                                Your build is requesting concurrent execution, but this project contains the \
                                following plugin(s) that have goals not built with Maven 4 to support concurrent \
                                execution. While this /may/ work fine, please look for plugin updates and/or \
                                request plugins be made thread-safe. If reporting an issue, report it against the \
                                plugin in question, not against Apache Maven.""")) {
                        logger.warn(s);
                    }
                    if (logger.isDebugEnabled()) {
                        Set<MojoDescriptor> unsafeGoals = unsafeExecutions.stream()
                                .map(MojoExecution::getMojoDescriptor)
                                .collect(Collectors.toSet());
                        logger.warn("The following goals are not Maven 4 goals:");
                        for (MojoDescriptor unsafeGoal : unsafeGoals) {
                            logger.warn("  " + unsafeGoal.getId());
                        }
                    } else {
                        Set<Plugin> unsafePlugins = unsafeExecutions.stream()
                                .map(MojoExecution::getPlugin)
                                .collect(Collectors.toSet());
                        logger.warn("The following plugins are not Maven 4 plugins:");
                        for (Plugin unsafePlugin : unsafePlugins) {
                            logger.warn("  " + unsafePlugin.getId());
                        }
                        logger.warn("");
                        logger.warn("Enable verbose output (-X) to see precisely which goals are not marked as"
                                + " thread-safe.");
                    }
                    logger.warn(MultilineMessageHelper.separatorLine());
                }
            }
        }

        void execute() {
            try (var phase = executor.phase()) {
                plan();
                executePlan();
            } catch (Exception e) {
                session.getResult().addException(e);
            }
        }

        @Override
        public void close() {
            this.executor.close();
        }

        private void executePlan() {
            if (reactorContext.getReactorBuildStatus().isHalted()) {
                return;
            }
            Clock global = clocks.computeIfAbsent(GLOBAL, p -> new Clock());
            global.start();
            lock.readLock().lock();
            try {
                plan.sortedNodes().stream()
                        .filter(step -> step.status.get() == CREATED)
                        .filter(step -> step.predecessors.stream().allMatch(s -> s.status.get() == EXECUTED))
                        .filter(step -> step.status.compareAndSet(CREATED, SCHEDULED))
                        .forEach(step -> {
                            boolean nextIsPlanning = step.successors.stream().anyMatch(st -> PLAN.equals(st.name));
                            executor.execute(() -> {
                                try {
                                    executeStep(step);
                                    if (nextIsPlanning) {
                                        lock.writeLock().lock();
                                        try {
                                            plan();
                                        } finally {
                                            lock.writeLock().unlock();
                                        }
                                    }
                                    executePlan();
                                } catch (Exception e) {
                                    step.status.compareAndSet(SCHEDULED, FAILED);
                                    global.stop();
                                    handleBuildError(reactorContext, session, step.project, e, global);
                                }
                            });
                        });
            } finally {
                lock.readLock().unlock();
            }
        }

        private void executeStep(BuildStep step) throws IOException, LifecycleExecutionException {
            Clock clock = getClock(step.project);
            switch (step.name) {
                case PLAN:
                    // Planning steps should be executed out of normal execution
                    throw new IllegalStateException();
                case SETUP:
                    consumerPomArtifactTransformer.injectTransformedArtifacts(
                            session.getRepositorySession(), step.project);
                    projectExecutionListener.beforeProjectExecution(new ProjectExecutionEvent(session, step.project));
                    eventCatapult.fire(ExecutionEvent.Type.ProjectStarted, session, null);
                    break;
                case TEARDOWN:
                    projectExecutionListener.afterProjectExecutionSuccess(
                            new ProjectExecutionEvent(session, step.project, Collections.emptyList()));
                    reactorContext
                            .getResult()
                            .addBuildSummary(new BuildSuccess(step.project, clock.wallTime(), clock.execTime()));
                    eventCatapult.fire(ExecutionEvent.Type.ProjectSucceeded, session, null);
                    break;
                default:
                    List<MojoExecution> executions = step.executions().collect(Collectors.toList());
                    if (!executions.isEmpty()) {
                        attachToThread(step.project);
                        session.setCurrentProject(step.project);
                        clock.start();
                        executions.forEach(mojoExecution -> {
                            mojoExecutionConfigurator(mojoExecution).configure(step.project, mojoExecution, true);
                            finalizeMojoConfiguration(mojoExecution);
                        });
                        mojoExecutor.execute(session, executions);
                        clock.stop();
                    }
                    break;
            }
            step.status.compareAndSet(SCHEDULED, EXECUTED);
        }

        private Clock getClock(Object key) {
            return clocks.computeIfAbsent(key, p -> new Clock());
        }

        private void plan() {
            lock.writeLock().lock();
            try {
                Set<BuildStep> planSteps = plan.allSteps()
                        .filter(st -> PLAN.equals(st.name))
                        .filter(step -> step.predecessors.stream().allMatch(s -> s.status.get() == EXECUTED))
                        .filter(step -> step.status.compareAndSet(PLANNING, SCHEDULED))
                        .collect(Collectors.toSet());
                for (BuildStep step : planSteps) {
                    MavenProject project = step.project;
                    for (Plugin plugin : project.getBuild().getPlugins()) {
                        for (PluginExecution execution : plugin.getExecutions()) {
                            for (String goal : execution.getGoals()) {
                                MojoDescriptor mojoDescriptor = getMojoDescriptor(project, plugin, goal);
                                String phase =
                                        execution.getPhase() != null ? execution.getPhase() : mojoDescriptor.getPhase();
                                if (phase == null) {
                                    continue;
                                }
                                String tmpResolvedPhase = plan.aliases().getOrDefault(phase, phase);
                                String resolvedPhase = tmpResolvedPhase.startsWith(AT)
                                        ? tmpResolvedPhase.substring(AT.length())
                                        : tmpResolvedPhase;
                                plan.step(project, resolvedPhase).ifPresent(n -> {
                                    MojoExecution mojoExecution = new MojoExecution(mojoDescriptor, execution.getId());
                                    mojoExecution.setLifecyclePhase(phase);
                                    n.addMojo(mojoExecution, execution.getPriority());
                                    if (mojoDescriptor.getDependencyCollectionRequired() != null
                                            || mojoDescriptor.getDependencyResolutionRequired() != null) {
                                        for (MavenProject p :
                                                plan.getAllProjects().get(project)) {
                                            plan.step(p, AFTER + PACKAGE)
                                                    .ifPresent(a -> plan.requiredStep(project, resolvedPhase)
                                                            .executeAfter(a));
                                        }
                                    }
                                });
                            }
                        }
                    }
                }

                BuildPlan buildPlan = plan;
                for (BuildStep step :
                        planSteps.stream().flatMap(p -> plan.steps(p.project)).toList()) {
                    for (MojoExecution execution : step.executions().toList()) {
                        buildPlan = computeForkPlan(step, execution, buildPlan);
                    }
                }

                for (BuildStep step : planSteps) {
                    MavenProject project = step.project;
                    buildPlanLogger.writePlan(plan, project);
                    step.status.compareAndSet(SCHEDULED, EXECUTED);
                }

                checkThreadSafety(plan);
                checkUnboundVersions(plan);
            } finally {
                lock.writeLock().unlock();
            }
        }

        protected BuildPlan computeForkPlan(BuildStep step, MojoExecution execution, BuildPlan buildPlan) {
            MojoDescriptor mojoDescriptor = execution.getMojoDescriptor();
            PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
            String forkedGoal = mojoDescriptor.getExecuteGoal();
            String phase = mojoDescriptor.getExecutePhase();
            // We have a fork goal
            if (forkedGoal != null && !forkedGoal.isEmpty()) {
                MojoDescriptor forkedMojoDescriptor = pluginDescriptor.getMojo(forkedGoal);
                if (forkedMojoDescriptor == null) {
                    throw new MavenException(new MojoNotFoundException(forkedGoal, pluginDescriptor));
                }

                List<MavenProject> toFork = new ArrayList<>();
                toFork.add(step.project);
                if (mojoDescriptor.isAggregator() && step.project.getCollectedProjects() != null) {
                    toFork.addAll(step.project.getCollectedProjects());
                }

                BuildPlan plan = new BuildPlan();
                for (MavenProject project : toFork) {
                    BuildStep st = new BuildStep(forkedGoal, project, null);
                    MojoExecution mojoExecution = new MojoExecution(forkedMojoDescriptor, forkedGoal);
                    st.addMojo(mojoExecution, 0);
                    Map<String, BuildStep> n = new HashMap<>();
                    n.put(forkedGoal, st);
                    plan.addProject(project, n);
                }

                for (BuildStep astep : plan.allSteps().toList()) {
                    for (MojoExecution aexecution : astep.executions().toList()) {
                        plan = computeForkPlan(astep, aexecution, plan);
                    }
                }

                return plan;

            } else if (phase != null && !phase.isEmpty()) {
                String forkedLifecycle = mojoDescriptor.getExecuteLifecycle();
                Lifecycle lifecycle;
                if (forkedLifecycle != null && !forkedLifecycle.isEmpty()) {
                    org.apache.maven.api.plugin.descriptor.lifecycle.Lifecycle lifecycleOverlay;
                    try {
                        lifecycleOverlay = pluginDescriptor.getLifecycleMapping(forkedLifecycle);
                    } catch (IOException | XMLStreamException e) {
                        throw new MavenException(new PluginDescriptorParsingException(
                                pluginDescriptor.getPlugin(), pluginDescriptor.getSource(), e));
                    }
                    if (lifecycleOverlay == null) {
                        Optional<Lifecycle> lf = lifecycles.lookup(forkedLifecycle);
                        if (lf.isPresent()) {
                            lifecycle = lf.get();
                        } else {
                            throw new MavenException(new LifecycleNotFoundException(forkedLifecycle));
                        }
                    } else {
                        lifecycle = new PluginLifecycle(lifecycleOverlay, pluginDescriptor);
                    }
                } else {
                    if (execution.getLifecyclePhase() != null) {
                        String n = execution.getLifecyclePhase();
                        String phaseName = n.startsWith(BEFORE)
                                ? n.substring(BEFORE.length())
                                : n.startsWith(AFTER) ? n.substring(AFTER.length()) : n;
                        lifecycle = lifecycles.stream()
                                .filter(l -> l.allPhases().anyMatch(p -> phaseName.equals(p.name())))
                                .findFirst()
                                .orElse(null);
                        if (lifecycle == null) {
                            throw new IllegalStateException();
                        }
                    } else {
                        lifecycle = lifecycles.require(Lifecycle.DEFAULT);
                    }
                }

                String resolvedPhase = getResolvedPhase(lifecycle, phase);

                Map<MavenProject, List<MavenProject>> map = Collections.singletonMap(
                        step.project, plan.getAllProjects().get(step.project));
                BuildPlan forkedPlan = calculateLifecycleMappings(map, lifecycle, resolvedPhase);
                forkedPlan.then(buildPlan);
                return forkedPlan;
            } else {
                return buildPlan;
            }
        }

        private String getResolvedPhase(Lifecycle lifecycle, String phase) {
            return lifecycle.aliases().stream()
                    .filter(a -> phase.equals(a.v3Phase()))
                    .findFirst()
                    .map(Lifecycle.Alias::v4Phase)
                    .orElse(phase);
        }

        private String getResolvedPhase(String phase) {
            return lifecycles.stream()
                    .flatMap(l -> l.aliases().stream())
                    .filter(a -> phase.equals(a.v3Phase()))
                    .findFirst()
                    .map(Lifecycle.Alias::v4Phase)
                    .orElse(phase);
        }

        protected void handleBuildError(
                final ReactorContext buildContext,
                final MavenSession session,
                final MavenProject mavenProject,
                Throwable t,
                final Clock clock) {
            // record the error and mark the project as failed
            buildContext.getResult().addException(t);
            buildContext
                    .getResult()
                    .addBuildSummary(new BuildFailure(mavenProject, clock.execTime(), clock.wallTime(), t));

            // notify listeners about "soft" project build failures only
            if (t instanceof Exception && !(t instanceof RuntimeException)) {
                eventCatapult.fire(ExecutionEvent.Type.ProjectFailed, session, null, (Exception) t);
            }

            // reactor failure modes
            if (t instanceof RuntimeException || !(t instanceof Exception)) {
                // fail fast on RuntimeExceptions, Errors and "other" Throwables
                // assume these are system errors and further build is meaningless
                buildContext.getReactorBuildStatus().halt();
            } else if (MavenExecutionRequest.REACTOR_FAIL_NEVER.equals(session.getReactorFailureBehavior())) {
                // continue the build
            } else if (MavenExecutionRequest.REACTOR_FAIL_AT_END.equals(session.getReactorFailureBehavior())) {
                // continue the build but ban all projects that depend on the failed one
                buildContext.getReactorBuildStatus().blackList(mavenProject);
            } else if (MavenExecutionRequest.REACTOR_FAIL_FAST.equals(session.getReactorFailureBehavior())) {
                buildContext.getReactorBuildStatus().halt();
            } else {
                logger.error("invalid reactor failure behavior " + session.getReactorFailureBehavior());
                buildContext.getReactorBuildStatus().halt();
            }
        }

        public BuildPlan calculateMojoExecutions(Map<MavenProject, List<MavenProject>> projects, List<Task> tasks) {
            BuildPlan buildPlan = new BuildPlan(projects);

            for (Task task : tasks) {
                BuildPlan step;

                if (task instanceof GoalTask) {
                    String pluginGoal = task.getValue();

                    String executionId = "default-cli";
                    int executionIdx = pluginGoal.indexOf('@');
                    if (executionIdx > 0) {
                        executionId = pluginGoal.substring(executionIdx + 1);
                    }

                    step = new BuildPlan();
                    for (MavenProject project : projects.keySet()) {
                        BuildStep st = new BuildStep(pluginGoal, project, null);
                        MojoDescriptor mojoDescriptor = getMojoDescriptor(project, pluginGoal);
                        MojoExecution mojoExecution =
                                new MojoExecution(mojoDescriptor, executionId, MojoExecution.Source.CLI);
                        st.addMojo(mojoExecution, 0);
                        Map<String, BuildStep> n = new HashMap<>();
                        n.put(pluginGoal, st);
                        step.addProject(project, n);
                    }
                } else if (task instanceof LifecycleTask) {
                    String lifecyclePhase = task.getValue();

                    step = calculateLifecycleMappings(projects, lifecyclePhase);

                } else {
                    throw new IllegalStateException("unexpected task " + task);
                }

                buildPlan.then(step);
            }

            return buildPlan;
        }

        private MojoDescriptor getMojoDescriptor(MavenProject project, Plugin plugin, String goal) {
            try {
                return mavenPluginManager.getMojoDescriptor(
                        plugin, goal, project.getRemotePluginRepositories(), session.getRepositorySession());
            } catch (MavenException e) {
                throw e;
            } catch (Exception e) {
                throw new MavenException(e);
            }
        }

        private MojoDescriptor getMojoDescriptor(MavenProject project, String task) {
            try {
                return mojoDescriptorCreator.getMojoDescriptor(task, session, project);
            } catch (MavenException e) {
                throw e;
            } catch (Exception e) {
                throw new MavenException(e);
            }
        }

        public BuildPlan calculateLifecycleMappings(
                Map<MavenProject, List<MavenProject>> projects, String lifecyclePhase) {

            String resolvedPhase = getResolvedPhase(lifecyclePhase);
            String mainPhase = resolvedPhase.startsWith(BEFORE)
                    ? resolvedPhase.substring(BEFORE.length())
                    : resolvedPhase.startsWith(AFTER)
                            ? resolvedPhase.substring(AFTER.length())
                            : resolvedPhase.startsWith(AT) ? resolvedPhase.substring(AT.length()) : resolvedPhase;

            /*
             * Determine the lifecycle that corresponds to the given phase.
             */
            Lifecycle lifecycle = lifecycles.stream()
                    .filter(l -> l.allPhases().anyMatch(p -> mainPhase.equals(p.name())))
                    .findFirst()
                    .orElse(null);

            if (lifecycle == null) {
                throw new MavenException(new LifecyclePhaseNotFoundException(
                        "Unknown lifecycle phase \"" + lifecyclePhase
                                + "\". You must specify a valid lifecycle phase"
                                + " or a goal in the format <plugin-prefix>:<goal> or"
                                + " <plugin-group-id>:<plugin-artifact-id>[:<plugin-version>]:<goal>. Available lifecycle phases are: "
                                + lifecycles.stream()
                                        .flatMap(l -> l.allPhases().map(Lifecycle.Phase::name))
                                        .collect(Collectors.joining(", "))
                                + ".",
                        lifecyclePhase));
            }

            return calculateLifecycleMappings(projects, lifecycle, resolvedPhase);
        }

        public BuildPlan calculateLifecycleMappings(
                Map<MavenProject, List<MavenProject>> projects, Lifecycle lifecycle, String lifecyclePhase) {
            BuildPlan plan = new BuildPlan(projects);

            for (MavenProject project : projects.keySet()) {
                // For each phase, create and sequence the pre, run and post steps
                Map<String, BuildStep> steps = lifecycle
                        .allPhases()
                        .flatMap(phase -> {
                            BuildStep a = new BuildStep(BEFORE + phase.name(), project, phase);
                            BuildStep b = new BuildStep(phase.name(), project, phase);
                            BuildStep c = new BuildStep(AFTER + phase.name(), project, phase);
                            b.executeAfter(a);
                            c.executeAfter(b);
                            return Stream.of(a, b, c);
                        })
                        .collect(Collectors.toMap(n -> n.name, n -> n));
                // for each phase, make sure children phases are execute between pre and post steps
                lifecycle.allPhases().forEach(phase -> phase.phases().forEach(child -> {
                    steps.get(BEFORE + child.name()).executeAfter(steps.get(BEFORE + phase.name()));
                    steps.get(AFTER + phase.name()).executeAfter(steps.get(AFTER + child.name()));
                }));
                // for each phase, create links between this project phases
                lifecycle.allPhases().forEach(phase -> {
                    phase.links().stream()
                            .filter(l -> l.pointer().type() == Lifecycle.Pointer.Type.PROJECT)
                            .forEach(link -> {
                                String n1 = phase.name();
                                String n2 = link.pointer().phase();
                                if (link.kind() == Lifecycle.Link.Kind.AFTER) {
                                    steps.get(BEFORE + n1).executeAfter(steps.get(AFTER + n2));
                                } else {
                                    steps.get(BEFORE + n2).executeAfter(steps.get(AFTER + n1));
                                }
                            });
                });

                // Only keep mojo executions before the end phase
                String endPhase = lifecyclePhase.startsWith(BEFORE) || lifecyclePhase.startsWith(AFTER)
                        ? lifecyclePhase
                        : lifecyclePhase.startsWith(AT)
                                ? lifecyclePhase.substring(AT.length())
                                : AFTER + lifecyclePhase;
                Set<BuildStep> toKeep = steps.get(endPhase).allPredecessors().collect(Collectors.toSet());
                toKeep.addAll(toKeep.stream()
                        .filter(s -> s.name.startsWith(BEFORE))
                        .map(s -> steps.get(AFTER + s.name.substring(BEFORE.length())))
                        .toList());
                steps.values().stream().filter(n -> !toKeep.contains(n)).forEach(BuildStep::skip);

                plan.addProject(project, steps);
            }

            // Create inter project dependencies
            plan.allSteps().filter(step -> step.phase != null).forEach(step -> {
                Lifecycle.Phase phase = step.phase;
                MavenProject project = step.project;
                phase.links().stream()
                        .filter(l -> l.pointer().type() != Lifecycle.Pointer.Type.PROJECT)
                        .forEach(link -> {
                            String n1 = phase.name();
                            String n2 = link.pointer().phase();
                            // for each project, if the phase in the build, link after it
                            getLinkedProjects(projects, project, link).forEach(p -> plan.step(p, AFTER + n2)
                                    .ifPresent(a -> plan.requiredStep(project, BEFORE + n1)
                                            .executeAfter(a)));
                        });
            });

            // Keep projects in reactors by GAV
            Map<String, MavenProject> reactorGavs =
                    projects.keySet().stream().collect(Collectors.toMap(BuildPlanExecutor::gav, p -> p));

            // Go through all plugins
            List<Runnable> toResolve = new ArrayList<>();
            projects.keySet().forEach(project -> project.getBuild().getPlugins().forEach(plugin -> {
                MavenProject pluginProject = reactorGavs.get(gav(plugin));
                if (pluginProject != null) {
                    // In order to plan the project, we need all its plugins...
                    plan.requiredStep(project, PLAN).executeAfter(plan.requiredStep(pluginProject, READY));
                } else {
                    toResolve.add(() -> resolvePlugin(session, project.getRemotePluginRepositories(), plugin));
                }
            }));

            // Eagerly resolve all plugins in parallel
            toResolve.parallelStream().forEach(Runnable::run);

            // Keep track of phase aliases
            lifecycle.aliases().forEach(alias -> plan.aliases().put(alias.v3Phase(), alias.v4Phase()));

            return plan;
        }

        private List<MavenProject> getLinkedProjects(
                Map<MavenProject, List<MavenProject>> projects, MavenProject project, Lifecycle.Link link) {
            if (link.pointer().type() == Lifecycle.Pointer.Type.DEPENDENCIES) {
                // TODO: String scope = ((Lifecycle.DependenciesPointer) link.pointer()).scope();
                return projects.get(project);
            } else if (link.pointer().type() == Lifecycle.Pointer.Type.CHILDREN) {
                return project.getCollectedProjects();
            } else {
                throw new IllegalArgumentException(
                        "Unsupported pointer type: " + link.pointer().type());
            }
        }
    }

    private void resolvePlugin(MavenSession session, List<RemoteRepository> repositories, Plugin plugin) {
        try {
            mavenPluginManager.getPluginDescriptor(plugin, repositories, session.getRepositorySession());
        } catch (Exception e) {
            throw new MavenException(e);
        }
    }

    private static String gav(MavenProject p) {
        return p.getGroupId() + ":" + p.getArtifactId() + ":" + p.getVersion();
    }

    private static String gav(Plugin p) {
        return p.getGroupId() + ":" + p.getArtifactId() + ":" + p.getVersion();
    }

    /**
     * Post-processes the effective configuration for the specified mojo execution. This step discards all parameters
     * from the configuration that are not applicable to the mojo and injects the default values for any missing
     * parameters.
     *
     * @param mojoExecution The mojo execution whose configuration should be finalized, must not be {@code null}.
     */
    private void finalizeMojoConfiguration(MojoExecution mojoExecution) {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        XmlNode executionConfiguration = mojoExecution.getConfiguration() != null
                ? mojoExecution.getConfiguration().getDom()
                : null;
        if (executionConfiguration == null) {
            executionConfiguration = new XmlNode("configuration");
        }

        XmlNode defaultConfiguration = getMojoConfiguration(mojoDescriptor);

        List<XmlNode> children = new ArrayList<>();
        if (mojoDescriptor.getParameters() != null) {
            for (Parameter parameter : mojoDescriptor.getParameters()) {
                XmlNode parameterConfiguration = executionConfiguration.getChild(parameter.getName());

                if (parameterConfiguration == null) {
                    parameterConfiguration = executionConfiguration.getChild(parameter.getAlias());
                }

                XmlNode parameterDefaults = defaultConfiguration.getChild(parameter.getName());

                if (parameterConfiguration != null) {
                    parameterConfiguration = XmlNodeUtil.merge(parameterConfiguration, parameterDefaults, Boolean.TRUE);
                } else {
                    parameterConfiguration = parameterDefaults;
                }

                if (parameterConfiguration != null) {
                    Map<String, String> attributes = new HashMap<>(parameterConfiguration.getAttributes());

                    String attributeForImplementation = parameterConfiguration.getAttribute("implementation");
                    String parameterForImplementation = parameter.getImplementation();
                    if ((attributeForImplementation == null || attributeForImplementation.isEmpty())
                            && ((parameterForImplementation != null) && !parameterForImplementation.isEmpty())) {
                        attributes.put("implementation", parameter.getImplementation());
                    }

                    parameterConfiguration = new XmlNode(
                            parameter.getName(),
                            parameterConfiguration.getValue(),
                            attributes,
                            parameterConfiguration.getChildren(),
                            parameterConfiguration.getInputLocation());

                    children.add(parameterConfiguration);
                }
            }
        }
        XmlNode finalConfiguration = new XmlNode("configuration", null, null, children, null);

        mojoExecution.setConfiguration(finalConfiguration);
    }

    private XmlNode getMojoConfiguration(MojoDescriptor mojoDescriptor) {
        if (mojoDescriptor.isV4Api()) {
            return MojoDescriptorCreator.convert(mojoDescriptor.getMojoDescriptorV4());
        } else {
            return MojoDescriptorCreator.convert(mojoDescriptor).getDom();
        }
    }

    private MojoExecutionConfigurator mojoExecutionConfigurator(MojoExecution mojoExecution) {
        String configuratorId = mojoExecution.getMojoDescriptor().getComponentConfigurator();
        if (configuratorId == null) {
            configuratorId = "default";
        }

        MojoExecutionConfigurator mojoExecutionConfigurator = mojoExecutionConfigurators.get(configuratorId);

        if (mojoExecutionConfigurator == null) {
            //
            // The plugin has a custom component configurator but does not have a custom mojo execution configurator
            // so fall back to the default mojo execution configurator.
            //
            mojoExecutionConfigurator = mojoExecutionConfigurators.get("default");
        }
        return mojoExecutionConfigurator;
    }

    public static void attachToThread(MavenProject currentProject) {
        ClassRealm projectRealm = currentProject.getClassRealm();
        if (projectRealm != null) {
            Thread.currentThread().setContextClassLoader(projectRealm);
        }
    }

    protected static class Clock {
        Instant start;
        Instant end;
        Instant resumed;
        Duration exec = Duration.ZERO;

        protected void start() {
            if (start == null) {
                start = MonotonicClock.now();
                resumed = start;
            } else {
                resumed = MonotonicClock.now();
            }
        }

        protected void stop() {
            end = MonotonicClock.now();
            exec = exec.plus(Duration.between(resumed, end));
        }

        protected Duration wallTime() {
            return start != null && end != null ? Duration.between(start, end) : Duration.ZERO;
        }

        protected Duration execTime() {
            return exec;
        }
    }
}
