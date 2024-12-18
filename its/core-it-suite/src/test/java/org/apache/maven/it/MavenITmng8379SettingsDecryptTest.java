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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8379">MNG-8379</a>.
 */
class MavenITmng8379SettingsDecryptTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8379SettingsDecryptTest() {
        super("[4.0.0-beta-6,)");
    }

    /**
     *  Verify that all settings are decrypted
     */
    @Test
    void testLegacy() throws Exception {
        File testDir = extractResources("/mng-8379-decrypt-settings");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setLogFileName("log-legacy.txt");
        verifier.setUserHomeDirectory(new File(testDir, "legacyhome").toPath());
        verifier.addCliArgument("org.apache.maven.plugins:maven-help-plugin:3.3.0:effective-settings");
        verifier.addCliArgument("-DshowPasswords");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // there is a warning and all fields decrypted
        verifier.verifyTextInLog(" encountered while building the effective settings (use -e to see details)");
        verifier.verifyTextInLog("<password>testtest</password>");
        verifier.verifyTextInLog("<value>testtest</value>");
    }

    /**
     *  Verify that all settings are decrypted
     */
    @Test
    void testModern() throws Exception {
        File testDir = extractResources("/mng-8379-decrypt-settings");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setLogFileName("log-modern.txt");
        verifier.setEnvironmentVariable("MAVEN_MASTER_PASSWORD", "master");
        verifier.setUserHomeDirectory(new File(testDir, "home").toPath());
        verifier.addCliArgument("org.apache.maven.plugins:maven-help-plugin:3.3.0:effective-settings");
        verifier.addCliArgument("-DshowPasswords");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // there is no warning and all fields decrypted
        verifier.verifyTextNotInLog("[WARNING]");
        verifier.verifyTextNotInLog(" encountered while building the effective settings (use -e to see details)");
        verifier.verifyTextInLog("<password>testtest</password>");
        verifier.verifyTextInLog("<value>secretHeader</value>");
    }
}
