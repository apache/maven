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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-8385">MNG-8385</a>.
 */
class MavenITmng8385PropertyContributoSPITest extends AbstractMavenIntegrationTestCase {

    MavenITmng8385PropertyContributoSPITest() {
        super("[4.0.0-beta-6,)");
    }

    /**
     *  Verify that PropertyContributorSPI is used and does it's job
     */
    @Test
    void testIt() throws Exception {
        File testDir = extractResources("/mng-8385");
        Verifier verifier;

        verifier = newVerifier(new File(testDir, "spi-extension").getAbsolutePath());
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier = newVerifier(new File(testDir, "spi-consumer").getAbsolutePath());
        verifier.addCliArgument("validate");
        verifier.addCliArgument("-X");
        verifier.setForkJvm(true); // TODO: remove when we updated MavenCLI+Invoker
        verifier.execute();
        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("washere!");
    }
}
