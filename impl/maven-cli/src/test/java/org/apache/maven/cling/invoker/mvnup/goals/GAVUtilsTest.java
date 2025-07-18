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

import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link GAVUtils} utility class.
 * Tests GAV extraction, computation, and parent resolution functionality.
 */
@DisplayName("GAVUtils")
class GAVUtilsTest {

    private SAXBuilder saxBuilder;

    @BeforeEach
    void setUp() {
        saxBuilder = new SAXBuilder();
    }

    private UpgradeContext createMockContext() {
        return TestUtils.createMockContext();
    }

    @Nested
    @DisplayName("GAV Extraction")
    class GAVExtractionTests {

        @Test
        @DisplayName("should extract GAV from complete POM")
        void shouldExtractGAVFromCompletePOM() throws Exception {
            String pomXml = PomBuilder.create()
                    .groupId("com.example")
                    .artifactId("test-project")
                    .version("1.0.0")
                    .build();

            Document document = saxBuilder.build(new StringReader(pomXml));
            UpgradeContext context = createMockContext();

            GAV gav = GAVUtils.extractGAVWithParentResolution(context, document);

            assertNotNull(gav);
            assertEquals("com.example", gav.groupId());
            assertEquals("test-project", gav.artifactId());
            assertEquals("1.0.0", gav.version());
        }

        @Test
        @DisplayName("should extract GAV with parent inheritance")
        void shouldExtractGAVWithParentInheritance() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>child-project</artifactId>
                    <!-- groupId and version inherited from parent -->
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            UpgradeContext context = createMockContext();

            GAV gav = GAVUtils.extractGAVWithParentResolution(context, document);

            assertNotNull(gav);
            assertEquals("com.example", gav.groupId());
            assertEquals("child-project", gav.artifactId());
            assertEquals("1.0.0", gav.version());
        }

        @Test
        @DisplayName("should handle partial parent inheritance")
        void shouldHandlePartialParentInheritance() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <groupId>com.example.child</groupId>
                    <artifactId>child-project</artifactId>
                    <version>2.0.0</version>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            UpgradeContext context = createMockContext();

            GAV gav = GAVUtils.extractGAVWithParentResolution(context, document);

            assertNotNull(gav);
            assertEquals("com.example.child", gav.groupId());
            assertEquals("child-project", gav.artifactId());
            assertEquals("2.0.0", gav.version());
        }

        @ParameterizedTest
        @MethodSource("provideInvalidGAVScenarios")
        @DisplayName("should return null for invalid GAV scenarios")
        void shouldReturnNullForInvalidGAVScenarios(
                String groupId, String artifactId, String version, String description) throws Exception {
            String pomXml = PomBuilder.create()
                    .groupId(groupId)
                    .artifactId(artifactId)
                    .version(version)
                    .build();

            Document document = saxBuilder.build(new StringReader(pomXml));
            UpgradeContext context = createMockContext();

            GAV gav = GAVUtils.extractGAVWithParentResolution(context, document);

            assertNull(gav, description);
        }

        private static Stream<Arguments> provideInvalidGAVScenarios() {
            return Stream.of(
                    Arguments.of(
                            null, "incomplete-project", null, "Should return null for missing groupId and version"),
                    Arguments.of("com.example", null, "1.0.0", "Should return null for missing artifactId"),
                    Arguments.of(null, null, "1.0.0", "Should return null for missing groupId and artifactId"),
                    Arguments.of("com.example", "test-project", null, "Should return null for missing version"),
                    Arguments.of("", "test-project", "1.0.0", "Should return null for empty groupId"),
                    Arguments.of("com.example", "", "1.0.0", "Should return null for empty artifactId"),
                    Arguments.of("com.example", "test-project", "", "Should return null for empty version"));
        }
    }

    @Nested
    @DisplayName("GAV Computation")
    class GAVComputationTests {

        @Test
        @DisplayName("should compute GAVs from multiple POMs")
        void shouldComputeGAVsFromMultiplePOMs() throws Exception {
            String parentPomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent-project</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                </project>
                """;

            String childPomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>child-project</artifactId>
                </project>
                """;

            Document parentDoc = saxBuilder.build(new StringReader(parentPomXml));
            Document childDoc = saxBuilder.build(new StringReader(childPomXml));

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("/project/pom.xml"), parentDoc);
            pomMap.put(Paths.get("/project/child/pom.xml"), childDoc);

            UpgradeContext context = createMockContext();

            Set<GAV> gavs = GAVUtils.computeAllGAVs(context, pomMap);

            assertEquals(2, gavs.size());
            assertTrue(gavs.contains(new GAV("com.example", "parent-project", "1.0.0")));
            assertTrue(gavs.contains(new GAV("com.example", "child-project", "1.0.0")));
        }

        @Test
        @DisplayName("should handle empty POM map")
        void shouldHandleEmptyPOMMap() {
            UpgradeContext context = createMockContext();
            Map<Path, Document> pomMap = new HashMap<>();

            Set<GAV> gavs = GAVUtils.computeAllGAVs(context, pomMap);

            assertNotNull(gavs);
            assertTrue(gavs.isEmpty());
        }

        @Test
        @DisplayName("should deduplicate identical GAVs")
        void shouldDeduplicateIdenticalGAVs() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>duplicate-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            Document doc1 = saxBuilder.build(new StringReader(pomXml));
            Document doc2 = saxBuilder.build(new StringReader(pomXml));

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("/project/pom1.xml"), doc1);
            pomMap.put(Paths.get("/project/pom2.xml"), doc2);

            UpgradeContext context = createMockContext();

            Set<GAV> gavs = GAVUtils.computeAllGAVs(context, pomMap);

            assertEquals(1, gavs.size());
            assertTrue(gavs.contains(new GAV("com.example", "duplicate-project", "1.0.0")));
        }

        @Test
        @DisplayName("should skip POMs with incomplete GAVs")
        void shouldSkipPOMsWithIncompleteGAVs() throws Exception {
            String validPomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>valid-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            String invalidPomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <artifactId>invalid-project</artifactId>
                    <!-- Missing groupId and version -->
                </project>
                """;

            Document validDoc = saxBuilder.build(new StringReader(validPomXml));
            Document invalidDoc = saxBuilder.build(new StringReader(invalidPomXml));

            Map<Path, Document> pomMap = new HashMap<>();
            pomMap.put(Paths.get("/project/valid.xml"), validDoc);
            pomMap.put(Paths.get("/project/invalid.xml"), invalidDoc);

            UpgradeContext context = createMockContext();

            Set<GAV> gavs = GAVUtils.computeAllGAVs(context, pomMap);

            assertEquals(1, gavs.size());
            assertTrue(gavs.contains(new GAV("com.example", "valid-project", "1.0.0")));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle POM with only whitespace elements")
        void shouldHandlePOMWithWhitespaceElements() throws Exception {
            String pomXml = PomBuilder.create()
                    .groupId("   ") // whitespace-only groupId
                    .artifactId("test-project")
                    .version("1.0.0")
                    .build();

            Document document = saxBuilder.build(new StringReader(pomXml));
            UpgradeContext context = createMockContext();

            GAV gav = GAVUtils.extractGAVWithParentResolution(context, document);

            // Should handle whitespace-only groupId as invalid
            assertNull(gav, "GAV should be null for whitespace-only groupId");
        }

        @Test
        @DisplayName("should handle POM with empty elements")
        void shouldHandlePOMWithEmptyElements() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId></groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            UpgradeContext context = createMockContext();

            GAV gav = GAVUtils.extractGAVWithParentResolution(context, document);

            assertNull(gav, "GAV should be null for empty groupId");
        }

        @Test
        @DisplayName("should handle POM with special characters in GAV")
        void shouldHandlePOMWithSpecialCharacters() throws Exception {
            String pomXml = PomBuilder.create()
                    .groupId("com.example-test_group")
                    .artifactId("test-project.artifact")
                    .version("1.0.0-SNAPSHOT")
                    .build();

            Document document = saxBuilder.build(new StringReader(pomXml));
            UpgradeContext context = createMockContext();

            GAV gav = GAVUtils.extractGAVWithParentResolution(context, document);

            assertNotNull(gav, "GAV should be valid for special characters");
            assertEquals("com.example-test_group", gav.groupId());
            assertEquals("test-project.artifact", gav.artifactId());
            assertEquals("1.0.0-SNAPSHOT", gav.version());
        }

        @Test
        @DisplayName("should handle deeply nested parent inheritance")
        void shouldHandleDeeplyNestedParentInheritance() throws Exception {
            String pomXml =
                    """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>grandparent</artifactId>
                        <version>1.0.0</version>
                        <relativePath>../../grandparent/pom.xml</relativePath>
                    </parent>
                    <artifactId>child-project</artifactId>
                </project>
                """;

            Document document = saxBuilder.build(new StringReader(pomXml));
            UpgradeContext context = createMockContext();

            GAV gav = GAVUtils.extractGAVWithParentResolution(context, document);

            assertNotNull(gav, "GAV should be resolved from parent");
            assertEquals("com.example", gav.groupId());
            assertEquals("child-project", gav.artifactId());
            assertEquals("1.0.0", gav.version());
        }

        @Test
        @DisplayName("should handle large number of POMs efficiently")
        void shouldHandleLargeNumberOfPOMsEfficiently() throws Exception {
            // Create a large number of POM documents for performance testing
            Map<Path, Document> largePomMap = new HashMap<>();

            for (int i = 0; i < 100; i++) {
                Path pomPath = Paths.get("module" + i + "/pom.xml");
                String pomContent = PomBuilder.create()
                        .groupId("com.example")
                        .artifactId("module" + i)
                        .version("1.0.0")
                        .build();
                Document document = saxBuilder.build(new StringReader(pomContent));
                largePomMap.put(pomPath, document);
            }

            UpgradeContext context = createMockContext();

            long startTime = System.currentTimeMillis();
            Set<GAV> gavs = GAVUtils.computeAllGAVs(context, largePomMap);
            long endTime = System.currentTimeMillis();

            // Performance assertion - should complete within reasonable time
            long duration = endTime - startTime;
            assertTrue(duration < 5000, "GAV computation should complete within 5 seconds for 100 POMs");

            // Verify correctness
            assertNotNull(gavs, "GAV set should not be null");
            assertEquals(100, gavs.size(), "Should have computed GAVs for all 100 POMs");
        }
    }
}
