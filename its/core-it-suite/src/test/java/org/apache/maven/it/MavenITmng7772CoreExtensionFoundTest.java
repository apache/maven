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
import java.nio.file.Paths;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MavenITmng7772CoreExtensionFoundTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng7772CoreExtensionFoundTest() {
        super("(4.0.0-alpha-7,)");
    }

    @Test
    public void testWithExtensionsXmlCoreExtensionsFound() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7772-core-extensions-found");

        Verifier verifier = newVerifier(new File(testDir, "extension").getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(testDir.getAbsolutePath());
        ItUtils.setUserHome(verifier, Paths.get(testDir.toPath().toString(), "home-extensions-xml"));
        verifier.setForkJvm(true);

        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyTextInLog("[INFO] Extension loaded!");
    }

    @Test
    public void testWithLibExtCoreExtensionsFound() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7772-core-extensions-found");

        Verifier verifier = newVerifier(new File(testDir, "extension").getAbsolutePath());
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path jarPath = Paths.get(verifier.getArtifactPath(
                "org.apache.maven.its.7772-core-extensions-scopes", "maven-it-core-extensions", "0.1", "jar", ""));

        assertNotNull(jarPath, "Jar output path was not found in the log");

        Path jarToPath = Paths.get(testDir.toString(), "home-lib-ext", ".m2", "ext", "extension.jar");
        try {
            Files.copy(jarPath, jarToPath);

            verifier = newVerifier(testDir.getAbsolutePath());
            ItUtils.setUserHome(verifier, Paths.get(testDir.toPath().toString(), "home-lib-ext"));
            verifier.setForkJvm(true);
            verifier.addCliArgument("validate");
            verifier.execute();
            verifier.verifyTextInLog("[INFO] Extension loaded!");
        } finally {
            Files.deleteIfExists(jarToPath);
        }
    }
}
