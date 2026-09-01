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
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a BOM's consumer POM inherits the version from an imported BOM
 * when the local dependency management entry declares only a scope but no version.
 * <p>
 * Reproducer for <a href="https://github.com/apache/maven/issues/12660">#12660</a>:
 * when a BOM module declares a managed dependency with {@code scope=provided} but
 * no version, and the version is expected to come from an imported BOM, the consumer
 * POM was generated without the version. The fix in
 * {@code DefaultDependencyManagementImporter.importManagement()} now merges the
 * version from the imported entry into the local entry when the local version is null.
 *
 * @since 4.0.0
 */
class MavenITgh12660BomVersionFromImportedBomTest extends AbstractMavenIntegrationTestCase {

    MavenITgh12660BomVersionFromImportedBomTest() {
        super("[4.0.0-rc-1,)");
    }

    /**
     * Verify that the BOM consumer POM contains the version inherited from the
     * imported BOM for a locally-declared dependency with no version.
     * <p>
     * The BOM module imports {@code ext-bom} (which declares {@code ext-lib} version
     * {@code 3.0.0}) and locally declares {@code ext-lib} with {@code scope=provided}
     * but no version. The consumer POM must contain {@code ext-lib} with version
     * {@code 3.0.0} and scope {@code provided}.
     */
    @Test
    void testBomConsumerPomInheritsVersionFromImportedBom() throws Exception {
        File basedir = extractResources("/gh-12660-bom-version-from-imported-bom");

        Verifier verifier = newVerifier(basedir.getAbsolutePath());
        verifier.deleteArtifacts("org.apache.maven.its.gh12660");
        verifier.addCliArguments("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Read the consumer POM that was installed to the local repo
        Path consumerPomPath =
                Path.of(verifier.getArtifactPath("org.apache.maven.its.gh12660", "bom", "1.0.0-SNAPSHOT", "pom"));

        assertTrue(Files.exists(consumerPomPath), "Consumer POM not found at " + consumerPomPath);

        String content = Files.readString(consumerPomPath);

        // 1. Packaging must be "pom" (not "bom")
        assertTrue(content.contains("<packaging>pom</packaging>"), "Consumer POM packaging should be 'pom'");

        // 2. Must contain the declared entries: mod-1, mod-2, and ext-lib
        assertTrue(
                content.contains("<artifactId>mod-1</artifactId>"),
                "Consumer POM must contain mod-1.\nActual:\n" + content);
        assertTrue(
                content.contains("<artifactId>mod-2</artifactId>"),
                "Consumer POM must contain mod-2.\nActual:\n" + content);
        assertTrue(
                content.contains("<artifactId>ext-lib</artifactId>"),
                "Consumer POM must contain ext-lib.\nActual:\n" + content);

        // 3. ext-lib must have version 3.0.0 (inherited from the imported ext-bom)
        // This is the core assertion for the #12660 fix
        assertTrue(
                content.contains("<version>3.0.0</version>"),
                "Consumer POM must contain ext-lib with version 3.0.0 from imported BOM.\nActual:\n" + content);

        // 4. ext-lib must preserve scope=provided
        assertTrue(
                content.contains("<scope>provided</scope>"),
                "Consumer POM must preserve ext-lib scope=provided.\nActual:\n" + content);

        // 5. Must not contain unresolved property references
        assertFalse(
                content.contains("${"),
                "Consumer POM must not contain unresolved property references.\nActual:\n" + content);
    }
}
