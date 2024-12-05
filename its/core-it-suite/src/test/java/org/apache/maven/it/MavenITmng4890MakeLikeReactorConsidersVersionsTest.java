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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4890">MNG-4890</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4890MakeLikeReactorConsidersVersionsTest extends AbstractMavenIntegrationTestCase {

    public MavenITmng4890MakeLikeReactorConsidersVersionsTest() {
        super("[3.0,)");
    }

    /**
     * Verify that the make-like reactor mode considers actual project versions when calculating the inter-module
     * dependencies and the modules which need to be build. This variant checks calculation of upstream modules.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitAM() throws Exception {
        File testDir = extractResources("/mng-4890");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("mod-a/target");
        verifier.deleteDirectory("mod-b/target");
        verifier.addCliArgument("--projects");
        verifier.addCliArgument("mod-b");
        verifier.addCliArgument("--also-make");
        verifier.setLogFileName("log-am.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("mod-b/target/touch.txt");
        verifier.verifyFileNotPresent("mod-a/target/touch.txt");
        verifier.verifyFileNotPresent("target/touch.txt");
    }

    /**
     * Verify that the make-like reactor mode considers actual project versions when calculating the inter-module
     * dependencies and the modules which need to be build. This variant checks calculation of downstream modules.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitAMD() throws Exception {
        File testDir = extractResources("/mng-4890");

        Verifier verifier = newVerifier(testDir.getAbsolutePath());
        verifier.setAutoclean(false);
        verifier.deleteDirectory("target");
        verifier.deleteDirectory("mod-a/target");
        verifier.deleteDirectory("mod-b/target");
        verifier.addCliArgument("--projects");
        verifier.addCliArgument("mod-a");
        verifier.addCliArgument("--also-make-dependents");
        verifier.setLogFileName("log-amd.txt");
        verifier.addCliArgument("validate");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyFilePresent("mod-a/target/touch.txt");
        verifier.verifyFileNotPresent("mod-b/target/touch.txt");
        verifier.verifyFileNotPresent("target/touch.txt");
    }
}
