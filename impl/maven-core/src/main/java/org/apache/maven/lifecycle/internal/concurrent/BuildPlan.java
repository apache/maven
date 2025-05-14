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
        return Optional.ofNullable(plan.get(project)).stream().flatMap(m -> m.values().stream());
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
        // all new phases should be added after the existing ones
        List<BuildStep> lasts =
                org.values().stream().filter(b -> b.successors.isEmpty()).toList();
        List<BuildStep> firsts =
                add.values().stream().filter(b -> b.predecessors.isEmpty()).toList();
        firsts.stream()
                .filter(addNode -> !org.containsKey(addNode.name))
                .forEach(addNode -> lasts.forEach(addNode::executeAfter));
        add.forEach((name, node) -> org.merge(name, node, this::merge));
        return org;
    }

    private BuildStep merge(BuildStep node1, BuildStep node2) {
        node1.predecessors.addAll(node2.predecessors);
        node1.successors.addAll(node2.successors);
        node2.mojos.forEach((k, v) -> node1.mojos.merge(k, v, this::mergeMojos));
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
