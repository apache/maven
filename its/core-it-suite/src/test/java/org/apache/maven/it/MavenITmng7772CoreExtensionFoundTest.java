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

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;

public class MavenITmng7772CoreExtensionFoundTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng7772CoreExtensionFoundTest() {
        super("(4.0.0-alpha-7,)");
    }

    @Test
    public void testWithExtensionsXmlCoreExtensionsFound() throws Exception {
        File testDir = extractResources("/mng-7772-core-extensions-found");

        Verifier verifier = newVerifier(new File(testDir, "extension").getAbsolutePath());
        verifier.setLogFileName("extension-install.txt");
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        String installedToLocalRepo = verifier.getLocalRepository();

        verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setUserHomeDirectory(Paths.get(testDir.toPath().toString(), "home-extensions-xml"));
        verifier.addCliArgument("-Dmaven.repo.local=" + installedToLocalRepo);

        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyTextInLog("[INFO] Extension loaded!");
    }

    @Test
    public void testWithLibExtCoreExtensionsFound() throws Exception {
        File testDir = extractResources("/mng-7772-core-extensions-found");

        Path extensionBasedir = new File(testDir, "extension").getAbsoluteFile().toPath();
        Verifier verifier = newVerifier(extensionBasedir.toString());
        verifier.setLogFileName("extension-package.txt");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Path jarPath = extensionBasedir.resolve("target").resolve("maven-it-core-extensions-0.1.jar");

        assertTrue("Jar output path was not built", Files.isRegularFile(jarPath));

        verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setUserHomeDirectory(Paths.get(testDir.toPath().toString(), "home-lib-ext"));
        verifier.addCliArgument("-Dmaven.ext.class.path=" + jarPath);
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyTextInLog("[INFO] Extension loaded!");
    }
}
