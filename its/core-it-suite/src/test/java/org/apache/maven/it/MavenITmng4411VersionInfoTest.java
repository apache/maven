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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4411">MNG-4411</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4411VersionInfoTest extends AbstractMavenIntegrationTestCase {

    /**
     * Verify that "mvn --version" outputs the Maven version and stops the execution after that.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        Path testDir = extractResources("mng-4411");

        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.addCliArgument("--version");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // When using --version, Maven may use the shell fast-path (no JVM) which outputs to stdout
        // rather than the -l log file. Check both sources so the test passes in both embedded
        // (in-process, output goes to log) and forked (shell fast-path, output goes to stdout) modes.
        boolean inLog = verifier.textOccurrencesInLog("Maven") > 0;
        boolean inStdout = verifier.getStdout().contains("Maven");
        assertTrue(inLog || inStdout, "Expected 'Maven' in log or stdout");
    }
}
