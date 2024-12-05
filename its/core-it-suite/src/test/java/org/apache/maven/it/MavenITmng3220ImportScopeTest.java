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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3220">MNG-3220</a>.
 *
 *
 */
public class MavenITmng3220ImportScopeTest extends AbstractMavenIntegrationTestCase {
    public MavenITmng3220ImportScopeTest() {
        super("(2.0.8,3.0-alpha-1),[3.0-alpha-3,)");
    }

    @Test
    public void testitMNG3220a() throws Exception {
        File testDir = extractResources("/mng-3220");

        testDir = new File(testDir, "imported-pom-depMgmt");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng3220");
        verifier.filterFile("../settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    public void testitMNG3220b() throws Exception {
        File testDir = extractResources("/mng-3220");

        testDir = new File(testDir, "depMgmt-pom-module-notImported");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng3220");
        verifier.filterFile("../settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");

        try {
            verifier.addCliArgument("validate");
            verifier.execute();
            verifier.verifyErrorFreeLog();
            fail("Should fail to build with missing junit version.");
        } catch (VerificationException e) {
            // expected
        }

        List<String> lines = verifier.loadFile(new File(testDir, "log.txt"), false);

        boolean found = false;
        for (String line : lines) {
            if (line.contains("\'dependencies.dependency.version\' is missing for junit:junit")
                    || line.contains("\'dependencies.dependency.version\' for junit:junit:jar is missing")) {
                found = true;
                break;
            }
        }

        assertTrue(found, "Should have found validation error line in output.");
    }
}
