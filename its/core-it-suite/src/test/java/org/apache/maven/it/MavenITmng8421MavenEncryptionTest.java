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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8421">MNG-8421</a>.
 */
class MavenITmng8421MavenEncryptionTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8421MavenEncryptionTest() {
        super("[4.0.0-rc-2-SNAPSHOT,)");
    }

    /**
     *  Verify that empty home causes diag output as expected.
     */
    @Test
    void testEmptyHome() throws Exception {
        Path basedir = extractResources("/mng-8421").getAbsoluteFile().toPath();

        Path home = basedir.resolve("home1");

        Verifier verifier = newVerifier(basedir.toString());
        verifier.setLogFileName("home1.txt");
        verifier.setUserHomeDirectory(home);
        verifier.setExecutable("mvnenc");
        verifier.addCliArgument("diag");
        verifier.execute();
        verifier.verifyTextInLog("[ERROR]");
        verifier.verifyTextInLog("No configuration file found");
        verifier.verifyTextInLog("settings-security4.xml");
    }

    /**
     *  Verify that set-upo home causes diag output as expected.
     */
    @Test
    void testSetupHome() throws Exception {
        Path basedir = extractResources("/mng-8421").getAbsoluteFile().toPath();

        Path home = basedir.resolve("home2");

        Verifier verifier = newVerifier(basedir.toString());
        verifier.setLogFileName("home2.txt");
        verifier.setUserHomeDirectory(home);
        verifier.setExecutable("mvnenc");
        verifier.addCliArgument("diag");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("[INFO] Configuration validation of MavenSecDispatcher: VALID");
        verifier.verifyTextInLog("[WARNING]       Configured environment variable not exist");
    }
}
