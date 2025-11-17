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
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8294">MNG-8294</a>.
 * @since 4.0.0-beta-5
 *
 */
class MavenITmng8294ParentChecksTest extends AbstractMavenIntegrationTestCase {

    /**
     *  Verify error when mismatch between GAV and relativePath
     */
    @Test
    void testitbadMismatch() throws Exception {
        Path testDir = extractResources("/mng-8294-parent-checks");

        Verifier verifier = newVerifier(testDir.resolve("bad-mismatch"));
        verifier.addCliArgument("validate");
        assertThrows(VerificationException.class, verifier::execute);
        verifier.verifyTextInLog(
                "at org.apache.maven.its.mng8294:parent instead of org.apache.maven.its.mng8294:bad-parent");
    }

    /**
     *  Verify error when the parent is not resolvable
     */
    @Test
    void testitbadNonResolvable() throws Exception {
        Path testDir = extractResources("/mng-8294-parent-checks");

        Verifier verifier = newVerifier(testDir.resolve("bad-non-resolvable"));
        verifier.addCliArgument("validate");
        assertThrows(VerificationException.class, verifier::execute);
        verifier.verifyTextInLog(
                "The following artifacts could not be resolved: org.apache.maven.its.mng8294:parent:pom:0.1-SNAPSHOT");
    }

    /**
     *  Verify error when a wrong path
     */
    @Test
    void testitbadWrongPath() throws Exception {
        Path testDir = extractResources("/mng-8294-parent-checks");

        Verifier verifier = newVerifier(testDir.resolve("bad-wrong-path"));
        verifier.addCliArgument("validate");
        assertThrows(VerificationException.class, verifier::execute);
        verifier.verifyTextInLog("points at '../foo' but no POM could be found");
    }

    /**
     *  Verify error when a wrong path
     */
    @Test
    void testitokUsingEmpty() throws Exception {
        Path testDir = extractResources("/mng-8294-parent-checks");

        Verifier verifier = newVerifier(testDir.resolve("ok-using-empty"));
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
    /**
     *  Verify error when a wrong path
     */
    @Test
    void testitokUsingGav() throws Exception {
        Path testDir = extractResources("/mng-8294-parent-checks");

        Verifier verifier = newVerifier(testDir.resolve("ok-using-gav"));
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
    /**
     *  Verify error when a wrong path
     */
    @Test
    void testitokUsingPath() throws Exception {
        Path testDir = extractResources("/mng-8294-parent-checks");

        Verifier verifier = newVerifier(testDir.resolve("ok-using-path"));
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }
}
