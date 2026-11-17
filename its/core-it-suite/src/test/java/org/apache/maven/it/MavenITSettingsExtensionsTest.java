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

/**
 * Integration tests for {@code <extensions>} in settings.xml (project, user, and installation tiers).
 *
 * <ul>
 *   <li>Project settings {@code <extensions>} loads the extension (replaces {@code extensions.xml})
 *   <li>Project settings {@code <extensions>} with compact {@code id="g:a:v"} form
 *   <li>When project settings declares {@code <extensions>}, {@code .mvn/extensions.xml} is ignored
 * </ul>
 */
class MavenITSettingsExtensionsTest extends AbstractMavenIntegrationTestCase {

    /**
     * Extension declared via full attributes in project {@code .mvn/settings.xml} is loaded.
     */
    @Test
    void projectSettingsExtensionsLoaded() throws Exception {
        Path testDir = extractResources("settings-extensions");

        // Build and install the extension first
        Verifier extVerifier = newVerifier(testDir.resolve("extension"));
        extVerifier.setLogFileName("extension-install.txt");
        extVerifier.addCliArgument("install");
        extVerifier.execute();
        extVerifier.verifyErrorFreeLog();
        Path localRepo = extVerifier.getLocalRepository();

        // Run client project that declares the extension via settings.xml <extensions>
        Verifier verifier = newVerifier(testDir.resolve("project-settings"));
        verifier.setLogFileName("project-settings.txt");
        verifier.addCliArgument("-Dmaven.repo.local=" + localRepo);
        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyTextInLog("[INFO] Settings extension loaded!");
        verifier.verifyErrorFreeLog();
    }

    /**
     * Extension declared via compact {@code id="groupId:artifactId:version"} in project settings is loaded.
     */
    @Test
    void projectSettingsExtensionsCompactIdLoaded() throws Exception {
        Path testDir = extractResources("settings-extensions");

        Verifier extVerifier = newVerifier(testDir.resolve("extension"));
        extVerifier.setLogFileName("extension-install-compact.txt");
        extVerifier.addCliArgument("install");
        extVerifier.execute();
        extVerifier.verifyErrorFreeLog();
        Path localRepo = extVerifier.getLocalRepository();

        Verifier verifier = newVerifier(testDir.resolve("project-settings-compact"));
        verifier.setLogFileName("project-settings-compact.txt");
        verifier.addCliArgument("-Dmaven.repo.local=" + localRepo);
        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyTextInLog("[INFO] Settings extension loaded!");
        verifier.verifyErrorFreeLog();
    }

    /**
     * When {@code <extensions>} is present in project settings, {@code .mvn/extensions.xml} is ignored.
     * The project has both; only the extension declared in settings must be loaded.
     */
    @Test
    void settingsExtensionsTakePrecedenceOverExtensionsXml() throws Exception {
        Path testDir = extractResources("settings-extensions");

        Verifier extVerifier = newVerifier(testDir.resolve("extension"));
        extVerifier.setLogFileName("extension-install-precedence.txt");
        extVerifier.addCliArgument("install");
        extVerifier.execute();
        extVerifier.verifyErrorFreeLog();
        Path localRepo = extVerifier.getLocalRepository();

        // This project has both .mvn/settings.xml <extensions> AND .mvn/extensions.xml
        // settings.xml wins → extension loaded; extensions.xml ignored
        Verifier verifier = newVerifier(testDir.resolve("precedence"));
        verifier.setLogFileName("precedence.txt");
        verifier.addCliArgument("-Dmaven.repo.local=" + localRepo);
        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyTextInLog("[INFO] Settings extension loaded!");
        verifier.verifyErrorFreeLog();
    }
}
