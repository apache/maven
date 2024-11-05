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
import java.util.List;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

public class MavenITmng7443ConsistencyOfOptionalProjectsAndProfilesTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng7443ConsistencyOfOptionalProjectsAndProfilesTest() {
        super("[4.0.0-alpha-1,)");
    }

    @Test
    public void testConsistentLoggingOfOptionalProfilesAndProjects() throws IOException, VerificationException {
        File testDir = ResourceExtractor.simpleExtractResources(
                getClass(), "/mng-7443-consistency-of-optional-profiles-and-projects");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("-pl");
        verifier.addCliArgument("?:does-not-exist");
        verifier.addCliArgument("-P");
        verifier.addCliArgument("?does-not-exist-either");

        verifier.addCliArguments("clean", "verify");
        verifier.execute();

        final List<String> logLines = verifier.loadFile(verifier.getBasedir(), verifier.getLogFileName(), false);

        int projectSelectorMissingCounter = 0;
        int profileSelectorMissingCounter = 0;

        for (String logLine : logLines) {
            if (logLine.contains("The requested optional projects")
                    && logLine.contains(":does-not-exist")
                    && logLine.contains("do not exist")) {
                projectSelectorMissingCounter++;
            }
            if (logLine.contains("The requested optional profiles")
                    && logLine.contains("does-not-exist-either")
                    && logLine.contains("do not exist")) {
                profileSelectorMissingCounter++;
            }
        }

        assertEquals(2, profileSelectorMissingCounter);
        assertEquals(2, projectSelectorMissingCounter);
    }
}
