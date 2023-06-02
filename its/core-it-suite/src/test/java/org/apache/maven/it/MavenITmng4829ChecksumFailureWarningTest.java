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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4829">MNG-4829</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4829ChecksumFailureWarningTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4829ChecksumFailureWarningTest() {
        super("[2.0.3,3.0-alpha-1)[3.0,)");
    }

    /**
     * Verify that artifacts with mismatching checksums cause a warning on the console.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-4829");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4829");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("settings-template.xml", "settings.xml", "UTF-8");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        List<String> lines = verifier.loadFile(new File(testDir, verifier.getLogFileName()), false);

        boolean foundWarningJar = false, foundWarningPom = false;

        for (String line : lines) {
            if (line.matches("(?i)\\[WARNING\\].*Checksum.*failed.*fa23720355eead3906fdf4ffd2a1a5f5.*")) {
                foundWarningPom = true;
            } else if (line.matches(
                    "(?i)\\[WARNING\\].*Checksum.*failed.*d912aa49cba88e7e9c578e042953f7ce307daac5.*")) {
                foundWarningJar = true;
            }
        }

        assertTrue("Checksum warning for corrupt.pom has not been logged.", foundWarningPom);
        assertTrue("Checksum warning for corrupt.jar has not been logged.", foundWarningJar);
    }
}
