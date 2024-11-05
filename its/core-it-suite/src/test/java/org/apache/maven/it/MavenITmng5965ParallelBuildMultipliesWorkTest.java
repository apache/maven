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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * Test set for <a href="https://issues.apache.org/jira/browse/MNG-5965">MNG-5965</a>.
 */
public class MavenITmng5965ParallelBuildMultipliesWorkTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng5965ParallelBuildMultipliesWorkTest() {
        super("[3.6.1,)");
    }

    @Test
    public void testItShouldOnlyRunEachTaskOnce() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-5965-parallel-build-multiplies-work");

        Verifier verifier = newVerifier(testDir.getAbsolutePath(), false);
        verifier.setAutoclean(false);

        verifier.setLogFileName("log-only.txt");
        verifier.addCliArgument("-T1");
        // include an aggregator task so that the two goals end up in different task segments
        verifier.addCliArguments("clean", "install:help");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> logLines = verifier.loadLines("log-only.txt", "UTF-8");

        List<String> cleanGoalsExecuted = findCleanExecutions(logLines);

        // clean only executed once per module
        assertNoRepeatedLines(cleanGoalsExecuted);

        // clean executed in the 3 modules
        assertEquals(cleanGoalsExecuted.size(), 3);
    }

    private void assertNoRepeatedLines(List<String> logLines) throws VerificationException {
        Set<String> uniqueLines = new LinkedHashSet<>();
        for (String line : logLines) {
            if (uniqueLines.contains(line)) {
                throw new VerificationException("Goal executed twice: " + line);
            }
            uniqueLines.add(line);
        }
    }

    private List<String> findCleanExecutions(List<String> fullLog) {
        List<String> cleanExecutions = new ArrayList<>();
        for (String line : fullLog) {
            if (line.contains("(default-clean)")) {
                cleanExecutions.add(line);
            }
        }

        return cleanExecutions;
    }
}
