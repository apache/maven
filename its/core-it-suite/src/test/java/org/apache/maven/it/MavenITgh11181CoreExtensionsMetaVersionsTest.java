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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for GH-11181.
 * @since 4.1.0-SNAPSHOT
 */
class MavenITgh11181CoreExtensionsMetaVersionsTest extends AbstractMavenIntegrationTestCase {

    /**
     * Project wide extensions: use of meta versions is invalid.
     */
    @Test
    void pwMetaVersionIsInvalid() throws Exception {
        Path testDir = extractResources("/gh-11181-core-extensions-meta-versions")
                .toPath()
                .toAbsolutePath()
                .resolve("pw-metaversion-is-invalid");
        Verifier verifier = newVerifier(testDir.toString());
        verifier.setUserHomeDirectory(testDir.resolve("HOME"));
        verifier.setAutoclean(false);
        verifier.addCliArgument("validate");
        try {
            verifier.execute();
            fail("Expected VerificationException");
        } catch (VerificationException e) {
            // there is not even a log; this is very early failure
            assertTrue(e.getMessage().contains("Error executing Maven."));
        }
    }

    /**
     * User wide extensions: use of meta versions is valid.
     */
    @Test
    void uwMetaVersionIsValid() throws Exception {
        Path testDir = extractResources("/gh-11181-core-extensions-meta-versions")
                .toPath()
                .toAbsolutePath()
                .resolve("uw-metaversion-is-valid");
        Verifier verifier = newVerifier(testDir.toString());
        verifier.setUserHomeDirectory(testDir.resolve("HOME"));
        verifier.setHandleLocalRepoTail(false);
        verifier.setAutoclean(false);
        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyErrorFreeLog();
    }

    /**
     * Same GA different V extensions in project-wide and user-wide: warn for conflict.
     */
    @Test
    void uwPwDifferentVersionIsConflict() throws Exception {
        Path testDir = extractResources("/gh-11181-core-extensions-meta-versions")
                .toPath()
                .toAbsolutePath()
                .resolve("uw-pw-different-version-is-conflict");
        Verifier verifier = newVerifier(testDir.toString());
        verifier.setUserHomeDirectory(testDir.resolve("HOME"));
        verifier.setHandleLocalRepoTail(false);
        verifier.setAutoclean(false);
        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("WARNING");
        verifier.verifyTextInLog("Conflicting extension io.takari.maven:takari-smart-builder");
    }

    /**
     * Same GAV extensions in project-wide and user-wide: do not warn for conflict.
     */
    @Test
    void uwPwSameVersionIsNotConflict() throws Exception {
        Path testDir = extractResources("/gh-11181-core-extensions-meta-versions")
                .toPath()
                .toAbsolutePath()
                .resolve("uw-pw-same-version-is-not-conflict");
        Verifier verifier = newVerifier(testDir.toString());
        verifier.setUserHomeDirectory(testDir.resolve("HOME"));
        verifier.setHandleLocalRepoTail(false);
        verifier.setAutoclean(false);
        verifier.addCliArgument("validate");
        verifier.execute();

        verifier.verifyErrorFreeLog();
        verifier.verifyTextNotInLog("WARNING");
    }
}
