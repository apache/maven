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
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3748">MNG-3748</a>.
 *
 * Verifies that the settings.xml file is parsed using strict mode, such that invalid
 * xml will cause an error (specifically, when repositories are not contained within a profile declaration)
 *
 * @author jdcasey
 *
 * NOTE (cstamas): this IT was written to test that settings.xml is STRICT, while later changes modified
 * this very IT into the opposite: to test that parsing is LENIENT.
 */
@Disabled("This is archaic test; we should strive to make settings.xml parsing strict again")
public class MavenITmng3748BadSettingsXmlTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3748BadSettingsXmlTest() {
        super("(2.0.8,)"); // only test in 2.0.9+
    }

    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-3748");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");

        // Maven 3.x will only print warnings (see MNG-4390)
        if (matchesVersionRange("(,3.0-alpha-3)")) {
            try {
                verifier.addCliArgument("validate");
                verifier.execute();
                verifier.verifyErrorFreeLog();

                fail("build should fail if settings.xml contains unrecognized elements.");
            } catch (VerificationException e) {
                // expected
            }
        } else {
            verifier.addCliArgument("validate");
            verifier.execute();
            verifier.verifyErrorFreeLog();

            List<String> lines = verifier.loadLogLines();
            boolean foundWarning = false;
            boolean isWarning = false;
            for (String line : lines) {
                if (!isWarning) {
                    isWarning = line.startsWith("[WARNING]");
                } else {
                    if (line.matches("(?i).*unrecognised tag.+unknown.+2.*")) {
                        foundWarning = true;
                        break;
                    }
                }
            }
            assertTrue(foundWarning);
        }
    }
}
