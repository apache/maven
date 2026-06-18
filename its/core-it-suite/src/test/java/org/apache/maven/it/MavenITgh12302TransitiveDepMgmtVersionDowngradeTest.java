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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for <a href="https://github.com/apache/maven/issues/12302">gh-12302</a>.
 *
 * <p>Verifies that {@code TransitiveDependencyManager} does not silently downgrade
 * dependency versions when an intermediate POM's {@code <dependencyManagement>}
 * declares a lower version of a transitive dependency.
 *
 * <p>Dependency graph:
 * <pre>
 *   root (test project)
 *     └── module-a:1.0 (parent = parent-a:1.0)
 *           └── module-b:1.0
 *                 └── lib-c:2.0
 *
 *   parent-a has &lt;dependencyManagement&gt; managing lib-c to 1.0
 * </pre>
 *
 * <p>Expected: lib-c resolves to 2.0 (declared by module-b).
 * <br>Actual (bug): lib-c is downgraded to 1.0 by parent-a's dependencyManagement
 * because {@code TransitiveDependencyManager} has {@code deriveUntil = Integer.MAX_VALUE},
 * collecting managed versions from every POM in the graph.
 */
public class MavenITgh12302TransitiveDepMgmtVersionDowngradeTest extends AbstractMavenIntegrationTestCase {

    MavenITgh12302TransitiveDepMgmtVersionDowngradeTest() {
        super("[4.0.0-rc-3,)");
    }

    @Test
    public void testTransitiveDependencyManagerDoesNotDowngradeVersions() throws Exception {
        File testDir = extractResources("/gh-12302-transitive-dep-mgmt-version-downgrade");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.gh12302");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> classpath = verifier.loadLines("target/classpath.txt");

        // lib-c should resolve to 2.0 (as declared by module-b), not 1.0
        // (managed by parent-a's dependencyManagement).
        // With the TransitiveDependencyManager bug, lib-c gets downgraded to 1.0.
        assertTrue(
                classpath.contains("lib-c-2.0.jar"),
                "lib-c should be version 2.0 (declared by module-b), not downgraded: " + classpath);
        assertFalse(
                classpath.contains("lib-c-1.0.jar"),
                "lib-c should NOT be downgraded to 1.0 by parent-a's dependencyManagement: " + classpath);
    }
}
