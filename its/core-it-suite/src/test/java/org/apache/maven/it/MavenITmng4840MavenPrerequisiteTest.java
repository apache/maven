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

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4840">MNG-4840</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4840MavenPrerequisiteTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4840MavenPrerequisiteTest() {
        super("[2.1.0,3.0-alpha-1),[3.0.2,)");
    }

    /**
     * Verify that builds fail straight when the current Maven version doesn't match a plugin's prerequisite.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMojoExecution() throws Exception {
        File testDir = extractResources("/mng-4840");

        Verifier verifier = newVerifier(new File(testDir, "test-1").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4840");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("../settings-template.xml", "settings.xml");
        try {
            verifier.addCliArgument("validate");
            verifier.execute();
            verifier.verifyErrorFreeLog();
            fail("Build did not fail despite unsatisfied prerequisite of plugin on Maven version.");
        } catch (Exception e) {
            // expected, unsolvable version conflict
        }
    }

    /**
     * Verify that automatic plugin version resolution automatically skips plugin versions whose prerequisite on
     * the current Maven version isn't satisfied.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitPluginVersionResolution() throws Exception {
        File testDir = extractResources("/mng-4840");

        Verifier verifier = newVerifier(new File(testDir, "test-2").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4840");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("../settings-template.xml", "settings.xml");
        verifier.addCliArgument("org.apache.maven.its.mng4840:maven-mng4840-plugin:touch");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/touch-1.txt");
        verifier.verifyFileNotPresent("target/touch-2.txt");
    }
}
