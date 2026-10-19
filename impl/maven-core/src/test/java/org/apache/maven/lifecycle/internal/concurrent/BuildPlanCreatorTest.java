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

    @Test
    void testReactorPlugin() {
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
     * Verifies that when the requested phase is before "compile" (e.g. "initialize"),
     * the new skip-clearing logic forces the reactor plugin project and all its
     * predecessors to reach READY so that the plugin is available when needed.
     */
    @Test
    void testReactorPluginPreCompilePhase() {
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
        projects.put(p1, Collections.emptyList());
        projects.put(p2, Collections.singletonList(p1));

        // Use a phase before "compile" — this is the path that triggers the skip-clearing logic
        BuildPlan plan = calculateLifecycleMappings(projects, "initialize");

        // The READY step of the plugin project (p2) must NOT be skipped, even though
        // "initialize" is before "compile" and most of p2's lifecycle steps would normally be skipped.
        assertFalse(
                plan.requiredStep(p2, "READY").skip.get(),
                "READY step of the reactor plugin project must not be skipped");

        // All predecessors of READY within p2 must also not be skipped (they are needed
        // to reach READY: resources, compile, etc.)
        plan.requiredStep(p2, "READY")
                .allPredecessors()
                .filter(s -> s.project == p2)
                .forEach(s -> assertFalse(
                        s.skip.get(), "Predecessor step " + s.name + " of plugin project must not be skipped"));
    }
}
