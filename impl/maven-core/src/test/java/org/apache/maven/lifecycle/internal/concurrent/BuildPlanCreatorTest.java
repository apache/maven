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
     * Tests that {@code filterByScope} expands scopes to match Maven dependency resolution semantics.
     * "compile" includes compile, provided, and system scoped dependencies (plus null-scoped which
     * defaults to compile). This ensures provided-scope reactor dependencies are properly ordered in
     * the build plan when needed at compile time.
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

        // "runtime" scope expands to compile + runtime
        List<MavenProject> runtimeFiltered =
                BuildPlanExecutor.BuildContext.filterByScope(consumer, upstream, "runtime");
        assertEquals(3, runtimeFiltered.size());
        assertTrue(runtimeFiltered.contains(compileDep));
        assertTrue(runtimeFiltered.contains(runtimeDep));
        assertTrue(runtimeFiltered.contains(nullScopeDep));

        // "test" scope expands to all scopes
        List<MavenProject> testFiltered = BuildPlanExecutor.BuildContext.filterByScope(consumer, upstream, "test");
        assertEquals(6, testFiltered.size());

        // "compile+runtime" scope expands to compile + provided + system + runtime (and null-scope)
        List<MavenProject> compileRuntimeFiltered =
                BuildPlanExecutor.BuildContext.filterByScope(consumer, upstream, "compile+runtime");
        assertEquals(5, compileRuntimeFiltered.size());
        assertTrue(compileRuntimeFiltered.contains(compileDep));
        assertTrue(compileRuntimeFiltered.contains(providedDep));
        assertTrue(compileRuntimeFiltered.contains(systemDep));
        assertTrue(compileRuntimeFiltered.contains(runtimeDep));
        assertTrue(compileRuntimeFiltered.contains(nullScopeDep));

        // "runtime+system" scope expands to compile + system + runtime (and null-scope)
        List<MavenProject> runtimeSystemFiltered =
                BuildPlanExecutor.BuildContext.filterByScope(consumer, upstream, "runtime+system");
        assertEquals(4, runtimeSystemFiltered.size());
        assertTrue(runtimeSystemFiltered.contains(compileDep));
        assertTrue(runtimeSystemFiltered.contains(systemDep));
        assertTrue(runtimeSystemFiltered.contains(runtimeDep));
        assertTrue(runtimeSystemFiltered.contains(nullScopeDep));

        // "test-only" scope matches only test-scoped dependencies
        List<MavenProject> testOnlyFiltered =
                BuildPlanExecutor.BuildContext.filterByScope(consumer, upstream, "test-only");
        assertEquals(1, testOnlyFiltered.size());
        assertTrue(testOnlyFiltered.contains(testDep));

        // unknown scope (e.g. "import") matches only dependencies with that exact scope — none here
        List<MavenProject> importFiltered = BuildPlanExecutor.BuildContext.filterByScope(consumer, upstream, "import");
        assertEquals(0, importFiltered.size());
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
}
