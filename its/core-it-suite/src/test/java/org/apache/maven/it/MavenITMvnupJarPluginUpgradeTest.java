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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for {@code mvnup} maven-jar-plugin upgrade target.
 * <p>
 * Verifies that {@code mvn --up apply} upgrades {@code maven-jar-plugin} to
 * <strong>3.4.1</strong> — the last release that does not carry the two
 * regressions introduced in 3.4.2 via maven-archiver 3.6.3:
 * <ul>
 *   <li>SOURCE_DATE_EPOCH=0 / 1970-01-01 timestamps are rejected
 *       (apache/maven-jar-plugin#595)</li>
 *   <li>Derived {@code Automatic-Module-Name} values containing hyphens fail
 *       the build (apache/maven-jar-plugin#596)</li>
 * </ul>
 * The test also guards against targeting a non-existent version: 3.3.1 was
 * never released (the series went 3.3.0 → 3.4.0), so an upgrade to "3.3.1"
 * would produce a resolution failure at build time.
 *
 * @since 4.1.0
 */
class MavenITMvnupJarPluginUpgradeTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that mvnup upgrades maven-jar-plugin 3.3.0 to 3.4.1 (not 3.3.1
     * which does not exist, and not 3.4.2+ which carries regressions).
     */
    @Test
    void testMvnupUpgradesJarPluginTo341() throws Exception {
        Path testDir = extractResources("mvnup-jar-plugin-upgrade");

        Verifier verifier = newVerifier(testDir);
        verifier.setForkJvm(true);
        verifier.addCliArgument("--up");
        verifier.addCliArgument("apply");
        verifier.addCliArgument("-d");
        verifier.addCliArgument(testDir.toString());
        verifier.execute();
        verifier.verifyErrorFreeLog();

        String pomContent = Files.readString(testDir.resolve("pom.xml"));

        // Must target 3.4.1 — the last clean version before the 3.4.2 regressions
        assertTrue(
                pomContent.contains("<version>3.4.1</version>"),
                "mvnup should upgrade maven-jar-plugin to 3.4.1 (first release with"
                        + " commons-io ClassNotFound fix and toolchain Build-Jdk, without"
                        + " the 3.4.2 timestamp / module-name regressions)");

        // Must NOT target 3.3.1 which was never released (3.3.0 → 3.4.0)
        assertFalse(
                pomContent.contains("<version>3.3.1</version>"),
                "mvnup must not target 3.3.1 — that version was never released");

        // Must NOT target 3.4.2 or later (those carry the two regressions)
        assertFalse(
                pomContent.contains("<version>3.4.2</version>"),
                "mvnup must not target 3.4.2 (SOURCE_DATE_EPOCH=0 rejection + module name regression)");
        assertFalse(
                pomContent.contains("<version>3.5.0</version>") || pomContent.contains("<version>3.5.1</version>"),
                "mvnup must not target 3.5.x (still carries the 3.4.2 timestamp regression)");
    }

    /**
     * Verify that a project already on 3.4.1 is left untouched (idempotent).
     */
    @Test
    void testMvnupIsIdempotentOn341() throws Exception {
        Path testDir = extractResources("mvnup-jar-plugin-upgrade");

        // First apply to reach 3.4.1
        Verifier verifier = newVerifier(testDir);
        verifier.setForkJvm(true);
        verifier.addCliArgument("--up");
        verifier.addCliArgument("apply");
        verifier.addCliArgument("-d");
        verifier.addCliArgument(testDir.toString());
        verifier.execute();
        verifier.verifyErrorFreeLog();

        String pomAfterFirst = Files.readString(testDir.resolve("pom.xml"));

        // Second apply — must not change anything
        verifier = newVerifier(testDir);
        verifier.setForkJvm(true);
        verifier.setLogFileName("second-run.txt");
        verifier.addCliArgument("--up");
        verifier.addCliArgument("apply");
        verifier.addCliArgument("-d");
        verifier.addCliArgument(testDir.toString());
        verifier.execute();
        verifier.verifyErrorFreeLog();

        String pomAfterSecond = Files.readString(testDir.resolve("pom.xml"));
        assertTrue(
                pomAfterFirst.equals(pomAfterSecond),
                "Second mvnup apply should produce an identical POM (idempotent)");
    }
}
