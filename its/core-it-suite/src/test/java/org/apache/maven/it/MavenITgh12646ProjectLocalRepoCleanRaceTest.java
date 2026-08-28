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
 * This is a test set for <a href="https://github.com/apache/maven/issues/12646">GH-12646</a>.
 *
 * Verifies that parallel builds with {@code clean install} do not fail with a race condition
 * when the root pom has a parent (super-pom) and other modules write to
 * {@code target/project-local-repo} while the root project's clean phase is running.
 *
 * @since 4.0.0-rc-6
 */
public class MavenITgh12646ProjectLocalRepoCleanRaceTest extends AbstractMavenIntegrationTestCase {

    public MavenITgh12646ProjectLocalRepoCleanRaceTest() {
        super("[4.0.0-rc-6,)");
    }

    /**
     * Verify that a parallel {@code clean install} succeeds when the root pom has a parent
     * that is also part of the reactor. In this scenario:
     * <ol>
     *   <li>super-pom builds first (no dependencies)</li>
     *   <li>root pom and module-a..d start in parallel (all depend on super-pom)</li>
     *   <li>root pom's clean phase runs maven-clean-plugin which deletes target/</li>
     *   <li>modules complete and install artifacts into target/project-local-repo</li>
     * </ol>
     * Without the fix, step 3 and 4 race, causing maven-clean-plugin to fail because
     * target/project-local-repo is being written to while it tries to delete target/.
     */
    @Test
    void testParallelCleanInstallWithParentPom() throws Exception {
        File testDir = extractResources("/gh-12646-project-local-repo-clean-race");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("-T");
        verifier.addCliArgument("4");
        verifier.addCliArguments("clean", "install");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
