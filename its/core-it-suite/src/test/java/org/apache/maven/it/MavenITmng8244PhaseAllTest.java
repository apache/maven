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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8244">MNG-8244</a>.
 */
class MavenITmng8244PhaseAllTest extends AbstractMavenIntegrationTestCase {

    MavenITmng8244PhaseAllTest() {
        super("[4.0.0-rc-2,)");
    }

    /**
     *  Verify phase after:all phase is called
     */
    @Test
    void testPhaseAllWihConcurrentBuilder() throws Exception {
        File testDir = extractResources("/mng-8244-phase-all");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setLogFileName("build-concurrent.txt");
        verifier.addCliArguments("-b", "concurrent", "build");
        verifier.execute();

        verifier.verifyTextInLog("Hallo 'before:all' phase.");
        verifier.verifyTextInLog("Hallo 'after:all' phase.");
    }

    /**
     *  Verify phase after:all phase is called
     */
    @Test
    void testPhaseAllWithLegacyBuilder() throws Exception {
        File testDir = extractResources("/mng-8244-phase-all");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setLogFileName("build-legacy.txt");
        verifier.addCliArguments("build");
        verifier.execute();

        verifier.verifyTextInLog("Hallo 'before:all' phase.");
        verifier.verifyTextInLog("Hallo 'after:all' phase.");
    }
}
