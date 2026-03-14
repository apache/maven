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
package org.apache.maven.it;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/11280">GH-11280</a>.
 * <p>
 * The issue occurs when a BOM (Bill of Materials) defines dependencies with both null and empty string
 * classifiers for the same artifact. Before the fix, the consumer POM generation would treat these as
 * different dependencies, but during the merge process they would be considered duplicates because both
 * null and empty string classifiers resolve to the same management key.
 * <p>
 * This was specifically seen with the Apache Arrow BOM which defines:
 * <ul>
 * <li>A dependency without a classifier (null)</li>
 * <li>A dependency with an empty string classifier from a property: {@code <classifier>${arrow.vector.classifier}</classifier>}</li>
 * </ul>
 * <p>
 * The fix ensures that both null and empty string classifiers are treated consistently in the
 * dependency management key generation, preventing the "Duplicate dependency" error during
 * consumer POM building.
 */
class MavenITgh11280DuplicateDependencyConsumerPomTest extends AbstractMavenIntegrationTestCase {

    /**
     * Tests that a project using a BOM with dependencies that have both null and empty string
     * classifiers can be built successfully without "Duplicate dependency" errors during
     * consumer POM generation.
     * <p>
     * This test reproduces the scenario where:
     * <ul>
     * <li>A BOM defines the same dependency twice: once without classifier and once with an empty string classifier</li>
     * <li>A project imports this BOM and uses one of the dependencies</li>
     * <li>The maven-install-plugin is executed, which triggers consumer POM generation</li>
     * </ul>
     * Before the fix, this would fail with "Duplicate dependency: groupId:artifactId:type:" during
     * the consumer POM building process.
     * <p>
     * The fix ensures that the dependency management key treats null and empty string classifiers
     * as equivalent, preventing the duplicate dependency error.
     */
    @Test
    void testDuplicateDependencyWithNullAndEmptyClassifier() throws Exception {
        File testDir = extractResources("/gh-11280-duplicate-dependency-consumer-pom");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();

        verifier.verifyErrorFreeLog();
    }
}
