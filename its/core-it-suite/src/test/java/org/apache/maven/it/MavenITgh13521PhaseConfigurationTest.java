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
 * This is a test set for <a href="https://github.com/apache/maven/issues/13521">GH-13521</a>.
 * Tests that phase configuration is applied to forked executions with no prior configuration.
 */
public class MavenITgh13521PhaseConfigurationTest extends AbstractMavenIntegrationTestCase {

    public MavenITgh13521PhaseConfigurationTest() {
        super("(4.0.0,)");
    }

    @Test
    public void testPhaseConfigurationAppliedToUnconfiguredExecution() throws Exception {
        File testDir = extractResources("/gh-13521-phase-configuration");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("org.apache.maven.its.plugins:maven-it-plugin-fork:fork-phase-config");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/phase-config/touch.txt");
        verifier.verifyFileNotPresent("target/touch.txt");
    }
}
