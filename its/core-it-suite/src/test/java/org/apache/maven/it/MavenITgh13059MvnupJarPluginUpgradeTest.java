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
 * Integration test for GH-13059: mvnup jar-plugin upgrade target.
 * <p>
 * Verifies that {@code mvn --up apply} upgrades {@code maven-jar-plugin} to
 * <strong>3.4.1</strong> — the last release before the two regressions introduced
 * in 3.4.2 via maven-archiver 3.6.3:
 * <ul>
 *   <li>{@code SOURCE_DATE_EPOCH=0} / {@code 1970-01-01T00:00:00Z} timestamps are
 *       rejected with {@code IllegalArgumentException}
 *       (apache/maven-jar-plugin#595)</li>
 *   <li>Derived {@code Automatic-Module-Name} values containing hyphens fail
 *       the build with {@code Invalid automatic module name: '...'}
 *       (apache/maven-jar-plugin#596)</li>
 * </ul>
 * After the upgrade, the project is built with {@code mvn package} to confirm
 * that maven-jar-plugin 3.4.1 works correctly under Maven 4.
 *
 * @see <a href="https://github.com/apache/maven/pull/13059">GH-13059</a>
 * @since 4.1.0
 */
class MavenITgh13059MvnupJarPluginUpgradeTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that mvnup upgrades maven-jar-plugin 3.3.0 to 3.4.1 (not 3.4.2+
     * which carries regressions), then that the upgraded project builds cleanly
     * with Maven 4.
     */
    @Test
    void testMvnupUpgradesJarPluginTo341AndBuilds() throws Exception {
        Path testDir = extractResources("gh-13059-mvnup-jar-plugin");

        // Step 1: run mvnup apply — must upgrade jar-plugin to 3.4.1
        Verifier verifier = newVerifier(testDir);
        verifier.setForkJvm(true);
        verifier.setLogFileName("mvnup.txt");
        verifier.addCliArgument("--up");
        verifier.addCliArgument("apply");
        verifier.addCliArgument("-d");
        verifier.addCliArgument(testDir.toString());
        verifier.execute();
        verifier.verifyErrorFreeLog();

        String pomContent = Files.readString(testDir.resolve("pom.xml"));

        // Must land on 3.4.1 — last clean version before the 3.4.2 regressions
        assertTrue(
                pomContent.contains("<version>3.4.1</version>"),
                "mvnup should upgrade maven-jar-plugin to 3.4.1 (3.4.2+ carries"
                        + " SOURCE_DATE_EPOCH and Automatic-Module-Name regressions)");

        // Must NOT target 3.4.2 or later (those carry the timestamp + module-name regressions)
        assertFalse(
                pomContent.contains("<version>3.4.2</version>")
                        || pomContent.contains("<version>3.5.0</version>")
                        || pomContent.contains("<version>3.5.1</version>"),
                "mvnup must not target 3.4.2+ (SOURCE_DATE_EPOCH=0 rejection + module name regression)");

        // Step 2: build the upgraded project with Maven 4 — 3.4.1 must work cleanly
        verifier = newVerifier(testDir);
        verifier.setForkJvm(true);
        verifier.setLogFileName("build.txt");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
