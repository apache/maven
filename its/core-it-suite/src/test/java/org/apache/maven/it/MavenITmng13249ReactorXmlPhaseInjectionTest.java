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
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that custom lifecycle phases defined in {@code .mvn/reactor.xml} are injected
 * into the lifecycle DAG and that plugin goals bound to those phases execute correctly.
 *
 * @see <a href="https://github.com/apache/maven/pull/13249">#13249</a>
 */
class MavenITmng13249ReactorXmlPhaseInjectionTest extends AbstractMavenIntegrationTestCase {

    /**
     * A plugin goal bound to a reactor.xml-injected phase must execute when running
     * through that phase (via {@code verify}).
     */
    @Test
    void pluginBoundToInjectedPhaseExecutesDuringVerify() throws Exception {
        Path testDir = extractResources("/mng-13249-reactor-xml-phase-injection");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/pre-integration-marker.txt");
    }

    /**
     * The injected phase can be used as a direct CLI target.
     */
    @Test
    void injectedPhaseCanBeUsedAsCliTarget() throws Exception {
        Path testDir = extractResources("/mng-13249-reactor-xml-phase-injection");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("pre-integration");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/pre-integration-marker.txt");
    }

    /**
     * The injected phase respects ordering: running only up to {@code compile}
     * (which comes before {@code pre-integration}) must NOT trigger the plugin.
     */
    @Test
    void pluginDoesNotRunBeforeInjectedPhase() throws Exception {
        Path testDir = extractResources("/mng-13249-reactor-xml-phase-injection");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("compile");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFileNotPresent("target/pre-integration-marker.txt");
    }

    /**
     * Verify the injected phase runs in the correct position: after unit-test
     * related phases but before integration-test phases. We check log ordering
     * by looking for the execution banner lines ({@code [INFO] ---}) which only
     * appear at actual phase-execution time, not at plan-calculation time.
     */
    @Test
    void injectedPhaseRunsInCorrectOrder() throws Exception {
        Path testDir = extractResources("/mng-13249-reactor-xml-phase-injection");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> logLines = verifier.loadLogLines();

        int touchIdx = -1;
        int compileIdx = -1;
        for (int i = 0; i < logLines.size(); i++) {
            String line = logLines.get(i);
            // Match the execution banner "[INFO] --- compiler:...:compile" which only
            // appears when the compiler goal actually executes, not during plan calculation.
            if (line.contains("[INFO] ---") && line.contains("compiler") && line.contains(":compile")) {
                if (compileIdx == -1) {
                    compileIdx = i;
                }
            }
            if (line.contains("touch-in-custom-phase")) {
                touchIdx = i;
            }
        }

        assertTrue(compileIdx >= 0, "compiler plugin execution banner must appear in log");
        assertTrue(touchIdx >= 0, "touch plugin for pre-integration must appear in log");
        assertTrue(
                compileIdx < touchIdx,
                "compile must run before the injected pre-integration phase (compile at "
                        + compileIdx + ", touch at " + touchIdx + ")");
    }
}
