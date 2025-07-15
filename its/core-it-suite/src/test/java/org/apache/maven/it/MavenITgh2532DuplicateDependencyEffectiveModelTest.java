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
 * This is a test set for <a href="https://github.com/apache/maven/issues/2532">GH-2532</a>.
 * <p>
 * The issue occurs when a project has duplicate dependencies in the effective model due to
 * property placeholders in dependency coordinates. Before the fix, deduplication was performed
 * before interpolation, causing dependencies like {@code scalatest_${scala.binary.version}} and
 * {@code scalatest_2.13} to be seen as different dependencies. After interpolation, they become
 * the same dependency, leading to a "duplicate dependency" error during the build.
 * <p>
 * The fix moves the deduplication step to after interpolation, ensuring that dependencies with
 * property placeholders are properly deduplicated after their values are resolved.
 */
class MavenITgh2532DuplicateDependencyEffectiveModelTest extends AbstractMavenIntegrationTestCase {

    MavenITgh2532DuplicateDependencyEffectiveModelTest() {
        super("[4.0.0-rc-3,)");
    }

    /**
     * Tests that a project with dependencies using property placeholders in artifact coordinates
     * can be built successfully without "duplicate dependency" errors when the same dependency
     * appears in multiple places in the effective model.
     * <p>
     * This test reproduces the scenario where:
     * <ul>
     * <li>A dependency is defined with a property placeholder in the artifactId (e.g., scalatest_${scala.binary.version})</li>
     * <li>The same dependency appears in parent and child modules</li>
     * <li>The maven-shade-plugin is used, which triggers the duplicate dependency check</li>
     * </ul>
     * Before the fix, deduplication happened before interpolation, so scalatest_${scala.binary.version}
     * and scalatest_2.13 were seen as different dependencies. After interpolation, they become the same,
     * causing a "duplicate dependency" error during the shade goal.
     * <p>
     * The fix moves deduplication to after interpolation, ensuring proper deduplication.
     */
    @Test
    void testDuplicateDependencyWithPropertyPlaceholders() throws Exception {
        File testDir = extractResources("/gh-2532-duplicate-dependency-effective-model");

        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.setLogFileName("testDuplicateDependencyWithPropertyPlaceholders.txt");
        verifier.addCliArgument("package");
        verifier.execute();

        verifier.verifyErrorFreeLog();
    }
}
