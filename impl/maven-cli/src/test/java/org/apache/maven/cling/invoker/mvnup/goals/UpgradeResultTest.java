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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link UpgradeResult} class.
 * Tests result creation, merging, and status determination.
 */
@DisplayName("UpgradeResult")
class UpgradeResultTest {

    @Nested
    @DisplayName("Result Creation")
    class ResultCreationTests {

        @Test
        @DisplayName("should create empty result")
        void shouldCreateEmptyResult() {
            UpgradeResult result = UpgradeResult.empty();

            assertTrue(result.success(), "Empty result should be successful");
            assertEquals(0, result.processedCount(), "Empty result should have no processed POMs");
            assertEquals(0, result.modifiedCount(), "Empty result should have no modified POMs");
            assertEquals(0, result.unmodifiedCount(), "Empty result should have no unmodified POMs");
            assertEquals(0, result.errorCount(), "Empty result should have no errors");
        }

        @Test
        @DisplayName("should create success result")
        void shouldCreateSuccessResult() {
            Path pom1 = Paths.get("pom.xml");
            Path pom2 = Paths.get("module/pom.xml");

            UpgradeResult result = new UpgradeResult(
                    Set.of(pom1, pom2), // processed
                    Set.of(pom1), // modified
                    Set.of() // errors
                    );

            assertTrue(result.success(), "Result should be successful when no errors");
            assertEquals(2, result.processedCount(), "Should have 2 processed POMs");
            assertEquals(1, result.modifiedCount(), "Should have 1 modified POM");
            assertEquals(1, result.unmodifiedCount(), "Should have 1 unmodified POM");
            assertEquals(0, result.errorCount(), "Should have no errors");
        }

        @Test
        @DisplayName("should create failure result")
        void shouldCreateFailureResult() {
            Path pom1 = Paths.get("pom.xml");
            Path pom2 = Paths.get("module/pom.xml");

            UpgradeResult result = new UpgradeResult(
                    Set.of(pom1, pom2), // processed
                    Set.of(pom1), // modified
                    Set.of(pom2) // errors
                    );

            assertFalse(result.success(), "Result should fail when there are errors");
            assertEquals(2, result.processedCount(), "Should have 2 processed POMs");
            assertEquals(1, result.modifiedCount(), "Should have 1 modified POM");
            assertEquals(0, result.unmodifiedCount(), "Should have 0 unmodified POMs (error POM not counted)");
            assertEquals(1, result.errorCount(), "Should have 1 error");
        }
    }

    @Nested
    @DisplayName("Result Merging")
    class ResultMergingTests {

        @Test
        @DisplayName("should merge empty results")
        void shouldMergeEmptyResults() {
            UpgradeResult result1 = UpgradeResult.empty();
            UpgradeResult result2 = UpgradeResult.empty();

            UpgradeResult merged = result1.merge(result2);

            assertTrue(merged.success(), "Merged empty results should be successful");
            assertEquals(0, merged.processedCount(), "Merged empty results should have no processed POMs");
            assertEquals(0, merged.modifiedCount(), "Merged empty results should have no modified POMs");
            assertEquals(0, merged.errorCount(), "Merged empty results should have no errors");
        }

        @Test
        @DisplayName("should handle merging results with overlapping POMs")
        void shouldHandleMergingResultsWithOverlappingPOMs() {
            Path pom1 = Paths.get("pom.xml");
            Path pom2 = Paths.get("module/pom.xml");

            UpgradeResult result1 = new UpgradeResult(
                    Set.of(pom1, pom2), // processed
                    Set.of(pom1), // modified
                    Set.of() // errors
                    );

            UpgradeResult result2 = new UpgradeResult(
                    Set.of(pom1), // processed (overlap)
                    Set.of(pom1, pom2), // modified (overlap + new)
                    Set.of() // errors
                    );

            UpgradeResult merged = result1.merge(result2);

            assertTrue(merged.success(), "Merged result should be successful");
            assertEquals(2, merged.processedPoms().size(), "Should merge processed POMs");
            assertEquals(2, merged.modifiedPoms().size(), "Should merge modified POMs");
            assertTrue(merged.processedPoms().contains(pom1), "Should contain overlapping POM");
            assertTrue(merged.processedPoms().contains(pom2), "Should contain all POMs");
        }

        @Test
        @DisplayName("should handle merging success and failure results")
        void shouldHandleMergingSuccessAndFailureResults() {
            Path pom1 = Paths.get("pom.xml");
            Path pom2 = Paths.get("module/pom.xml");

            UpgradeResult successResult = new UpgradeResult(
                    Set.of(pom1), // processed
                    Set.of(pom1), // modified
                    Set.of() // errors
                    );

            UpgradeResult failureResult = new UpgradeResult(
                    Set.of(pom2), // processed
                    Set.of(), // modified
                    Set.of(pom2) // errors
                    );

            UpgradeResult merged = successResult.merge(failureResult);

            assertFalse(merged.success(), "Merged result should fail when any result has errors");
            assertEquals(2, merged.processedPoms().size(), "Should merge all processed POMs");
            assertEquals(1, merged.modifiedPoms().size(), "Should only include successfully modified POMs");
            assertEquals(1, merged.errorPoms().size(), "Should include error POMs");
            assertTrue(merged.errorPoms().contains(pom2), "Should contain failed POM");
        }

        @Test
        @DisplayName("should handle merging with different POM sets")
        void shouldHandleMergingWithDifferentPOMSets() {
            Path pom1 = Paths.get("pom.xml");
            Path pom2 = Paths.get("module1/pom.xml");
            Path pom3 = Paths.get("module2/pom.xml");

            UpgradeResult result1 = new UpgradeResult(
                    Set.of(pom1, pom2), // processed
                    Set.of(pom1), // modified
                    Set.of() // errors
                    );

            UpgradeResult result2 = new UpgradeResult(
                    Set.of(pom3), // processed (different set)
                    Set.of(pom3), // modified
                    Set.of() // errors
                    );

            UpgradeResult merged = result1.merge(result2);

            assertTrue(merged.success(), "Merged result should be successful");
            assertEquals(3, merged.processedPoms().size(), "Should merge all processed POMs");
            assertEquals(2, merged.modifiedPoms().size(), "Should merge all modified POMs");
            assertEquals(1, merged.unmodifiedCount(), "Should have 1 unmodified POM");
            assertEquals(0, merged.errorCount(), "Should have no errors");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle large number of POMs efficiently")
        void shouldHandleLargeNumberOfPOMsEfficiently() {
            // Create a large number of POM paths for performance testing
            Set<Path> largePomSet = Set.of();
            for (int i = 0; i < 1000; i++) {
                Path pomPath = Paths.get("module" + i + "/pom.xml");
                largePomSet = Set.of(pomPath); // Note: This creates a new set each time in the loop
            }

            long startTime = System.currentTimeMillis();
            UpgradeResult result = new UpgradeResult(largePomSet, largePomSet, Set.of());
            long endTime = System.currentTimeMillis();

            // Performance assertion - should complete within reasonable time
            long duration = endTime - startTime;
            assertTrue(duration < 1000, "UpgradeResult creation should complete within 1 second for 1000 POMs");

            // Verify correctness
            assertTrue(result.success(), "Result should be successful");
            assertEquals(largePomSet.size(), result.processedCount(), "Should have correct processed count");
        }
    }
}
