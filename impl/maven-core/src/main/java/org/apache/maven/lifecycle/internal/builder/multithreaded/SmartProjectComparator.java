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
package org.apache.maven.lifecycle.internal.builder.multithreaded;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToLongFunction;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.project.MavenProject;

/**
 * Smart project comparator that orders projects based on critical path analysis.
 * Projects with longer downstream dependency chains are prioritized to maximize
 * parallel execution efficiency.
 *
 * <p>The algorithm calculates a weight for each project as:
 * weight = 1 + max(downstream_project_weights)
 *
 * <p>Projects are then sorted by weight in descending order, ensuring that
 * projects with longer dependency chains are built first. When projects have
 * the same weight, they are ordered by project ID for deterministic results.
 *
 * <p><b>Example:</b>
 * <p>Consider projects with dependencies: A → B → D, A → C → D
 * <ul>
 * <li>Project D: weight = 1 (no downstream dependencies)</li>
 * <li>Project B: weight = 2 (1 + max(D=1))</li>
 * <li>Project C: weight = 2 (1 + max(D=1))</li>
 * <li>Project A: weight = 3 (1 + max(B=2, C=2))</li>
 * </ul>
 * <p>Build order: A (weight=3), then B and C (weight=2, ordered by project ID), then D (weight=1)
 * <p>If projects have identical weights and IDs, the order is deterministic but may not preserve
 * the original declaration order.
 *
 * @since 4.0.0
 */
public class SmartProjectComparator {

    private final ProjectDependencyGraph dependencyGraph;
    private final Map<MavenProject, Long> projectWeights;
    private final Comparator<MavenProject> comparator;

    public SmartProjectComparator(ProjectDependencyGraph dependencyGraph) {
        this.dependencyGraph = dependencyGraph;
        this.projectWeights = new ConcurrentHashMap<>();
        this.comparator = createComparator();
    }

    /**
     * Gets the comparator for ordering projects by critical path priority.
     *
     * @return comparator that orders projects with longer dependency chains first
     */
    public Comparator<MavenProject> getComparator() {
        return comparator;
    }

    /**
     * Gets the calculated weight for a project, representing its dependency chain length.
     *
     * @param project the project
     * @return the project's weight (higher means longer dependency chain)
     */
    public long getProjectWeight(MavenProject project) {
        // First check if weight is already calculated
        Long existingWeight = projectWeights.get(project);
        if (existingWeight != null) {
            return existingWeight;
        }

        // Calculate weight without using computeIfAbsent to avoid recursive update issues
        long weight = calculateWeight(project);

        // Use putIfAbsent to handle concurrent access safely
        Long previousWeight = projectWeights.putIfAbsent(project, weight);
        return previousWeight != null ? previousWeight : weight;
    }

    private Comparator<MavenProject> createComparator() {
        return Comparator.comparingLong((ToLongFunction<MavenProject>) this::getProjectWeight)
                .reversed() // Higher weights first
                .thenComparing(this::getProjectId); // Secondary sort for deterministic ordering
    }

    private long calculateWeight(MavenProject project) {
        // Calculate maximum weight of downstream dependencies
        long maxDownstreamWeight = dependencyGraph.getDownstreamProjects(project, false).stream()
                .mapToLong(this::getProjectWeight)
                .max()
                .orElse(0L);

        // Weight = 1 + max downstream weight (similar to Takari Smart Builder)
        return 1L + maxDownstreamWeight;
    }

    private String getProjectId(MavenProject project) {
        return project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
    }
}
