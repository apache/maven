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

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link Apply} goal class.
 * Tests the Apply-specific functionality including file modification behavior.
 */
@DisplayName("Apply")
class ApplyTest {

    private Apply applyGoal;
    private StrategyOrchestrator mockOrchestrator;

    @BeforeEach
    void setUp() {
        mockOrchestrator = mock(StrategyOrchestrator.class);
        applyGoal = new Apply(mockOrchestrator);
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    @Nested
    @DisplayName("Modification Behavior")
    class ModificationBehaviorTests {

        @Test
        @DisplayName("should save modifications to disk")
        void shouldSaveModificationsToDisk() {
            assertTrue(applyGoal.shouldSaveModifications(), "Apply goal should save modifications to disk");
        }
    }

    @Nested
    @DisplayName("Execution")
    class ExecutionTests {

        @Test
        @DisplayName("should log appropriate header message")
        void shouldLogAppropriateHeaderMessage() throws Exception {
            UpgradeContext context = createMockContext();

            // Create a temporary directory with a POM file for the test
            Path tempDir = Files.createTempDirectory("apply-test");
            try {
                Path pomFile = tempDir.resolve("pom.xml");
                String pomContent = PomBuilder.create()
                        .groupId("test")
                        .artifactId("test")
                        .version("1.0.0")
                        .build();
                Files.writeString(pomFile, pomContent);

                // Update context to use the temp directory
                when(context.invokerRequest.cwd()).thenReturn(tempDir);

                // Mock successful strategy execution
                when(mockOrchestrator.executeStrategies(Mockito.any(), Mockito.any()))
                        .thenReturn(UpgradeResult.empty());

                applyGoal.execute(context);

                // Verify that the Apply-specific header is logged
                verify(context.logger).info("Maven Upgrade Tool - Apply");
            } finally {
                // Clean up - delete all files in the directory first
                try {
                    Files.walk(tempDir)
                            .sorted(java.util.Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (Exception e) {
                                    // Ignore cleanup errors
                                }
                            });
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    @Nested
    @DisplayName("Integration with AbstractUpgradeGoal")
    class IntegrationTests {

        @Test
        @DisplayName("should inherit behavior from AbstractUpgradeGoal")
        void shouldInheritBehaviorFromAbstractUpgradeGoal() {
            // This test verifies that Apply inherits the model version logic from AbstractUpgradeGoal
            // The actual logic is tested in AbstractUpgradeGoalTest
            // Here we just verify that Apply is properly configured as a subclass
            assertTrue(applyGoal instanceof AbstractUpgradeGoal, "Apply should extend AbstractUpgradeGoal");
            assertTrue(applyGoal.shouldSaveModifications(), "Apply should save modifications unlike Check goal");
        }
    }
}
