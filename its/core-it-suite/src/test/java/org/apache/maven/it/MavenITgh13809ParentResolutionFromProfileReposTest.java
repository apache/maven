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

import static org.junit.jupiter.api.Assertions.assertThrows;

class MavenITgh13809ParentResolutionFromProfileReposTest extends AbstractMavenIntegrationTestCase {

    MavenITgh13809ParentResolutionFromProfileReposTest() {
        super("[4.0.0,)");
    }

    @Test
    void testExplicitProfileInReactor() throws Exception {
        Verifier verifier = verifier("explicit");
        verifier.addCliArgument("-Pparent-repository");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    void testPropertyActivatedProfileInReactor() throws Exception {
        Verifier verifier = verifier("property");
        verifier.addCliArgument("-Dparent.repository=true");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    void testNonRecursiveBuild() throws Exception {
        Verifier verifier = verifier("nonrecursive");
        verifier.addCliArgument("-Pparent-repository");
        verifier.addCliArgument("-N");
        verifier.execute();
        verifier.verifyErrorFreeLog();
    }

    @Test
    void testInactiveProfileDoesNotProvideRepository() throws Exception {
        Verifier verifier = verifier("inactive");
        assertThrows(VerificationException.class, verifier::execute);
        verifier.verifyTextInLog("Non-resolvable parent POM");
    }

    private Verifier verifier(String name) throws Exception {
        File testDir = extractResources("/gh-13809-parent-profile-repository");
        Verifier verifier = newVerifier(testDir.getPath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("local-repo-" + name);
        verifier.setLogFileName("log-" + name + ".txt");
        verifier.filterFile("pom-template.xml", "pom.xml");
        verifier.addCliArgument("-Dmaven.repo.local=" + new File(testDir, "local-repo-" + name).getPath());
        verifier.addCliArgument("validate");
        return verifier;
    }
}
