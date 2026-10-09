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
 * Verifies that {@code -Dmaven.lifecycle.filter=phase(test)} skips the {@code test} phase,
 * while running the same lifecycle without the filter executes tests normally.
 *
 * @see <a href="https://github.com/apache/maven/issues/12535">MNG-12535</a>
 */
class MavenITmng12535LifecycleFilterPhaseTest extends AbstractMavenIntegrationTestCase {

    @Test
    void lifecycleFilterSkipsTestPhase() throws Exception {
        Path testDir = extractResources("/mng-12535-lifecycle-filter-phase");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("-Dmaven.lifecycle.filter=phase(test)");
        verifier.addCliArgument("test");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Surefire ran → would produce "Tests run:" in log; with filter it must NOT appear
        verifier.verifyTextNotInLog("Tests run:");
    }

    @Test
    void withoutFilterSurefireRunsNormally() throws Exception {
        Path testDir = extractResources("/mng-12535-lifecycle-filter-phase");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("test");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("Tests run:");
    }
}
