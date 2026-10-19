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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;

public class BuildPlan {

    private final Map<MavenProject, Map<String, BuildStep>> plan = new LinkedHashMap<>();
    private final Map<MavenProject, List<MavenProject>> projects;
    private final Map<String, String> aliases = new HashMap<>();
    private volatile Set<String> duplicateIds;
    private volatile List<BuildStep> sortedNodes;

    BuildPlan() {
        this.projects = null;
    }

    public BuildPlan(Map<MavenProject, List<MavenProject>> projects) {
        this.projects = projects;
    }

    public Map<MavenProject, List<MavenProject>> getAllProjects() {
        return projects;
    }

    public Map<String, String> aliases() {
        return aliases;
    }

    public Stream<MavenProject> projects() {
        return plan.keySet().stream();
    }

    public void addProject(MavenProject project, Map<String, BuildStep> steps) {
        plan.put(project, steps);
    }

    public void addStep(MavenProject project, String name, BuildStep step) {
        plan.get(project).put(name, step);
    }

    public Stream<BuildStep> allSteps() {
        return plan.values().stream().flatMap(m -> m.values().stream());
    }

    public Stream<BuildStep> steps(MavenProject project) {
        return Optional.ofNullable(plan.get(project))
                .map(m -> m.values().stream())
                .orElse(Stream.empty());
    }

    public Optional<BuildStep> step(MavenProject project, String name) {
        return Optional.ofNullable(plan.get(project)).map(m -> m.get(name));
    }

    public BuildStep requiredStep(MavenProject project, String name) {
        return step(project, name).orElseThrow(() -> new NoSuchElementException("Step " + name + " not found"));
    }

    // add a follow-up plan to this one
    public void then(BuildPlan step) {
        step.plan.forEach((k, v) -> plan.merge(k, v, this::merge));
        aliases.putAll(step.aliases);
    }

    private Map<String, BuildStep> merge(Map<String, BuildStep> org, Map<String, BuildStep> add) {
        // When add contains the infrastructure $plan$ step, this merge is from
        // computeForkPlan (execution plan merged into a fork). In that case, use the
        // original terminal-step chaining to avoid creating cycles with the frontier approach.
        boolean isSegmentMerge = !add.containsKey(BuildStep.PLAN);

        if (isSegmentMerge) {
            // Track which steps are currently skipped before the merge
            Set<String> skippedInOrg = org.entrySet().stream()
                    .filter(e -> e.getValue().skip.get())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            // The "active frontier" consists of non-skipped steps that have at least one
            // skipped same-project successor. This is the boundary between the active and
            // skipped regions — the right insertion point for new non-lifecycle steps
            // (e.g., aggregator goals). Using the frontier avoids creating transitive
            // dependencies on all lifecycle phases through wrapper steps like after:ALL.
            List<BuildStep> activeFrontier = org.values().stream()
                    .filter(b -> !b.skip.get())
                    .filter(b -> b.successors.stream().anyMatch(s -> s.project == b.project && s.skip.get()))
                    .toList();
            if (activeFrontier.isEmpty()) {
                activeFrontier = org.values().stream()
                        .filter(b -> !b.skip.get())
                        .filter(b -> b.successors.isEmpty())
                        .toList();
            }

            // Chain truly new steps (not in org at all) after the active frontier
            List<BuildStep> insertionPoints = activeFrontier;
            List<BuildStep> firsts =
                    add.values().stream().filter(b -> b.predecessors.isEmpty()).toList();
            firsts.stream()
                    .filter(addNode -> !org.containsKey(addNode.name))
                    .forEach(addNode -> insertionPoints.forEach(orgNode -> addNode.executeAfter(orgNode)));

            // Merge all steps by name
            add.forEach((name, node) -> org.merge(name, node, this::merge));

            // Chain newly-activated entry points after non-lifecycle goal steps.
            // When a follow-up plan un-skips phases beyond the existing plan, those
            // newly-active phases must wait for any non-lifecycle steps (e.g., aggregator
            // goals) that were inserted at the frontier.
            List<BuildStep> goalLasts = org.values().stream()
                    .filter(b -> b.successors.stream().noneMatch(s -> s.project == b.project))
                    .filter(b -> !b.skip.get())
                    .filter(b -> b.phase == null)
                    .toList();
            if (!goalLasts.isEmpty()) {
                org.values().stream()
                        .filter(step -> skippedInOrg.contains(step.name))
                        .filter(step -> !step.skip.get())
                        .filter(step -> step.predecessors.stream()
                                .filter(p -> p.project == step.project)
                                .allMatch(p -> !skippedInOrg.contains(p.name)))
                        .forEach(step -> goalLasts.forEach(step::executeAfter));
            }
        } else {
            // computeForkPlan path: use simple terminal-step chaining
            List<BuildStep> lasts =
                    org.values().stream().filter(b -> b.successors.isEmpty()).toList();
            List<BuildStep> firsts =
                    add.values().stream().filter(b -> b.predecessors.isEmpty()).toList();
            firsts.stream()
                    .filter(addNode -> !org.containsKey(addNode.name))
                    .forEach(addNode -> lasts.forEach(orgNode -> addNode.executeAfter(orgNode)));
            add.forEach((name, node) -> org.merge(name, node, this::merge));
        }

        return org;
    }

    private BuildStep merge(BuildStep node1, BuildStep node2) {
        node1.predecessors.addAll(node2.predecessors);
        node1.successors.addAll(node2.successors);
        node2.mojos.forEach((k, v) -> node1.mojos.merge(k, v, this::mergeMojos));
        // Un-skip the surviving step if the merged-in step is active.
        // Without this, plan() silently drops mojos via addMojo()'s skip guard.
        if (!node2.skip.get()) {
            node1.skip.set(false);
        }
        return node1;
    }

    private Map<String, MojoExecution> mergeMojos(Map<String, MojoExecution> l1, Map<String, MojoExecution> l2) {
        l2.forEach(l1::putIfAbsent);
        return l1;
    }

    // gather artifactIds which are not unique so that the respective thread names can be extended with the groupId
    public Set<String> duplicateIds() {
        if (duplicateIds == null) {
            synchronized (this) {
                if (duplicateIds == null) {
                    duplicateIds = projects()
                            .map(MavenProject::getArtifactId)
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                            .entrySet()
                            .stream()
                            .filter(p -> p.getValue() > 1)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toSet());
                }
            }
        }
        return duplicateIds;
    }

    public List<BuildStep> sortedNodes() {
        if (sortedNodes == null) {
            synchronized (this) {
                if (sortedNodes == null) {
                    List<BuildStep> sortedNodes = new ArrayList<>();
                    Set<BuildStep> visited = new HashSet<>();
                    // Visit each unvisited node
                    allSteps().forEach(node -> visitNode(node, visited, sortedNodes));
                    // Reverse the sorted nodes to get the correct order
                    Collections.reverse(sortedNodes);
                    this.sortedNodes = sortedNodes;
                }
            }
        }
        return sortedNodes;
    }

    // Helper method to visit a node
    private static void visitNode(BuildStep node, Set<BuildStep> visited, List<BuildStep> sortedNodes) {
        if (visited.add(node)) {
            // For each successor of the current node, visit unvisited successors
            node.successors.forEach(successor -> visitNode(successor, visited, sortedNodes));
            sortedNodes.add(node);
        }
    }
}
