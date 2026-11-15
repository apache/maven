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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.maven.internal.impl.DefaultLifecycleRegistry;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.Lifecycle.AFTER;
import static org.apache.maven.api.Lifecycle.BEFORE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildPlanCreatorTest {

    @Test
    void testMulti() {
        MavenProject project = new MavenProject();
        project.setCollectedProjects(List.of());
        Map<MavenProject, List<MavenProject>> projects = Collections.singletonMap(project, Collections.emptyList());

        BuildPlan plan = calculateLifecycleMappings(projects, "package");

        new BuildPlanLogger().writePlan(System.out::println, plan);
    }

    @Test
    void testCondense() {
        MavenProject p1 = new MavenProject();
        p1.setCollectedProjects(List.of());
        p1.setArtifactId("p1");
        MavenProject p2 = new MavenProject();
        p2.setCollectedProjects(List.of());
        p2.setArtifactId("p2");
        Map<MavenProject, List<MavenProject>> projects = new HashMap<>();
        projects.put(p1, Collections.emptyList());
        projects.put(p2, Collections.singletonList(p1));

        BuildPlan plan = calculateLifecycleMappings(projects, "verify");
        plan.then(calculateLifecycleMappings(projects, "install"));

        Stream.of(p1, p2).forEach(project -> {
            plan.requiredStep(project, "after:resources").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "after:test-resources").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "compile").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "test-compile").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "test").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "package").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "install").addMojo(new MojoExecution(null), 0);
        });

        new BuildPlanLogger() {
            @Override
            protected void mojo(Consumer<String> writer, MojoExecution mojoExecution) {}
        }.writePlan(System.out::println, plan);

        plan.allSteps().forEach(phase -> {
            phase.predecessors.forEach(
                    pred -> assertTrue(plan.step(pred.project, pred.name).isPresent(), "Phase not present: " + pred));
        });
    }

    /**
     * Regression test for <a href="https://github.com/apache/maven/issues/13529">#13529</a>:
     * merging overlapping lifecycle plans (e.g., {@code package} then {@code install})
     * must un-skip the non-overlapping phases so that mojos can be bound during planning.
     */
    @Test
    void testMergeUnskipsNonOverlappingPhases() {
        MavenProject project = new MavenProject();
        project.setCollectedProjects(List.of());
        project.setArtifactId("test");
        Map<MavenProject, List<MavenProject>> projects = Collections.singletonMap(project, Collections.emptyList());

        BuildPlan plan = calculateLifecycleMappings(projects, "package");

        // Verify install is skipped in the package plan
        assertTrue(plan.requiredStep(project, "install").skip.get());

        plan.then(calculateLifecycleMappings(projects, "install"));

        // Verify install-related steps are NOT skipped after merge
        for (String stepName : List.of("before:install", "install", "after:install")) {
            assertFalse(
                    plan.requiredStep(project, stepName).skip.get(),
                    "Step " + stepName + " should not be skipped after merging package+install plans");
        }

        // Verify install is reachable from package
        assertTrue(
                plan.requiredStep(project, "install").isSuccessorOf(plan.requiredStep(project, "after:package")),
                "install must be a successor of package");

        // Verify all predecessor references point to steps actually in the plan
        plan.allSteps()
                .forEach(step -> step.predecessors.forEach(pred -> assertTrue(
                        plan.step(pred.project, pred.name).isPresent(), "Predecessor not in plan: " + pred)));
    }

    /**
     * Simulates {@code package javadoc:aggregate-jar install} with three task segments.
     * The aggregator goal must run after package but before install, without creating
     * cycles in the build plan.
     */
    @Test
    void testMergeThreeSegmentsWithAggregatorGoal() {
        MavenProject root = new MavenProject();
        root.setCollectedProjects(List.of());
        root.setArtifactId("root");
        MavenProject child = new MavenProject();
        child.setCollectedProjects(List.of());
        child.setArtifactId("child");
        Map<MavenProject, List<MavenProject>> allProjects = new HashMap<>();
        allProjects.put(root, Collections.emptyList());
        allProjects.put(child, Collections.singletonList(root));

        // Segment 1: package (all projects)
        BuildPlan plan = new BuildPlan(allProjects);
        plan.then(calculateLifecycleMappings(allProjects, "package"));

        // Segment 2: aggregator goal (root only)
        BuildPlan goalPlan = new BuildPlan();
        BuildStep goalStep = new BuildStep("javadoc:aggregate-jar", root, null);
        Map<String, BuildStep> goalSteps = new HashMap<>();
        goalSteps.put("javadoc:aggregate-jar", goalStep);
        goalPlan.addProject(root, goalSteps);
        plan.then(goalPlan);

        // Segment 3: install (all projects)
        plan.then(calculateLifecycleMappings(allProjects, "install"));

        // Verify: all predecessor references point to plan objects (no dangling)
        plan.allSteps()
                .forEach(step -> step.predecessors.forEach(pred -> assertTrue(
                        plan.step(pred.project, pred.name).isPresent(), "Predecessor not in plan: " + pred)));

        // Verify: install is not skipped for both projects
        for (MavenProject p : List.of(root, child)) {
            assertFalse(
                    plan.requiredStep(p, "install").skip.get(),
                    "install should not be skipped for " + p.getArtifactId());
        }

        // Verify: no cycle — javadoc must NOT be a successor of install
        BuildStep jdocStep = plan.requiredStep(root, "javadoc:aggregate-jar");
        BuildStep installStep = plan.requiredStep(root, "install");
        assertFalse(
                jdocStep.isSuccessorOf(installStep), "javadoc must not be a successor of install (would create cycle)");

        // Verify: javadoc runs after package
        assertTrue(
                jdocStep.isSuccessorOf(plan.requiredStep(root, "after:package")),
                "javadoc must be a successor of package");

        // Verify: install runs after javadoc
        assertTrue(installStep.isSuccessorOf(jdocStep), "install must be a successor of javadoc:aggregate-jar");
    }

    @Test
    void testAlias() {
        MavenProject p1 = new MavenProject();
        p1.setArtifactId("p1");
        p1.setCollectedProjects(List.of());
        Map<MavenProject, List<MavenProject>> projects = Collections.singletonMap(p1, Collections.emptyList());

        BuildPlan plan = calculateLifecycleMappings(projects, "generate-resources");
        assertNotNull(plan);
    }

    @Test
    void testAllPhase() {
        MavenProject c1 = new MavenProject();
        c1.setArtifactId("c1");
        c1.setCollectedProjects(List.of());
        MavenProject c2 = new MavenProject();
        c2.setArtifactId("c2");
        c2.setCollectedProjects(List.of());
        MavenProject p = new MavenProject();
        p.setArtifactId("p");
        p.setCollectedProjects(List.of(c1, c2));
        Map<MavenProject, List<MavenProject>> projects = Map.of(p, List.of(), c1, List.of(), c2, List.of());

        BuildPlan plan = calculateLifecycleMappings(projects, "all");
        assertNotNull(plan);
        assertIsSuccessor(plan.requiredStep(p, "before:all"), plan.requiredStep(p, "before:each"));
        assertIsSuccessor(plan.requiredStep(p, "before:all"), plan.requiredStep(c1, "before:all"));
        assertIsSuccessor(plan.requiredStep(p, "before:all"), plan.requiredStep(c2, "before:all"));
        assertIsSuccessor(plan.requiredStep(c1, "after:all"), plan.requiredStep(p, "after:all"));
        assertIsSuccessor(plan.requiredStep(c2, "after:all"), plan.requiredStep(p, "after:all"));
    }

    private void assertIsSuccessor(BuildStep predecessor, BuildStep successor) {
        assertTrue(
                successor.isSuccessorOf(predecessor),
                String.format("Expected '%s' to be a successor of '%s'", successor.toString(), predecessor.toString()));
    }

    @SuppressWarnings("checkstyle:UnusedLocalVariable")
    private BuildPlan calculateLifecycleMappings(Map<MavenProject, List<MavenProject>> projects, String phase) {
        DefaultLifecycleRegistry lifecycles = new DefaultLifecycleRegistry(Collections.emptyList());
        BuildPlanExecutor builder = new BuildPlanExecutor(null, null, null, null, null, null, null, null, lifecycles);
        BuildPlanExecutor.BuildContext context = builder.new BuildContext();
        return context.calculateLifecycleMappings(projects, phase);
    }

    /**
     * Tests that PROJECT-type @After link ordering constraints work correctly.
     * Simulates what {@code applyAfterLinks} does for {@code @After(phase="resources", type=PROJECT)}.
     * <p>
     * This is a real constraint: in the V4 lifecycle, "compile" and "resources" are
     * parallel siblings (compile depends on sources, not resources), so the @After
     * link creates a genuine ordering edge that doesn't exist naturally.
     */
    @Test
    void testAfterLinkProjectOrdering() {
        MavenProject project = new MavenProject();
        project.setCollectedProjects(List.of());
        Map<MavenProject, List<MavenProject>> projects = Collections.singletonMap(project, Collections.emptyList());

        BuildPlan plan = calculateLifecycleMappings(projects, "package");

        // Simulate @After(phase="resources", type=PROJECT) on a mojo bound to "compile"
        // This means: compile's BEFORE step must wait for resources' AFTER step
        // This is a real constraint since compile and resources are parallel in the lifecycle
        BuildStep compileBefore = plan.requiredStep(project, BEFORE + "compile");
        BuildStep resourcesAfter = plan.requiredStep(project, AFTER + "resources");
        compileBefore.executeAfter(resourcesAfter);

        // Verify: compile is now a successor of resources (via the after link)
        assertIsSuccessor(resourcesAfter, compileBefore);
    }

    /**
     * Tests that DEPENDENCIES-type @After link ordering constraints work correctly.
     * Simulates what {@code applyAfterLinks} does for {@code @After(phase="ready", type=DEPENDENCIES)}.
     */
    @Test
    void testAfterLinkDependenciesOrdering() {
        MavenProject p1 = new MavenProject();
        p1.setArtifactId("p1");
        p1.setCollectedProjects(List.of());
        MavenProject p2 = new MavenProject();
        p2.setArtifactId("p2");
        p2.setCollectedProjects(List.of());
        Map<MavenProject, List<MavenProject>> projects = new HashMap<>();
        projects.put(p1, Collections.emptyList());
        projects.put(p2, Collections.singletonList(p1));

        BuildPlan plan = calculateLifecycleMappings(projects, "package");

        // Simulate @After(phase="ready", type=DEPENDENCIES, scope="compile") on p2's compile phase
        // This means: p2's compile BEFORE must wait for p1's ready AFTER
        BuildStep p2CompileBefore = plan.requiredStep(p2, BEFORE + "compile");
        BuildStep p1ReadyAfter = plan.requiredStep(p1, AFTER + "ready");

        // Apply the DEPENDENCIES link (same logic as applyAfterLinks)
        for (MavenProject dep : projects.get(p2)) {
            plan.step(dep, AFTER + "ready").ifPresent(p2CompileBefore::executeAfter);
        }

        // Verify: p2's compile is now a successor of p1's ready
        assertIsSuccessor(p1ReadyAfter, p2CompileBefore);
    }

    /**
     * Tests that CHILDREN-type @After link ordering constraints work correctly.
     * Simulates what {@code applyAfterLinks} does for {@code @After(phase="package", type=CHILDREN)}.
     */
    @Test
    void testAfterLinkChildrenOrdering() {
        MavenProject child = new MavenProject();
        child.setArtifactId("child");
        child.setCollectedProjects(List.of());
        MavenProject parent = new MavenProject();
        parent.setArtifactId("parent");
        parent.setCollectedProjects(List.of(child));
        Map<MavenProject, List<MavenProject>> projects = Map.of(parent, List.of(), child, List.of());

        BuildPlan plan = calculateLifecycleMappings(projects, "install");

        // Simulate @After(phase="package", type=CHILDREN) on parent's install phase
        // This means: parent waits for children's package before its install completes
        BuildStep parentInstallBefore = plan.requiredStep(parent, BEFORE + "install");
        BuildStep parentInstallAfter = plan.requiredStep(parent, AFTER + "install");

        // Apply the CHILDREN link (same logic as applyAfterLinks)
        parent.getCollectedProjects().forEach(c -> {
            plan.step(c, BEFORE + "package").ifPresent(parentInstallBefore::executeBefore);
            plan.step(c, AFTER + "package").ifPresent(parentInstallAfter::executeAfter);
        });

        // Verify: parent's install after waits for child's package after
        BuildStep childPackageAfter = plan.requiredStep(child, AFTER + "package");
        assertIsSuccessor(childPackageAfter, parentInstallAfter);
    }

    /**
     * Tests that {@code filterByScope} returns all upstream projects when scope is null or empty.
     */
    @Test
    void testFilterByScopeNullReturnsAll() {
        MavenProject p1 = createProjectWithId("g", "p1");
        MavenProject p2 = createProjectWithId("g", "p2");
        List<MavenProject> upstream = List.of(p1, p2);

        MavenProject consumer = new MavenProject();
        assertEquals(upstream, BuildPlanExecutor.BuildContext.filterByScope(consumer, upstream, null));
        assertEquals(upstream, BuildPlanExecutor.BuildContext.filterByScope(consumer, upstream, ""));
    }

    /**
     * Tests that {@code filterByScope} expands scopes for build ordering.
     * "compile" includes compile, provided, system, and null-scoped (Maven default) dependencies
     * so that provided/system-scope reactor dependencies are properly ordered.
     */
    @Test
    void testFilterByScopeMatchesExact() {
        MavenProject compileDep = createProjectWithId("g", "compile-dep");
        MavenProject providedDep = createProjectWithId("g", "provided-dep");
        MavenProject systemDep = createProjectWithId("g", "system-dep");
        MavenProject runtimeDep = createProjectWithId("g", "runtime-dep");
        MavenProject testDep = createProjectWithId("g", "test-dep");
        MavenProject nullScopeDep = createProjectWithId("g", "null-scope-dep");
        List<MavenProject> upstream = List.of(compileDep, providedDep, systemDep, runtimeDep, testDep, nullScopeDep);

        MavenProject consumer = new MavenProject();
        consumer.getDependencies().add(createDependency("g", "compile-dep", "compile"));
        consumer.getDependencies().add(createDependency("g", "provided-dep", "provided"));
        consumer.getDependencies().add(createDependency("g", "system-dep", "system"));
        consumer.getDependencies().add(createDependency("g", "runtime-dep", "runtime"));
        consumer.getDependencies().add(createDependency("g", "test-dep", "test"));
        consumer.getDependencies().add(createDependency("g", "null-scope-dep", null));

        // "compile" scope expands to compile + provided + system + null-scoped (Maven default)
        List<MavenProject> compileFiltered =
                BuildPlanExecutor.BuildContext.filterByScope(consumer, upstream, "compile");
        assertEquals(4, compileFiltered.size());
        assertTrue(compileFiltered.contains(compileDep));
        assertTrue(compileFiltered.contains(providedDep));
        assertTrue(compileFiltered.contains(systemDep));
        assertTrue(compileFiltered.contains(nullScopeDep));

        // "runtime" scope expands to compile + runtime + null-scoped
        List<MavenProject> runtimeFiltered =
                BuildPlanExecutor.BuildContext.filterByScope(consumer, upstream, "runtime");
        assertEquals(3, runtimeFiltered.size());
        assertTrue(runtimeFiltered.contains(compileDep));
        assertTrue(runtimeFiltered.contains(runtimeDep));
        assertTrue(runtimeFiltered.contains(nullScopeDep));

        // "test" scope expands to all scopes
        List<MavenProject> testFiltered = BuildPlanExecutor.BuildContext.filterByScope(consumer, upstream, "test");
        assertEquals(6, testFiltered.size());

        // "test-only" scope matches only test-scoped dependencies
        List<MavenProject> testOnlyFiltered =
                BuildPlanExecutor.BuildContext.filterByScope(consumer, upstream, "test-only");
        assertEquals(1, testOnlyFiltered.size());
        assertTrue(testOnlyFiltered.contains(testDep));
    }

    /**
     * Tests that {@code filterByScope} excludes upstream projects not declared as dependencies.
     */
    @Test
    void testFilterByScopeExcludesNonDependencies() {
        MavenProject dep = createProjectWithId("g", "dep");
        MavenProject nonDep = createProjectWithId("g", "non-dep");
        List<MavenProject> upstream = List.of(dep, nonDep);

        MavenProject consumer = new MavenProject();
        consumer.getDependencies().add(createDependency("g", "dep", "compile"));

        List<MavenProject> filtered = BuildPlanExecutor.BuildContext.filterByScope(consumer, upstream, "compile");
        assertEquals(1, filtered.size());
        assertTrue(filtered.contains(dep));
    }

    private static MavenProject createProjectWithId(String groupId, String artifactId) {
        MavenProject project = new MavenProject();
        project.setGroupId(groupId);
        project.setArtifactId(artifactId);
        project.setCollectedProjects(List.of());
        return project;
    }

    private static Dependency createDependency(String groupId, String artifactId, String scope) {
        Dependency dep = new Dependency();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setScope(scope);
        return dep;
    }

    /**
     * Verifies that when the requested phase is before "compile" (e.g. "initialize"),
     * the skip-clearing logic in buildInitialPlan forces the reactor plugin project and
     * all its predecessors to reach READY so that the plugin is available when needed.
     * <p>
     * This test exercises the core invariant directly: calculateLifecycleMappings skips
     * "ready" (and phases above "initialize") for the plugin project; the fix must un-skip
     * "ready" and all its skipped predecessors.
     */
    @Test
    void testReactorPluginSkipClearingPreCompilePhase() {
        MavenProject p1 = new MavenProject();
        p1.setGroupId("g");
        p1.setArtifactId("p1");
        p1.setVersion("1.0");
        p1.setCollectedProjects(List.of());
        Plugin plugin = new Plugin();
        plugin.setGroupId("g");
        plugin.setArtifactId("p2");
        plugin.setVersion("1.0");
        p1.getBuild().addPlugin(plugin);

        MavenProject p2 = new MavenProject();
        p2.setGroupId("g");
        p2.setArtifactId("p2");
        p2.setVersion("1.0");
        p2.setCollectedProjects(List.of());

        Map<MavenProject, List<MavenProject>> projects = new HashMap<>();
        projects.put(p2, Collections.emptyList());
        projects.put(p1, Collections.singletonList(p2));

        // Build a minimal plan to simulate the state after calculateLifecycleMappings("initialize")
        // and after the PLAN steps are added by buildInitialPlan.
        // The key invariant: p2's "ready" step is skipped (initialize < ready), but after
        // the fix it must be un-skipped so the plugin can be loaded.
        BuildPlan plan = new BuildPlan(projects);
        // addProject must be called before addStep to register the project in the plan
        plan.addProject(p1, new java.util.LinkedHashMap<>());
        plan.addProject(p2, new java.util.LinkedHashMap<>());

        // Add PLAN steps (normally added by buildInitialPlan after calculateLifecycleMappings)
        BuildStep planStepP1 = new BuildStep(BuildStep.PLAN, p1, null);
        planStepP1.status.set(BuildStep.PLANNING);
        plan.addStep(p1, BuildStep.PLAN, planStepP1);

        BuildStep planStepP2 = new BuildStep(BuildStep.PLAN, p2, null);
        planStepP2.status.set(BuildStep.PLANNING);
        plan.addStep(p2, BuildStep.PLAN, planStepP2);

        // Add "ready" for p2 and mark it skipped (as calculateLifecycleMappings does
        // when the requested phase "initialize" is before "ready").
        BuildStep readyStepP2 = new BuildStep("ready", p2, null);
        readyStepP2.skip.set(true);
        // Add a predecessor compile step, also skipped
        BuildStep compileStepP2 = new BuildStep("compile", p2, null);
        compileStepP2.skip.set(true);
        readyStepP2.executeAfter(compileStepP2);
        plan.addStep(p2, "ready", readyStepP2);
        plan.addStep(p2, "compile", compileStepP2);

        // Baseline: "ready" on the plugin project IS skipped before the fix.
        assertTrue(
                plan.requiredStep(p2, "ready").skip.get(),
                "Baseline: ready step of plugin project should be skipped when phase is 'initialize'");

        // Apply the fix: wire PLAN(p1) → READY(p2) and un-skip READY(p2) and all its predecessors.
        BuildStep planStep = plan.requiredStep(p1, BuildStep.PLAN);
        BuildStep readyStep = plan.requiredStep(p2, "ready");
        planStep.executeAfter(readyStep);
        readyStep.allPredecessors().filter(s -> s.skip.get()).forEach(s -> s.skip.set(false));

        // After the fix: the READY step of the plugin project (p2) must NOT be skipped.
        assertFalse(
                plan.requiredStep(p2, "ready").skip.get(),
                "READY step of the reactor plugin project must not be skipped after the fix");

        // The compile step (predecessor of ready) must also be un-skipped.
        assertFalse(
                plan.requiredStep(p2, "compile").skip.get(),
                "Predecessor step 'compile' of plugin project must not be skipped");
    }

    /**
     * Verifies that when the reactor plugin project (p2) itself depends on a reactor library (p3),
     * the skip-clearing also un-skips p3's steps, so that p2 can compile against p3.
     * <p>
     * Scenario: library (p3) → plugin (p2) → consumer (p1). Without transitive un-skipping,
     * p3's compile steps remain skipped, causing p2's compilation to fail even after the
     * direct fix for p2.
     */
    @Test
    void testReactorPluginWithLibraryDependencySkipClearing() {
        // p3: reactor library (no special plugins)
        MavenProject p3 = new MavenProject();
        p3.setGroupId("g");
        p3.setArtifactId("p3");
        p3.setVersion("1.0");
        p3.setCollectedProjects(List.of());

        // p2: reactor plugin that compile-depends on the library p3
        MavenProject p2 = new MavenProject();
        p2.setGroupId("g");
        p2.setArtifactId("p2");
        p2.setVersion("1.0");
        p2.setCollectedProjects(List.of());
        p2.getDependencies().add(createDependency("g", "p3", "compile"));

        // p1: consumer that uses the reactor plugin p2
        MavenProject p1 = new MavenProject();
        p1.setGroupId("g");
        p1.setArtifactId("p1");
        p1.setVersion("1.0");
        p1.setCollectedProjects(List.of());
        Plugin plugin = new Plugin();
        plugin.setGroupId("g");
        plugin.setArtifactId("p2");
        plugin.setVersion("1.0");
        p1.getBuild().addPlugin(plugin);

        // Order: p3 (library) built first, then p2 (plugin), then p1 (consumer)
        Map<MavenProject, List<MavenProject>> projects = new LinkedHashMap<>();
        projects.put(p3, Collections.emptyList());
        projects.put(p2, Collections.singletonList(p3));
        projects.put(p1, Collections.singletonList(p2));

        // Build a minimal plan simulating post-calculateLifecycleMappings + PLAN-step state.
        BuildPlan plan = new BuildPlan(projects);
        // addProject must be called before addStep to register each project in the plan
        plan.addProject(p3, new java.util.LinkedHashMap<>());
        plan.addProject(p2, new java.util.LinkedHashMap<>());
        plan.addProject(p1, new java.util.LinkedHashMap<>());

        // PLAN steps for all projects
        for (MavenProject project : projects.keySet()) {
            BuildStep planStep = new BuildStep(BuildStep.PLAN, project, null);
            planStep.status.set(BuildStep.PLANNING);
            plan.addStep(project, BuildStep.PLAN, planStep);
        }

        // p3: "ready" and "compile" both skipped (initialize < compile < ready)
        BuildStep compileStepP3 = new BuildStep("compile", p3, null);
        compileStepP3.skip.set(true);
        BuildStep readyStepP3 = new BuildStep("ready", p3, null);
        readyStepP3.skip.set(true);
        readyStepP3.executeAfter(compileStepP3);
        plan.addStep(p3, "compile", compileStepP3);
        plan.addStep(p3, "ready", readyStepP3);

        // p2: "compile" and "ready" both skipped; "compile" depends on p3's "compile"
        // (simulating the cross-project DEPENDENCIES ordering edge)
        BuildStep compileStepP2 = new BuildStep("compile", p2, null);
        compileStepP2.skip.set(true);
        compileStepP2.executeAfter(compileStepP3); // p2 compiles after p3
        BuildStep readyStepP2 = new BuildStep("ready", p2, null);
        readyStepP2.skip.set(true);
        readyStepP2.executeAfter(compileStepP2);
        // p2's "ready" also depends on p3's "ready" (the library must be ready before the plugin)
        readyStepP2.executeAfter(readyStepP3);
        plan.addStep(p2, "compile", compileStepP2);
        plan.addStep(p2, "ready", readyStepP2);

        // Baseline: "ready" on both plugin and library must be skipped.
        assertTrue(
                plan.requiredStep(p2, "ready").skip.get(),
                "Baseline: ready step of plugin project (p2) should be skipped");
        assertTrue(
                plan.requiredStep(p3, "ready").skip.get(),
                "Baseline: ready step of library project (p3) should be skipped");

        // Apply the fix: wire PLAN(p1) → READY(p2) and un-skip READY(p2) and all its predecessors.
        // allPredecessors() traverses cross-project edges, so p3's skipped steps are also un-skipped.
        BuildStep planStepP1 = plan.requiredStep(p1, BuildStep.PLAN);
        BuildStep readyStep = plan.requiredStep(p2, "ready");
        planStepP1.executeAfter(readyStep);
        readyStep.allPredecessors().filter(s -> s.skip.get()).forEach(s -> s.skip.set(false));

        // After the fix: the plugin project (p2) must be un-skipped all the way to READY.
        assertFalse(
                plan.requiredStep(p2, "ready").skip.get(),
                "READY step of the reactor plugin project (p2) must not be skipped");

        // Also: the library project (p3) that p2 depends on must be un-skipped.
        assertFalse(
                plan.requiredStep(p3, "ready").skip.get(),
                "READY step of the reactor library project (p3) must not be skipped — "
                        + "the plugin (p2) compiles against it and cannot reach READY otherwise");

        // Compile steps of both p2 and p3 must also be un-skipped.
        assertFalse(plan.requiredStep(p2, "compile").skip.get(), "compile step of p2 must not be skipped");
        assertFalse(plan.requiredStep(p3, "compile").skip.get(), "compile step of p3 must not be skipped");
    }

    /*
    @Test
    void testPlugins() {
        DefaultLifecycleRegistry lifecycles =
                new DefaultLifecycleRegistry(Collections.emptyList(), Collections.emptyMap());
        BuildPlanCreator builder = new BuildPlanCreator(null, null, null, null, null, lifecycles);
        MavenProject p1 = new MavenProject();
        p1.setGroupId("g");
        p1.setArtifactId("p1");
        p1.getBuild().getPlugins().add(new Plugin(org.apache.maven.api.model.Plugin.newBuilder()
                .groupId("g").artifactId("p2")
                .
                .build()))
        MavenProject p2 = new MavenProject();
        p2.setGroupId("g");
        p2.setArtifactId("p2");

        Map<MavenProject, List<MavenProject>> projects = new HashMap<>();
        projects.put(p1, Collections.emptyList());
        projects.put(p2, Collections.singletonList(p1));
        Lifecycle lifecycle = lifecycles.require("default");
        BuildPlan plan = builder.calculateLifecycleMappings(null, projects, lifecycle, "verify");
        plan.then(builder.calculateLifecycleMappings(null, projects, lifecycle, "install"));

        Stream.of(p1, p2).forEach(project -> {
            plan.requiredStep(project, "post:resources").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "post:test-resources").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "compile").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "test-compile").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "test").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "package").addMojo(new MojoExecution(null), 0);
            plan.requiredStep(project, "install").addMojo(new MojoExecution(null), 0);
        });

        plan.condense();

        new BuildPlanLogger() {
            @Override
            protected void mojo(Consumer<String> writer, MojoExecution mojoExecution) {}
        }.writePlan(System.out::println, plan);

        plan.allSteps().forEach(phase -> {
            phase.predecessors.forEach(
                    pred -> assertTrue(plan.step(pred.project, pred.name).isPresent(), "Phase not present: " + pred));
        });
    }
     */
}
