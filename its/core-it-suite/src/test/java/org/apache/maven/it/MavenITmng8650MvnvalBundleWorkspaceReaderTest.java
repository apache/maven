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

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@code mvnval} bundle workspace reader and {@code --offline} support
 * (MNG-8650).
 *
 * <h2>Bundle workspace reader</h2>
 * <p>
 * When several POMs are given at once in {@code effective} mode, {@code mvnval} pre-scans them
 * to build an in-memory GA index and installs a workspace reader on the resolver session that
 * answers parent and BOM lookups for bundle members from disk.  This means:
 * <ul>
 *   <li>A child whose parent lives in the same bundle is resolved locally, not from a
 *       repository — so the run succeeds even when the parent has not been published yet.</li>
 *   <li>The verdict is the same regardless of the order in which the POMs are named on the
 *       command line.</li>
 * </ul>
 *
 * <h2>Offline mode</h2>
 * <p>
 * {@code -o} is now honoured in effective mode.  Combined with {@code --temp-local-repository}
 * it proves that the named POMs are fully self-contained: if a parent or BOM import is absent
 * the run exits with code 1 (validation error), never silently fetches it from the network.
 *
 * @since 4.1.0
 */
class MavenITmng8650MvnvalBundleWorkspaceReaderTest extends AbstractMavenIntegrationTestCase {

    /**
     * Validates a parent+child bundle in {@code effective} mode when the child's
     * {@code <relativePath/>} is disabled, so the parent cannot be found by filesystem
     * discovery.  The bundle workspace reader must supply the parent from the bundle itself.
     * <p>
     * Both POMs are passed in the natural order (parent first).
     */
    @Test
    void testBundleParentResolvedFromDiskParentFirst() throws Exception {
        Path testDir = extractResources("mvnval-validate-pom");

        Verifier verifier = newVerifier(testDir, null);
        verifier.setForkJvm(true);
        verifier.setLogFileName("bundle-parent-first.txt");
        verifier.addCliArgument("--val");
        // Both paths are relative to the verifier basedir (testDir).
        verifier.addCliArgument("bundle-parent/pom.xml");
        verifier.addCliArgument("bundle-child/pom.xml");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("no problems");
    }

    /**
     * Same as {@link #testBundleParentResolvedFromDiskParentFirst} but with the POMs named in
     * reverse order (child first).  The verdict must be identical: the workspace index is built
     * before any validation runs, so order on the command line cannot affect the result.
     */
    @Test
    void testBundleParentResolvedFromDiskChildFirst() throws Exception {
        Path testDir = extractResources("mvnval-validate-pom");

        Verifier verifier = newVerifier(testDir, null);
        verifier.setForkJvm(true);
        verifier.setLogFileName("bundle-child-first.txt");
        verifier.addCliArgument("--val");
        verifier.addCliArgument("bundle-child/pom.xml");
        verifier.addCliArgument("bundle-parent/pom.xml");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("no problems");
    }

    /**
     * Validates a bundle in {@code effective} mode with {@code --offline} and a throwaway local
     * repository.  The parent lives in the bundle, so resolution succeeds even though no
     * repository is reachable and the local repository is empty.
     * <p>
     * This is the "prove self-containment" idiom: if any parent or BOM were absent from the
     * bundle the run would fail with exit code 1.
     */
    @Test
    void testOfflineBundleResolvesFromDisk() throws Exception {
        Path testDir = extractResources("mvnval-validate-pom");

        Verifier verifier = newVerifier(testDir, null);
        verifier.setForkJvm(true);
        verifier.setLogFileName("bundle-offline.txt");
        verifier.addCliArgument("--val");
        verifier.addCliArgument("--offline");
        verifier.addCliArgument("--temp-local-repository");
        verifier.addCliArgument("bundle-parent/pom.xml");
        verifier.addCliArgument("bundle-child/pom.xml");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("no problems");
    }

    /**
     * Validates that {@code --offline} with a throwaway local repository fails when the parent
     * is NOT in the bundle (only the child is given).  The run must exit with a non-zero code
     * and report a "Non-resolvable parent" problem — not silently fetch anything.
     */
    @Test
    void testOfflineSinglePomFailsWhenParentAbsent() throws Exception {
        Path testDir = extractResources("mvnval-validate-pom");

        Verifier verifier = newVerifier(testDir, null);
        verifier.setForkJvm(true);
        verifier.setLogFileName("bundle-offline-missing-parent.txt");
        verifier.addCliArgument("--val");
        verifier.addCliArgument("--offline");
        verifier.addCliArgument("--temp-local-repository");
        verifier.addCliArgument("bundle-child/pom.xml");

        assertThrows(
                VerificationException.class, verifier::execute, "--offline with a missing parent must fail validation");

        assertTrue(
                verifier.loadLogLines().stream().anyMatch(l -> l.contains("Non-resolvable parent")),
                "Failure must be reported as a non-resolvable parent: " + verifier.loadLogLines());
    }
}
