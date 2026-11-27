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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.execution.ProjectExecutionEvent;
import org.apache.maven.execution.ProjectExecutionListener;
import org.apache.maven.internal.impl.DefaultLifecycleRegistry;
import org.apache.maven.internal.transformation.TransformerManager;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.GoalTask;
import org.apache.maven.lifecycle.internal.LifecycleTask;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.lifecycle.internal.ReactorBuildStatus;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.lifecycle.internal.stub.ExecutionEventCatapultStub;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.installation.InstallRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuildPlanExecutorTest {

    @Test
    void aggregateOutputsAreReadyBeforeExternalConsumers() {
        MavenProject root = newProject();
        MavenProject child = newProject();
        child.setArtifactId("child");
        MavenProject consumer = newProject();
        consumer.setArtifactId("consumer");
        root.setCollectedProjects(List.of(child));
        BuildPlanExecutor.BuildContext context = newExecutor((BeforeProjectExecution) event -> {}).new BuildContext();
        context.plan = new BuildPlan(Map.of(root, List.of(), child, List.of(root), consumer, List.of(root)));
        for (MavenProject project : List.of(root, child, consumer)) {
            context.plan.addProject(project, new HashMap<>());
        }
        BuildStep end = addStep(context.plan, root, "after:package");
        BuildStep aggregate = addStep(context.plan, root, "$aggregator:package");
        BuildStep childEnd = addStep(context.plan, child, "after:package");
        BuildStep childCompile = addStep(context.plan, child, "compile");
        BuildStep consumerCompile = addStep(context.plan, consumer, "compile");
        aggregate.executeAfter(end);
        aggregate.executeAfter(childEnd);
        childEnd.executeAfter(childCompile);
        childCompile.executeAfter(end);
        consumerCompile.executeAfter(end);

        context.linkAggregatorSteps();

        assertTrue(consumerCompile.isSuccessorOf(aggregate));
        assertFalse(childCompile.isSuccessorOf(aggregate), "do not introduce a parent/child dependency cycle");
        assertTrue(aggregate.isSuccessorOf(childEnd));
    }

    @Test
    void laterPlannedAggregatorUsesAggregationInsteadOfInheritance() {
        MavenProject root = newProject();
        MavenProject child = newProject();
        child.setArtifactId("child");
        root.setCollectedProjects(List.of(child));
        BuildPlanExecutor.BuildContext context = newExecutor((BeforeProjectExecution) event -> {}).new BuildContext();
        context.plan = new BuildPlan(Map.of(root, List.of(), child, List.of()));
        context.plan.addProject(root, new HashMap<>());
        context.plan.addProject(child, new HashMap<>());
        BuildStep end = addStep(context.plan, root, "after:package");
        BuildStep childEnd = addStep(context.plan, child, "after:package");
        BuildStep aggregate = addStep(context.plan, root, "$aggregator:package");
        aggregate.executeAfter(end);
        aggregate.executeAfter(childEnd);
        context.linkAggregatorSteps();

        BuildStep childAggregate = addStep(context.plan, child, "$aggregator:package");
        childAggregate.executeAfter(childEnd);
        context.linkAggregatorSteps();

        assertTrue(aggregate.isSuccessorOf(childAggregate));
        assertFalse(childAggregate.isSuccessorOf(aggregate));
    }

    @Test
    void laterPlannedConsumerWaitsForExistingAggregator() {
        MavenProject root = newProject();
        MavenProject consumer = newProject();
        consumer.setArtifactId("consumer");
        BuildPlanExecutor.BuildContext context = newExecutor((BeforeProjectExecution) event -> {}).new BuildContext();
        context.plan = new BuildPlan(Map.of(root, List.of(), consumer, List.of(root)));
        context.plan.addProject(root, new HashMap<>());
        context.plan.addProject(consumer, new HashMap<>());
        BuildStep end = addStep(context.plan, root, "after:package");
        BuildStep aggregate = addStep(context.plan, root, "$aggregator:package");
        aggregate.executeAfter(end);
        context.linkAggregatorSteps();

        BuildStep consumerCompile = addStep(context.plan, consumer, "compile");
        consumerCompile.executeAfter(end);
        context.linkAggregatorSteps();

        assertTrue(consumerCompile.isSuccessorOf(aggregate));
    }

    @Test
    void transitiveConsumerWaitsForAggregate() {
        MavenProject root = newProject();
        MavenProject child = newProject();
        child.setArtifactId("child");
        MavenProject consumer = newProject();
        consumer.setArtifactId("consumer");
        root.setCollectedProjects(List.of(child));
        BuildPlanExecutor.BuildContext context = newExecutor((BeforeProjectExecution) event -> {}).new BuildContext();
        context.plan = new BuildPlan(Map.of(root, List.of(), child, List.of(root), consumer, List.of(root, child)));
        for (MavenProject project : List.of(root, child, consumer)) {
            context.plan.addProject(project, new HashMap<>());
        }
        BuildStep end = addStep(context.plan, root, "after:package");
        BuildStep childEnd = addStep(context.plan, child, "after:package");
        BuildStep aggregate = addStep(context.plan, root, "$aggregator:package");
        BuildStep consume = addStep(context.plan, consumer, "compile");
        childEnd.executeAfter(end);
        aggregate.executeAfter(childEnd);
        consume.executeAfter(childEnd);
        consume.executeAfter(end); // Elided because it is already reachable through childEnd.

        context.linkAggregatorSteps();

        assertTrue(consume.isSuccessorOf(aggregate));
        assertFalse(childEnd.isSuccessorOf(aggregate));
    }

    @Test
    void laterPhaseWaitsForAggregateWhenReadinessCannot() {
        MavenProject root = newProject();
        MavenProject child = newProject();
        child.setArtifactId("child");
        root.setCollectedProjects(List.of(child));
        BuildPlanExecutor.BuildContext context = newExecutor((BeforeProjectExecution) event -> {}).new BuildContext();
        context.plan = new BuildPlan(Map.of(root, List.of(), child, List.of(root)));
        context.plan.addProject(root, new HashMap<>());
        context.plan.addProject(child, new HashMap<>());
        BuildStep end = addStep(context.plan, root, "after:package");
        BuildStep ready = addStep(context.plan, root, "ready");
        BuildStep childEnd = addStep(context.plan, child, "after:package");
        BuildStep aggregate = addStep(context.plan, root, "$aggregator:package");
        BuildStep laterPhase = addStep(context.plan, root, "install");
        ready.executeAfter(end);
        childEnd.executeAfter(ready);
        aggregate.executeAfter(childEnd);
        laterPhase.executeAfter(ready);

        context.linkAggregatorSteps();
        context.linkAggregatorSteps();

        assertFalse(ready.isSuccessorOf(aggregate));
        assertTrue(laterPhase.isSuccessorOf(aggregate));
        assertFalse(aggregate.isSuccessorOf(laterPhase));
    }

    @Test
    void nestedAggregationPrecedesAncestorRegardlessOfPlanningOrder() {
        for (boolean deferred : List.of(false, true)) {
            MavenProject root = newProject();
            MavenProject child = newProject();
            child.setArtifactId("child");
            root.setCollectedProjects(List.of(child));
            BuildPlanExecutor.BuildContext context =
                    newExecutor((BeforeProjectExecution) event -> {}).new BuildContext();
            context.plan = new BuildPlan(Map.of(root, List.of(), child, List.of(root)));
            context.plan.addProject(root, new HashMap<>());
            context.plan.addProject(child, new HashMap<>());
            BuildStep end = addStep(context.plan, root, "after:package");
            BuildStep childEnd = addStep(context.plan, child, "after:package");
            childEnd.executeAfter(end);
            BuildStep aggregate = addStep(context.plan, root, "$aggregator:package");
            aggregate.executeAfter(childEnd);
            if (deferred) {
                context.linkAggregatorSteps();
            }
            BuildStep childAggregate = addStep(context.plan, child, "$aggregator:package");
            childAggregate.executeAfter(childEnd);

            context.linkAggregatorSteps();
            context.linkAggregatorSteps();

            assertTrue(aggregate.isSuccessorOf(childAggregate));
            assertFalse(childAggregate.isSuccessorOf(aggregate));
        }
    }

    @Test
    void earlierChildAggregationPrecedesAncestorBarrier() {
        MavenProject root = newProject();
        MavenProject child = newProject();
        child.setArtifactId("child");
        root.setCollectedProjects(List.of(child));
        BuildPlanExecutor.BuildContext context = newExecutor((BeforeProjectExecution) event -> {}).new BuildContext();
        context.plan = new BuildPlan(Map.of(root, List.of(), child, List.of(root)));
        context.plan.addProject(root, new HashMap<>());
        context.plan.addProject(child, new HashMap<>());
        BuildStep end = addStep(context.plan, root, "after:package");
        BuildStep childBefore = addStep(context.plan, child, "before:package");
        BuildStep childEnd = addStep(context.plan, child, "after:package");
        childBefore.executeAfter(end);
        childEnd.executeAfter(childBefore);
        BuildStep aggregate = addStep(context.plan, root, "$aggregator:package");
        aggregate.executeAfter(childEnd);
        BuildStep childAggregate = addStep(context.plan, child, "$aggregator:before:package:0");
        childAggregate.executeAfter(childBefore);
        context.aggregationEnds.put(childAggregate, childBefore);

        context.linkAggregatorSteps();

        assertTrue(aggregate.isSuccessorOf(childAggregate));
        assertTrue(childEnd.isSuccessorOf(childAggregate));
        assertFalse(childAggregate.isSuccessorOf(aggregate));
    }

    private BuildStep addStep(BuildPlan plan, MavenProject project, String name) {
        Lifecycle.Phase phase = new DefaultLifecycleRegistry(Collections.emptyList())
                .lookup("default")
                .orElseThrow()
                .allPhases()
                .filter(p -> p.name().equals("package"))
                .findFirst()
                .orElseThrow();
        BuildStep step = new BuildStep(name, project, phase);
        plan.addStep(project, name, step);
        return step;
    }

    @Test
    void teardownWaitsForAnOrdinaryDirectGoalFailure() throws Exception {
        String goal = "test:example-plugin:1:run";
        MavenProject project = newProject();
        MavenSession session = newSession(project);
        session.getRequest().setGoals(List.of(goal));

        PluginDescriptor plugin = new PluginDescriptor();
        plugin.setPlugin(new Plugin());
        MojoDescriptor descriptor = new MojoDescriptor();
        descriptor.setGoal("run");
        descriptor.setPluginDescriptor(plugin);
        descriptor.setMojoConfiguration(new XmlPlexusConfiguration("configuration"));
        MojoDescriptorCreator descriptors = mock(MojoDescriptorCreator.class);
        when(descriptors.getMojoDescriptor(goal, session, project)).thenReturn(descriptor);
        MojoExecutor mojoExecutor = mock(MojoExecutor.class);
        LifecycleExecutionException failure = new LifecycleExecutionException("goal failed");
        doThrow(failure).when(mojoExecutor).execute(same(session), anyList());

        TaskSegment segment = new TaskSegment(false);
        segment.getTasks().add(new GoalTask(goal));
        try (BuildPlanExecutor.BuildContext context = newDirectGoalExecutor(mojoExecutor, descriptors)
        .new BuildContext(session, newReactorContext(session), List.of(segment))) {
            BuildStep goalStep = context.plan.requiredStep(project, goal);
            assertTrue(goalStep.isSuccessorOf(context.plan.requiredStep(project, BuildStep.SETUP)));
            assertTrue(
                    context.plan.requiredStep(project, BuildStep.TEARDOWN).isSuccessorOf(goalStep),
                    "project teardown must wait for the direct goal");
            context.execute();
        }

        verify(mojoExecutor).execute(same(session), anyList());
        assertEquals(List.of(failure), session.getResult().getExceptions());
        assertTrue(session.getResult().getBuildSummary(project) instanceof org.apache.maven.execution.BuildFailure);
    }

    private BuildPlanExecutor newDirectGoalExecutor(MojoExecutor mojoExecutor, MojoDescriptorCreator descriptors) {
        return new BuildPlanExecutor(
                mojoExecutor,
                new ExecutionEventCatapultStub(),
                List.of(),
                new NoopTransformerManager(),
                new BuildPlanLogger(),
                Map.of("default", (buildProject, execution, usePluginConfig) -> {}),
                null,
                descriptors,
                new DefaultLifecycleRegistry(Collections.emptyList()));
    }

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

    /**
     * A checked exception thrown by a build step is a soft failure: the reactor must not be halted, and
     * other projects must still be built when {@code REACTOR_FAIL_AT_END} is in effect. This pins the
     * soft-failure path of {@code isFatal} so a future change does not silently break {@code --fail-at-end}
     * for ordinary plugin failures.
     */
    @Test
    void checkedExceptionThrownByBuildStepDoesNotHaltReactor() throws Exception {
        MavenProject project = newProject();
        MavenSession session = newSession(project);
        session.getRequest().setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_AT_END);

        ReactorContext reactorContext = execute(session, project, event -> {}, plan -> {
            BuildStep step = plan.step(project, "validate")
                    .orElseThrow(() -> new IllegalStateException("no validate step in the plan"));
            // LifecycleExecutionException is a checked exception — the soft-failure case for isFatal.
            step.exception = new LifecycleExecutionException("plugin failure");
            step.status.set(BuildStep.FAILED);
        });

        assertTrue(
                session.getResult().getBuildSummary(project) instanceof org.apache.maven.execution.BuildFailure,
                "the project must be recorded as a failure");
        assertTrue(
                !reactorContext.getReactorBuildStatus().isHalted(),
                "a checked exception must not halt the reactor when REACTOR_FAIL_AT_END is set");
    }

    /**
     * A project can end up with more than one failure: when a build step fails, the matching after:* step is
     * still run for cleanup and may fail on its own. Those failures are reported through a wrapper exception,
     * and the wrapper is a checked exception, so reading the severity off the wrapper hides the {@link Error}
     * that is inside it. A cleanup step failing after an Error must not downgrade the build from "halt" to a
     * plain per-project failure.
     */
    @Test
    void errorIsStillFatalWhenASecondFailureJoinsIt() throws Exception {
        Error thrown = new NoClassDefFoundError("some/Class");
        MavenProject project = newProject();
        MavenSession session = newSession(project);
        // with fail-fast the build halts whatever happens, so the downgrade is only observable with fail-at-end
        session.getRequest().setReactorFailureBehavior(MavenExecutionRequest.REACTOR_FAIL_AT_END);

        ReactorContext reactorContext = execute(
                session,
                project,
                event -> {
                    throw thrown;
                },
                plan -> {
                    BuildStep cleanup = plan.step(project, Lifecycle.AFTER + "validate")
                            .orElseThrow(() -> new IllegalStateException("no after:validate step in the plan"));
                    // stands in for the cleanup step failing after the Error, the way processStep records it
                    cleanup.exception = new LifecycleExecutionException("cleanup failed");
                });

        assertTrue(
                reactorContext.getReactorBuildStatus().isHalted(),
                "an Error must halt the reactor even when a second failure is recorded for the same project, but"
                        + " the build was not halted; recorded exceptions: "
                        + session.getResult().getExceptions());
    }

    /**
     * When a build step (e.g. {@code test}) fails, the matching {@code after:test} step is run for
     * cleanup and ends up with status {@code SKIPPED}. Steps that come after it in the lifecycle
     * (e.g. {@code after:unit-test}, {@code after:verify}, {@code install}, {@code deploy}) must
     * also be skipped: they must not run as if the build had succeeded.
     *
     * <p>This is the scenario reported in
     * <a href="https://github.com/apache/maven/issues/14114">#14114</a>: with the concurrent builder,
     * {@code install} and {@code deploy} ran even when {@code test} failed, because the {@code after:*}
     * cleanup chain converted a {@code FAILED} predecessor into a {@code SKIPPED} predecessor, and the
     * subsequent {@code shouldExecute} check only guarded against {@code FAILED}, not {@code SKIPPED}.
     */
    @Test
    void afterStepSkippedStatusPropagatesAcrossNestedPhases() throws Exception {
        MavenProject project = newProject();
        MavenSession session = newSession(project);

        ReactorContext reactorContext = newReactorContext(session);
        try (BuildPlanExecutor.BuildContext context = newExecutor((BeforeProjectExecution) event -> {})
        .new BuildContext(session, reactorContext, List.of(newTaskSegment("deploy")))) {

            BuildStep testStep = context.plan
                    .step(project, "test")
                    .orElseThrow(() -> new IllegalStateException("no test step in the deploy plan"));
            testStep.exception = new LifecycleExecutionException("test failure");
            testStep.status.set(BuildStep.FAILED);

            context.execute();

            // after:test ran for cleanup and must be SKIPPED (not EXECUTED)
            int afterTestStatus = context.plan
                    .step(project, Lifecycle.AFTER + "test")
                    .map(s -> s.status.get())
                    .orElse(-1);
            assertEquals(
                    BuildStep.SKIPPED, afterTestStatus, "after:test must be SKIPPED (ran for cleanup) when test fails");

            // install must NOT have executed
            int installStatus = context.plan
                    .step(project, "install")
                    .map(s -> s.status.get())
                    .orElse(-1);
            assertNotEquals(
                    BuildStep.EXECUTED,
                    installStatus,
                    "install must not execute when a predecessor step (test) has failed");

            // deploy must NOT have executed
            int deployStatus = context.plan
                    .step(project, "deploy")
                    .map(s -> s.status.get())
                    .orElse(-1);
            assertNotEquals(
                    BuildStep.EXECUTED,
                    deployStatus,
                    "deploy must not execute when a predecessor step (test) has failed");
        }

        assertTrue(
                session.getResult().getBuildSummary(project) instanceof org.apache.maven.execution.BuildFailure,
                "the project must be recorded as a failure when test fails");
    }

    private ReactorContext execute(MavenSession session, MavenProject project, BeforeProjectExecution listener)
            throws Exception {
        ReactorContext reactorContext = newReactorContext(session);
        newExecutor(listener).execute(session, reactorContext, List.of(newTaskSegment()));
        return reactorContext;
    }

    /**
     * Same, but the plan is handed to {@code planCustomizer} after it is created and before it is executed.
     * A step that runs no mojo cannot be made to fail from the outside, so this is the only way to put a
     * second failure on the project.
     */
    private ReactorContext execute(
            MavenSession session,
            MavenProject project,
            BeforeProjectExecution listener,
            Consumer<BuildPlan> planCustomizer)
            throws Exception {
        return execute(session, project, listener, planCustomizer, "validate");
    }

    private ReactorContext execute(
            MavenSession session,
            MavenProject project,
            BeforeProjectExecution listener,
            Consumer<BuildPlan> planCustomizer,
            String phase)
            throws Exception {
        ReactorContext reactorContext = newReactorContext(session);
        try (BuildPlanExecutor.BuildContext context =
                newExecutor(listener).new BuildContext(session, reactorContext, List.of(newTaskSegment(phase)))) {
            planCustomizer.accept(context.plan);
            context.execute();
        }
        return reactorContext;
    }

    private ReactorContext newReactorContext(MavenSession session) {
        return new ReactorContext(
                session.getResult(),
                Thread.currentThread().getContextClassLoader(),
                new ReactorBuildStatus(session.getProjectDependencyGraph()));
    }

    private TaskSegment newTaskSegment() {
        return newTaskSegment("validate");
    }

    private TaskSegment newTaskSegment(String phase) {
        TaskSegment taskSegment = new TaskSegment(false);
        taskSegment.getTasks().add(new LifecycleTask(phase));
        return taskSegment;
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
