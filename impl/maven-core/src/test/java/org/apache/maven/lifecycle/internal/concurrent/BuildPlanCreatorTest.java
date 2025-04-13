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
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

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
