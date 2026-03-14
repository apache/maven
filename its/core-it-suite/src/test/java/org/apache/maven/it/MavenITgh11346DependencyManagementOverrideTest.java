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
import java.util.List;

import org.apache.maven.api.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for dependency management override scenarios when
 * consumer POM flattening is disabled (maven.consumer.pom.flatten=false).
 *
 * Scenario:
 * - A 1.0 depends on B 1.0 and manages C to 1.2
 * - B 1.0 has no dependencies
 * - B 2.0 depends on C 1.1
 * - D depends on A 1.0 and manages B to 2.0
 *
 * Question: Does D depend on C, and which version?
 *
 * Expected behavior when flattening is disabled: D should get C 1.2 (from A's dependency management),
 * not C 1.1 (from B 2.0's dependency), because A's dependency
 * management applies to D's transitive dependencies.
 *
 * @see <a href="https://github.com/apache/maven/issues/11346">gh-11346</a>
 */
public class MavenITgh11346DependencyManagementOverrideTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that when consumer POM flattening is disabled, dependency management
     * from intermediate dependencies applies to the consumer's transitive dependencies.
     * This test uses -Dmaven.consumer.pom.flatten=false to enable dependency management
     * inheritance from transitive dependencies.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testDependencyManagementOverride() throws Exception {
        File testDir = extractResources("/gh-11346-dependency-management-override");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteArtifacts("org.apache.maven.its.mng.depman");
        // Test with dependency manager transitivity disabled instead of consumer POM flattening
        verifier.addCliArgument("-D" + Constants.MAVEN_CONSUMER_POM_FLATTEN + "=false");
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Check module D's classpath
        List<String> dClasspath = verifier.loadLines("module-d/target/classpath.txt");

        // D should have A 1.0
        assertTrue(dClasspath.contains("module-a-1.0.jar"), "D should depend on A 1.0: " + dClasspath);

        // D should have B 2.0 (managed by D)
        assertTrue(dClasspath.contains("module-b-2.0.jar"), "D should depend on B 2.0 (managed by D): " + dClasspath);
        assertFalse(dClasspath.contains("module-b-1.0.jar"), "D should not depend on B 1.0: " + dClasspath);

        // D should have C 1.2 (from A's dependency management)
        // A's dependency management of C to 1.2 should apply to D
        assertTrue(
                dClasspath.contains("module-c-1.2.jar"),
                "D should depend on C 1.2 (A's dependency management should apply): " + dClasspath);
        assertFalse(
                dClasspath.contains("module-c-1.1.jar"),
                "D should not depend on C 1.1 (should be managed to 1.2): " + dClasspath);
    }

    @Test
    public void testDependencyManagementOverrideNoTransitive() throws Exception {
        File testDir = extractResources("/gh-11346-dependency-management-override");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.deleteArtifacts("org.apache.maven.its.mng.depman");
        // Test with dependency manager transitivity disabled instead of consumer POM flattening
        verifier.addCliArgument("-D" + Constants.MAVEN_CONSUMER_POM_FLATTEN + "=false");
        verifier.addCliArgument("-D" + Constants.MAVEN_RESOLVER_DEPENDENCY_MANAGER_TRANSITIVITY + "=false");
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Check module D's classpath
        List<String> dClasspath = verifier.loadLines("module-d/target/classpath.txt");

        // D should have A 1.0
        assertTrue(dClasspath.contains("module-a-1.0.jar"), "D should depend on A 1.0: " + dClasspath);

        // D should have B 2.0 (managed by D)
        assertTrue(dClasspath.contains("module-b-2.0.jar"), "D should depend on B 2.0 (managed by D): " + dClasspath);
        assertFalse(dClasspath.contains("module-b-1.0.jar"), "D should not depend on B 1.0: " + dClasspath);

        // D should have C 1.1 as the resolver is not transitive
        assertFalse(
                dClasspath.contains("module-c-1.2.jar"),
                "D should depend on C 1.2 (A's dependency management should apply): " + dClasspath);
        assertTrue(
                dClasspath.contains("module-c-1.1.jar"),
                "D should not depend on C 1.1 (should be managed to 1.2): " + dClasspath);
    }
}
