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

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This is a test set for <a href="https://github.com/apache/maven/issues/11321">GH-11321</a>.
 * Verify that Maven properly rejects setups where a parent POM is located above the root directory
 * when a .mvn directory exists in a subdirectory and Maven is invoked with -f pointing to that subdirectory.
 *
 * @since 4.0.0
 */
public class MavenITgh11321Test extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that Maven properly rejects setups where a parent POM is located above the root directory.
     * When Maven is invoked with -f deps/ where deps contains a .mvn directory, and the deps/pom.xml
     * uses parent inference to find a parent above the root directory, it should fail with a proper error message.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testParentAboveRootDirectoryRejected() throws Exception {
        File testDir = extractResources("/gh-11321-parent-above-root");

        // First, verify that normal build works from the actual root
        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // Now test with -f pointing to the subdirectory that contains .mvn
        // This should fail with a proper error message about parent being above root
        verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("-f");
        verifier.addCliArgument("deps");
        verifier.addCliArgument("validate");
        assertThrows(
                VerificationException.class,
                verifier::execute,
                "Expected validation to fail when using invalid project structure");
        verifier.verifyTextInLog("Parent POM");
        verifier.verifyTextInLog("is located above the root directory");
        verifier.verifyTextInLog("This setup is invalid when a .mvn directory exists in a subdirectory");
    }
}
