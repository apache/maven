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
 * This is a test for
 * <a href="https://github.com/apache/maven/issues/11796">MNG-11796</a>.
 * <p>
 * Verifies that {@code <default-phases>} in a custom lifecycle's {@code components.xml}
 * correctly binds plugin goals to standard lifecycle phases (e.g., {@code process-sources}).
 */
class MavenITmng11796DefaultPhasesStandardLifecycleTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that a plugin extension with {@code <default-phases>} mapping a goal
     * to the standard {@code process-sources} phase causes that goal to execute
     * during {@code mvn compile}.
     */
    @Test
    void testDefaultPhasesBindToStandardLifecyclePhases() throws Exception {
        File testDir = extractResources("/mng-11796-default-phases-standard-lifecycle");

        // Install the extension plugin
        Verifier verifier = newVerifier(new File(testDir, "extension-plugin").getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Run compile on the consumer project - the touch goal should execute at process-sources
        verifier = newVerifier(new File(testDir, "consumer-project").getAbsolutePath());
        verifier.addCliArgument("compile");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextInLog("MNG-11796 touch goal executed");
        verifier.verifyFilePresent("target/touch.txt");
    }
}
