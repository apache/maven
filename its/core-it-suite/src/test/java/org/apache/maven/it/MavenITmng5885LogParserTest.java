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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MavenITmng5885LogParserTest extends AbstractMavenIntegrationTestCase {

    private static final String OVERLAP_WARNING_PREFIX =
            "The following lifecycle phases overlap and will cause some plugins to execute multiple times:";

    private static final String EXPECTED_WARNING_LINE = "[WARNING] " + OVERLAP_WARNING_PREFIX
            + " [after:resources, compile]. If this is not intended, review the phases passed on the command line.";

    /**
     * Test that executes 'mvn package compile'.
     * This should produce a warning message about duplicate lifecycle phase execution.
     */
    @Test
    public void testDuplicateLifecyclePhases() throws Exception {
        Path testDir = extractResources("mng-5885");
        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("package");
        verifier.addCliArgument("compile");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> logLines = verifier.loadLogLines();
        long matchingLines = logLines.stream()
                .map(Verifier::stripAnsi)
                .filter(EXPECTED_WARNING_LINE::equals)
                .count();
        assertEquals(1, matchingLines, "Expected exactly one log line: " + EXPECTED_WARNING_LINE);
    }

    /**
     * Test that executes 'mvn package'.
     * The warning message should not be logged.
     */
    @Test
    public void testNormalLifecyclePhases() throws Exception {
        Path testDir = extractResources("mng-5885");
        Verifier verifier = newVerifier(testDir);
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.addCliArgument("package");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        verifier.verifyTextNotInLog(OVERLAP_WARNING_PREFIX);
    }
}
