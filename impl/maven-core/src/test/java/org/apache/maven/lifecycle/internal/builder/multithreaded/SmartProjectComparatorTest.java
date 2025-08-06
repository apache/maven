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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.lifecycle.internal.stub.ProjectDependencyGraphStub;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for SmartProjectComparator to verify critical path scheduling logic.
 */
class SmartProjectComparatorTest {

    private SmartProjectComparator comparator;
    private ProjectDependencyGraph dependencyGraph;

    @BeforeEach
    void setUp() {
        dependencyGraph = new ProjectDependencyGraphStub();
        comparator = new SmartProjectComparator(dependencyGraph);
    }

    @Test
    void testProjectWeightCalculation() {
        // Test that projects with longer downstream chains get higher weights
        // Graph: A -> B,C; B -> X,Y; C -> X,Z
        MavenProject projectA = ProjectDependencyGraphStub.A;
        MavenProject projectB = ProjectDependencyGraphStub.B;
        MavenProject projectC = ProjectDependencyGraphStub.C;
        MavenProject projectX = ProjectDependencyGraphStub.X;

        long weightA = comparator.getProjectWeight(projectA);
        long weightB = comparator.getProjectWeight(projectB);
        long weightC = comparator.getProjectWeight(projectC);
        long weightX = comparator.getProjectWeight(projectX);

        // Project A should have the highest weight as it's at the root
        assertTrue(weightA > weightB, "Project A should have weight > Project B");
        assertTrue(weightA > weightC, "Project A should have weight > Project C");
        assertTrue(weightB > weightX, "Project B should have weight > Project X");
        assertTrue(weightC > weightX, "Project C should have weight > Project X");
    }

    @Test
    void testComparatorOrdering() {
        List<MavenProject> projects = Arrays.asList(
                ProjectDependencyGraphStub.X,
                ProjectDependencyGraphStub.C,
                ProjectDependencyGraphStub.A,
                ProjectDependencyGraphStub.B);

        // Sort using the comparator
        projects.sort(comparator.getComparator());

        // Project A should come first (highest weight)
        assertEquals(
                ProjectDependencyGraphStub.A,
                projects.get(0),
                "Project A should be first (highest critical path weight)");

        // B and C should come before X (they have higher weights)
        assertTrue(
                projects.indexOf(ProjectDependencyGraphStub.B) < projects.indexOf(ProjectDependencyGraphStub.X),
                "Project B should come before X");
        assertTrue(
                projects.indexOf(ProjectDependencyGraphStub.C) < projects.indexOf(ProjectDependencyGraphStub.X),
                "Project C should come before X");
    }

    @Test
    void testWeightConsistency() {
        // Test that weights are consistent across multiple calls
        MavenProject project = ProjectDependencyGraphStub.A;

        long weight1 = comparator.getProjectWeight(project);
        long weight2 = comparator.getProjectWeight(project);

        assertEquals(weight1, weight2, "Project weight should be consistent");
    }

    @Test
    void testDependencyChainLength() {
        // Test that projects with longer dependency chains get higher weights
        // In the stub: A -> B,C; B -> X,Y; C -> X,Z
        long weightA = comparator.getProjectWeight(ProjectDependencyGraphStub.A);
        long weightB = comparator.getProjectWeight(ProjectDependencyGraphStub.B);
        long weightC = comparator.getProjectWeight(ProjectDependencyGraphStub.C);
        long weightX = comparator.getProjectWeight(ProjectDependencyGraphStub.X);
        long weightY = comparator.getProjectWeight(ProjectDependencyGraphStub.Y);
        long weightZ = comparator.getProjectWeight(ProjectDependencyGraphStub.Z);

        // Verify the actual chain length calculation
        // Leaf nodes (no downstream dependencies)
        assertEquals(1L, weightX, "Project X should have weight 1 (1 + 0)");
        assertEquals(1L, weightY, "Project Y should have weight 1 (1 + 0)");
        assertEquals(1L, weightZ, "Project Z should have weight 1 (1 + 0)");

        // Middle nodes
        assertEquals(2L, weightB, "Project B should have weight 2 (1 + max(X=1, Y=1))");
        assertEquals(2L, weightC, "Project C should have weight 2 (1 + max(X=1, Z=1))");

        // Root node
        assertEquals(3L, weightA, "Project A should have weight 3 (1 + max(B=2, C=2))");
    }

    @Test
    void testSameWeightOrdering() {
        // Test that projects with the same weight are ordered by project ID
        // Projects B and C both have weight 2, so they should be ordered by project ID
        List<MavenProject> projects = Arrays.asList(
                ProjectDependencyGraphStub.C, // weight=2, ID contains "C"
                ProjectDependencyGraphStub.B // weight=2, ID contains "B"
                );

        projects.sort(comparator.getComparator());

        // Both have same weight (2), so ordering should be by project ID
        // Project B should come before C alphabetically by project ID
        assertEquals(
                ProjectDependencyGraphStub.B,
                projects.get(0),
                "Project B should come before C when they have the same weight (ordered by project ID)");
        assertEquals(
                ProjectDependencyGraphStub.C,
                projects.get(1),
                "Project C should come after B when they have the same weight (ordered by project ID)");

        // Verify they actually have the same weight
        long weightB = comparator.getProjectWeight(ProjectDependencyGraphStub.B);
        long weightC = comparator.getProjectWeight(ProjectDependencyGraphStub.C);
        assertEquals(weightB, weightC, "Projects B and C should have the same weight");
    }

    @Test
    void testConcurrentWeightCalculation() throws Exception {
        // Test that concurrent weight calculation doesn't cause recursive update issues
        // This test simulates the scenario that causes the IllegalStateException

        int numThreads = 10;
        int numIterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicReference<Exception> exception = new AtomicReference<>();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < numIterations; j++) {
                        // Simulate concurrent access to weight calculation
                        // This can trigger the recursive update issue
                        List<MavenProject> projects = Arrays.asList(
                                ProjectDependencyGraphStub.A,
                                ProjectDependencyGraphStub.B,
                                ProjectDependencyGraphStub.C,
                                ProjectDependencyGraphStub.X,
                                ProjectDependencyGraphStub.Y,
                                ProjectDependencyGraphStub.Z);

                        // Sort projects concurrently - this triggers weight calculation
                        projects.sort(comparator.getComparator());

                        // Also directly access weights to increase contention
                        for (MavenProject project : projects) {
                            comparator.getProjectWeight(project);
                        }
                    }
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        if (exception.get() != null) {
            throw exception.get();
        }
    }
}
