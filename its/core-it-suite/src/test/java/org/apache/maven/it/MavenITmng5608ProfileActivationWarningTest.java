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
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-5608">MNG-5608</a>:
 * Profile activation warning test when file specification contains <code>${project.basedir}</code>
 * instead of <code>${basedir}</code>
 */
public class MavenITmng5608ProfileActivationWarningTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng5608ProfileActivationWarningTest() {
        // changed in https://issues.apache.org/jira/browse/MNG-7895
        // TODO - consider a separate test
        super("(3.2.1,3.9.4]");
    }

    @Test
    public void testitMNG5608() throws Exception {
        File testDir = extractResources("/mng-5608-profile-activation-warning");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        // check expected profiles activated, just for sanity (or build should have failed, given other profiles)
        assertFileExists(testDir, "target/exists-basedir");
        assertFileExists(testDir, "target/mng-5608-missing-project.basedir");

        // check that the 2 profiles using ${project.basedir} caused warnings
        List<String> logFile = verifier.loadLogLines();
        assertNotNull(findWarning(logFile, "mng-5608-exists-project.basedir"));
        assertNotNull(findWarning(logFile, "mng-5608-missing-project.basedir"));
    }

    private void assertFileExists(File dir, String filename) {
        File file = new File(dir, filename);
        assertTrue(file.exists(), "expected file: " + file);
    }

    private String findWarning(List<String> logLines, String profileId) {
        Pattern pattern =
                Pattern.compile("(?i).*Failed to interpolate file location ..project.basedir./pom.xml for profile "
                        + profileId + ": .*");

        for (String logLine : logLines) {
            if (pattern.matcher(logLine).matches()) {
                return logLine;
            }
        }

        return null;
    }
}
