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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8245">MNG-8245</a>
 *   and <a href="https://issues.apache.org/jira/browse/MNG-8246">MNG-8246</a>.
 */
class MavenITmng8245BeforePhaseCliTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8245BeforePhaseCliTest() {
        super("[4.0.0-rc-2,)");
    }

    /**
     *  Verify phase before:clean spits a warning and calls clean
     */
    @Test
    void testPhaseBeforeCleanAllWihConcurrentBuilder() throws Exception {
        File testDir = extractResources("/mng-8245-before-after-phase-all");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setLogFileName("before-clean-concurrent.txt");
        verifier.addCliArguments("-b", "concurrent", "before:clean");
        verifier.execute();

        verifier.verifyTextInLog("Illegal call to phase 'before:clean'. The main phase 'clean' will be used instead.");
        verifier.verifyTextInLog("Hallo 'before:clean' phase.");
        verifier.verifyTextInLog("Hallo 'after:clean' phase.");
    }

    /**
     *  Verify phase before:clean spits a warning and calls clean
     */
    @Test
    void testPhaseBeforeCleanAllWithLegacyBuilder() throws Exception {
        File testDir = extractResources("/mng-8245-before-after-phase-all");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setLogFileName("before-clean-legacy.txt");
        verifier.addCliArguments("before:clean");
        verifier.execute();

        verifier.verifyTextInLog("Illegal call to phase 'before:clean'. The main phase 'clean' will be used instead.");
        verifier.verifyTextInLog("Hallo 'before:clean' phase.");
        verifier.verifyTextInLog("Hallo 'after:clean' phase.");
    }

    /**
     *  Verify phase after:clean spits a warning and calls clean
     */
    @Test
    void testPhaseAfterCleanAllWihConcurrentBuilder() throws Exception {
        File testDir = extractResources("/mng-8245-before-after-phase-all");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setLogFileName("after-clean-concurrent.txt");
        verifier.addCliArguments("-b", "concurrent", "after:clean");
        verifier.execute();

        verifier.verifyTextInLog("Illegal call to phase 'after:clean'. The main phase 'clean' will be used instead.");
        verifier.verifyTextInLog("Hallo 'before:clean' phase.");
        verifier.verifyTextInLog("Hallo 'after:clean' phase.");
    }

    /**
     *  Verify phase after:clean spits a warning and calls clean
     */
    @Test
    void testPhaseAfterCleanAllWithLegacyBuilder() throws Exception {
        File testDir = extractResources("/mng-8245-before-after-phase-all");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setLogFileName("after-clean-legacy.txt");
        verifier.addCliArguments("after:clean");
        verifier.execute();

        verifier.verifyTextInLog("Illegal call to phase 'after:clean'. The main phase 'clean' will be used instead.");
        verifier.verifyTextInLog("Hallo 'before:clean' phase.");
        verifier.verifyTextInLog("Hallo 'after:clean' phase.");
    }
}
