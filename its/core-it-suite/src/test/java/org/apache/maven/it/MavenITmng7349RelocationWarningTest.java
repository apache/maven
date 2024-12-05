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
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MavenITmng7349RelocationWarningTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng7349RelocationWarningTest() {
        super("[3.8.5,)");
    }

    @Test
    public void testit() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7349-relocation-warning");
        File artifactsDir = new File(testDir, "artifacts");
        File projectDir = new File(testDir, "project");

        Verifier verifier;

        verifier = newVerifier(artifactsDir.getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(projectDir.getAbsolutePath());
        verifier.addCliArgument("verify");
        verifier.execute();
        verifier.verifyErrorFreeLog();
        List<String> lines = verifier.loadLogLines();
        List<String> relocated = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("has been relocated")) {
                relocated.add(line);
            }
        }
        assertEquals(2, relocated.size(), "Expected 2 relocations, but found multiple");
        assertTrue(
                relocated.get(0).contains("Test relocation reason for old-plugin"),
                "Expected the relocation messages to be logged");
        assertTrue(
                relocated.get(1).contains("Test relocation reason for old-dep"),
                "Expected the relocation messages to be logged");
    }
}
