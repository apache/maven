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

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.v4.MavenStaxReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the consumer POM builder can resolve BOM imports from repositories
 * defined only in settings.xml profiles (not in the project POM itself).
 * <p>
 * This is a regression test for a bug where {@code DefaultConsumerPomBuilder.buildModel()}
 * did not pass repositories, profiles, or active profile IDs to the {@code ModelBuilderRequest},
 * and {@code DefaultModelBuilder.derive()} ignored the request's repositories when creating
 * derived sessions. This caused "Non-resolvable import POM" failures during the install phase
 * for artifacts hosted in private/corporate repositories configured via settings.xml.
 *
 * @since 4.0.0
 */
class MavenITConsumerPomBomFromSettingsRepoTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verifies that consumer POM flattening works when the BOM is only available
     * from a repository defined in a settings.xml profile.
     * <p>
     * Without the fix, this test fails with:
     * <pre>
     * Non-resolvable import POM: Could not find artifact
     * org.apache.maven.its.cpbom:the-bom:pom:1.0 in central
     * </pre>
     */
    @Test
    void testConsumerPomWithBomFromSettingsProfileRepo() throws Exception {
        Path basedir = extractResources("/gh-11767-consumer-pom-bom-from-settings-repo")
                .toPath()
                .toAbsolutePath();

        Verifier verifier = newVerifier(basedir.toString());
        verifier.deleteArtifacts("org.apache.maven.its.cpbom");

        // Apply settings template with the custom repository URL
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");

        // Enable consumer POM flattening to trigger full BOM resolution
        // during the install phase consumer POM transformation
        verifier.addCliArgument("-Dmaven.consumer.pom.flatten=true");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Verify the consumer POM was generated
        Path consumerPom = basedir.resolve(Path.of(
                "target",
                "project-local-repo",
                "org.apache.maven.its.cpbom",
                "consumer-pom-bom-settings-repo",
                "1.0",
                "consumer-pom-bom-settings-repo-1.0-consumer.pom"));
        assertTrue(Files.exists(consumerPom), "consumer POM not found at " + consumerPom);

        // Read and validate the consumer POM content
        Model consumerPomModel;
        try (Reader r = Files.newBufferedReader(consumerPom)) {
            consumerPomModel = new MavenStaxReader().read(r);
        }

        // The consumer POM should have the dependency with the version resolved from the BOM
        assertNotNull(consumerPomModel.getDependencies(), "Consumer POM should have dependencies");
        assertFalse(consumerPomModel.getDependencies().isEmpty(), "Consumer POM should have at least one dependency");

        boolean hasLibA = consumerPomModel.getDependencies().stream()
                .anyMatch(d -> "lib-a".equals(d.getArtifactId())
                        && "org.apache.maven.its.cpbom".equals(d.getGroupId())
                        && "2.0".equals(d.getVersion()));
        assertTrue(
                hasLibA,
                "Consumer POM should contain lib-a with version 2.0 resolved from the BOM. "
                        + "Actual dependencies: " + consumerPomModel.getDependencies());

        // The BOM import should NOT appear in the consumer POM (it's been flattened)
        if (consumerPomModel.getDependencyManagement() != null) {
            boolean hasBomImport = consumerPomModel.getDependencyManagement().getDependencies().stream()
                    .anyMatch(d -> "the-bom".equals(d.getArtifactId()) && "import".equals(d.getScope()));
            assertFalse(hasBomImport, "Consumer POM should not contain the BOM import after flattening");
        }
    }
}
