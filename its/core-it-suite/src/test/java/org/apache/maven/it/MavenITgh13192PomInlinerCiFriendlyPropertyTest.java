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
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@code PomInlinerTransformer} correctly inlines CI-friendly version properties
 * (e.g. {@code ${revision}}) that are defined in the project's own {@code <properties>} section
 * rather than being passed via {@code -Drevision=...} on the command line.
 *
 * <p>Prior to the fix, Maven 4 would throw
 * {@code IllegalArgumentException: Cannot inline property revision} in legacy mode
 * when {@code revision} was present only in POM properties.</p>
 *
 * @see <a href="https://github.com/apache/maven/issues/13192">GH-13192</a>
 */
class MavenITgh13192PomInlinerCiFriendlyPropertyTest extends AbstractMavenIntegrationTestCase {

    MavenITgh13192PomInlinerCiFriendlyPropertyTest() {
        super("[4.0.0-rc-7,)");
    }

    /**
     * Verify that {@code mvn install} in legacy mode succeeds when {@code ${revision}} is defined
     * only in POM {@code <properties>} (not via {@code -Drevision} on the command line), and that
     * the installed POM contains the literal version instead of the property placeholder.
     */
    @Test
    void testInstallSucceedsWithRevisionInPomProperties() throws Exception {
        File testDir = extractResources("gh-13192-ci-friendly-pom-property");

        Verifier verifier = newVerifier(testDir.getPath());
        verifier.setAutoclean(false);
        // Legacy (Maven 3 personality) mode triggers PomInlinerTransformer
        verifier.addCliArgument("-Dmaven.maven3Personality=true");
        // Intentionally do NOT pass -Drevision=... — revision is only in <properties>
        verifier.addCliArguments("clean", "install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // The installed POM for base-project should contain the literal version "1.0.0",
        // not the placeholder "${revision}".
        String installedParentPom = verifier.getArtifactPath("gh-13192-ci-friendly", "base-project", "1.0.0", "pom");
        assertTrue(new File(installedParentPom).exists(), "Installed parent POM should exist: " + installedParentPom);
        String parentPomContent = Files.readString(Paths.get(installedParentPom));
        assertFalse(
                parentPomContent.contains("${revision}"),
                "Installed parent POM should not contain '${revision}' placeholder");
        assertTrue(
                parentPomContent.contains("1.0.0"),
                "Installed parent POM should contain literal version '1.0.0'");

        // module-1 installed POM should also have the literal version
        String installedModule1Pom = verifier.getArtifactPath("gh-13192-ci-friendly", "module-1", "1.0.0", "pom");
        assertTrue(
                new File(installedModule1Pom).exists(), "Installed module-1 POM should exist: " + installedModule1Pom);
        String module1PomContent = Files.readString(Paths.get(installedModule1Pom));
        assertFalse(
                module1PomContent.contains("${revision}"),
                "Installed module-1 POM should not contain '${revision}' placeholder");
    }

    /**
     * Verify that a subsequent partial reactor build can resolve the installed artifact from
     * the local repository (i.e. the installed POM's version is resolvable without -Drevision).
     */
    @Test
    void testPartialBuildAfterInstall() throws Exception {
        File testDir = extractResources("gh-13192-ci-friendly-pom-property");

        // First: install all modules (revision from POM properties, no -D flag)
        Verifier installVerifier = newVerifier(testDir.getPath());
        installVerifier.setAutoclean(false);
        installVerifier.setLogFileName("install-log.txt");
        installVerifier.addCliArgument("-Dmaven.maven3Personality=true");
        installVerifier.addCliArguments("clean", "install");
        installVerifier.execute();
        installVerifier.verifyErrorFreeLog();

        // Then: build only module-2, which depends on module-1 (must come from local repo)
        Verifier partialVerifier = newVerifier(testDir.getPath());
        partialVerifier.setAutoclean(false);
        partialVerifier.setLogFileName("partial-log.txt");
        partialVerifier.addCliArgument("-Dmaven.maven3Personality=true");
        partialVerifier.addCliArgument("-pl");
        partialVerifier.addCliArgument("module-2");
        partialVerifier.addCliArgument("package");
        partialVerifier.execute();
        partialVerifier.verifyErrorFreeLog();
    }
}
