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

import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-7566">MNG-7566</a>.
 * Similar to {@link MavenITmng4840MavenPrerequisiteTest}.
 *
 */
class MavenITmng7566JavaPrerequisiteTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng7566JavaPrerequisiteTest() {
        super("[4.0.0-alpha-3,)");
    }

    /**
     * Verify that builds fail straight when the current Java version doesn't match a plugin's prerequisite.
     *
     * @throws Exception in case of failure
     */
    @Test
    void testitMojoExecution() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7566");

        Verifier verifier = newVerifier(new File(testDir, "test-1").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng7566");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("../settings-template.xml", "settings.xml", "UTF-8");
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
     * the current Java version isn't satisfied.
     *
     * @throws Exception in case of failure
     */
    @Test
    void testitPluginVersionResolution() throws Exception {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/mng-7566");

        Verifier verifier = newVerifier(new File(testDir, "test-2").getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng7566");
        verifier.addCliArgument("-s");
        verifier.addCliArgument("settings.xml");
        verifier.filterFile("../settings-template.xml", "settings.xml", "UTF-8");
        verifier.addCliArgument("org.apache.maven.its.mng7566:maven-mng7566-plugin:touch");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("target/touch-1.txt");
        verifier.verifyFileNotPresent("target/touch-2.txt");
    }
}
