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
import java.util.Map;

import eu.maveniverse.domtrip.Document;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AbstractUpgradeStrategy} — in particular the reactor
 * pre-build logic introduced to fix #13190.
 */
@DisplayName("AbstractUpgradeStrategy")
class AbstractUpgradeStrategyTest {

    /** Concrete strategy used to exercise {@code AbstractUpgradeStrategy.apply()}. */
    private PluginUpgradeStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PluginUpgradeStrategy();
    }

    /**
     * Regression test for #13190.
     *
     * <p>Before the fix, {@code buildEffectiveModel} created a fresh
     * {@code ModelBuilderSession} per POM with {@code BUILD_EFFECTIVE}. A fresh
     * session has an empty {@code mappedSources} map, so Maven 4 coordinate
     * inference (inferring a child {@code <version>} / {@code <groupId>} from
     * its reactor parent) silently returns {@code null}. Strict effective-model
     * validation then cascades {@code "version is missing"} errors for every
     * dependency that relied on inference.
     *
     * <p>The fix: {@link AbstractUpgradeStrategy#apply} calls
     * {@code prebuildReactorModels} once with
     * {@code BUILD_PROJECT + recursive=true} on the root POM. This populates
     * {@code mappedSources} for the whole reactor before any effective model is
     * assembled.  The resulting {@code Map<Path, Model>} cache is consulted by
     * {@code buildEffectiveModel}; for cache misses (external parents) the same
     * {@code ModelBuilderSession} is reused — which shares the populated map.
     */
    @Test
    @DisplayName("apply() on a multi-module project succeeds without version-missing cascade (regression #13190)")
    void applyOnMultiModuleProjectSucceedsWithoutVersionMissingCascade() throws Exception {
        Path tempDir = Files.createTempDirectory("mvnup-prebuild-test-");
        try {
            Files.createDirectories(tempDir.resolve(".mvn"));

            // Root POM — declares a child module
            Path parentPom = tempDir.resolve("pom.xml");
            Files.writeString(parentPom, """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>test.group</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                      <packaging>pom</packaging>
                      <modules><module>child</module></modules>
                    </project>
                    """);

            // Child POM — <version> intentionally omitted; must be inferred from
            // the parent via mappedSources.  Without the fix this causes a
            // "version is missing" cascade error during effective-model validation.
            Path childDir = tempDir.resolve("child");
            Files.createDirectories(childDir);
            Path childPom = childDir.resolve("pom.xml");
            Files.writeString(childPom, """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>test.group</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0.0</version>
                      </parent>
                      <artifactId>child</artifactId>
                    </project>
                    """);

            Document parentDoc = Document.of(Files.readString(parentPom));
            Document childDoc = Document.of(Files.readString(childPom));
            Map<Path, Document> pomMap = Map.of(parentPom, parentDoc, childPom, childDoc);

            UpgradeContext context = TestUtils.createMockContext(tempDir);
            UpgradeResult result = strategy.apply(context, pomMap);

            assertTrue(result.success(), "apply() must succeed — no version-missing cascade from inference");
            assertTrue(result.errorPoms().isEmpty(), "No POM should be in the error set: " + result.errorPoms());
        } finally {
            try (var walk = Files.walk(tempDir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        }
    }
}
