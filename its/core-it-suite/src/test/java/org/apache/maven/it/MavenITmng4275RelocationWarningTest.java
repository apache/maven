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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4275">MNG-4275</a>.
 *
 * @author John Casey
 */
public class MavenITmng4275RelocationWarningTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4275RelocationWarningTest() {
        super("[2.0,2.0.9),[2.2.1,3.0-alpha-1),[3.0-alpha-3,)");
    }

    /**
     * Verify that relocations are logged (at warning level).
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4275");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4275");
        verifier.filterFile("settings-template.xml", "settings.xml", "UTF-8");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> lines = verifier.loadFile(new File(testDir, verifier.getLogFileName()), false);
        boolean foundWarning = false;
        for (String line : lines) {
            if (foundWarning) {
                assertTrue(
                        "Relocation target should have been logged right after warning.",
                        line.contains("This artifact has been relocated to org.apache.maven.its.mng4275:relocated:1"));
                break;
            } else if (line.startsWith("[WARNING] While downloading org.apache.maven.its.mng4275:relocation:1")) {
                foundWarning = true;
            } else if (line.matches(
                    "\\[WARNING\\].* org.apache.maven.its.mng4275:relocation:jar:1 .* org.apache.maven.its"
                            + ".mng4275:relocated:jar:1.*")) {
                foundWarning = true;
                break;
            }
        }

        assertTrue("Relocation warning should haven been logged.", foundWarning);
    }
}
