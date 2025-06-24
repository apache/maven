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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4522">MNG-4522</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4522FailUponMissingDependencyParentPomTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4522FailUponMissingDependencyParentPomTest() {
        super("[2.0.3,3.0-alpha-1),[3.0-alpha-7,)");
    }

    /**
     * Test that dependency resolution fails/aborts in case a dependency has a POM that inherits from a missing parent.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit() throws Exception {
        File testDir = extractResources("/mng-4522");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteArtifacts("org.apache.maven.its.mng4522");
        verifier.filterFile("settings-template.xml", "settings.xml");
        verifier.addCliArgument("--settings");
        verifier.addCliArgument("settings.xml");
        try {
            verifier.addCliArgument("validate");
            verifier.execute();
            verifier.verifyErrorFreeLog();
            fail("Missing parent POM of dependency did not fail the build.");
        } catch (VerificationException e) {
            // expected
        }
    }
}
