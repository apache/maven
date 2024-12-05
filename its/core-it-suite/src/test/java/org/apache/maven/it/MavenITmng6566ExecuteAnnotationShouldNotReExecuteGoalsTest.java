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
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MavenITmng6566ExecuteAnnotationShouldNotReExecuteGoalsTest extends AbstractMavenIntegrationTestCase {
    private static final String RESOURCE_PATH = "/mng-6566-execute-annotation-should-not-re-execute-goals";
    private static final String PLUGIN_KEY = "org.apache.maven.its.mng6566:plugin:1.0-SNAPSHOT";

    private File testDir;

    public MavenITmng6566ExecuteAnnotationShouldNotReExecuteGoalsTest() {
        super("[4.0.0-alpha-1,)");
    }

    @BeforeEach
    public void setUp() throws Exception {
        testDir = extractResources(RESOURCE_PATH);

        File pluginDir = new File(testDir, "plugin");
        Verifier verifier = newVerifier(pluginDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testRunsCompileGoalOnceWithDirectPluginInvocation() throws Exception {
        File consumerDir = new File(testDir, "consumer");

        Verifier verifier = newVerifier(consumerDir.getAbsolutePath());
        verifier.setLogFileName("log-direct-plugin-invocation.txt");
        verifier.addCliArgument(PLUGIN_KEY + ":require-compile-phase");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertCompiledOnce(verifier);
        verifier.verifyTextInLog("MNG-6566 plugin require-compile-phase goal executed");
    }

    /**
     * This test uses the <pre>require-compile-phase</pre> goal of the test plugin.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testRunsCompileGoalOnceWithPhaseExecution() throws Exception {
        File consumerDir = new File(testDir, "consumer");

        Verifier verifier = newVerifier(consumerDir.getAbsolutePath());
        verifier.setLogFileName("log-phase-execution.txt");
        verifier.addCliArgument("compile");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        assertCompiledOnce(verifier);
        verifier.verifyTextInLog("MNG-6566 plugin require-compile-phase goal executed");
    }

    private void assertCompiledOnce(Verifier verifier) throws IOException {
        long count = verifier.textOccurrencesInLog("compiler:0.1-stub-SNAPSHOT:compile");
        assertEquals(count, 1L, "Compile goal was expected to run once");
    }
}
