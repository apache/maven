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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3948">MNG-3948</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3948ParentResolutionFromProfileReposTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng3948ParentResolutionFromProfileReposTest() {
        super("(2.0.10,2.1.0-M1),(2.1.0-M1,)");
    }

    /**
     * Test that parent POMs can be resolved from remote repositories defined by (active) profiles in the POM.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitFromPom() throws Exception {
        requiresMavenVersion("[2.0,3.0-alpha-1),[3.0-beta-1,)");

        File testDir = extractResources("/mng-3948/test-2");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteArtifacts("org.apache.maven.its.mng3948");
        verifier.filterFile("pom.xml", "pom.xml");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyArtifactPresent("org.apache.maven.its.mng3948", "parent", "0.2", "pom");
    }
}
